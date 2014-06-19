package com.bumptech.glide.manager;

import android.content.Context;
import com.bumptech.glide.request.Request;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

class LifecycleRequestManager implements RequestManager, ConnectivityMonitor.ConnectivityListener {
    // Most requests will be for views and will therefore be held strongly (and safely) by the view via the tag.
    // However, a user can always pass in a different type of target which may end up not being strongly referenced even
    // though the user still would like the request to finish. Weak references are therefore only really functional in
    // this context for view targets. Despite the side affects, WeakReferences are still essentially required. A user
    // can always make repeated requests into targets other than views, or use an activity manager in a fragment pager
    // where holding strong references would steadily leak bitmaps and/or views.
    private final Set<Request> requests = Collections.newSetFromMap(new WeakHashMap<Request, Boolean>());
    private final ConnectivityMonitor connectivityMonitor;

    LifecycleRequestManager(Context context) {
        this(context, new ConnectivityMonitorFactory());
    }

    LifecycleRequestManager(Context context, ConnectivityMonitorFactory factory) {
        this.connectivityMonitor = factory.build(context, this);
        connectivityMonitor.register();
    }

    @Override
    public void addRequest(Request request) {
        requests.add(request);
    }

    @Override
    public void removeRequest(Request request) {
        requests.remove(request);
    }

    public void onStart() {
        // onStart might not be called because this object may be created after the fragment/activity's onStart method.
        connectivityMonitor.register();

        for (Request request : requests) {
            if (!request.isComplete() && !request.isRunning()) {
                request.run();
            }
        }

    }

    public void onStop() {
        connectivityMonitor.unregister();
        for (Request request : requests) {
            if (!request.isComplete() && !request.isFailed()) {
                request.clear();
            }
        }
    }

    public void onDestroy() {
        for (Request request : requests) {
            request.clear();
        }
    }

    @Override
    public void onConnectivityChanged(boolean isConnected) {
        for (Request request : requests) {
            if (request.isFailed()) {
                request.run();
            } else if (!request.isComplete()) {
                request.clear();
                request.run();
            }
        }
    }
}
