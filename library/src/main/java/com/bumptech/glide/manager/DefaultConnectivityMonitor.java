package com.bumptech.glide.manager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

class DefaultConnectivityMonitor implements ConnectivityMonitor {
    private final Context context;
    private final ConnectivityListener listener;

    private boolean isConnected;
    private boolean isRegistered;

    private final BroadcastReceiver connectivityReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean wasConnected = isConnected;
            isConnected = isConnected(context);
            if (wasConnected != isConnected) {
                listener.onConnectivityChanged(isConnected);
            }
        }
    };

    public DefaultConnectivityMonitor(Context context, ConnectivityListener listener) {
        this.context = context.getApplicationContext();
        this.listener = listener;
    }

    private void register() {
        if (isRegistered) {
            return;
        }

        isConnected = isConnected(context);
        context.registerReceiver(connectivityReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        isRegistered = true;
    }

    private void unregister() {
        if (!isRegistered) {
            return;
        }

        context.unregisterReceiver(connectivityReceiver);
        isRegistered = false;
    }

    private boolean isConnected(Context context) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
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
