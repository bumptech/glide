package com.bumptech.glide.request;

import com.bumptech.glide.request.target.Target;

/**
 * A request that loads an asset for an {@link Target}.
 */
public interface Request {

    /**
     * Starts an asynchronous load.
     */
    public void run();

    /**
     * Prevents any bitmaps being loaded from previous requests, releases any resources held by this request and
     * displays the current placeholder if one was provided.
     */
    public void clear();

    /**
     * Returns true if the request has successfully completed.
     */
    public boolean isComplete();

    /**
     * Returns true if the request has failed.
     */
    public boolean isFailed();
}
