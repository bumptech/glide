package com.bumptech.glide.request.target;

/**
 * A callback that must be called when the target has determined its size. For fixed size targets it can
 * be called synchronously.
 */
public interface SizeReadyCallback {
    /**
     * A callback called on the main thread.
     *
     * @param width The width in pixels of the target.
     * @param height The height in pixels of the target.
     */
    void onSizeReady(int width, int height);
}
