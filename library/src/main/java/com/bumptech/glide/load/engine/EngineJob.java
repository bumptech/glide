package com.bumptech.glide.load.engine;

import android.os.Handler;
import android.util.Log;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.request.ResourceCallback;
import com.bumptech.glide.util.LogTime;
import com.bumptech.glide.util.Util;

import java.util.ArrayList;
import java.util.List;

/**
 * A class that manages a load by adding and removing callbacks for for the load and notifying callbacks when the
 * load completes.
 */
class EngineJob implements ResourceCallback {
    private static final String TAG = "EngineJob";
    private static final EngineResourceFactory DEFAULT_FACTORY = new DefaultEngineResourceFactory();

    private final List<ResourceCallback> cbs = new ArrayList<ResourceCallback>();
    private final EngineResourceFactory engineResourceFactory;
    private final EngineJobListener listener;
    private final Key key;
    private final Handler mainHandler;
    private final boolean isCacheable;

    private boolean isCancelled;
    private boolean isComplete;

    public EngineJob(Key key, Handler mainHandler, boolean isCacheable, EngineJobListener listener) {
        this(key, mainHandler, isCacheable, listener, DEFAULT_FACTORY);
    }

    public EngineJob(Key key, Handler mainHandler, boolean isCacheable, EngineJobListener listener,
                     EngineResourceFactory engineResourceFactory) {
        this.key = key;
        this.isCacheable = isCacheable;
        this.listener = listener;
        this.mainHandler = mainHandler;
        this.engineResourceFactory = engineResourceFactory;
    }

    public void addCallback(ResourceCallback cb) {
        Util.assertMainThread();
        cbs.add(cb);
    }

    public void removeCallback(ResourceCallback cb) {
        Util.assertMainThread();
        cbs.remove(cb);
        if (cbs.isEmpty()) {
            cancel();
        }
    }

    // Exposed for testing.
    void cancel() {
        if (isComplete || isCancelled) {
            return;
        }
        isCancelled = true;
        listener.onEngineJobCancelled(this, key);
    }

    // Exposed for testing.
    boolean isCancelled() {
        return isCancelled;
    }

    @Override
    public void onResourceReady(final Resource<?> resource) {
        final long start = LogTime.getLogTime();
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log.v(TAG, "Posted to main thread in onResourceReady in " + LogTime.getElapsedMillis(start)
                            + " cancelled: " + isCancelled);
                }
                if (isCancelled) {
                    resource.recycle();
                    return;
                } else if (cbs.isEmpty()) {
                    throw new IllegalStateException("Received a resource without any callbacks to notify");
                }
                isComplete = true;
                EngineResource engineResource = engineResourceFactory.build(resource);
                engineResource.setCacheable(isCacheable);

                // Hold on to resource for duration of request so we don't recycle it in the middle of notifying if it
                // synchronously released by one of the callbacks.
                engineResource.acquire(1);
                listener.onEngineJobComplete(key, engineResource);
                engineResource.acquire(cbs.size());
                for (ResourceCallback cb : cbs) {
                    cb.onResourceReady(engineResource);
                }
                // Our request is complete, so we can release the resource.
                engineResource.release();
                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log.v(TAG, "Finished resource ready in " + LogTime.getElapsedMillis(start));
                }
            }
        });
    }

    @Override
    public void onException(final Exception e) {
        final long start = LogTime.getLogTime();
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log.v(TAG, "posted to main thread in onException in " + LogTime.getElapsedMillis(start)
                            + " cancelled: " + isCancelled);
                }
                if (isCancelled) {
                    return;
                } else if (cbs.isEmpty()) {
                    throw new IllegalStateException("Received an exception without any callbacks to notify");
                }
                isComplete = true;

                listener.onEngineJobComplete(key, null);
                for (ResourceCallback cb : cbs) {
                    cb.onException(e);
                }
                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log.v(TAG, "finished onException in " + LogTime.getElapsedMillis(start));
                }
            }
        });
    }

    interface EngineResourceFactory {
        <R> EngineResource<R> build(Resource<R> resource);
    }

    private static final class DefaultEngineResourceFactory implements EngineResourceFactory {

        @Override
        public <R> EngineResource<R> build(Resource<R> resource) {
            return new EngineResource<R>(resource);
        }
    }
}
