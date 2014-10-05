package com.bumptech.glide.request.target;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;

/**
 * A one time use {@link com.bumptech.glide.request.target.Target} class that loads a resource into memory and then
 * clears itself.
 *
 * @param <Z> The type of resource that will be loaded into memory.
 */
public final class PreloadTarget<Z> extends SimpleTarget<Z> {

    /**
     * Returns a PreloadTarget.
     *
     * @param width The width of the desired resourece.
     * @param height The height of the desired resource.
     * @param <Z> The type of the desired resource.
     */
    public static <Z> PreloadTarget<Z> obtain(int width, int height) {
        return new PreloadTarget<Z>(width, height);
    }

    private PreloadTarget(int width, int height) {
        super(width, height);
    }

    @Override
    public void onResourceReady(Z resource, GlideAnimation<? super Z> glideAnimation) {
        Glide.clear(this);
    }
}
