package com.bumptech.glide.resize;

import android.os.Handler;
import com.bumptech.glide.resize.cache.ResourceCache;

import java.util.HashSet;
import java.util.Set;

/**
 * @param <Z> The type of the resource that will be decoded.
 */
public class EngineJob<Z> implements ResourceCallback<Z> {
    private final String id;
    private final EngineJobListener listener;
    private final ResourceReferenceCounter referenceCounter;
    private ResourceCache cache;
    private Handler mainHandler;
    private Set<ResourceCallback<Z>> cbs = new HashSet<ResourceCallback<Z>>();
    private boolean isCancelled;
    private boolean isComplete;

    public EngineJob(String id, ResourceCache cache, Handler mainHandler, ResourceReferenceCounter referenceCounter,
            EngineJobListener listener) {
        this.id = id;
        this.cache = cache;
        this.listener = listener;
        this.mainHandler = mainHandler;
        this.referenceCounter = referenceCounter;
    }

    public void addCallback(ResourceCallback<Z> cb) {
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
        listener.onEngineJobCancelled(id);
    }

    // Exposed for testing.
    boolean isCancelled() {
        return isCancelled;
    }

    @Override
    public void onResourceReady(final Resource<Z> resource) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (isCancelled) {
                    return;
                }
                isComplete = true;

                referenceCounter.acquireResource(resource);
                listener.onEngineJobComplete(id);
                referenceCounter.acquireResource(resource);
                cache.put(id, resource);
                for (ResourceCallback<Z> cb : cbs) {
                    referenceCounter.acquireResource(resource);
                    cb.onResourceReady(resource);
                }
                referenceCounter.releaseResource(resource);

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

                listener.onEngineJobComplete(id);
                for (ResourceCallback cb : cbs) {
                    cb.onException(e);
                }
            }
        });
    }
}
