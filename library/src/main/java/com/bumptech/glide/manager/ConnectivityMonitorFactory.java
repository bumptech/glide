package com.bumptech.glide.manager;

import android.content.Context;
import android.content.pm.PackageManager;

/**
 * A factory class that produces a functional {@link com.bumptech.glide.manager.ConnectivityMonitor} if the application
 * has the {@code android.permission.ACCESS_NETWORK_STATE} permission and a no-op non functional
 * {@link com.bumptech.glide.manager.ConnectivityMonitor} if the app does not have the required permission.
 */
public class ConnectivityMonitorFactory {
    public ConnectivityMonitor build(Context context, ConnectivityMonitor.ConnectivityListener listener) {
        final int res = context.checkCallingOrSelfPermission("android.permission.ACCESS_NETWORK_STATE");
        final boolean hasPermission = res == PackageManager.PERMISSION_GRANTED;
        if (hasPermission) {
            return new DefaultConnectivityMonitor(context, listener);
        } else {
            return new NullConnectivityMonitor();
        }
    }
}
