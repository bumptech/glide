package com.bumptech.glide.request.target;

/**
 * A simpler interface for targets with default (usually noop) implementations of non essential methods that allows the
 * caller to specify an exact width/height.
 */
@SuppressWarnings("unused")
public abstract class SimpleTarget<Z> extends BaseTarget<Z> {
    private final int width;
    private final int height;

    public SimpleTarget(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public final void getSize(SizeReadyCallback cb) {
        cb.onSizeReady(width, height);
    }
}
