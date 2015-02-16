package com.bumptech.glide.request;

/**
 * A request that loads a resource for an {@link com.bumptech.glide.request.target.Target}.
 */
public interface Request {

  /**
   * Starts an asynchronous load.
   */
  void begin();

  /**
   * Identical to {@link #clear()} except that the request may later be restarted.
   */
  void pause();

  /**
   * Prevents any bitmaps being loaded from previous requests, releases any resources held by this
   * request, displays the current placeholder if one was provided, and marks the request as having
   * been cancelled.
   */
  void clear();

  /**
   * Returns true if this request is paused and may be restarted.
   */
  boolean isPaused();

  /**
   * Returns true if this request is running and has not completed or failed.
   */
  boolean isRunning();

  /**
   * Returns true if the request has completed successfully.
   */
  boolean isComplete();

  /**
   * Returns true if a non-placeholder resource is put. For Requests that load more than one
   * resource, isResourceSet may return true even if {@link #isComplete()}} returns false.
   */
  boolean isResourceSet();

  /**
   * Returns true if the request has been cancelled.
   */
  boolean isCancelled();

  /**
   * Returns true if the request has failed.
   */
  boolean isFailed();

  /**
   * Recycles the request object and releases its resources.
   */
  void recycle();
}
