package com.bumptech.glide.load.engine;

import android.support.v4.util.Pools;
import android.util.Log;

import com.bumptech.glide.GlideContext;
import com.bumptech.glide.Logs;
import com.bumptech.glide.Priority;
import com.bumptech.glide.Registry;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.EncodeStrategy;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.data.DataRewinder;
import com.bumptech.glide.load.engine.cache.DiskCache;
import com.bumptech.glide.util.LogTime;
import com.bumptech.glide.util.Preconditions;

import java.util.Map;

/**
 * A class responsible for decoding resources either from cached data or from the original source
 * and applying transformations and transcodes.
 *
 * <p>Note: this class has a natural ordering that is inconsistent with equals.
 *
 * @param <R> The type of resource that will be transcoded from the decoded and transformed
 *            resource.
 */
class DecodeJob<R> implements DataFetcherGenerator.FetcherReadyCallback,
    Runnable,
    Comparable<DecodeJob<?>> {
  private static final String TAG = "DecodeJob";
  private static final RunReason INITIAL_RUN_REASON = RunReason.INITIALIZE;

  private final DiskCacheProvider diskCacheProvider;
  private final Pools.Pool<DecodeJob<?>> pool;
  private final DecodeHelper<R> decodeHelper = new DecodeHelper<>();
  private GlideContext glideContext;
  private Key signature;
  private Priority priority;
  private EngineKey loadKey;
  private int width;
  private int height;
  private DiskCacheStrategy diskCacheStrategy;
  private Options options;
  private Callback<R> callback;
  private int order;
  private Stage stage;
  private RunReason runReason = INITIAL_RUN_REASON;
  private volatile DataFetcherGenerator generator;
  private Thread currentThread;
  private Key currentSourceKey;
  private Object currentData;
  private DataSource currentDataSource;
  private DataFetcher<?> currentFetcher;
  private long startFetchTime;

  private volatile boolean isCancelled;
  private volatile boolean isCallbackNotified;

  DecodeJob(DiskCacheProvider diskCacheProvider, Pools.Pool<DecodeJob<?>> pool) {
    this.diskCacheProvider = diskCacheProvider;
    this.pool = pool;
  }

  DecodeJob<R> init(
      GlideContext glideContext,
      Object model,
      EngineKey loadKey,
      Key signature,
      int width,
      int height,
      Class<?> resourceClass,
      Class<R> transcodeClass,
      Priority priority,
      DiskCacheStrategy diskCacheStrategy,
      Map<Class<?>, Transformation<?>> transformations,
      boolean isTransformationRequired,
      Options options,
      Callback<R> callback,
      int order) {
    decodeHelper.init(
        glideContext,
        model,
        signature,
        width,
        height,
        diskCacheStrategy,
        resourceClass,
        transcodeClass,
        priority,
        options,
        transformations,
        isTransformationRequired,
        diskCacheProvider);
    this.glideContext = glideContext;
    this.signature = signature;
    this.priority = priority;
    this.loadKey = loadKey;
    this.width = width;
    this.height = height;
    this.diskCacheStrategy = diskCacheStrategy;
    this.options = options;
    this.callback = callback;
    this.order = order;
    return this;
  }

  void release() {
    decodeHelper.clear();
    glideContext = null;
    signature = null;
    options = null;
    priority = null;
    loadKey = null;
    callback = null;
    stage = null;
    runReason = INITIAL_RUN_REASON;
    generator = null;
    currentThread = null;
    currentSourceKey = null;
    currentData = null;
    currentDataSource = null;
    currentFetcher = null;
    startFetchTime = 0L;
    isCancelled = false;
    isCallbackNotified = false;
    pool.release(this);
  }

  @Override
  public int compareTo(DecodeJob<?> other) {
    int result = getPriority() - other.getPriority();
    if (result == 0) {
      result = order - other.order;
    }
    return result;
  }

  private int getPriority() {
    return priority.ordinal();
  }

  /**
   * Why we're being executed again.
   */
  private enum RunReason {
    /** The first time we've been submitted. */
    INITIALIZE,
    /**
     * We want to switch from the disk cache service to the source executor.
     */
    SWITCH_TO_SOURCE_SERVICE,
    /**
     * We retrieved some data on a thread we don't own and want to switch back to our thread to
     * process the data.
     */
    DECODE_DATA,
  }

  /**
   * Where we're trying to decode data from.
   */
  private enum Stage {
    /** The initial stage. */
    INITIALIZE,
    /** Decode from a cached resource. */
    RESOURCE_CACHE,
    /** Decode from cached source data. */
    DATA_CACHE,
    /** Decode from retrieved source. */
    SOURCE,
  }

  public void cancel() {
    isCancelled = true;
    DataFetcherGenerator local = generator;
    if (local != null) {
      local.cancel();
    }
  }

  @Override
  public void run() {
    // This should be much more fine grained, but since Java's thread pool implementation silently
    // swallows all otherwise fatal exceptions, this will at least make it obvious to developers
    // that something is failing.
    try {
      if (isCancelled) {
        notifyFailed();
        return;
      }
      runWrapped();
    } catch (RuntimeException e) {
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, "DecodeJob threw unexpectedly, isCancelled: " + isCancelled, e);
      }
      notifyFailed();
      if (!isCancelled) {
        throw e;
      }
    }
  }

  private void runWrapped() {
     switch (runReason) {
      case INITIALIZE:
        stage = getNextStage(Stage.INITIALIZE);
        generator = getNextGenerator();
        runGenerators();
        break;
      case SWITCH_TO_SOURCE_SERVICE:
        runGenerators();
        break;
      case DECODE_DATA:
        decodeFromRetrievedData();
        break;
      default:
        throw new IllegalStateException("Unrecognized run reason: " + runReason);
    }
  }

  private DataFetcherGenerator getNextGenerator() {
    if (stage == null) {
      return null;
    }
    switch (stage) {
      case RESOURCE_CACHE:
        return new ResourceCacheGenerator(decodeHelper, this);
      case DATA_CACHE:
        return new DataCacheGenerator(decodeHelper, this);
      case SOURCE:
        return new SourceGenerator<>(decodeHelper, this);
      default:
        throw new IllegalStateException("Unrecognized stage: " + stage);
    }
  }

  private void runGenerators() {
    currentThread = Thread.currentThread();
    startFetchTime = LogTime.getLogTime();
    boolean isStarted = false;
    while (!isCancelled && generator != null && !(isStarted = generator.startNext())) {
      stage = getNextStage(stage);
      generator = getNextGenerator();

      if (stage == Stage.SOURCE) {
        reschedule();
        return;
      }
    }
    // We've run out of stages and generators, give up.
    if ((stage == null || isCancelled) && !isStarted) {
      notifyFailed();
    }

    // Otherwise a generator started a new load and we expect to be called back in
    // onDataFetcherReady.
  }

  private void notifyFailed() {
    setNotifiedOrThrow();
    callback.onLoadFailed();
  }

  private void notifyComplete(Resource<R> resource) {
    setNotifiedOrThrow();
    callback.onResourceReady(resource);
  }

  private void setNotifiedOrThrow() {
    Preconditions.checkArgument(!isCallbackNotified, "Already notified callback");
    isCallbackNotified = true;
  }

  private Stage getNextStage(Stage current) {
    if (current == null) {
      return null;
    }
    switch (current) {
      case INITIALIZE:
        return diskCacheStrategy.decodeCachedResource()
            ? Stage.RESOURCE_CACHE : getNextStage(Stage.RESOURCE_CACHE);
      case RESOURCE_CACHE:
        return diskCacheStrategy.decodeCachedData()
            ? Stage.DATA_CACHE : getNextStage(Stage.DATA_CACHE);
      case DATA_CACHE:
        return Stage.SOURCE;
      default:
        return null;
    }
  }

  @Override
  public void reschedule() {
    runReason = RunReason.SWITCH_TO_SOURCE_SERVICE;
    callback.reschedule(this);
  }

  @Override
  public void onDataFetcherReady(Key sourceKey, Object data, DataFetcher<?> fetcher,
      DataSource dataSource) {
    this.currentSourceKey = sourceKey;
    this.currentData = data;
    this.currentFetcher = fetcher;
    this.currentDataSource = dataSource;
    if (Thread.currentThread() != currentThread) {
      runReason = RunReason.DECODE_DATA;
      callback.reschedule(this);
    } else {
      decodeFromRetrievedData();
    }
  }

  private void decodeFromRetrievedData() {
    if (Logs.isEnabled(Log.VERBOSE)) {
      logWithTimeAndKey("Retrieved data", startFetchTime,
          "data: " + currentData
          + ", cache key: " + currentSourceKey
          + ", fetcher: " + currentFetcher);
    }
    Resource<R> resource = decodeFromData(currentFetcher, currentData, currentDataSource);
    if (resource != null) {
      notifyComplete(resource);
      cleanup();
    } else {
      runGenerators();
    }
  }

  private void cleanup() {
    currentData = null;
    currentDataSource = null;
    currentFetcher = null;
    currentSourceKey = null;
    currentThread = null;
  }

  private <Data> Resource<R> decodeFromData(DataFetcher<?> fetcher, Data data,
      DataSource dataSource) {
    try {
      if (data == null) {
        return null;
      }
      long startTime = LogTime.getLogTime();
      Resource<R> result = decodeFromFetcher(data, dataSource);
      if (Logs.isEnabled(Log.VERBOSE)) {
        logWithTimeAndKey("Decoded result " + result, startTime);
      }
      return result;
    } finally {
      fetcher.cleanup();
    }
  }

  @SuppressWarnings("unchecked")
  private <Data> Resource<R> decodeFromFetcher(Data data, DataSource dataSource) {
    LoadPath<Data, ?, R> path = decodeHelper.getLoadPath((Class<Data>) data.getClass());
    if (path != null) {
      return runLoadPath(data, dataSource, path);
    } else {
      return null;
    }
  }

  private <Data, ResourceType> Resource<R> runLoadPath(Data data, DataSource dataSource,
      LoadPath<Data, ResourceType, R> path) {
    DataRewinder<Data> rewinder = glideContext.getRegistry().getRewinder(data);
    return path.load(rewinder, options, width, height,
        new DecodeCallback<ResourceType>(dataSource));
  }

  private void logWithTimeAndKey(String message, long startTime) {
    logWithTimeAndKey(message, startTime, null /*extraArgs*/);
  }

  private void logWithTimeAndKey(String message, long startTime, String extraArgs) {
    Logs.log(Log.VERBOSE,
        message + " in " + LogTime.getElapsedMillis(startTime) + ", load key: " + loadKey + (
            extraArgs != null ? ", " + extraArgs : "") + ", thread: " + Thread.currentThread()
            .getName());
  }


  class DecodeCallback<Z> implements DecodePath.DecodeCallback<Z> {

    private final DataSource dataSource;

    public DecodeCallback(DataSource dataSource) {
      this.dataSource = dataSource;
    }

    @Override
    public Resource<Z> onResourceDecoded(Resource<Z> decoded) {
      Class<Z> resourceSubClass = getResourceClass(decoded);
      Transformation<Z> appliedTransformation = null;
      Resource<Z> transformed = decoded;
      if (dataSource != DataSource.RESOURCE_DISK_CACHE) {
        appliedTransformation = decodeHelper.getTransformation(resourceSubClass);
        transformed = appliedTransformation.transform(decoded, width, height);
      }
      // TODO: Make this the responsibility of the Transformation.
      if (!decoded.equals(transformed)) {
        decoded.recycle();
      }

      final EncodeStrategy encodeStrategy;
      final ResourceEncoder<Z> encoder;
      if (decodeHelper.isResourceEncoderAvailable(transformed)) {
        encoder = decodeHelper.getResultEncoder(transformed);
        encodeStrategy = encoder.getEncodeStrategy(options);
      } else {
        encoder = null;
        encodeStrategy = EncodeStrategy.NONE;
      }

      long startEncodeTime = LogTime.getLogTime();
      boolean isFromAlternateCacheKey = !decodeHelper.isSourceKey(currentSourceKey);
      if (diskCacheStrategy.isResourceCacheable(isFromAlternateCacheKey, dataSource,
          encodeStrategy)) {
        if (encoder == null) {
          throw new Registry.NoResultEncoderAvailableException(transformed.get().getClass());
        }
        final Key key;
        if (encodeStrategy == EncodeStrategy.SOURCE) {
          key = new DataCacheKey(currentSourceKey, signature);
        } else if (encodeStrategy == EncodeStrategy.TRANSFORMED) {
          key = new ResourceCacheKey(currentSourceKey, signature, width, height,
              appliedTransformation, resourceSubClass, options);
        } else {
          throw new IllegalArgumentException("Unknown strategy: " + encodeStrategy);
        }

        diskCacheProvider.getDiskCache().put(key, new DataCacheWriter<>(encoder, transformed,
            options));
        if (Logs.isEnabled(Log.VERBOSE)) {
          logWithTimeAndKey("Encoded resource to cache", startEncodeTime,
              "cache key: " + key
              + ", encode strategy: " + encodeStrategy);
        }
      }
      return transformed;
    }

    @SuppressWarnings("unchecked")
    private Class<Z> getResourceClass(Resource<Z> resource) {
      return (Class<Z>) resource.get().getClass();
    }
  }

  interface Callback<R> {

    void onResourceReady(Resource<R> resource);

    void onLoadFailed();

    void reschedule(DecodeJob<?> job);
  }

  interface DiskCacheProvider {
    DiskCache getDiskCache();
  }
}
