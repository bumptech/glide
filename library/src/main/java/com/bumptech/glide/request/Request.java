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
     * Prevents any bitmaps being loaded from previous requests, releases any resources held by this request and
     * displays the current placeholder if one was provided.
     */
    public void clear();

    /**
     * Returns true if this request is running and has not completed or failed.
     */
    public boolean isRunning();

    /**
     * Returns true if the request has successfully completed.
     */
    public boolean isComplete();

    /**
     * Returns true if the request has failed.
     */
    public boolean isFailed();

    /**
     * Recycles the request object and releases its resources.
     */
    public void recycle();
}
