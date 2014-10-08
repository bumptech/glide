package com.bumptech.glide.load.engine;

import android.os.Handler;
import android.util.Log;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.request.ResourceCallback;
import com.bumptech.glide.util.LogTime;
import com.bumptech.glide.util.Util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A class that manages a load by adding and removing callbacks for for the load and notifying callbacks when the
 * load completes.
 */
class EngineJob implements ResourceCallback {
    private static final String TAG = "EngineJob";
    private static final EngineResourceFactory DEFAULT_FACTORY = new EngineResourceFactory();

    private final List<ResourceCallback> cbs = new ArrayList<ResourceCallback>();
    private final EngineResourceFactory engineResourceFactory;
    private final EngineJobListener listener;
    private final Key key;
    private final Handler mainHandler;
    private final boolean isCacheable;

    private boolean isCancelled;

    // Either resource or exception (particularly exception) may be returned to us null, so use booleans to track if
    // we've received them instead of relying on them to be non-null. See issue #180.
    private Resource<?> resource;
    private boolean hasResource;
    private Exception exception;
    private boolean hasException;
    // A set of callbacks that are removed while we're notifying other callbacks of a change in status.
    private Set<ResourceCallback> ignoredCallbacks;

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
        if (hasResource) {
            cb.onResourceReady(resource);
        } else if (hasException) {
            cb.onException(exception);
        } else {
            cbs.add(cb);
        }
    }

    public void removeCallback(ResourceCallback cb) {
        Util.assertMainThread();
        if (hasResource || hasException) {
            addIgnoredCallback(cb);
        } else {
            cbs.remove(cb);
            if (cbs.isEmpty()) {
                cancel();
            }
        }
    }

    // We cannot remove callbacks while notifying our list of callbacks directly because doing so would cause a
    // ConcurrentModificationException. However, we need to obey the cancellation request such that if notifying a
    // callback early in the callbacks list cancels a callback later in the request list, the cancellation for the later
    // request is still obeyed. Using a set of ignored callbacks allows us to avoid the exception while still meeting
    // the requirement.
    private void addIgnoredCallback(ResourceCallback cb) {
        if (ignoredCallbacks == null) {
            ignoredCallbacks = new HashSet<ResourceCallback>();
        }
        ignoredCallbacks.add(cb);
    }

    private boolean isInIgnoredCallbacks(ResourceCallback cb) {
        return ignoredCallbacks != null && ignoredCallbacks.contains(cb);
    }

    // Exposed for testing.
    void cancel() {
        if (hasException || hasResource || isCancelled) {
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
                EngineResource engineResource = engineResourceFactory.build(resource);
                engineResource.setCacheable(isCacheable);
                hasResource = true;
                EngineJob.this.resource = engineResource;

                // Hold on to resource for duration of request so we don't recycle it in the middle of notifying if it
                // synchronously released by one of the callbacks.
                engineResource.acquire(1);
                engineResource.acquire(cbs.size());
                listener.onEngineJobComplete(key, engineResource);

                for (ResourceCallback cb : cbs) {
                    if (!isInIgnoredCallbacks(cb)) {
                        cb.onResourceReady(engineResource);
                    }
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
                hasException = true;
                exception = e;

                listener.onEngineJobComplete(key, null);

                for (ResourceCallback cb : cbs) {
                    if (!isInIgnoredCallbacks(cb)) {
                        cb.onException(e);
                    }
                }

                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log.v(TAG, "finished onException in " + LogTime.getElapsedMillis(start));
                }
            }
        });
    }

    // Visible for testing.
    static class EngineResourceFactory {
        public <R> EngineResource<R> build(Resource<R> resource) {
            return new EngineResource<R>(resource);
        }
    }
}
