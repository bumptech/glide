package com.bumptech.glide.request;

/** A request that loads a resource for an {@link com.bumptech.glide.request.target.Target}. */
public interface Request {
  /** Starts an asynchronous load. */
  void begin();

  /**
   * Prevents any bitmaps being loaded from previous requests, releases any resources held by this
   * request, displays the current placeholder if one was provided, and marks the request as having
   * been cancelled.
   */
  void clear();

  /**
   * Similar to {@link #clear} for in progress requests (or portions of a request), but does nothing
   * if the request is already complete.
   *
   * <p>Unlike {@link #clear()}, this method allows implementations to act differently on subparts
   * of a request. For example if a Request has both a thumbnail and a primary request and the
   * thumbnail portion of the request is complete, this method allows only the primary portion of
   * the request to be paused without clearing the previously completed thumbnail portion.
   */
  void pause();

  /** Returns true if this request is running and has not completed or failed. */
  boolean isRunning();

  /** Returns true if the request has completed successfully. */
  boolean isComplete();

  /** Returns true if the request has been cleared. */
  boolean isCleared();

  /**
   * Returns true if a resource is set, even if the request is not yet complete or the primary
   * request has failed.
   */
  boolean isAnyResourceSet();

  /**
   * Returns {@code true} if this {@link Request} is equivalent to the given {@link Request} (has
   * all of the same options and sizes).
   *
   * <p>This method is identical to {@link Object#equals(Object)} except that it's specific to
   * {@link Request} subclasses. We do not use {@link Object#equals(Object)} directly because we
   * track {@link Request}s in collections like {@link java.util.Set} and it's perfectly legitimate
   * to have two different {@link Request} objects for two different {@link
   * com.bumptech.glide.request.target.Target}s (for example). Using a similar but different method
   * let's us selectively compare {@link Request} objects to each other when it's useful in specific
   * scenarios.
   */
  boolean isEquivalentTo(Request other);
}
