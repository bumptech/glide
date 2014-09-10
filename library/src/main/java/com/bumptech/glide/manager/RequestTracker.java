package com.bumptech.glide.manager;

import com.bumptech.glide.request.Request;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * A class for tracking, canceling, and restarting in progress, completed, and failed requests.
 */
public class RequestTracker {
    // Most requests will be for views and will therefore be held strongly (and safely) by the view via the tag.
    // However, a user can always pass in a different type of target which may end up not being strongly referenced even
    // though the user still would like the request to finish. Weak references are therefore only really functional in
    // this context for view targets. Despite the side affects, WeakReferences are still essentially required. A user
    // can always make repeated requests into targets other than views, or use an activity manager in a fragment pager
    // where holding strong references would steadily leak bitmaps and/or views.
    private final Set<Request> requests = Collections.newSetFromMap(new WeakHashMap<Request, Boolean>());
    private boolean isPaused;

    /**
     * Starts tracking the given request.
     */
    public void runRequest(Request request) {
        requests.add(request);
        if (!isPaused) {
            request.begin();
        }
    }

    // Exposed for testing.
    void addRequest(Request request) {
        requests.add(request);
    }

    /**
     * Stops tracking the given request.
     */
    public void removeRequest(Request request) {
        requests.remove(request);
    }

    /**
     * Returns {@code true} if requests are currently paused, and {@code false} otherwise.
     */
    public boolean isPaused() {
        return isPaused;
    }

    /**
     * Stops any in progress requests.
     */
    public void pauseRequests() {
        isPaused = true;
        for (Request request : requests) {
            if (request.isRunning()) {
                request.pause();
            }
        }
    }

    /**
     * Starts any not yet completed or failed requests.
     */
    public void resumeRequests() {
        isPaused = false;
        for (Request request : requests) {
            if (!request.isComplete() && !request.isCancelled() && !request.isRunning()) {
                request.begin();
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
            if (!request.isComplete() && !request.isCancelled()) {
                // Ensure the request will be restarted in onResume.
                request.pause();
                if (!isPaused) {
                    request.begin();
                }
            }
        }
    }
}
