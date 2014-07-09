package com.bumptech.glide.request;

/**
 * A coordinator that coordinates two individual {@link Request}s that load a small thumbnail version of an image and
 * the full size version of the image at the same time.
 */
public class ThumbnailRequestCoordinator implements RequestCoordinator, Request {
    private Request full;
    private Request thumb;

    public void setRequests(Request full, Request thumb) {
        this.full = full;
        this.thumb = thumb;
    }

    /**
     * Returns true if the request is either the request loading the fullsize image or if the request loading the
     * fullsize image has not yet completed.
     *
     * @param request {@inheritDoc}
     */
    @Override
    public boolean canSetImage(Request request) {
        return request == full || !full.isComplete();
    }

    /**
     * Returns true if the request is the request loading the fullsize image and if neither the full nor the thumbnail
     * image have completed sucessfully.
     *
     * @param request {@inheritDoc}.
     */
    @Override
    public boolean canSetPlaceholder(Request request) {
        return request == full && !isAnyRequestComplete();
    }

    /**
     * Returns true if either the full request has completed successfully or the thumb request has completed
     * successfully.
     */
    @Override
    public boolean isAnyRequestComplete() {
        return full.isComplete() || thumb.isComplete();
    }

    /**
     * Starts first the thumb request and then the full request.
     */
    @Override
    public void begin() {
        if (!thumb.isRunning()) {
            thumb.begin();
        }
        if (!full.isRunning()) {
            full.begin();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        full.clear();
        thumb.clear();
    }

    /**
     * Returns true if the full request is still running.
     */
    @Override
    public boolean isRunning() {
        return full.isRunning();
    }

    /**
     * Returns true if the full request is complete.
     */
    @Override
    public boolean isComplete() {
        return full.isComplete();
    }

    /**
     * Returns true if the full request has failed.
     */
    @Override
    public boolean isFailed() {
        return full.isFailed();
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public void recycle() {
        full.recycle();
        thumb.recycle();
    }
}
