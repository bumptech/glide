package com.bumptech.glide.manager;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import com.bumptech.glide.manager.ConnectivityMonitor.ConnectivityListener;
import com.bumptech.glide.util.Preconditions;
import com.bumptech.glide.util.Synthetic;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Uses {@link android.net.ConnectivityManager} to identify connectivity changes. */
final class SingletonConnectivityReceiver {
  private static volatile SingletonConnectivityReceiver instance;
  @Synthetic static final String TAG = "ConnectivityMonitor";
  // Only accessed on the main thread.
  @Synthetic boolean isConnected;

  private final BroadcastReceiver connectivityReceiver =
      new BroadcastReceiver() {
        @Override
        public void onReceive(@NonNull Context context, Intent intent) {
          List<ConnectivityListener> listenersToNotify = null;
          boolean wasConnected = isConnected;
          isConnected = isConnected(context);
          if (wasConnected != isConnected) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
              Log.d(TAG, "connectivity changed, isConnected: " + isConnected);
            }

            synchronized (SingletonConnectivityReceiver.this) {
              listenersToNotify = new ArrayList<>(listeners);
            }
          }
          // Make sure that we do not hold our lock while calling our listener. Otherwise we could
          // deadlock where our listener acquires its lock, then tries to acquire ours elsewhere and
          // then here we acquire our lock and try to acquire theirs.
          // The consequence of this is that we may notify a listener after it has been
          // unregistered in a few specific (unlikely) scenarios. That appears to be safe and is
          // documented in the unregister method.
          if (listenersToNotify != null) {
            for (ConnectivityListener listener : listenersToNotify) {
              listener.onConnectivityChanged(isConnected);
            }
          }
        }
      };

  private final Context context;

  @GuardedBy("this")
  @Synthetic
  final Set<ConnectivityListener> listeners = new HashSet<ConnectivityListener>();

  @GuardedBy("this")
  private boolean isRegistered;

  static SingletonConnectivityReceiver get(@NonNull Context context) {
    if (instance == null) {
      synchronized (SingletonConnectivityReceiver.class) {
        if (instance == null) {
          instance = new SingletonConnectivityReceiver(context);
        }
      }
    }
    return instance;
  }

  @VisibleForTesting
  static void reset() {
    instance = null;
  }

  private SingletonConnectivityReceiver(@NonNull Context context) {
    this.context = context.getApplicationContext();
  }

  synchronized void register(ConnectivityListener listener) {
    listeners.add(listener);
    maybeRegisterReceiver();
  }

  /**
   * To avoid holding a lock while notifying listeners, the unregisterd listener may still be
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
    isConnected = isConnected(context);
    try {
      // See #1405
      context.registerReceiver(
          connectivityReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
      isRegistered = true;
    } catch (SecurityException e) {
      // See #1417, registering the receiver can throw SecurityException.
      if (Log.isLoggable(TAG, Log.WARN)) {
        Log.w(TAG, "Failed to register", e);
      }
    }
  }

  @GuardedBy("this")
  private void maybeUnregisterReceiver() {
    if (!isRegistered || !listeners.isEmpty()) {
      return;
    }

    context.unregisterReceiver(connectivityReceiver);
    isRegistered = false;
  }

  @SuppressWarnings("WeakerAccess")
  @Synthetic
  // Permissions are checked in the factory instead.
  @SuppressLint("MissingPermission")
  boolean isConnected(@NonNull Context context) {
    ConnectivityManager connectivityManager =
        Preconditions.checkNotNull(
            (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE));
    NetworkInfo networkInfo;
    try {
      networkInfo = connectivityManager.getActiveNetworkInfo();
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
