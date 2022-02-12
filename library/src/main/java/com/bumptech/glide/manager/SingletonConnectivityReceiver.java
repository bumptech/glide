package com.bumptech.glide.manager;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.util.Log;
import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;
import com.bumptech.glide.manager.ConnectivityMonitor.ConnectivityListener;
import com.bumptech.glide.util.GlideSuppliers;
import com.bumptech.glide.util.GlideSuppliers.GlideSupplier;
import com.bumptech.glide.util.Synthetic;
import com.bumptech.glide.util.Util;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

/** Uses {@link android.net.ConnectivityManager} to identify connectivity changes. */
final class SingletonConnectivityReceiver {
  private static volatile SingletonConnectivityReceiver instance;
  private static final String TAG = "ConnectivityMonitor";

  private final FrameworkConnectivityMonitor frameworkConnectivityMonitor;

  @GuardedBy("this")
  @Synthetic
  final Set<ConnectivityListener> listeners = new HashSet<ConnectivityListener>();

  @GuardedBy("this")
  private boolean isRegistered;

  static SingletonConnectivityReceiver get(@NonNull Context context) {
    if (instance == null) {
      synchronized (SingletonConnectivityReceiver.class) {
        if (instance == null) {
          instance = new SingletonConnectivityReceiver(context.getApplicationContext());
        }
      }
    }
    return instance;
  }

  @VisibleForTesting
  static void reset() {
    instance = null;
  }

