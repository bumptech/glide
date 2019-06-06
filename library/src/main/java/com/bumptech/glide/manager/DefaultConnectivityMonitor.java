package com.bumptech.glide.manager;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import androidx.annotation.NonNull;
import com.bumptech.glide.util.Preconditions;
import com.bumptech.glide.util.Synthetic;

/** Uses {@link android.net.ConnectivityManager} to identify connectivity changes. */
final class DefaultConnectivityMonitor implements ConnectivityMonitor {
  private static final String TAG = "ConnectivityMonitor";
  private final Context context;

  @SuppressWarnings("WeakerAccess")
  @Synthetic
  final ConnectivityListener listener;

  @SuppressWarnings("WeakerAccess")
  @Synthetic
  boolean isConnected;

  private boolean isRegistered;

  private final BroadcastReceiver connectivityReceiver =
      new BroadcastReceiver() {
        @Override
        public void onReceive(@NonNull Context context, Intent intent) {
          boolean wasConnected = isConnected;
          isConnected = isConnected(context);
          if (wasConnected != isConnected) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
              Log.d(TAG, "connectivity changed, isConnected: " + isConnected);
            }

            listener.onConnectivityChanged(isConnected);
          }
        }
      };

  DefaultConnectivityMonitor(@NonNull Context context, @NonNull ConnectivityListener listener) {
    this.context = context.getApplicationContext();
    this.listener = listener;
  }

  private void register() {
    if (isRegistered) {
      return;
    }

    // Initialize isConnected.
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

  private void unregister() {
    if (!isRegistered) {
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

  @Override
  public void onStart() {
    register();
  }

  @Override
  public void onStop() {
    unregister();
  }

  @Override
  public void onDestroy() {
    // Do nothing.
  }
}
