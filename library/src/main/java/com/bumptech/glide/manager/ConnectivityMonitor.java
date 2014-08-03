package com.bumptech.glide.manager;

/**
 * An interface for monitoring network connectivity events.
 */
public interface ConnectivityMonitor extends LifecycleListener {

    /**
     * An interface for listening to network connectivity events picked up by the monitor.
     */
    public interface ConnectivityListener {
        /**
         * Called when the connectivity state changes.
         *
         * @param isConnected True if we're currently connected to a network, false otherwise.
         */
        public void onConnectivityChanged(boolean isConnected);
    }
}
