package com.bumptech.glide.manager;

interface ConnectivityMonitor {

    public interface ConnectivityListener {
        public void onConnectivityChanged(boolean isConnected);
    }

    public void register();

    public void unregister();
}
