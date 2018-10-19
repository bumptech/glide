package com.bumptech.glide.load.engine;

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.support.v4.util.Pools;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.Key;
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
class EngineJob<R> implements DecodeJob.Callback<R>,
    Poolable {
  private static final EngineResourceFactory DEFAULT_FACTORY = new EngineResourceFactory();

  @SuppressWarnings("WeakerAccess")
  @Synthetic
  final ResourceCallbacksAndExecutors cbs = new ResourceCallbacksAndExecutors();
  private final StateVerifier stateVerifier = StateVerifier.newInstance();
  private final Pools.Pool<EngineJob<?>> pool;
  private final EngineResourceFactory engineResourceFactory;
  private final EngineJobListener listener;
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
  // A put of callbacks that are removed while we're notifying other callbacks of a change in
  // status.
  private List<ResourceCallback> ignoredCallbacks;
  @SuppressWarnings("WeakerAccess")
  @Synthetic
  EngineResource<?> engineResource;
  private DecodeJob<R> decodeJob;

  // Checked primarily on the main thread, but also on other threads in reschedule.
  private volatile boolean isCancelled;

  EngineJob(
      GlideExecutor diskCacheExecutor,
      GlideExecutor sourceExecutor,
      GlideExecutor sourceUnlimitedExecutor,
      GlideExecutor animationExecutor,
      EngineJobListener listener,
      Pools.Pool<EngineJob<?>> pool) {
    this(
        diskCacheExecutor,
        sourceExecutor,
        sourceUnlimitedExecutor,
        animationExecutor,
        listener,
        pool,
        DEFAULT_FACTORY);
  }

  @VisibleForTesting
  EngineJob(
      GlideExecutor diskCacheExecutor,
      GlideExecutor sourceExecutor,
      GlideExecutor sourceUnlimitedExecutor,
      GlideExecutor animationExecutor,
      EngineJobListener listener,
      Pools.Pool<EngineJob<?>> pool,
      EngineResourceFactory engineResourceFactory) {
    this.diskCacheExecutor = diskCacheExecutor;
    this.sourceExecutor = sourceExecutor;
    this.sourceUnlimitedExecutor = sourceUnlimitedExecutor;
    this.animationExecutor = animationExecutor;
    this.listener = listener;
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
    GlideExecutor executor = decodeJob.willDecodeFromCache()
        ? diskCacheExecutor
        : getActiveSourceExecutor();
    executor.execute(decodeJob);
  }

  synchronized void addCallback(final ResourceCallback cb, Executor callbackExecutor) {
    stateVerifier.throwIfRecycled();
    if (hasResource) {
      // Acquire early so that the resource isn't recycled while the Runnable below is still sitting
      // in the executors queue.
      incrementPendingCallbacks(1);
      callbackExecutor.execute(new Runnable() {
        @Override
        public void run() {
          synchronized (EngineJob.this) {
            if (!isInIgnoredCallbacks(cb)) {
              engineResource.acquire();
              cb.onResourceReady(engineResource, dataSource);
            }
            decrementPendingCallbacks();
          }
        }
      });
    } else if (hasLoadFailed) {
      incrementPendingCallbacks(1);
      callbackExecutor.execute(new Runnable() {
        @Override
        public void run() {
          synchronized (EngineJob.class) {
            if (!isInIgnoredCallbacks(cb)) {
              cb.onLoadFailed(exception);
            }
            decrementPendingCallbacks();
          }
        }
      });
    } else {
      cbs.add(cb, callbackExecutor);
    }
  }

  synchronized void removeCallback(ResourceCallback cb) {
    stateVerifier.throwIfRecycled();
    if (hasResource || hasLoadFailed) {
      addIgnoredCallback(cb);
    } else {
      cbs.remove(cb);
      if (cbs.isEmpty()) {
        cancel();
      }
    }
  }

  boolean onlyRetrieveFromCache() {
    return onlyRetrieveFromCache;
  }

  private GlideExecutor getActiveSourceExecutor() {
    return useUnlimitedSourceGeneratorPool
        ? sourceUnlimitedExecutor : (useAnimationPool ? animationExecutor : sourceExecutor);
  }

  // We cannot remove callbacks while notifying our list of callbacks directly because doing so
  // would cause a ConcurrentModificationException. However, we need to obey the cancellation
  // request such that if notifying a callback early in the callbacks list cancels a callback later
  // in the request list, the cancellation for the later request is still obeyed. Using a put of
  // ignored callbacks allows us to avoid the exception while still meeting the requirement.
  private synchronized void addIgnoredCallback(ResourceCallback cb) {
    if (ignoredCallbacks == null) {
      ignoredCallbacks = new ArrayList<>(2);
    }
    if (!ignoredCallbacks.contains(cb)) {
      ignoredCallbacks.add(cb);
    }
  }

  @SuppressWarnings("WeakerAccess")
  @Synthetic
  synchronized boolean isInIgnoredCallbacks(ResourceCallback cb) {
    return ignoredCallbacks != null && ignoredCallbacks.contains(cb);
  }

  // Exposed for testing.
  void cancel() {
    Key localKey;
    synchronized (this) {
      if (isDone()) {
        return;
      }

      isCancelled = true;
      decodeJob.cancel();
      localKey = key;
    }
    // TODO: Consider trying to remove jobs that have never been run before from executor queues.
    // Removing jobs that have run before can break things. See #1996.
    listener.onEngineJobCancelled(this, localKey);
  }

  // Exposed for testing.
  synchronized boolean isCancelled() {
    return isCancelled;
  }

  private boolean isDone() {
    return hasLoadFailed || hasResource || isCancelled;
  }

  // We have to post Runnables in a loop. Typically there will be very few callbacks.
  @SuppressWarnings({"WeakerAccess", "PMD.AvoidInstantiatingObjectsInLoops"})
  @Synthetic
  void notifyCallbacksOfResult() {
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
      engineResource = engineResourceFactory.build(resource, isCacheable);
      // Hold on to resource for duration of our callbacks below so we don't recycle it in the
      // middle of notifying if it synchronously released by one of the callbacks. Acquire it under
      // a lock here so that any newly added callback that executes before the next locked section
      // below can't recycle the resource before we call the callbacks.
      hasResource = true;
      incrementPendingCallbacks(1);

      localKey = key;
      localResource = engineResource;
    }

    listener.onEngineJobComplete(this, localKey, localResource);

    synchronized (this) {
      incrementPendingCallbacks(cbs.size());
      for (final ResourceCallbackAndExecutor entry : cbs) {
        final ResourceCallback cb = entry.cb;
        entry.executor.execute(new Runnable() {
          @Override
          public void run() {
            synchronized (EngineJob.this) {
              if (cbs.contains(cb) && !isInIgnoredCallbacks(cb)) {
                // Acquire for this particular callback.
                engineResource.acquire();
                cb.onResourceReady(engineResource, dataSource);
              }
              decrementPendingCallbacks();
            }
          }
        });
      }

      decrementPendingCallbacks();
    }
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
  synchronized void decrementPendingCallbacks() {
    Preconditions.checkArgument(isDone(), "Not yet complete!");
    int decremented = pendingCallbacks.decrementAndGet();
    Preconditions.checkArgument(decremented >= 0, "Can't decrement below 0");
    if (decremented == 0) {
      if (engineResource != null) {
        engineResource.release();
      }

      release();
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
    if (ignoredCallbacks != null) {
      ignoredCallbacks.clear();
    }
    hasLoadFailed = false;
    isCancelled = false;
    hasResource = false;
    decodeJob.release(/*isRemovedFromQueue=*/ false);
    decodeJob = null;
    exception = null;
    dataSource = null;
    pool.release(this);
  }

  @Override
  public void onResourceReady(Resource<R> resource, DataSource dataSource) {
    synchronized (this) {
      this.resource = resource;
      this.dataSource = dataSource;
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

  // We have to post Runnables in a loop. Typically there will be very few callbacks.
  @SuppressWarnings({"WeakerAccess", "PMD.AvoidInstantiatingObjectsInLoops"})
  @Synthetic
  void notifyCallbacksOfException() {
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
      incrementPendingCallbacks(1);

      localKey = key;
    }

    listener.onEngineJobComplete(this, localKey, /*resource=*/ null);

    synchronized (this) {
      incrementPendingCallbacks(cbs.size());
      for (ResourceCallbackAndExecutor entry : cbs) {
        final ResourceCallback cb = entry.cb;
        entry.executor.execute(new Runnable() {
          @Override
          public void run() {
            synchronized (EngineJob.this) {
              if (cbs.contains(cb) && !isInIgnoredCallbacks(cb)) {
                cb.onLoadFailed(exception);
              }

              decrementPendingCallbacks();
            }
          }
        });
      }

      decrementPendingCallbacks();
    }
  }

  @NonNull
  @Override
  public StateVerifier getVerifier() {
    return stateVerifier;
  }

  static final class ResourceCallbacksAndExecutors implements
      Iterable<ResourceCallbackAndExecutor> {
    private final List<ResourceCallbackAndExecutor> callbacksAndExecutors = new ArrayList<>(2);

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
    public <R> EngineResource<R> build(Resource<R> resource, boolean isMemoryCacheable) {
      return new EngineResource<>(resource, isMemoryCacheable, /*isRecyclable=*/ true);
    }
  }
}
