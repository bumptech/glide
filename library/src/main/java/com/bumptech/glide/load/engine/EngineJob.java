package com.bumptech.glide.load.engine;

import android.os.Handler;
import com.bumptech.glide.Resource;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.engine.cache.MemoryCache;
import com.bumptech.glide.request.ResourceCallback;

import java.util.ArrayList;
import java.util.List;

public class EngineJob implements ResourceCallback {
    private boolean isCacheable;
    private final EngineJobListener listener;
    private Key key;
    private MemoryCache cache;
    private Handler mainHandler;
    private List<ResourceCallback> cbs = new ArrayList<ResourceCallback>();
    private boolean isCancelled;
    private boolean isComplete;

    public EngineJob(Key key, MemoryCache cache, Handler mainHandler, boolean isCacheable, EngineJobListener listener) {
        this.key = key;
        this.cache = cache;
        this.isCacheable = isCacheable;
        this.listener = listener;
        this.mainHandler = mainHandler;
    }

    public void addCallback(ResourceCallback cb) {
        cbs.add(cb);
    }

    public void removeCallback(ResourceCallback cb) {
        cbs.remove(cb);
        if (cbs.size() == 0) {
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
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (isCancelled) {
                    resource.recycle();
                    return;
                }
                isComplete = true;

                // 1 to hold on for duration of request.
                resource.acquire(cbs.size() + 2);
                listener.onEngineJobComplete(key);
                if (isCacheable) {
                    cache.put(key, resource);
                } else {
                    resource.release();
                }
                for (ResourceCallback cb : cbs) {
                    cb.onResourceReady(resource);
                }
                // Our request is complete, so we can release the resource.
                resource.release();

            }
        });
    }

    @Override
    public void onException(final Exception e) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (isCancelled) {
                    return;
                }
                isComplete = true;

                listener.onEngineJobComplete(key);
                for (ResourceCallback cb : cbs) {
                    cb.onException(e);
                }
            }
        });
    }
}
