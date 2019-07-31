package com.bumptech.glide.request;

import androidx.annotation.Nullable;

/**
 * Runs a single primary {@link Request} until it completes and then a fallback error request only
 * if the single primary request fails.
 *
 * <p>TODO: The locking here isn't really correct. We should be able lock only to check/change
 * states in the coordinator without holding the lock while calling the requests.
 */
public final class ErrorRequestCoordinator implements RequestCoordinator, Request {

  @Nullable private final RequestCoordinator parent;
  // Effectively final x2.
  private Request primary;
  private Request error;

  private volatile RequestState primaryState = RequestState.CLEARED;
  private volatile RequestState errorState = RequestState.CLEARED;

  public ErrorRequestCoordinator(@Nullable RequestCoordinator parent) {
    this.parent = parent;
  }

  public void setRequests(Request primary, Request error) {
    this.primary = primary;
    this.error = error;
  }

  @Override
  public void begin() {
    if (primaryState != RequestState.RUNNING) {
      primaryState = RequestState.RUNNING;
      primary.begin();
    }
  }

  @Override
  public void clear() {
    primaryState = RequestState.CLEARED;
    primary.clear();
    // Don't check primary's failed state here because it will have been reset by the clear call
    // immediately before this.
    if (errorState != RequestState.CLEARED) {
      errorState = RequestState.CLEARED;
      error.clear();
    }
  }

  @Override
  public void pause() {
    if (primaryState == RequestState.RUNNING) {
      primaryState = RequestState.PAUSED;
      primary.pause();
    }
    if (errorState == RequestState.RUNNING) {
      errorState = RequestState.PAUSED;
      error.pause();
    }
  }

  @Override
  public boolean isRunning() {
    return primaryState == RequestState.RUNNING || errorState == RequestState.RUNNING;
  }

  @Override
  public boolean isComplete() {
    return primaryState == RequestState.SUCCESS || errorState == RequestState.SUCCESS;
  }

  @Override
  public boolean isCleared() {
    return primaryState == RequestState.CLEARED && errorState == RequestState.CLEARED;
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
    return parent == null || parent.canSetImage(this);
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
    return parent == null || parent.canNotifyCleared(this);
  }

  private boolean parentCanNotifyStatusChanged() {
    return parent == null || parent.canNotifyStatusChanged(this);
  }

  private boolean isValidRequest(Request request) {
    return request.equals(primary)
        || (primaryState == RequestState.FAILED && request.equals(error));
  }

  @Override
  public boolean isAnyResourceSet() {
    return parentIsAnyResourceSet() || isComplete();
  }

  private boolean parentIsAnyResourceSet() {
    return parent != null && parent.isAnyResourceSet();
  }

  @Override
  public void onRequestSuccess(Request request) {
    if (request == primary) {
      primaryState = RequestState.SUCCESS;
    } else if (request == error) {
      errorState = RequestState.SUCCESS;
    }
    if (parent != null) {
      parent.onRequestSuccess(this);
    }
  }

  @Override
  public void onRequestFailed(Request request) {
    if (!request.equals(error)) {
      primaryState = RequestState.FAILED;
      if (errorState != RequestState.RUNNING) {
        errorState = RequestState.RUNNING;
        error.begin();
      }
      return;
    }

    errorState = RequestState.FAILED;

    if (parent != null) {
      parent.onRequestFailed(this);
    }
  }
}
