package com.bumptech.glide.request;

import android.support.annotation.Nullable;

/**
 * Runs a single primary {@link Request} until it completes and then a fallback error request only
 * if the single primary request fails.
 */
public final class ErrorRequestCoordinator implements RequestCoordinator,
    Request {

  @Nullable
  private final RequestCoordinator coordinator;
  private Request primary;
  private Request error;

  public ErrorRequestCoordinator(@Nullable RequestCoordinator coordinator) {
    this.coordinator = coordinator;
  }

  public void setRequests(Request primary, Request error) {
    this.primary = primary;
    this.error = error;
  }

  @Override
  public void begin() {
    if (!primary.isRunning()) {
      primary.begin();
    }
  }

  @Override
  public void pause() {
    if (!primary.isFailed()) {
      primary.pause();
    }
    if (error.isRunning()) {
      error.pause();
    }
  }

  @Override
  public void clear() {
    primary.clear();
    if (primary.isFailed()) {
      error.clear();
    }
  }

  @Override
  public boolean isPaused() {
    return primary.isFailed() ? error.isPaused() : primary.isPaused();
  }

  @Override
  public boolean isRunning() {
    return primary.isFailed() ? error.isRunning() : primary.isRunning();
  }

  @Override
  public boolean isComplete() {
    return primary.isFailed() ? error.isComplete() : primary.isComplete();
  }

  @Override
  public boolean isResourceSet() {
    return primary.isFailed() ? error.isResourceSet() : primary.isResourceSet();
  }

  @Override
  public boolean isCancelled() {
    return primary.isFailed() ? error.isCancelled() : primary.isCancelled();
  }

  @Override
  public boolean isFailed() {
    return primary.isFailed() && error.isFailed();
  }

  @Override
  public void recycle() {
    primary.recycle();
    error.recycle();
  }

  @Override
  public boolean isEquivalentTo(Request o) {
    if (o instanceof ErrorRequestCoordinator) {
      ErrorRequestCoordinator other = (ErrorRequestCoordinator) o;
      return primary.isEquivalentTo(other.primary) && error.isEquivalentTo(other.error);
    }
    return false;
  }

  @Override
  public boolean canSetImage(Request request) {
    return parentCanSetImage() && isValidRequest(request);
  }

  private boolean parentCanSetImage() {
    return coordinator == null || coordinator.canSetImage(this);
  }

  @Override
  public boolean canNotifyStatusChanged(Request request) {
    return parentCanNotifyStatusChanged() && isValidRequest(request);
  }

  @Override
  public boolean canNotifyCleared(Request request) {
    return parentCanNotifyCleared() && isValidRequest(request);
  }

  private boolean parentCanNotifyCleared() {
    return coordinator == null || coordinator.canNotifyCleared(this);
  }

  private boolean parentCanNotifyStatusChanged() {
    return coordinator == null || coordinator.canNotifyStatusChanged(this);
  }

  private boolean isValidRequest(Request request) {
    return request.equals(primary) || (primary.isFailed() && request.equals(error));
  }

  @Override
  public boolean isAnyResourceSet() {
    return parentIsAnyResourceSet() || isResourceSet();
  }

  private boolean parentIsAnyResourceSet() {
    return coordinator != null && coordinator.isAnyResourceSet();
  }

  @Override
  public void onRequestSuccess(Request request) {
    if (coordinator != null) {
      coordinator.onRequestSuccess(this);
    }
  }

  @Override
  public void onRequestFailed(Request request) {
    if (!request.equals(error)) {
      if (!error.isRunning()) {
        error.begin();
      }
      return;
    }

    if (coordinator != null) {
      coordinator.onRequestFailed(this);
    }
  }
}