  private SingletonConnectivityReceiver(final @NonNull Context context) {
    GlideSupplier<ConnectivityManager> connectivityManager =
        GlideSuppliers.memorize(
            new GlideSupplier<ConnectivityManager>() {
              @Override
              public ConnectivityManager get() {
                return (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
              }
            });
    ConnectivityListener connectivityListener =
        new ConnectivityListener() {
          @Override
          public void onConnectivityChanged(boolean isConnected) {
            Util.assertMainThread();
            List<ConnectivityListener> toNotify;
            synchronized (SingletonConnectivityReceiver.this) {
              toNotify = new ArrayList<>(listeners);
            }
            for (ConnectivityListener listener : toNotify) {
              listener.onConnectivityChanged(isConnected);
            }
          }
        };

    frameworkConnectivityMonitor =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
            ? new FrameworkConnectivityMonitorPostApi24(connectivityManager, connectivityListener)
            : new FrameworkConnectivityMonitorPreApi24(
                context, connectivityManager, connectivityListener);
  }

  synchronized void register(ConnectivityListener listener) {
    listeners.add(listener);
    maybeRegisterReceiver();
  }

  /**
   * To avoid holding a lock while notifying listeners, the unregistered listener may still be
   * notified about a connectivity change after this method completes if this method is called on a
   * thread other than the main thread and if a connectivity broadcast is racing with this method.
   * Callers must handle this case.
   */
  synchronized void unregister(ConnectivityListener listener) {
    listeners.remove(listener);
    maybeUnregisterReceiver();
  }

  @GuardedBy("this")
  private void maybeRegisterReceiver() {
    if (isRegistered || listeners.isEmpty()) {
      return;
    }
    isRegistered = frameworkConnectivityMonitor.register();
  }

  @GuardedBy("this")
  private void maybeUnregisterReceiver() {
    if (!isRegistered || !listeners.isEmpty()) {
      return;
    }

    frameworkConnectivityMonitor.unregister();
    isRegistered = false;
  }

  private interface FrameworkConnectivityMonitor {
    boolean register();

    void unregister();
  }

  @RequiresApi(VERSION_CODES.N)
  private static final class FrameworkConnectivityMonitorPostApi24
      implements FrameworkConnectivityMonitor {

    @Synthetic boolean isConnected;
    @Synthetic final ConnectivityListener listener;
    private final GlideSupplier<ConnectivityManager> connectivityManager;
    private final NetworkCallback networkCallback =
        new NetworkCallback() {
          @Override
          public void onAvailable(@NonNull Network network) {
            postOnConnectivityChange(true);
          }

          @Override
          public void onLost(@NonNull Network network) {
            postOnConnectivityChange(false);
          }

          private void postOnConnectivityChange(final boolean newState) {
            // We could use registerDefaultNetworkCallback with a Handler, but that's only available
            // on API 26, instead of API 24. We can mimic the same behavior here manually by
            // posting to the UI thread. All calls have to be posted to make sure that we retain the
            // original order. Otherwise a call on a background thread, followed by a call on the UI
            // thread could result in the first call running second.
            Util.postOnUiThread(
                new Runnable() {
                  @Override
                  public void run() {
                    onConnectivityChange(newState);
                  }
                });
          }

          @Synthetic
          void onConnectivityChange(boolean newState) {
            // See b/201425456.
            Util.assertMainThread();

            boolean wasConnected = isConnected;
            isConnected = newState;
            if (wasConnected != newState) {
              listener.onConnectivityChanged(newState);
            }
          }
        };

    FrameworkConnectivityMonitorPostApi24(
        GlideSupplier<ConnectivityManager> connectivityManager, ConnectivityListener listener) {
      this.connectivityManager = connectivityManager;
      this.listener = listener;
    }

    // Permissions are checked in the factory instead.
    @SuppressLint("MissingPermission")
    @Override
    public boolean register() {
      isConnected = connectivityManager.get().getActiveNetwork() != null;
      try {
        connectivityManager.get().registerDefaultNetworkCallback(networkCallback);
        return true;
        // See b/201664814, b/204226444: At least TooManyRequestsException is not public and
        // doesn't extend from any subclass :/.
      } catch (RuntimeException e) {
        if (Log.isLoggable(TAG, Log.WARN)) {
          Log.w(TAG, "Failed to register callback", e);
        }
        return false;
      }
    }

    @Override
    public void unregister() {
      connectivityManager.get().unregisterNetworkCallback(networkCallback);
    }
  }

  /**
   * All interactions with connectivity manager and registering/unregistering broadcast receivers
   * are punted to a background thread. We use serial execution to make sure that they still happen
   * in the correct order. The system calls required to register/unregister the receiver and to
   * determine connectivity status are expensive to run on the main thread. isConnected and
   * isRegistered should only be used on the serial background thread. Listeners should only be
   * notified on the main thread. Because of the delays caused by punting threads, listeners may be
   * notified with the incorrect state. Howeve the strict ordering means that they will shortly
   * after be notified with the correct state.
   */
  private static final class FrameworkConnectivityMonitorPreApi24
      implements FrameworkConnectivityMonitor {
    // Using AsyncTasks's executor is a hack. We need a background thread, but it's not trivial to
    // pass one through to this point. We could make a breaking API change, which upsets external
    // users. Or we could try to add an API to expose one of Glide's executors via the singleton,
    // but that could allow Glide's executors to be misused as general purpose executors. Given that
    // this code is deprecated anyway, using some pre-existing general purpose executor doesn't seem
    // wildly unreasonable.
    @Synthetic static final Executor EXECUTOR = AsyncTask.SERIAL_EXECUTOR;
    @Synthetic final Context context;
    @Synthetic final ConnectivityListener listener;
    private final GlideSupplier<ConnectivityManager> connectivityManager;
    // These are only manipulated serially, but the executor might use separate threads to do so,
    // so we use volatile.
    @Synthetic volatile boolean isConnected;
    @Synthetic volatile boolean isRegistered;

    @Synthetic
    final BroadcastReceiver connectivityReceiver =
        new BroadcastReceiver() {
          @Override
          public void onReceive(@NonNull Context context, Intent intent) {
            onConnectivityChange();
          }
        };

    FrameworkConnectivityMonitorPreApi24(
        Context context,
        GlideSupplier<ConnectivityManager> connectivityManager,
        ConnectivityListener listener) {
      this.context = context.getApplicationContext();
      this.connectivityManager = connectivityManager;
      this.listener = listener;
    }

    @Override
    public boolean register() {
      EXECUTOR.execute(
          new Runnable() {
            @Override
            public void run() {
              // Initialize isConnected so that we notice the first time around when there's a
              // broadcast.
              // TODO(judds): This causes a race where:
              // 1. Connectivity is disconnected
              // 2. Register is called, but punted to a background thread and not run yet
              // 3. Some network requiring request is started, runs and fails due to connectivity
              // 4. Connectivity is re-established
              // 5. This code finally runs on the background thread.
              // In step 5, we'll think that we're currently connected and won't trigger a retry for
              // any previously failed requests.
              // In the long run it might be nice to define some explicit initialization step for
              // Glide where we do this and other expensive things on a background thread prior to
              // starting the first request. For now it seems better to just accept this race than
              // either take the latency hit of the IPC on the main thread, or try something like
              // always notifying all listeners once right after this logic runs just in case
              // something failed.
              isConnected = isConnected();
              try {
                // See #1405
                context.registerReceiver(
                    connectivityReceiver,
                    new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
                isRegistered = true;
              } catch (SecurityException e) {
                // See #1417, registering the receiver can throw SecurityException.
                if (Log.isLoggable(TAG, Log.WARN)) {
                  Log.w(TAG, "Failed to register", e);
                }
                isRegistered = false;
              }
            }
          });

      // We track our registration status internally, so we always need to be called to unregister.
      return true;
    }

    @Override
    public void unregister() {
      // Always post to the executor to make sure everything runs in the correct order. If we short
      // circuit that by checking isConnected on this thread, we might leak a receiver.
      EXECUTOR.execute(
          new Runnable() {
            @Override
            public void run() {
              if (!isRegistered) {
                return;
              }
              isRegistered = false;
              context.unregisterReceiver(connectivityReceiver);
            }
          });
    }

    @Synthetic
    void onConnectivityChange() {
      EXECUTOR.execute(
          new Runnable() {
            @Override
            public void run() {
              boolean wasConnected = isConnected;
              isConnected = isConnected();
              if (wasConnected != isConnected) {
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                  Log.d(TAG, "connectivity changed, isConnected: " + isConnected);
                }

                notifyChangeOnUiThread(isConnected);
              }
            }
          });
    }

    // Pass through the boolean because the instance variable could change while we're waiting for
    // the runnable to be executed on the main thread.
    @Synthetic
    void notifyChangeOnUiThread(final boolean isConnected) {
      Util.postOnUiThread(
          new Runnable() {
            @Override
            public void run() {
              listener.onConnectivityChanged(isConnected);
            }
          });
    }

    @SuppressWarnings("WeakerAccess")
    @Synthetic
    // Permissions are checked in the factory instead.
    @SuppressLint("MissingPermission")
    boolean isConnected() {
      NetworkInfo networkInfo;
      try {
        networkInfo = connectivityManager.get().getActiveNetworkInfo();
      } catch (RuntimeException e) {
        // #1405 shows that this throws a SecurityException.
        // b/70869360 shows that this throws NullPointerException on APIs 22, 23, and 24.
        // b/70869360 also shows that this throws RuntimeException on API 24 and 25.
        if (Log.isLoggable(TAG, Log.WARN)) {
          Log.w(TAG, "Failed to determine connectivity status when connectivity changed", e);
        }
        // Default to true;
        return true;
      }
      return networkInfo != null && networkInfo.isConnected();
    }
  }
}
