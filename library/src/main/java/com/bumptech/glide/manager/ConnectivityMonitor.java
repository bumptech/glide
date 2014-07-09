package com.bumptech.glide.manager;

/**
 * An interface for monitoring network connectivity events.
 */
public interface ConnectivityMonitor {

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

    /**
     * Indicates the monitor should register itself to listen to connectivity events.
     */
    public void register();

    /**
     * Indicates the monitor should unregister itself and stop listening to connectivity events.
     */
    public void unregister();
}
