package com.bumptech.glide.load.engine;

import android.os.Looper;
import com.bumptech.glide.load.Key;

/**
 * A resource base class that handles reference counting so resources can safely be reused.
 *
 * <p>
 *     Public methods are non final only to allow for mocking, subclasses must only override abstract methods.
 * </p>
 *
 * @param <Z> The type of resource wrapped by this class.
 */
public abstract class Resource<Z> {
    private volatile int acquired;
    private volatile boolean isRecycled;
    private ResourceListener<Z> listener;
    private Key key;
    private boolean isCacheable;

    interface ResourceListener<Z> {
        public void onResourceReleased(Key key, Resource<Z> resource);
    }

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
    public abstract Z get();

    /**
     * Returns the size in bytes of the wrapped resource to use to determine how much of the memory cache this resource
     * uses.
     */
    public abstract int getSize();

    /**
     * A method that can be used to clean up or reuse inner resources when this resource is about to be destroyed.
     *
     * <p>
     *     Must not be called directly and otherwise is guaranteed to only be called at most once. May also never be
     *     called.
     * </p>
     */
    protected abstract void recycleInternal();

    void setResourceListener(Key key, ResourceListener<Z> listener) {
        this.key = key;
        this.listener = listener;
    }

    void setCacheable(boolean isCacheable) {
        this.isCacheable = isCacheable;
    }

    boolean isCacheable() {
        return isCacheable;
    }

    /**
     * Cleans up and recycles internal resources.
     *
     * <p>
     *     It is only safe to call this method if there are no current resource consumers and if this method has not
     *     yet been called. Typically this occurs at one of two times:
     *     <ul>
     *         <li>During a resource load when the resource is transformed or transcoded before any consumer have
     *         ever had access to this resource</li>
     *         <li>After all consumers have released this resource and it has been evicted from the cache</li>
     *     </ul>
     *
     *     For most users of this class, the only time this method should ever be called is during transformations or
     *     transcoders, the framework will call this method when all consumers have released this resource and it has
     *     been evicted from the cache.
     * </p>
     */
    public void recycle() {
        if (acquired > 0) {
            throw new IllegalStateException("Cannot recycle a resource while it is still acquired");
        }
        if (isRecycled) {
            throw new IllegalStateException("Cannot recycle a resource that has already been recycled");
        }
        isRecycled = true;
        recycleInternal();
    }

    /**
     * Increments the number of consumers using the wrapped resource. Must be called on the main thread.
     *
     * <p>
     *     This must be called with a number corresponding to the number of new consumers each time new consumers
     *     begin using the wrapped resource. It is always safer to call acquire more often than necessary. Generally
     *     external users should never call this method, the framework will take care of this for you.
     * </p>
     *
     * @param times The number of consumers that have just started using the resource.
     */
    public void acquire(int times) {
        if (isRecycled) {
            throw new IllegalStateException("Cannot acquire a recycled resource");
        }
        if (times <= 0) {
            throw new IllegalArgumentException("Must acquire a number of times >= 0");
        }
        if (!Looper.getMainLooper().equals(Looper.myLooper())) {
            throw new IllegalThreadStateException("Must call acquire on the main thread");
        }
        acquired += times;
    }

    /**
     * Decrements the number of consumers using the wrapped resource. Must be called on the main thread.
     *
     * <p>
     *     This must only be called when a consumer that called the {@link #acquire(int)} method is now done with the
     *     resource. Generally external users should never callthis method, the framework will take care of this for
     *     you.
     * </p>
     */
    public void release() {
        if (acquired <= 0) {
            throw new IllegalStateException("Cannot release a recycled or not yet acquired resource");
        }
        if (!Looper.getMainLooper().equals(Looper.myLooper())) {
            throw new IllegalThreadStateException("Must call release on the main thread");
        }
        if (--acquired == 0) {
            listener.onResourceReleased(key, this);
        }
    }
}
