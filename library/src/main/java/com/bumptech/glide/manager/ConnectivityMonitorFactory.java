package com.bumptech.glide.manager;

import android.content.Context;
import android.content.pm.PackageManager;

public class ConnectivityMonitorFactory {
    public ConnectivityMonitor build(Context context, ConnectivityMonitor.ConnectivityListener listener) {
        int res = context.checkCallingOrSelfPermission("android.permission.ACCESS_NETWORK_STATE");
        boolean hasPermission = res == PackageManager.PERMISSION_GRANTED;
        if (hasPermission) {
            return new DefaultConnectivityMonitor(context, listener);
        } else {
            return new NullConnectivityMonitor();
        }
    }
}
