package com.bumptech.glide.manager;

import com.bumptech.glide.request.Request;
import com.bumptech.glide.util.Util;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * A class for tracking, canceling, and restarting in progress, completed, and failed requests.
 *
 * <p>This class is not thread safe and must be accessed on the main thread.
 */
public class RequestTracker {
  // Most requests will be for views and will therefore be held strongly (and safely) by the view
  // via the tag. However, a user can always pass in a different type of target which may end up not
  // being strongly referenced even though the user still would like the request to finish. Weak
  // references are therefore only really functional in this context for view targets. Despite the
  // side affects, WeakReferences are still essentially required. A user can always make repeated
  // requests into targets other than views, or use an activity manager in a fragment pager where
  // holding strong references would steadily leak bitmaps and/or views.
  private final Set<Request> requests =
      Collections.newSetFromMap(new WeakHashMap<Request, Boolean>());
  // A set of requests that have not completed and are queued to be run again. We use this list to
  // maintain hard references to these requests to ensure that they are not garbage collected
  // before
  // they start running or while they are paused. See #346.
  @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
  private final List<Request> pendingRequests = new ArrayList<>();
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
   * Stops tracking the given request, clears, and recycles it, and returns {@code true} if the
   * request was removed or {@code false} if the request was not found.
   */
  public boolean clearRemoveAndRecycle(Request request) {
    boolean isOwnedByUs =
        request != null && (requests.remove(request) || pendingRequests.remove(request));
    if (isOwnedByUs) {
      request.clear();
      request.recycle();
    }
    return isOwnedByUs;
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
    for (Request request : Util.getSnapshot(requests)) {
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
    for (Request request : Util.getSnapshot(requests)) {
      if (!request.isComplete() && !request.isCancelled() && !request.isRunning()) {
        request.begin();
      }
    }
    pendingRequests.clear();
  }

  /**
   * Cancels all requests and clears their resources.
   *
   * <p>After this call requests cannot be restarted.
   */
  public void clearRequests() {
    for (Request request : Util.getSnapshot(requests)) {
      clearRemoveAndRecycle(request);
    }
    pendingRequests.clear();
  }

  /**
   * Restarts failed requests and cancels and restarts in progress requests.
   */
  public void restartRequests() {
    for (Request request : Util.getSnapshot(requests)) {
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

  @Override
  public String toString() {
    return super.toString() + "{numRequests=" + requests.size() + ", isPaused=" + isPaused + "}";
  }
}
