package com.bumptech.glide.load.engine;

import android.os.Handler;
import android.util.Log;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.request.ResourceCallback;
import com.bumptech.glide.util.LogTime;

import java.util.ArrayList;
import java.util.List;

public class EngineJob implements ResourceCallback {
    private static final String TAG = "EngineJob";
    private boolean isCacheable;
    private final EngineJobListener listener;
    private Key key;
    private Handler mainHandler;
    private List<ResourceCallback> cbs;
    private ResourceCallback cb;
    private boolean isCancelled;
    private boolean isComplete;

    public EngineJob(Key key, Handler mainHandler, boolean isCacheable, EngineJobListener listener) {
        this.key = key;
        this.isCacheable = isCacheable;
        this.listener = listener;
        this.mainHandler = mainHandler;
    }

    public void addCallback(ResourceCallback cb) {
        if (this.cb == null) {
            this.cb = cb;
        } else {
            if (cbs == null) {
                cbs = new ArrayList<ResourceCallback>(2);
                cbs.add(this.cb);
            }
            cbs.add(cb);
        }
    }

    public void removeCallback(ResourceCallback cb) {
        if (cbs != null) {
            cbs.remove(cb);
            if (cbs.size() == 0) {
                cancel();
            }
        } else if (this.cb == cb) {
            this.cb = null;
            cancel();
        }
    }

    // Exposed for testing.
    void cancel() {
        if (isComplete || isCancelled) {
            return;
        }
        isCancelled = true;
        listener.onEngineJobCancelled(key);
    }

    // Exposed for testing.
    boolean isCancelled() {
        return isCancelled;
    }

    @Override
    public void onResourceReady(final Resource resource) {
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
                }
                resource.setCacheable(isCacheable);
                isComplete = true;

                // Hold on to resource for duration of request so we don't recycle it in the middle of notifying if it
                // synchronously released by one of the callbacks.
                resource.acquire(1);
                listener.onEngineJobComplete(key, resource);
                if (cbs != null) {
                    resource.acquire(cbs.size());
                    for (ResourceCallback cb : cbs) {
                        cb.onResourceReady(resource);
                    }
                } else {
                    resource.acquire(1);
                    cb.onResourceReady(resource);
                }
                // Our request is complete, so we can release the resource.
                resource.release();
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
                }
                isComplete = true;

                listener.onEngineJobComplete(key, null);
                if (cbs != null) {
                    for (ResourceCallback cb : cbs) {
                        cb.onException(e);
                    }
                } else {
                    cb.onException(e);
                }
                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log.v(TAG, "finished onException in " + LogTime.getElapsedMillis(start));
                }
            }
        });
    }
}
