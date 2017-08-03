package com.bumptech.glide.request;

import android.support.annotation.Nullable;

/**
 * A coordinator that coordinates two individual {@link Request}s that load a small thumbnail
 * version of an image and the full size version of the image at the same time.
 */
public class ThumbnailRequestCoordinator implements RequestCoordinator,
    Request {
  private Request full;
  private Request thumb;
  @Nullable private RequestCoordinator coordinator;
  private boolean isRunning;

  public ThumbnailRequestCoordinator() {
    this(null);
  }

  public ThumbnailRequestCoordinator(RequestCoordinator coordinator) {
    this.coordinator = coordinator;
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
    return parentCanSetImage() && (request.equals(full) || !full.isResourceSet());
  }

  private boolean parentCanSetImage() {
    return coordinator == null || coordinator.canSetImage(this);
  }

  /**
   * Returns true if the request is the request loading the full size image and if neither the full
   * nor the thumbnail image have completed successfully.
   *
   * @param request {@inheritDoc}.
   */
  @Override
  public boolean canNotifyStatusChanged(Request request) {
    return parentCanNotifyStatusChanged() && request.equals(full) && !isAnyResourceSet();
  }

  private boolean parentCanNotifyStatusChanged() {
    return coordinator == null || coordinator.canNotifyStatusChanged(this);
  }

  @Override
  public boolean isAnyResourceSet() {
    return parentIsAnyResourceSet() || isResourceSet();
  }

  @Override
  public void onRequestSuccess(Request request) {
    if (request.equals(thumb)) {
      return;
    }
    if (coordinator != null) {
      coordinator.onRequestSuccess(this);
    }
    // Clearing the thumb is not necessarily safe if the thumb is being displayed in the Target,
    // as a layer in a cross fade for example. The only way we know the thumb is not being
    // displayed and is therefore safe to clear is if the thumb request has not yet completed.
    if (!thumb.isComplete()) {
      thumb.clear();
    }
  }

  private boolean parentIsAnyResourceSet() {
    return coordinator != null && coordinator.isAnyResourceSet();
  }

  /**
   * Starts first the thumb request and then the full request.
   */
  @Override
  public void begin() {
    isRunning = true;
    if (!thumb.isRunning()) {
      thumb.begin();
    }
    if (isRunning && !full.isRunning()) {
      full.begin();
    }
  }

  @Override
  public void pause() {
    isRunning = false;
    full.pause();
    thumb.pause();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void clear() {
    isRunning = false;
    thumb.clear();
    full.clear();
  }

  @Override
  public boolean isPaused() {
    return full.isPaused();
  }

  /**
   * Returns true if the full request is still running.
   */
  @Override
  public boolean isRunning() {
    return full.isRunning();
  }

  /**
   * Returns true if the full request is complete.
   */
  @Override
  public boolean isComplete() {
    return full.isComplete() || thumb.isComplete();
  }

  @Override
  public boolean isResourceSet() {
    return full.isResourceSet() || thumb.isResourceSet();
  }

  @Override
  public boolean isCancelled() {
    return full.isCancelled();
  }

  /**
   * Returns true if the full request has failed.
   */
  @Override
  public boolean isFailed() {
    return full.isFailed();
  }

  /**
   * {@inheritDoc}.
   */
  @Override
  public void recycle() {
    full.recycle();
    thumb.recycle();
  }
}
