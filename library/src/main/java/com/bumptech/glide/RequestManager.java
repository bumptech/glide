package com.bumptech.glide;

import android.content.Context;
import com.bumptech.glide.manager.ConnectivityMonitor;
import com.bumptech.glide.manager.ConnectivityMonitorFactory;
import com.bumptech.glide.manager.RequestTracker;

public class RequestManager {

    private final ConnectivityMonitor connectivityMonitor;
    private final RequestTracker requestTracker;

    public RequestManager(Context context) {
        this(context, new RequestTracker(), new ConnectivityMonitorFactory());
    }

    RequestManager(Context context, RequestTracker requestTracker, ConnectivityMonitorFactory factory) {
        this.requestTracker = requestTracker;
        this.connectivityMonitor = factory.build(context, new RequestManagerConnectivityListener());
        connectivityMonitor.register();
    }

    public RequestTracker getRequestTracker() {
        return requestTracker;
    }

    public void onStart() {
        // onStart might not be called because this object may be created after the fragment/activity's onStart method.
        connectivityMonitor.register();

        requestTracker.resumeRequests();
    }

    public void onStop() {
        connectivityMonitor.unregister();
        requestTracker.pauseRequests();
    }

    public void onDestroy() {
        requestTracker.clearRequests();
    }

    private class RequestManagerConnectivityListener implements ConnectivityMonitor.ConnectivityListener {

        @Override
        public void onConnectivityChanged(boolean isConnected) {
            if (isConnected) {
                requestTracker.restartRequests();
            }
        }
    }
}
