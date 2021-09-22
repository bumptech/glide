package com.bumptech.glide.load.engine;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.core.util.Pools;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.engine.EngineResource.ResourceListener;
import com.bumptech.glide.load.engine.executor.GlideExecutor;
import com.bumptech.glide.request.ResourceCallback;
import com.bumptech.glide.util.Executors;
import com.bumptech.glide.util.Preconditions;
import com.bumptech.glide.util.Synthetic;
import com.bumptech.glide.util.pool.FactoryPools.Poolable;
import com.bumptech.glide.util.pool.StateVerifier;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A class that manages a load by adding and removing callbacks for for the load and notifying
 * callbacks when the load completes.
 */
class EngineJob<R> implements DecodeJob.Callback<R>, Poolable {
  private static final EngineResourceFactory DEFAULT_FACTORY = new EngineResourceFactory();

  @SuppressWarnings("WeakerAccess")
  @Synthetic
  final ResourceCallbacksAndExecutors cbs = new ResourceCallbacksAndExecutors();

  private final StateVerifier stateVerifier = StateVerifier.newInstance();
  private final ResourceListener resourceListener;
  private final Pools.Pool<EngineJob<?>> pool;
  private final EngineResourceFactory engineResourceFactory;
  private final EngineJobListener engineJobListener;
  private final GlideExecutor diskCacheExecutor;
  private final GlideExecutor sourceExecutor;
  private final GlideExecutor sourceUnlimitedExecutor;
  private final GlideExecutor animationExecutor;
  private final AtomicInteger pendingCallbacks = new AtomicInteger();

  private Key key;
  private boolean isCacheable;
  private boolean useUnlimitedSourceGeneratorPool;
  private boolean useAnimationPool;
  private boolean onlyRetrieveFromCache;
  private Resource<?> resource;

  @SuppressWarnings("WeakerAccess")
  @Synthetic
  DataSource dataSource;

  private boolean hasResource;

  @SuppressWarnings("WeakerAccess")
  @Synthetic
  GlideException exception;

  private boolean hasLoadFailed;

  @SuppressWarnings("WeakerAccess")
  @Synthetic
  EngineResource<?> engineResource;

  private DecodeJob<R> decodeJob;

  // Checked primarily on the main thread, but also on other threads in reschedule.
  private volatile boolean isCancelled;
  private boolean isLoadedFromAlternateCacheKey;

  EngineJob(
      GlideExecutor diskCacheExecutor,
      GlideExecutor sourceExecutor,
      GlideExecutor sourceUnlimitedExecutor,
      GlideExecutor animationExecutor,
      EngineJobListener engineJobListener,
      ResourceListener resourceListener,
      Pools.Pool<EngineJob<?>> pool) {
    this(
        diskCacheExecutor,
        sourceExecutor,
        sourceUnlimitedExecutor,
        animationExecutor,
        engineJobListener,
        resourceListener,
        pool,
        DEFAULT_FACTORY);
  }

  @VisibleForTesting
  EngineJob(
      GlideExecutor diskCacheExecutor,
      GlideExecutor sourceExecutor,
      GlideExecutor sourceUnlimitedExecutor,
      GlideExecutor animationExecutor,
      EngineJobListener engineJobListener,
      ResourceListener resourceListener,
      Pools.Pool<EngineJob<?>> pool,
      EngineResourceFactory engineResourceFactory) {
    this.diskCacheExecutor = diskCacheExecutor;
    this.sourceExecutor = sourceExecutor;
    this.sourceUnlimitedExecutor = sourceUnlimitedExecutor;
    this.animationExecutor = animationExecutor;
    this.engineJobListener = engineJobListener;
    this.resourceListener = resourceListener;
    this.pool = pool;
    this.engineResourceFactory = engineResourceFactory;
  }

  @VisibleForTesting
  synchronized EngineJob<R> init(
      Key key,
      boolean isCacheable,
      boolean useUnlimitedSourceGeneratorPool,
      boolean useAnimationPool,
      boolean onlyRetrieveFromCache) {
    this.key = key;
    this.isCacheable = isCacheable;
    this.useUnlimitedSourceGeneratorPool = useUnlimitedSourceGeneratorPool;
    this.useAnimationPool = useAnimationPool;
    this.onlyRetrieveFromCache = onlyRetrieveFromCache;
    return this;
  }

