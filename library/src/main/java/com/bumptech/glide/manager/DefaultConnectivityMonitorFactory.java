package com.bumptech.glide.manager;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

/**
 * A factory class that produces a functional {@link com.bumptech.glide.manager.ConnectivityMonitor}
 * if the application has the {@code android.permission.ACCESS_NETWORK_STATE} permission and a no-op
 * non functional {@link com.bumptech.glide.manager.ConnectivityMonitor} if the app does not have
 * the required permission.
 */
public class DefaultConnectivityMonitorFactory implements ConnectivityMonitorFactory {
  private static final String TAG = "ConnectivityMonitor";
  private static final String NETWORK_PERMISSION = "android.permission.ACCESS_NETWORK_STATE";

  @NonNull
  @Override
  public ConnectivityMonitor build(
      @NonNull Context context, @NonNull ConnectivityMonitor.ConnectivityListener listener) {
    int permissionResult = ContextCompat.checkSelfPermission(context, NETWORK_PERMISSION);
    boolean hasPermission = permissionResult == PackageManager.PERMISSION_GRANTED;
    if (Log.isLoggable(TAG, Log.DEBUG)) {
      Log.d(
          TAG,
          hasPermission
              ? "ACCESS_NETWORK_STATE permission granted, registering connectivity monitor"
              : "ACCESS_NETWORK_STATE permission missing, cannot register connectivity monitor");
    }
    return hasPermission
        ? new DefaultConnectivityMonitor(context, listener)
        : new NullConnectivityMonitor();
  }
}
