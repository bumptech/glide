package com.bumptech.glide.load.engine;

import android.os.Handler;
import android.os.Looper;
import android.os.MessageQueue;
import android.util.Log;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.engine.cache.DiskCache;
import com.bumptech.glide.load.engine.cache.MemoryCache;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;
import com.bumptech.glide.request.ResourceCallback;
import com.bumptech.glide.util.LogTime;

import java.io.InputStream;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

public class Engine implements EngineJobListener, MemoryCache.ResourceRemovedListener, Resource.ResourceListener {
    private static final String TAG = "Engine";
    private final Map<Key, ResourceRunner> runners;
    private final ResourceRunnerFactory factory;
    private final EngineKeyFactory keyFactory;
    private final MemoryCache cache;
    private final Map<Key, WeakReference<Resource>> activeResources;
    private final ReferenceQueue<Resource> resourceReferenceQueue;

    public static class LoadStatus {
        private final EngineJob engineJob;
        private final ResourceCallback cb;

        public LoadStatus(ResourceCallback cb, EngineJob engineJob) {
            this.cb = cb;
            this.engineJob = engineJob;
        }

        public void cancel() {
            engineJob.removeCallback(cb);
        }
    }

    public Engine(MemoryCache memoryCache, DiskCache diskCache, ExecutorService resizeService,
            ExecutorService diskCacheService) {
        this(null, memoryCache, diskCache, resizeService, diskCacheService, null, null, null);
    }

    Engine(ResourceRunnerFactory factory, MemoryCache cache, DiskCache diskCache, ExecutorService resizeService,
            ExecutorService diskCacheService, Map<Key, ResourceRunner> runners, EngineKeyFactory keyFactory,
            Map<Key, WeakReference<Resource>> activeResources) {
        this.cache = cache;

        if (activeResources == null) {
            activeResources = new HashMap<Key, WeakReference<Resource>>();
        }
        this.activeResources = activeResources;

        if (keyFactory == null) {
            keyFactory = new EngineKeyFactory();
        }
        this.keyFactory = keyFactory;

        if (runners == null) {
            runners = new HashMap<Key, ResourceRunner>();
        }
        this.runners = runners;

        if (factory == null) {
            factory = new DefaultResourceRunnerFactory(diskCache, new Handler(Looper.getMainLooper()),
                    diskCacheService, resizeService);
        }
        this.factory = factory;

        resourceReferenceQueue = new ReferenceQueue<Resource>();
        MessageQueue queue = Looper.myQueue();
        queue.addIdleHandler(new RefQueueIdleHandler(activeResources, resourceReferenceQueue));
        cache.setResourceRemovedListener(this);
    }

    /**
     * @param cacheDecoder
     * @param fetcher
     * @param decoder
     * @param encoder
     * @param transcoder
     * @param priority
     * @param <T>          The type of data the resource will be decoded from.
     * @param <Z>          The type of the resource that will be decoded.
     * @param <R>          The type of the resource that will be transcoded from the decoded resource.
     */
    public <T, Z, R> LoadStatus load(int width, int height, ResourceDecoder<InputStream, Z> cacheDecoder,
            DataFetcher<T> fetcher, ResourceDecoder<T, Z> decoder, Transformation<Z> transformation,
            ResourceEncoder<Z> encoder, ResourceTranscoder<Z, R> transcoder, Priority priority,
            boolean isMemoryCacheable, ResourceCallback cb) {
        long startTime = LogTime.getLogTime();

        final String id = fetcher.getId();
        EngineKey key = keyFactory.buildKey(id, width, height, cacheDecoder, decoder, transformation, encoder,
                transcoder);

        Resource cached = cache.remove(key);
        if (cached != null) {
            cached.acquire(1);
            activeResources.put(key, new ResourceWeakReference(key, cached, resourceReferenceQueue));
            cb.onResourceReady(cached);
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "loaded resource from cache in " + LogTime.getElapsedMillis(startTime));
            }
            return null;
        }

        WeakReference<Resource> activeRef = activeResources.get(key);
        if (activeRef != null) {
            Resource active = activeRef.get();
            if (active != null) {
                active.acquire(1);
                cb.onResourceReady(active);
                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log.v(TAG, "loaded resource from active resources in " + LogTime.getElapsedMillis(startTime));
                }
                return null;
            } else {
                activeResources.remove(key);
            }
        }

        ResourceRunner current = runners.get(key);
        if (current != null) {
            EngineJob job = current.getJob();
            job.addCallback(cb);
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "added to existing load in " + LogTime.getElapsedMillis(startTime));
            }
            return new LoadStatus(cb, job);
        }

        long start = LogTime.getLogTime();
        ResourceRunner<Z, R> runner = factory.build(key, width, height, cacheDecoder, fetcher, decoder, transformation,
                encoder, transcoder, priority, isMemoryCacheable, this);
        runner.getJob().addCallback(cb);
        runners.put(key, runner);
        runner.queue();
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "queued new load in " + LogTime.getElapsedMillis(start));
            Log.v(TAG, "finished load in engine in " + LogTime.getElapsedMillis(startTime));
        }
        return new LoadStatus(cb, runner.getJob());
    }

    @Override
    public void onEngineJobComplete(Key key, Resource resource) {
        // A null resource indicates that the load failed, usually due to an exception.
        if (resource != null) {
            resource.setResourceListener(key, this);
            activeResources.put(key, new ResourceWeakReference(key, resource, resourceReferenceQueue));
        }
        runners.remove(key);
    }

    @Override
    public void onEngineJobCancelled(Key key) {
        ResourceRunner runner = runners.remove(key);
        runner.cancel();
    }

    @Override
    public void onResourceRemoved(Resource resource) {
        resource.recycle();
    }

    @Override
    public void onResourceReleased(Key cacheKey, Resource resource) {
        activeResources.remove(cacheKey);
        if (resource.isCacheable()) {
            cache.put(cacheKey, resource);
        } else {
            resource.recycle();
        }
    }

    private static class ResourceWeakReference extends WeakReference<Resource> {
        public final Object resource;
        public final Key key;

        public ResourceWeakReference(Key key, Resource r, ReferenceQueue<? super Resource> q) {
            super(r, q);
            this.key = key;
            resource = r.get();
        }
    }

    private static class RefQueueIdleHandler implements MessageQueue.IdleHandler {
        private Map<Key, WeakReference<Resource>> activeResources;
        private ReferenceQueue<Resource> queue;

        public RefQueueIdleHandler(Map<Key, WeakReference<Resource>> activeResources, ReferenceQueue<Resource> queue) {
            this.activeResources = activeResources;
            this.queue = queue;
        }

        @Override
        public boolean queueIdle() {
            ResourceWeakReference ref = (ResourceWeakReference) queue.poll();
            if (ref != null) {
                activeResources.remove(ref.key);
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "Maybe leaked a resource: " + ref.resource);
                }
            }

            return true;
        }
    }
}
