package com.bumptech.glide.manager;

import com.bumptech.glide.request.Request;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public class RequestTracker {
    // Most requests will be for views and will therefore be held strongly (and safely) by the view via the tag.
    // However, a user can always pass in a different type of target which may end up not being strongly referenced even
    // though the user still would like the request to finish. Weak references are therefore only really functional in
    // this context for view targets. Despite the side affects, WeakReferences are still essentially required. A user
    // can always make repeated requests into targets other than views, or use an activity manager in a fragment pager
    // where holding strong references would steadily leak bitmaps and/or views.
    private final Set<Request> requests = Collections.newSetFromMap(new WeakHashMap<Request, Boolean>());

    public void addRequest(Request request) {
        requests.add(request);
    }

    public void removeRequest(Request request) {
        requests.remove(request);
    }

    /**
     * Stops any in progress requests.
     */
    public void pauseRequests() {
        for (Request request : requests) {
            if (!request.isComplete() && !request.isFailed()) {
                request.clear();
            }
        }
    }

    /**
     * Starts any not yet completed or failed requests.
     */
    public void resumeRequests() {
        for (Request request : requests) {
            if (!request.isComplete() && !request.isRunning()) {
                request.run();
            }
        }
    }

    /**
     * Cancels all requests and clears their resources.
     */
    public void clearRequests() {
        for (Request request : requests) {
            request.clear();
        }
    }

    /**
     * Restarts failed requests and cancels and restarts in progress requests.
     */
    public void restartRequests() {
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
