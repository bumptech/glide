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
   * Prevents any bitmaps being loaded from previous requests, releases any resources held by this
   * request, displays the current placeholder if one was provided, and marks the request as having
   * been cancelled.
   */
  void clear();

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
   * Returns true if the request has been cleared.
   */
  boolean isCleared();

  /**
   * Returns true if the request has failed.
   */
  boolean isFailed();

  /**
   * Recycles the request object and releases its resources.
   */
  void recycle();

  /**
   * Returns {@code true} if this {@link Request} is equivalent to the given {@link Request} (has
   * all of the same options and sizes).
   *
   * <p>This method is identical to {@link Object#equals(Object)} except that it's specific to
   * {@link Request} subclasses. We do not use {@link Object#equals(Object)} directly because we
   * track {@link Request}s in collections like {@link java.util.Set} and it's perfectly legitimate
   * to have two different {@link Request} objects for two different
   * {@link com.bumptech.glide.request.target.Target}s (for example). Using a similar but different
   * method let's us selectively compare {@link Request} objects to each other when it's useful in
   * specific scenarios.
   */
  boolean isEquivalentTo(Request other);
}
