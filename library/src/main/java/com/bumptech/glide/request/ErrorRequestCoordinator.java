package com.bumptech.glide.request;

import androidx.annotation.GuardedBy;
import androidx.annotation.Nullable;

/**
 * Runs a single primary {@link Request} until it completes and then a fallback error request only
 * if the single primary request fails.
 */
public final class ErrorRequestCoordinator implements RequestCoordinator, Request {

  private final Object requestLock;
  @Nullable private final RequestCoordinator parent;

  private volatile Request primary;
  private volatile Request error;

  @GuardedBy("requestLock")
  private RequestState primaryState = RequestState.CLEARED;

  @GuardedBy("requestLock")
  private RequestState errorState = RequestState.CLEARED;

  public ErrorRequestCoordinator(Object requestLock, @Nullable RequestCoordinator parent) {
    this.requestLock = requestLock;
    this.parent = parent;
  }

  public void setRequests(Request primary, Request error) {
    this.primary = primary;
    this.error = error;
  }

  @Override
  public void begin() {
    synchronized (requestLock) {
      if (primaryState != RequestState.RUNNING) {
        primaryState = RequestState.RUNNING;
        primary.begin();
      }
    }
  }

  @Override
  public void clear() {
    synchronized (requestLock) {
      primaryState = RequestState.CLEARED;
      primary.clear();
      // Don't check primary's failed state here because it will have been reset by the clear call
      // immediately before this.
      if (errorState != RequestState.CLEARED) {
        errorState = RequestState.CLEARED;
        error.clear();
      }
    }
  }

  @Override
  public void pause() {
    synchronized (requestLock) {
      if (primaryState == RequestState.RUNNING) {
        primaryState = RequestState.PAUSED;
        primary.pause();
      }
      if (errorState == RequestState.RUNNING) {
        errorState = RequestState.PAUSED;
        error.pause();
      }
    }
  }

  @Override
  public boolean isRunning() {
    synchronized (requestLock) {
      return primaryState == RequestState.RUNNING || errorState == RequestState.RUNNING;
    }
  }

  @Override
  public boolean isComplete() {
    synchronized (requestLock) {
      return primaryState == RequestState.SUCCESS || errorState == RequestState.SUCCESS;
    }
  }

  @Override
  public boolean isCleared() {
    synchronized (requestLock) {
      return primaryState == RequestState.CLEARED && errorState == RequestState.CLEARED;
    }
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
    synchronized (requestLock) {
      // Only one of primary or error runs at a time, so if we've reached this point and nothing
      // else is broken, we should have nothing else to enforce.
      return parentCanSetImage();
    }
  }

  @GuardedBy("requestLock")
  private boolean parentCanSetImage() {
    return parent == null || parent.canSetImage(this);
  }

  @Override
  public boolean canNotifyStatusChanged(Request request) {
    synchronized (requestLock) {
      return parentCanNotifyStatusChanged() && isValidRequestForStatusChanged(request);
    }
  }

  @Override
  public boolean canNotifyCleared(Request request) {
    synchronized (requestLock) {
      return parentCanNotifyCleared() && request.equals(primary);
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

  @GuardedBy("requestLock")
  private boolean isValidRequestForStatusChanged(Request request) {
    if (primaryState != RequestState.FAILED) {
      return request.equals(primary);
    } else {
      return request.equals(error)
          // We don't want to call onLoadStarted once for the primary request and then again
          // if it fails and the error request starts. It's already running, so we might as well
          // avoid the duplicate notification by only notifying about the error state when it's
          // final.
          && (errorState == RequestState.SUCCESS || errorState == RequestState.FAILED);
    }
  }

  @Override
  public boolean isAnyResourceSet() {
    synchronized (requestLock) {
      return primary.isAnyResourceSet() || error.isAnyResourceSet();
    }
  }

  @Override
  public void onRequestSuccess(Request request) {
    synchronized (requestLock) {
      if (request.equals(primary)) {
        primaryState = RequestState.SUCCESS;
      } else if (request.equals(error)) {
        errorState = RequestState.SUCCESS;
      }
      if (parent != null) {
        parent.onRequestSuccess(this);
      }
    }
  }

  @Override
  public void onRequestFailed(Request request) {
    synchronized (requestLock) {
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

  @Override
  public RequestCoordinator getRoot() {
    synchronized (requestLock) {
      return parent != null ? parent.getRoot() : this;
    }
  }
}
