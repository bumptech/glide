package com.bumptech.glide.load.engine;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.util.Pools;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.engine.executor.GlideExecutor;
import com.bumptech.glide.request.ResourceCallback;
import com.bumptech.glide.util.Synthetic;
import com.bumptech.glide.util.Util;
import com.bumptech.glide.util.pool.FactoryPools.Poolable;
import com.bumptech.glide.util.pool.StateVerifier;
import java.util.ArrayList;
import java.util.List;

/**
 * A class that manages a load by adding and removing callbacks for for the load and notifying
 * callbacks when the load completes.
 */
class EngineJob<R> implements DecodeJob.Callback<R>,
    Poolable {
  private static final EngineResourceFactory DEFAULT_FACTORY = new EngineResourceFactory();
  private static final Handler MAIN_THREAD_HANDLER =
      new Handler(Looper.getMainLooper(), new MainThreadCallback());

  private static final int MSG_COMPLETE = 1;
  private static final int MSG_EXCEPTION = 2;
  // Used when we realize we're cancelled on a background thread in reschedule and can recycle
  // immediately rather than waiting for a result or an error.
  private static final int MSG_CANCELLED = 3;

  private final List<ResourceCallback> cbs = new ArrayList<>(2);
  private final StateVerifier stateVerifier = StateVerifier.newInstance();
  private final Pools.Pool<EngineJob<?>> pool;
  private final EngineResourceFactory engineResourceFactory;
  private final EngineJobListener listener;
  private final GlideExecutor diskCacheExecutor;
  private final GlideExecutor sourceExecutor;
  private final GlideExecutor sourceUnlimitedExecutor;

  private Key key;
  private boolean isCacheable;
  private boolean useUnlimitedSourceGeneratorPool;
  private Resource<?> resource;
  private DataSource dataSource;
  private boolean hasResource;
  private GlideException exception;
  private boolean hasLoadFailed;
  // A put of callbacks that are removed while we're notifying other callbacks of a change in
  // status.
  private List<ResourceCallback> ignoredCallbacks;
  private EngineResource<?> engineResource;
  private DecodeJob<R> decodeJob;

  // Checked primarily on the main thread, but also on other threads in reschedule.
  private volatile boolean isCancelled;

  EngineJob(GlideExecutor diskCacheExecutor, GlideExecutor sourceExecutor,
      GlideExecutor sourceUnlimitedExecutor,
      EngineJobListener listener, Pools.Pool<EngineJob<?>> pool) {
    this(diskCacheExecutor, sourceExecutor, sourceUnlimitedExecutor, listener, pool,
        DEFAULT_FACTORY);
  }

  // Visible for testing.
  EngineJob(GlideExecutor diskCacheExecutor, GlideExecutor sourceExecutor,
      GlideExecutor sourceUnlimitedExecutor,
      EngineJobListener listener, Pools.Pool<EngineJob<?>> pool,
      EngineResourceFactory engineResourceFactory) {
    this.diskCacheExecutor = diskCacheExecutor;
    this.sourceExecutor = sourceExecutor;
    this.sourceUnlimitedExecutor = sourceUnlimitedExecutor;
    this.listener = listener;
    this.pool = pool;
    this.engineResourceFactory = engineResourceFactory;
  }

  // Visible for testing.
  EngineJob<R> init(Key key, boolean isCacheable, boolean useUnlimitedSourceGeneratorPool) {
    this.key = key;
    this.isCacheable = isCacheable;
    this.useUnlimitedSourceGeneratorPool = useUnlimitedSourceGeneratorPool;
    return this;
  }

  public void start(DecodeJob<R> decodeJob) {
    this.decodeJob = decodeJob;
    GlideExecutor executor = decodeJob.willDecodeFromCache()
        ? diskCacheExecutor
        : getActiveSourceExecutor();
    executor.execute(decodeJob);
  }

  public void addCallback(ResourceCallback cb) {
    Util.assertMainThread();
    stateVerifier.throwIfRecycled();
    if (hasResource) {
      cb.onResourceReady(engineResource, dataSource);
    } else if (hasLoadFailed) {
      cb.onLoadFailed(exception);
    } else {
      cbs.add(cb);
    }
  }

  public void removeCallback(ResourceCallback cb) {
    Util.assertMainThread();
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

  private GlideExecutor getActiveSourceExecutor() {
    return useUnlimitedSourceGeneratorPool ? sourceUnlimitedExecutor : sourceExecutor;
  }

  // We cannot remove callbacks while notifying our list of callbacks directly because doing so
  // would cause a ConcurrentModificationException. However, we need to obey the cancellation
  // request such that if notifying a callback early in the callbacks list cancels a callback later
  // in the request list, the cancellation for the later request is still obeyed. Using a put of
  // ignored callbacks allows us to avoid the exception while still meeting the requirement.
  private void addIgnoredCallback(ResourceCallback cb) {
    if (ignoredCallbacks == null) {
      ignoredCallbacks = new ArrayList<>(2);
    }
    if (!ignoredCallbacks.contains(cb)) {
      ignoredCallbacks.add(cb);
    }
  }

  private boolean isInIgnoredCallbacks(ResourceCallback cb) {
    return ignoredCallbacks != null && ignoredCallbacks.contains(cb);
  }

  // Exposed for testing.
  void cancel() {
    if (hasLoadFailed || hasResource || isCancelled) {
      return;
    }

    isCancelled = true;
    decodeJob.cancel();
    boolean isPendingJobRemoved = diskCacheExecutor.remove(decodeJob)
        || sourceExecutor.remove(decodeJob)
        || sourceUnlimitedExecutor.remove(decodeJob);
    listener.onEngineJobCancelled(this, key);

    if (isPendingJobRemoved) {
      release(true /*isRemovedFromQueue*/);
    }
  }

  // Exposed for testing.
  boolean isCancelled() {
    return isCancelled;
  }

  @Synthetic
  void handleResultOnMainThread() {
    stateVerifier.throwIfRecycled();
    if (isCancelled) {
      resource.recycle();
      release(false /*isRemovedFromQueue*/);
      return;
    } else if (cbs.isEmpty()) {
      throw new IllegalStateException("Received a resource without any callbacks to notify");
    } else if (hasResource) {
      throw new IllegalStateException("Already have resource");
    }
    engineResource = engineResourceFactory.build(resource, isCacheable);
    hasResource = true;

    // Hold on to resource for duration of request so we don't recycle it in the middle of
    // notifying if it synchronously released by one of the callbacks.
    engineResource.acquire();
    listener.onEngineJobComplete(key, engineResource);

    for (ResourceCallback cb : cbs) {
      if (!isInIgnoredCallbacks(cb)) {
        engineResource.acquire();
        cb.onResourceReady(engineResource, dataSource);
      }
    }
    // Our request is complete, so we can release the resource.
    engineResource.release();

    release(false /*isRemovedFromQueue*/);
  }

  @Synthetic
  void handleCancelledOnMainThread() {
    stateVerifier.throwIfRecycled();
    if (!isCancelled) {
      throw new IllegalStateException("Not cancelled");
    }
    listener.onEngineJobCancelled(this, key);
    release(false /*isRemovedFromQueue*/);
  }

  private void release(boolean isRemovedFromQueue) {
    Util.assertMainThread();
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
    decodeJob.release(isRemovedFromQueue);
    decodeJob = null;
    exception = null;
    dataSource = null;
    pool.release(this);
  }

  @Override
  public void onResourceReady(Resource<R> resource, DataSource dataSource) {
    this.resource = resource;
    this.dataSource = dataSource;
    MAIN_THREAD_HANDLER.obtainMessage(MSG_COMPLETE, this).sendToTarget();
  }

  @Override
  public void onLoadFailed(GlideException e) {
    this.exception = e;
    MAIN_THREAD_HANDLER.obtainMessage(MSG_EXCEPTION, this).sendToTarget();
  }

  @Override
  public void reschedule(DecodeJob<?> job) {
    if (isCancelled) {
      MAIN_THREAD_HANDLER.obtainMessage(MSG_CANCELLED, this).sendToTarget();
    } else {
      getActiveSourceExecutor().execute(job);
    }
  }

  @Synthetic
  void handleExceptionOnMainThread() {
    stateVerifier.throwIfRecycled();
    if (isCancelled) {
      release(false /*isRemovedFromQueue*/);
      return;
    } else if (cbs.isEmpty()) {
      throw new IllegalStateException("Received an exception without any callbacks to notify");
    } else if (hasLoadFailed) {
      throw new IllegalStateException("Already failed once");
    }
    hasLoadFailed = true;

    listener.onEngineJobComplete(key, null);

    for (ResourceCallback cb : cbs) {
      if (!isInIgnoredCallbacks(cb)) {
        cb.onLoadFailed(exception);
      }
    }

    release(false /*isRemovedFromQueue*/);
  }

  @Override
  public StateVerifier getVerifier() {
    return stateVerifier;
  }

  // Visible for testing.
  static class EngineResourceFactory {
    public <R> EngineResource<R> build(Resource<R> resource, boolean isMemoryCacheable) {
      return new EngineResource<>(resource, isMemoryCacheable);
    }
  }

  private static class MainThreadCallback implements Handler.Callback {

    @Synthetic
    MainThreadCallback() { }

    @Override
    public boolean handleMessage(Message message) {
      EngineJob<?> job = (EngineJob<?>) message.obj;
      switch (message.what) {
        case MSG_COMPLETE:
          job.handleResultOnMainThread();
          break;
        case MSG_EXCEPTION:
          job.handleExceptionOnMainThread();
          break;
        case MSG_CANCELLED:
          job.handleCancelledOnMainThread();
          break;
        default:
          throw new IllegalStateException("Unrecognized message: " + message.what);
      }
      return true;
    }
  }
}
