package com.bumptech.glide.resize;

import android.os.Handler;
import com.bumptech.glide.resize.cache.ResourceCache;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @param <Z> The type of the resource that will be decoded.
 */
public class EngineJob<Z> implements ResourceCallback<Z> {
    private final String id;
    private ResourceCache cache;
    private final Map<String, ResourceRunner> runners;
    private Handler mainHandler;
    private Set<ResourceCallback<Z>> cbs = new HashSet<ResourceCallback<Z>>();
    private boolean isCancelled = false;

    public EngineJob(String id, ResourceCache cache, Map<String, ResourceRunner> runners, Handler mainHandler) {
        this.id = id;
        this.cache = cache;
        this.runners = runners;
        this.mainHandler = mainHandler;
    }

    public void addCallback(ResourceCallback<Z> cb) {
        cbs.add(cb);
    }

    public void cancel() {
        isCancelled = true;
        ResourceRunner runner = runners.remove(id);
        runner.cancel();
    }

    @Override
    public void onResourceReady(final Resource<Z> resource) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (isCancelled) {
                    return;
                }

                runners.remove(id);
                cache.put(id, resource);
                for (ResourceCallback<Z> cb : cbs) {
                    cb.onResourceReady(resource);
                }

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

                runners.remove(id);
                for (ResourceCallback cb : cbs) {
                    cb.onException(e);
                }
            }
        });
    }
}