  public synchronized void start(DecodeJob<R> decodeJob) {
    this.decodeJob = decodeJob;
    GlideExecutor executor =
        decodeJob.willDecodeFromCache() ? diskCacheExecutor : getActiveSourceExecutor();
    executor.execute(decodeJob);
  }

  synchronized void addCallback(final ResourceCallback cb, Executor callbackExecutor) {
    stateVerifier.throwIfRecycled();
    cbs.add(cb, callbackExecutor);
    if (hasResource) {
      // Acquire early so that the resource isn't recycled while the Runnable below is still sitting
      // in the executors queue.
      incrementPendingCallbacks(1);
      callbackExecutor.execute(new CallResourceReady(cb));
    } else if (hasLoadFailed) {
      incrementPendingCallbacks(1);
      callbackExecutor.execute(new CallLoadFailed(cb));
    } else {
      Preconditions.checkArgument(!isCancelled, "Cannot add callbacks to a cancelled EngineJob");
    }
  }

  @SuppressWarnings("WeakerAccess")
  @Synthetic
  @GuardedBy("this")
  void callCallbackOnResourceReady(ResourceCallback cb) {
    try {
      // This is overly broad, some Glide code is actually called here, but it's much
      // simpler to encapsulate here than to do so at the actual call point in the
      // Request implementation.
      cb.onResourceReady(engineResource, dataSource, isLoadedFromAlternateCacheKey);
    } catch (Throwable t) {
      throw new CallbackException(t);
    }
  }

  @SuppressWarnings("WeakerAccess")
  @Synthetic
  @GuardedBy("this")
  void callCallbackOnLoadFailed(ResourceCallback cb) {
    // This is overly broad, some Glide code is actually called here, but it's much
    // simpler to encapsulate here than to do so at the actual call point in the Request
    // implementation.
    try {
      cb.onLoadFailed(exception);
    } catch (Throwable t) {
      throw new CallbackException(t);
    }
  }

  synchronized void removeCallback(ResourceCallback cb) {
    stateVerifier.throwIfRecycled();
    cbs.remove(cb);
    if (cbs.isEmpty()) {
      cancel();
      boolean isFinishedRunning = hasResource || hasLoadFailed;
      if (isFinishedRunning && pendingCallbacks.get() == 0) {
        release();
      }
    }
  }

  boolean onlyRetrieveFromCache() {
    return onlyRetrieveFromCache;
  }

  private GlideExecutor getActiveSourceExecutor() {
    return useUnlimitedSourceGeneratorPool
        ? sourceUnlimitedExecutor
        : (useAnimationPool ? animationExecutor : sourceExecutor);
  }

  // Exposed for testing.
  void cancel() {
    if (isDone()) {
      return;
    }

    isCancelled = true;
    decodeJob.cancel();
    engineJobListener.onEngineJobCancelled(this, key);
  }

  // Exposed for testing.
  synchronized boolean isCancelled() {
    return isCancelled;
  }

  private boolean isDone() {
    return hasLoadFailed || hasResource || isCancelled;
  }

