package com.bumptech.glide.load.engine;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.MessageQueue;
import android.util.Log;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.Encoder;
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
import com.bumptech.glide.util.Util;

import java.io.File;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * Responsible for starting loads and managing active and cached resources.
 */
public class Engine implements EngineJobListener, MemoryCache.ResourceRemovedListener, EngineResource.ResourceListener {
    private static final String TAG = "Engine";
    private final Map<Key, ResourceRunner> runners;
    private final ResourceRunnerFactory factory;
    private final EngineKeyFactory keyFactory;
    private final MemoryCache cache;
    private final Map<Key, WeakReference<EngineResource<?>>> activeResources;
    private final ReferenceQueue<EngineResource<?>> resourceReferenceQueue;
    private final Handler mainHandler;

    /**
     * Allows a request to indicate it no longer is interested in a given load.
     */
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
            Map<Key, WeakReference<EngineResource<?>>> activeResources) {
        this.cache = cache;

        if (activeResources == null) {
            activeResources = new HashMap<Key, WeakReference<EngineResource<?>>>();
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
            factory = new ResourceRunnerFactory(diskCache, new Handler(Looper.getMainLooper()),
                    diskCacheService, resizeService);
        }
        this.factory = factory;

        resourceReferenceQueue = new ReferenceQueue<EngineResource<?>>();
        MessageQueue queue = Looper.myQueue();
        queue.addIdleHandler(new RefQueueIdleHandler(activeResources, resourceReferenceQueue));
        cache.setResourceRemovedListener(this);

        mainHandler = new Handler(Looper.getMainLooper(), new ResourceRecyclerCallback());
    }

    /**
     * Starts a load for the given arguments. Must be called on the main thread.
     *
     * <p>
     *     The flow for any request is as follows:
     *     <ul>
     *         <li>Check the memory cache and provide the cached resource if present</li>
     *         <li>Check the current set of actively used resources and return the active resource if present</li>
     *         <li>Check the current set of in progress loads and add the cb to the in progress load if present</li>
     *         <li>Start a new load</li>
     *     </ul>
     * </p>
     *
     * <p>
     *     Active resources are those that have been provided to at least one request and have not yet been released.
     *     Once all consumers of a resource have released that resource, the resource then goes to cache. If the
     *     resource is ever returned to a new consumer from cache, it is re-added to the active resources. If the
     *     resource is evicted from the cache, its resources are recycled and re-used if possible and the resource is
     *     discarded. There is no strict requirement that consumers release their resources so active resources are
     *     held weakly.
     * </p>
     *
     * @param width The target width of the retrieved resource.
     * @param height The target height of the retrieved resource.
     * @param cacheDecoder The decoder to use to decode data already in the disk cache.
     * @param fetcher The fetcher to use to retrieve data not in the disk cache.
     * @param sourceEncoder The encoder to use to encode any retrieved data directly to cache.
     * @param decoder The decoder to use to decode any retrieved data not in cache.
     * @param transformation The transformation to use to transform the decoded resource.
     * @param encoder The encoder to to use to write the decoded and transformed resource to the disk cache.
     * @param transcoder The transcoder to use to transcode the decoded and transformed resource.
     * @param priority The priority with which the request should run.
     * @param isMemoryCacheable True if the transcoded resource can be cached in memory.
     * @param diskCacheStrategy The strategy to use that determines what type of data, if any,
     *                          will be cached in the local disk cache.
     * @param cb The callback that will be called when the load completes.
     *
     * @param <T> The type of data the resource will be decoded from.
     * @param <Z> The type of the resource that will be decoded.
     * @param <R> The type of the resource that will be transcoded from the decoded resource.
     */
    public <T, Z, R> LoadStatus load(int width, int height, ResourceDecoder<File, Z> cacheDecoder,
            DataFetcher<T> fetcher, Encoder<T> sourceEncoder, ResourceDecoder<T, Z> decoder,
            Transformation<Z> transformation, ResourceEncoder<Z> encoder, ResourceTranscoder<Z, R> transcoder,
            Priority priority, boolean isMemoryCacheable, DiskCacheStrategy diskCacheStrategy, ResourceCallback cb) {
        Util.assertMainThread();
        long startTime = LogTime.getLogTime();

        final String id = fetcher.getId();
        EngineKey key = keyFactory.buildKey(id, width, height, cacheDecoder, decoder, transformation, encoder,
                transcoder, sourceEncoder);

        EngineResource<?> cached = getFromCache(key);
        if (cached != null) {
            cached.acquire();
            activeResources.put(key, new ResourceWeakReference(key, cached, resourceReferenceQueue));
            cb.onResourceReady(cached);
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "loaded resource from cache in " + LogTime.getElapsedMillis(startTime));
            }
            return null;
        }

        WeakReference<EngineResource<?>> activeRef = activeResources.get(key);
        if (activeRef != null) {
            EngineResource<?> active = activeRef.get();
            if (active != null) {
                active.acquire();
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
        ResourceRunner<Z, R> runner = factory.build(key, width, height, cacheDecoder, fetcher, sourceEncoder, decoder,
                transformation, encoder, transcoder, priority, isMemoryCacheable, diskCacheStrategy, this);
        runner.getJob().addCallback(cb);
        runners.put(key, runner);
        runner.queue();
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "queued new load in " + LogTime.getElapsedMillis(start));
            Log.v(TAG, "finished load in engine in " + LogTime.getElapsedMillis(startTime));
        }
        return new LoadStatus(cb, runner.getJob());
    }

    @SuppressWarnings("unchecked")
    private EngineResource<?> getFromCache(Key key) {
        Resource<?> cached = cache.remove(key);

        final EngineResource result;
        if (cached == null) {
            result = null;
        } else if (cached instanceof EngineResource) {
            // Save an object allocation if we've cached an EngineResource (the typical case).
            result = (EngineResource) cached;
        } else {
            result = new EngineResource(cached);
        }
        return result;
    }

    public void release(Resource resource) {
        if (resource instanceof EngineResource) {
            ((EngineResource) resource).release();
        } else {
            throw new IllegalArgumentException("Cannot release anything but an EngineResource");
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onEngineJobComplete(Key key, EngineResource<?> resource) {
        // A null resource indicates that the load failed, usually due to an exception.
        if (resource != null) {
            resource.setResourceListener(key, this);
            activeResources.put(key, new ResourceWeakReference(key, resource, resourceReferenceQueue));
        }
        runners.remove(key);
    }

    @Override
    public void onEngineJobCancelled(EngineJob engineJob, Key key) {
        ResourceRunner runner = runners.get(key);
        if (runner.getJob() == engineJob) {
            runners.remove(key);
            runner.cancel();
        }
    }

    @Override
    public void onResourceRemoved(final Resource<?> resource) {
        recycleResource(resource);
    }

    @Override
    public void onResourceReleased(Key cacheKey, EngineResource resource) {
        activeResources.remove(cacheKey);
        if (resource.isCacheable()) {
            cache.put(cacheKey, resource);
        } else {
            recycleResource(resource);
        }
    }

    private void recycleResource(Resource<?> resource) {
        // If a resource has sub-resources, releasing a sub resource can cause it's parent to be synchronously
        // evicted which leads to a recycle loop when the parent the releases it's children. Posting breaks this loops.
        mainHandler.obtainMessage(ResourceRecyclerCallback.RECYCLE_RESOURCE, resource).sendToTarget();
    }

    private static class ResourceRecyclerCallback implements Handler.Callback {
        public static final int RECYCLE_RESOURCE = 1;

        @Override
        public boolean handleMessage(Message message) {
            if (message.what == RECYCLE_RESOURCE) {
                Resource resource = (Resource) message.obj;
                resource.recycle();
                return true;
            }
            return false;
        }
    }

    private static class ResourceWeakReference extends WeakReference<EngineResource<?>> {
        private final Key key;

        public ResourceWeakReference(Key key, EngineResource<?> r, ReferenceQueue<? super EngineResource<?>> q) {
            super(r, q);
            this.key = key;
        }
    }

    // Responsible for cleaning up the active resource map by remove weak references that have been cleared.
    private static class RefQueueIdleHandler implements MessageQueue.IdleHandler {
        private final Map<Key, WeakReference<EngineResource<?>>> activeResources;
        private final ReferenceQueue<EngineResource<?>> queue;

        public RefQueueIdleHandler(Map<Key, WeakReference<EngineResource<?>>> activeResources,
                ReferenceQueue<EngineResource<?>> queue) {
            this.activeResources = activeResources;
            this.queue = queue;
        }

        @Override
        public boolean queueIdle() {
            ResourceWeakReference ref = (ResourceWeakReference) queue.poll();
            if (ref != null) {
                activeResources.remove(ref.key);
            }

            return true;
        }
    }
}
