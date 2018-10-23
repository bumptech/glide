package com.bumptech.glide.manager;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.util.Log;
import com.bumptech.glide.util.Preconditions;
import com.bumptech.glide.util.Synthetic;

/**
 * Uses {@link android.net.ConnectivityManager} to identify connectivity changes.
 */
final class DefaultConnectivityMonitor implements ConnectivityMonitor {
  private static final String TAG = "ConnectivityMonitor";
  private final Context context;
  @SuppressWarnings("WeakerAccess") @Synthetic
  static  ConnectivityListener slistener = null;

  @SuppressWarnings("WeakerAccess") @Synthetic
  static  boolean isConnected;


  static final BroadcastReceiver connectivityReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(@NonNull Context context, Intent intent) {
      boolean wasConnected = isConnected;
      isConnected = isConnected(context);
      if (wasConnected != isConnected) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
          Log.d(TAG, "connectivity changed, isConnected: " + isConnected);
        }

        slistener.onConnectivityChanged(isConnected);
      }
    }
  };

  DefaultConnectivityMonitor(@NonNull Context context, @NonNull ConnectivityListener listener) {
    this.context = context.getApplicationContext();
    slistener = listener;
  }

  private void register() {

    context.unregisterReceiver(connectivityReceiver);

    // Initialize isConnected.
    isConnected = isConnected(context);
    try {
      // See #1405
      context.registerReceiver(connectivityReceiver,
          new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

    } catch (SecurityException e) {
      // See #1417, registering the receiver can throw SecurityException.
      if (Log.isLoggable(TAG, Log.WARN)) {
        Log.w(TAG, "Failed to register", e);
      }
    }
  }

  private void unregister() {
    context.unregisterReceiver(connectivityReceiver);
  }

  @SuppressWarnings("WeakerAccess")
  @Synthetic
  // Permissions are checked in the factory instead.
  @SuppressLint("MissingPermission")
 static boolean isConnected(@NonNull Context context) {
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