  // We have to post Runnables in a loop. Typically there will be very few callbacks. AccessorMethod
  // seems to be a false positive
  @SuppressWarnings({
    "WeakerAccess",
    "PMD.AvoidInstantiatingObjectsInLoops",
    "PMD.AccessorMethodGeneration"
  })
  @Synthetic
  void notifyCallbacksOfResult() {
    ResourceCallbacksAndExecutors copy;
    Key localKey;
    EngineResource<?> localResource;
    synchronized (this) {
      stateVerifier.throwIfRecycled();
      if (isCancelled) {
        // TODO: Seems like we might as well put this in the memory cache instead of just recycling
        // it since we've gotten this far...
        resource.recycle();
        release();
        return;
      } else if (cbs.isEmpty()) {
        throw new IllegalStateException("Received a resource without any callbacks to notify");
      } else if (hasResource) {
        throw new IllegalStateException("Already have resource");
      }
      engineResource = engineResourceFactory.build(resource, isCacheable, key, resourceListener);
      // Hold on to resource for duration of our callbacks below so we don't recycle it in the
      // middle of notifying if it synchronously released by one of the callbacks. Acquire it under
      // a lock here so that any newly added callback that executes before the next locked section
      // below can't recycle the resource before we call the callbacks.
      hasResource = true;
      copy = cbs.copy();
      incrementPendingCallbacks(copy.size() + 1);

      localKey = key;
      localResource = engineResource;
    }

    engineJobListener.onEngineJobComplete(this, localKey, localResource);

    for (final ResourceCallbackAndExecutor entry : copy) {
      entry.executor.execute(new CallResourceReady(entry.cb));
    }
    decrementPendingCallbacks();
  }

  @SuppressWarnings("WeakerAccess")
  @Synthetic
  synchronized void incrementPendingCallbacks(int count) {
    Preconditions.checkArgument(isDone(), "Not yet complete!");
    if (pendingCallbacks.getAndAdd(count) == 0 && engineResource != null) {
      engineResource.acquire();
    }
  }

  @SuppressWarnings("WeakerAccess")
  @Synthetic
  void decrementPendingCallbacks() {
    EngineResource<?> toRelease = null;
    synchronized (this) {
      stateVerifier.throwIfRecycled();
      Preconditions.checkArgument(isDone(), "Not yet complete!");
      int decremented = pendingCallbacks.decrementAndGet();
      Preconditions.checkArgument(decremented >= 0, "Can't decrement below 0");
      if (decremented == 0) {
        toRelease = engineResource;

        release();
      }
    }

    if (toRelease != null) {
      toRelease.release();
    }
  }

  private synchronized void release() {
    if (key == null) {
      throw new IllegalArgumentException();
    }
    cbs.clear();
    key = null;
    engineResource = null;
    resource = null;
    hasLoadFailed = false;
    isCancelled = false;
    hasResource = false;
    isLoadedFromAlternateCacheKey = false;
    decodeJob.release(/*isRemovedFromQueue=*/ false);
    decodeJob = null;
    exception = null;
    dataSource = null;
    pool.release(this);
  }

  @Override
  public void onResourceReady(
      Resource<R> resource, DataSource dataSource, boolean isLoadedFromAlternateCacheKey) {
    synchronized (this) {
      this.resource = resource;
      this.dataSource = dataSource;
      this.isLoadedFromAlternateCacheKey = isLoadedFromAlternateCacheKey;
    }
    notifyCallbacksOfResult();
  }

  @Override
  public void onLoadFailed(GlideException e) {
    synchronized (this) {
      this.exception = e;
    }
    notifyCallbacksOfException();
  }

  @Override
  public void reschedule(DecodeJob<?> job) {
    // Even if the job is cancelled here, it still needs to be scheduled so that it can clean itself
    // up.
    getActiveSourceExecutor().execute(job);
  }

  // We have to post Runnables in a loop. Typically there will be very few callbacks. Acessor method
  // warning seems to be false positive.
  @SuppressWarnings({
    "WeakerAccess",
    "PMD.AvoidInstantiatingObjectsInLoops",
    "PMD.AccessorMethodGeneration"
  })
  @Synthetic
  void notifyCallbacksOfException() {
    ResourceCallbacksAndExecutors copy;
    Key localKey;
    synchronized (this) {
      stateVerifier.throwIfRecycled();
      if (isCancelled) {
        release();
        return;
      } else if (cbs.isEmpty()) {
        throw new IllegalStateException("Received an exception without any callbacks to notify");
      } else if (hasLoadFailed) {
        throw new IllegalStateException("Already failed once");
      }
      hasLoadFailed = true;

      localKey = key;

      copy = cbs.copy();
      // One for each callback below, plus one for ourselves so that we finish if a callback runs on
      // another thread before we finish scheduling all of them.
      incrementPendingCallbacks(copy.size() + 1);
    }

    engineJobListener.onEngineJobComplete(this, localKey, /*resource=*/ null);

    for (ResourceCallbackAndExecutor entry : copy) {
      entry.executor.execute(new CallLoadFailed(entry.cb));
    }
    decrementPendingCallbacks();
  }

