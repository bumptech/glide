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

    @Override
    public boolean canSetImage(Request request) {
        return request == full || !full.isComplete();
    }

    @Override
    public boolean canSetPlaceholder(Request request) {
        return request == full && !isAnyRequestComplete();
    }

    @Override
    public boolean isAnyRequestComplete() {
        return full.isComplete() || thumb.isComplete();
    }

    @Override
    public void run() {
        if (!thumb.isRunning()) {
            thumb.run();
        }
        if (!full.isRunning()) {
            full.run();
        }
    }

    @Override
    public void clear() {
        full.clear();
        thumb.clear();
    }

    @Override
    public boolean isRunning() {
        return full.isRunning();
    }

    @Override
    public boolean isComplete() {
        return full.isComplete();
    }

    @Override
    public boolean isFailed() {
        return full.isFailed();
    }

    @Override
    public void recycle() {
        full.recycle();
        thumb.recycle();
    }
}
