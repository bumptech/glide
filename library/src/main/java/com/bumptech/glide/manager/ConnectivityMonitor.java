package com.bumptech.glide.manager;

public interface ConnectivityMonitor {

    public interface ConnectivityListener {
        public void onConnectivityChanged(boolean isConnected);
    }

    public void register();

    public void unregister();
}
