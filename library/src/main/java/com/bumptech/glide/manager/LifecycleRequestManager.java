package com.bumptech.glide.manager;

import android.content.Context;
import com.bumptech.glide.request.Request;

class LifecycleRequestManager implements RequestManager {

    private final ConnectivityMonitor connectivityMonitor;
    private final RequestTracker requestTracker;

    LifecycleRequestManager(Context context) {
        this(context, new RequestTracker(), new ConnectivityMonitorFactory());
    }

    LifecycleRequestManager(Context context, RequestTracker requestTracker, ConnectivityMonitorFactory factory) {
        this.requestTracker = requestTracker;
        this.connectivityMonitor = factory.build(context, new RequestManagerConnectivityListener());
        connectivityMonitor.register();
    }

    @Override
    public void addRequest(Request request) {
        requestTracker.addRequest(request);
    }

    @Override
    public void removeRequest(Request request) {
        requestTracker.removeRequest(request);
    }

    void onStart() {
        // onStart might not be called because this object may be created after the fragment/activity's onStart method.
        connectivityMonitor.register();

        requestTracker.resumeRequests();
    }

    void onStop() {
        connectivityMonitor.unregister();
        requestTracker.pauseRequests();
    }

    void onDestroy() {
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
