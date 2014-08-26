package com.bumptech.glide.load.engine;

import android.os.Looper;
import com.bumptech.glide.load.Key;

/**
 * A wrapper resource that allows reference counting a wrapped {@link com.bumptech.glide.load.engine.Resource}
 * interface.
 *
 * @param <Z> The type of data returned by the wrapped {@link Resource}.
 */
public class EngineResource<Z> implements Resource<Z> {
    private final Resource<Z> resource;
    private volatile int acquired;
    private volatile boolean isRecycled;
    private ResourceListener listener;
    private Key key;
    private boolean isCacheable;

    interface ResourceListener {
        public void onResourceReleased(Key key, EngineResource<?> resource);
    }

    EngineResource(Resource<Z> toWrap) {
        resource = toWrap;
    }

    void setResourceListener(Key key, ResourceListener listener) {
        this.key = key;
        this.listener = listener;
    }

    void setCacheable(boolean isCacheable) {
        this.isCacheable = isCacheable;
    }

    boolean isCacheable() {
        return isCacheable;
    }

    @Override
    public Z get() {
        return resource.get();
    }

    @Override
    public int getSize() {
        return resource.getSize();
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
    @Override
    public void recycle() {
        if (acquired > 0) {
            throw new IllegalStateException("Cannot recycle a resource while it is still acquired");
        }
        if (isRecycled) {
            throw new IllegalStateException("Cannot recycle a resource that has already been recycled");
        }
        isRecycled = true;
        resource.recycle();
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