  @NonNull
  @Override
  public StateVerifier getVerifier() {
    return stateVerifier;
  }

  private class CallLoadFailed implements Runnable {

    private final ResourceCallback cb;

    CallLoadFailed(ResourceCallback cb) {
      this.cb = cb;
    }

    @Override
    public void run() {
      // Make sure we always acquire the request lock, then the EngineJob lock to avoid deadlock
      // (b/136032534).
      synchronized (cb.getLock()) {
        synchronized (EngineJob.this) {
          if (cbs.contains(cb)) {
            callCallbackOnLoadFailed(cb);
          }

          decrementPendingCallbacks();
        }
      }
    }
  }

  private class CallResourceReady implements Runnable {

    private final ResourceCallback cb;

    CallResourceReady(ResourceCallback cb) {
      this.cb = cb;
    }

    @Override
    public void run() {
      // Make sure we always acquire the request lock, then the EngineJob lock to avoid deadlock
      // (b/136032534).
      synchronized (cb.getLock()) {
        synchronized (EngineJob.this) {
          if (cbs.contains(cb)) {
            // Acquire for this particular callback.
            engineResource.acquire();
            callCallbackOnResourceReady(cb);
            removeCallback(cb);
          }
          decrementPendingCallbacks();
        }
      }
    }
  }

  static final class ResourceCallbacksAndExecutors
      implements Iterable<ResourceCallbackAndExecutor> {
    private final List<ResourceCallbackAndExecutor> callbacksAndExecutors;

    ResourceCallbacksAndExecutors() {
      this(new ArrayList<ResourceCallbackAndExecutor>(2));
    }

    ResourceCallbacksAndExecutors(List<ResourceCallbackAndExecutor> callbacksAndExecutors) {
      this.callbacksAndExecutors = callbacksAndExecutors;
    }

    void add(ResourceCallback cb, Executor executor) {
      callbacksAndExecutors.add(new ResourceCallbackAndExecutor(cb, executor));
    }

    void remove(ResourceCallback cb) {
      callbacksAndExecutors.remove(defaultCallbackAndExecutor(cb));
    }

    boolean contains(ResourceCallback cb) {
      return callbacksAndExecutors.contains(defaultCallbackAndExecutor(cb));
    }

    boolean isEmpty() {
      return callbacksAndExecutors.isEmpty();
    }

    int size() {
      return callbacksAndExecutors.size();
    }

    void clear() {
      callbacksAndExecutors.clear();
    }

    ResourceCallbacksAndExecutors copy() {
      return new ResourceCallbacksAndExecutors(new ArrayList<>(callbacksAndExecutors));
    }

    private static ResourceCallbackAndExecutor defaultCallbackAndExecutor(ResourceCallback cb) {
      return new ResourceCallbackAndExecutor(cb, Executors.directExecutor());
    }

    @NonNull
    @Override
    public Iterator<ResourceCallbackAndExecutor> iterator() {
      return callbacksAndExecutors.iterator();
    }
  }

  static final class ResourceCallbackAndExecutor {
    final ResourceCallback cb;
    final Executor executor;

    ResourceCallbackAndExecutor(ResourceCallback cb, Executor executor) {
      this.cb = cb;
      this.executor = executor;
    }

    @Override
    public boolean equals(Object o) {
      if (o instanceof ResourceCallbackAndExecutor) {
        ResourceCallbackAndExecutor other = (ResourceCallbackAndExecutor) o;
        return cb.equals(other.cb);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return cb.hashCode();
    }
  }

  @VisibleForTesting
  static class EngineResourceFactory {
    public <R> EngineResource<R> build(
        Resource<R> resource, boolean isMemoryCacheable, Key key, ResourceListener listener) {
      return new EngineResource<>(
          resource, isMemoryCacheable, /*isRecyclable=*/ true, key, listener);
    }
  }
}
