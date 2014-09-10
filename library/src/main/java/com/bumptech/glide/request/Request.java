package com.bumptech.glide.request;

/**
 * A request that loads a resource for an {@link com.bumptech.glide.request.target.Target}.
 */
public interface Request {

    /**
     * Starts an asynchronous load.
     */
    public void begin();

    /**
     * Identical to {@link #clear()} except that the request may later be restarted.
     */
    public void pause();

    /**
     * Prevents any bitmaps being loaded from previous requests, releases any resources held by this request,
     * displays the current placeholder if one was provided, and marks the request as having been cancelled.
     */
    public void clear();

    /**
     * Returns true if this request is paused and may be restarted.
     */
    public boolean isPaused();

    /**
     * Returns true if this request is running and has not completed or failed.
     */
    public boolean isRunning();

    /**
     * Returns true if the request has completed successfully.
     */
    public boolean isComplete();

    /**
     * Returns true if the request has been cancelled.
     */
    public boolean isCancelled();

    /**
     * Returns true if the request has failed.
     */
    public boolean isFailed();

    /**
     * Recycles the request object and releases its resources.
     */
    public void recycle();
}
