package com.bumptech.glide.request;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

/**
 * A coordinator that coordinates two individual {@link Request}s that load a small thumbnail
 * version of an image and the full size version of the image at the same time.
 *
 * <p>TODO: The locking here isn't really correct. We should be able lock only to check/change
 * states in the coordinator without holding the lock while calling the requests.
 */
public class ThumbnailRequestCoordinator implements RequestCoordinator, Request {
  @Nullable private final RequestCoordinator parent;

  // Effectively final x2.
  private Request full;
  private Request thumb;

  private volatile RequestState fullState = RequestState.CLEARED;
  private volatile RequestState thumbState = RequestState.CLEARED;
  // Only used to check if the full request is cleared by the thumbnail request.
  private volatile boolean isRunningDuringBegin;

  @VisibleForTesting
  ThumbnailRequestCoordinator() {
    this(/*parent=*/ null);
  }

  public ThumbnailRequestCoordinator(@Nullable RequestCoordinator parent) {
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
    return parentCanSetImage() && (request.equals(full) || fullState != RequestState.SUCCESS);
  }

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
    return parentCanNotifyStatusChanged() && request.equals(full) && !isResourceSet();
  }

  @Override
  public boolean canNotifyCleared(Request request) {
    return parentCanNotifyCleared() && request.equals(full) && fullState != RequestState.PAUSED;
  }

  private boolean parentCanNotifyCleared() {
    return parent == null || parent.canNotifyCleared(this);
  }

  private boolean parentCanNotifyStatusChanged() {
    return parent == null || parent.canNotifyStatusChanged(this);
  }

  @Override
  public boolean isAnyResourceSet() {
    return parentIsAnyResourceSet() || isResourceSet();
  }

  @Override
  public void onRequestSuccess(Request request) {
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

  @Override
  public void onRequestFailed(Request request) {
    if (!request.equals(full)) {
      thumbState = RequestState.FAILED;
      return;
    }
    fullState = RequestState.FAILED;

    if (parent != null) {
      parent.onRequestFailed(this);
    }
  }

  private boolean parentIsAnyResourceSet() {
    return parent != null && parent.isAnyResourceSet();
  }

  /** Starts first the thumb request and then the full request. */
  @Override
  public void begin() {
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

  @Override
  public void clear() {
    isRunningDuringBegin = false;
    fullState = RequestState.CLEARED;
    thumbState = RequestState.CLEARED;
    thumb.clear();
    full.clear();
  }

  @Override
  public void pause() {
    if (!thumbState.isComplete()) {
      thumbState = RequestState.PAUSED;
      thumb.pause();
    }
    if (!fullState.isComplete()) {
      fullState = RequestState.PAUSED;
      full.pause();
    }
  }

  @Override
  public boolean isRunning() {
    return fullState == RequestState.RUNNING;
  }

  @Override
  public boolean isComplete() {
    return fullState == RequestState.SUCCESS;
  }

  private boolean isResourceSet() {
    return fullState == RequestState.SUCCESS || thumbState == RequestState.SUCCESS;
  }

  @Override
  public boolean isCleared() {
    return fullState == RequestState.CLEARED;
  }

  @Override
  public void recycle() {
    full.recycle();
    thumb.recycle();
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
