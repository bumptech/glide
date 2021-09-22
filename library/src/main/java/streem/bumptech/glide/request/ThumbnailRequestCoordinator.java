package com.bumptech.glide.request;

import androidx.annotation.GuardedBy;
import androidx.annotation.Nullable;

/**
 * A coordinator that coordinates two individual {@link Request}s that load a small thumbnail
 * version of an image and the full size version of the image at the same time.
 */
public class ThumbnailRequestCoordinator implements RequestCoordinator, Request {
  @Nullable private final RequestCoordinator parent;
  private final Object requestLock;

  private volatile Request full;
  private volatile Request thumb;

  @GuardedBy("requestLock")
  private RequestState fullState = RequestState.CLEARED;

  @GuardedBy("requestLock")
  private RequestState thumbState = RequestState.CLEARED;
  // Only used to check if the full request is cleared by the thumbnail request.
  @GuardedBy("requestLock")
  private boolean isRunningDuringBegin;

  public ThumbnailRequestCoordinator(Object requestLock, @Nullable RequestCoordinator parent) {
    this.requestLock = requestLock;
    this.parent = parent;
  }

  public void setRequests(Request full, Request thumb) {
    this.full = full;
    this.thumb = thumb;
  }

  /**
   * Returns true if the request is either the request loading the full size image or if the request
   * loading the full size image has not yet completed.
   *
   * @param request {@inheritDoc}
   */
  @Override
  public boolean canSetImage(Request request) {
    synchronized (requestLock) {
      return parentCanSetImage() && (request.equals(full) || fullState != RequestState.SUCCESS);
    }
  }

  @GuardedBy("requestLock")
  private boolean parentCanSetImage() {
    return parent == null || parent.canSetImage(this);
  }

  /**
   * Returns true if the request is the request loading the full size image and if neither the full
   * nor the thumbnail image have completed successfully.
   *
   * @param request {@inheritDoc}.
   */
  @Override
  public boolean canNotifyStatusChanged(Request request) {
    synchronized (requestLock) {
      return parentCanNotifyStatusChanged() && request.equals(full) && !isAnyResourceSet();
    }
  }

  @Override
  public boolean canNotifyCleared(Request request) {
    synchronized (requestLock) {
      return parentCanNotifyCleared() && request.equals(full) && fullState != RequestState.PAUSED;
    }
  }

  @GuardedBy("requestLock")
  private boolean parentCanNotifyCleared() {
    return parent == null || parent.canNotifyCleared(this);
  }

  @GuardedBy("requestLock")
  private boolean parentCanNotifyStatusChanged() {
    return parent == null || parent.canNotifyStatusChanged(this);
  }

  @Override
  public boolean isAnyResourceSet() {
    synchronized (requestLock) {
      return thumb.isAnyResourceSet() || full.isAnyResourceSet();
    }
  }

  @Override
  public void onRequestSuccess(Request request) {
    synchronized (requestLock) {
      if (request.equals(thumb)) {
        thumbState = RequestState.SUCCESS;
        return;
      }
      fullState = RequestState.SUCCESS;
      if (parent != null) {
        parent.onRequestSuccess(this);
      }
      // Clearing the thumb is not necessarily safe if the thumb is being displayed in the Target,
      // as a layer in a cross fade for example. The only way we know the thumb is not being
      // displayed and is therefore safe to clear is if the thumb request has not yet completed.
      if (!thumbState.isComplete()) {
        thumb.clear();
      }
    }
  }

  @Override
  public void onRequestFailed(Request request) {
    synchronized (requestLock) {
      if (!request.equals(full)) {
        thumbState = RequestState.FAILED;
        return;
      }
      fullState = RequestState.FAILED;

      if (parent != null) {
        parent.onRequestFailed(this);
      }
    }
  }

  @Override
  public RequestCoordinator getRoot() {
    synchronized (requestLock) {
      return parent != null ? parent.getRoot() : this;
    }
  }

  /** Starts first the thumb request and then the full request. */
  @Override
  public void begin() {
    synchronized (requestLock) {
      isRunningDuringBegin = true;
      try {
        // If the request has completed previously, there's no need to restart both the full and the
        // thumb, we can just restart the full.
        if (fullState != RequestState.SUCCESS && thumbState != RequestState.RUNNING) {
          thumbState = RequestState.RUNNING;
          thumb.begin();
        }
        if (isRunningDuringBegin && fullState != RequestState.RUNNING) {
          fullState = RequestState.RUNNING;
          full.begin();
        }
      } finally {
        isRunningDuringBegin = false;
      }
    }
  }

  @Override
  public void clear() {
    synchronized (requestLock) {
      isRunningDuringBegin = false;
      fullState = RequestState.CLEARED;
      thumbState = RequestState.CLEARED;
      thumb.clear();
      full.clear();
    }
  }

  @Override
  public void pause() {
    synchronized (requestLock) {
      if (!thumbState.isComplete()) {
        thumbState = RequestState.PAUSED;
        thumb.pause();
      }
      if (!fullState.isComplete()) {
        fullState = RequestState.PAUSED;
        full.pause();
      }
    }
  }

  @Override
  public boolean isRunning() {
    synchronized (requestLock) {
      return fullState == RequestState.RUNNING;
    }
  }

  @Override
  public boolean isComplete() {
    synchronized (requestLock) {
      return fullState == RequestState.SUCCESS;
    }
  }

  @Override
  public boolean isCleared() {
    synchronized (requestLock) {
      return fullState == RequestState.CLEARED;
    }
  }

  @Override
  public boolean isEquivalentTo(Request o) {
    if (o instanceof ThumbnailRequestCoordinator) {
      ThumbnailRequestCoordinator that = (ThumbnailRequestCoordinator) o;
      return (full == null ? that.full == null : full.isEquivalentTo(that.full))
          && (thumb == null ? that.thumb == null : thumb.isEquivalentTo(that.thumb));
    }
    return false;
  }
}
