package com.bumptech.glide.request;

/**
 * An interface for coordinating multiple requests with the same {@link
 * com.bumptech.glide.request.target.Target}.
 *
 * <p>To avoid deadlock, implemenations must <em>not</em> call into individual {@link Request}s to
 * determine their state (ie do not call {@link Request#isCleared()} or {@link Request#isRunning()}
 * etc). Instead use {@link RequestState} and the various methods available on this interface and
 * {@link Request} to track states manually.
 */
public interface RequestCoordinator {

  /**
   * Returns true if the {@link Request} can display a loaded bitmap.
   *
   * @param request The {@link Request} requesting permission to display a bitmap.
   */
  boolean canSetImage(Request request);

  /**
   * Returns true if the {@link Request} can display a placeholder.
   *
   * @param request The {@link Request} requesting permission to display a placeholder.
   */
  boolean canNotifyStatusChanged(Request request);

  /**
   * Returns {@code true} if the {@link Request} can clear the {@link
   * com.bumptech.glide.request.target.Target}.
   */
  boolean canNotifyCleared(Request request);

  /**
   * Returns true if any coordinated {@link Request} has successfully completed.
   *
   * @see Request#isComplete()
   */
  boolean isAnyResourceSet();

  /** Must be called when a {@link Request} coordinated by this object completes successfully. */
  void onRequestSuccess(Request request);

  /** Must be called when a {@link Request} coordinated by this object fails. */
  void onRequestFailed(Request request);

  /** Returns the top most parent {@code RequestCoordinator}. */
  RequestCoordinator getRoot();

  /** A simple state enum to keep track of the states of individual subrequests. */
  enum RequestState {
    RUNNING(false),
    PAUSED(false),
    CLEARED(false),
    SUCCESS(true),
    FAILED(true);

    private final boolean isComplete;

    RequestState(boolean isComplete) {

      this.isComplete = isComplete;
    }

    boolean isComplete() {
      return isComplete;
    }
  }
}
