package com.bumptech.glide.manager;

import com.bumptech.glide.request.Request;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
    // A set of requests that have not completed and are queued to be run again. We use this list to maintain hard
    // references to these requests to ensure that they are not garbage collected before they start running or
    // while they are paused. See #346.
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private final List<Request> pendingRequests = new ArrayList<Request>();

    private boolean isPaused;

    /**
     * Starts tracking the given request.
     */
    public void runRequest(Request request) {
        requests.add(request);
        if (!isPaused) {
            request.begin();
        } else {
            pendingRequests.add(request);
        }
    }

    // Visible for testing.
    void addRequest(Request request) {
        requests.add(request);
    }

    /**
     * Stops tracking the given request.
     */
    public void removeRequest(Request request) {
        requests.remove(request);
        pendingRequests.remove(request);
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
        for (Request request : getSnapshot()) {
            if (request.isRunning()) {
                request.pause();
                pendingRequests.add(request);
            }
        }
    }

    /**
     * Starts any not yet completed or failed requests.
     */
    public void resumeRequests() {
        isPaused = false;
        for (Request request : getSnapshot()) {
            if (!request.isComplete() && !request.isCancelled() && !request.isRunning()) {
                request.begin();
            }
        }
        pendingRequests.clear();
    }

    /**
     * Cancels all requests and clears their resources.
     */
    public void clearRequests() {
        for (Request request : getSnapshot()) {
            request.clear();
        }
        pendingRequests.clear();
    }

    /**
     * Restarts failed requests and cancels and restarts in progress requests.
     */
    public void restartRequests() {
        for (Request request : getSnapshot()) {
            if (!request.isComplete() && !request.isCancelled()) {
                // Ensure the request will be restarted in onResume.
                request.pause();
                if (!isPaused) {
                    request.begin();
                } else {
                    pendingRequests.add(request);
                }
            }
        }
    }

    // Avoids a ConcurrentModificationException when requests are started by another request completing. See #303.
    private List<Request> getSnapshot() {
        // toArray creates a new ArrayList internally and this way we can guarantee entries will not be
        // null. See #322.
        List<Request> result = new ArrayList<Request>(requests.size());
        // We could also just call new ArrayList<Request>(requests) but that actually creates two new ArrayLists because
        // that constructor in ArrayList calls toArray().
        for (Request request : requests) {
            result.add(request);
        }
        return result;
    }
}
