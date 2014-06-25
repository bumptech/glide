package com.bumptech.glide.load.engine;

import android.os.Looper;
import com.bumptech.glide.load.Key;

/**
 * A generic resource that handles reference counting so resources can safely be reused.
 * <p>
 *     Public methods are non final only to allow for mocking, subclasses must only override abstract methods.
 * </p>
 *
 * @param <Z> The type of resource wrapped by this class.
 */
public abstract class Resource<Z> {
    private volatile int acquired;
    private volatile boolean isRecycled;
    private ResourceListener listener;
    private Key key;
    private boolean isCacheable;

    interface ResourceListener {
        public void onResourceReleased(Key key, Resource resource);
    }

    public abstract Z get();

    public abstract int getSize();

    protected abstract void recycleInternal();

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
