package com.bumptech.glide.load.engine;


/**
 * A resource interface that wraps a particular type so that it can be pooled and reused.
 *
 * @param <Z> The type of resource wrapped by this class.
 */
public interface Resource<Z> {

    /**
     * Returns an instance of the wrapped resource.
     * <p>
     *     Note - This does not have to be the same instance of the wrapped resource class and in fact it is often
     *     appropriate to return a new instance for each call. For example,
     *     {@link android.graphics.drawable.Drawable Drawable}s should only be used by a single
     *     {@link android.view.View View} at a time so each call to this method for Resources that wrap
     *     {@link android.graphics.drawable.Drawable Drawable}s should always return a new
     *     {@link android.graphics.drawable.Drawable Drawable}.
     * </p>
     */
    Z get();

    /**
     * Returns the size in bytes of the wrapped resource to use to determine how much of the memory cache this resource
     * uses.
     */
    int getSize();

    /**
     * A method that can be used to clean up or reuse inner resources when this resource is about to be destroyed.
     *
     * <p>
     *     Must not be called directly and otherwise is guaranteed to only be called at most once. May also never be
     *     called.
     * </p>
     */
    void recycle();
}
