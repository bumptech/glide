package com.bumptech.glide.load.engine;

import android.util.Log;

import com.bumptech.glide.Logs;
import com.bumptech.glide.Registry;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.EncodeStrategy;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.engine.cache.DiskCache;
import com.bumptech.glide.load.engine.executor.Prioritized;
import com.bumptech.glide.util.LogTime;

/**
 * A class responsible for decoding resources either from cached data or from the original source
 * and applying transformations and transcodes.
 *
 * @param <R> The type of resource that will be transcoded from the decoded and transformed
 *            resource.
 */
class DecodeJob<R> implements DataFetcherGenerator.FetcherReadyCallback,
    Runnable,
    Prioritized {
  private static final String TAG = "DecodeJob";

  private final RequestContext<?, R> requestContext;
  private final EngineKey loadKey;
  private final int width;
  private final int height;
  private final DiskCacheProvider diskCacheProvider;
  private final Callback<R> callback;

  private Stage stage;
  private RunReason runReason = RunReason.INITIALIZE;
  private volatile DataFetcherGenerator generator;

  private Thread currentThread;
  private Key currentSourceKey;
  private Object currentData;
  private DataSource currentDataSource;
  private DataFetcher<?> currentFetcher;
  private long startFetchTime;

  private volatile boolean isCancelled;

  public DecodeJob(RequestContext<?, R> requestContext, EngineKey loadKey, int width, int height,
      DiskCacheProvider diskCacheProvider, Callback<R> callback) {
    this.requestContext = requestContext;
    this.loadKey = loadKey;
    this.width = width;
    this.height = height;
    this.diskCacheProvider = diskCacheProvider;
    this.callback = callback;
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
  public int getPriority() {
    return requestContext.getPriority().ordinal();
  }

  @Override
  public void run() {
    // This should be much more fine grained, but since Java's thread pool implementation silently
    // swallows all otherwise fatal exceptions, this will at least make it obvious to developers
    // that something is failing.
    try {
      runWrapped();
    } catch (RuntimeException e) {
      if (Log.isLoggable(TAG, Log.ERROR)) {
        Log.e(TAG, "DecodeJob threw unexpectedly", e);
      }
      callback.onLoadFailed();
      throw e;
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
        return new ResourceCacheGenerator(width, height, diskCacheProvider.getDiskCache(),
            requestContext, this);
      case DATA_CACHE:
        return new DataCacheGenerator(requestContext.getCacheKeys(), width, height,
            diskCacheProvider.getDiskCache(), requestContext, this);
      case SOURCE:
        return new SourceGenerator<>(width, height, requestContext,
            diskCacheProvider.getDiskCache(), this);
      default:
        throw new IllegalStateException("Unrecognized stage: " + stage);
    }
  }

  private void runGenerators() {
    currentThread = Thread.currentThread();
    startFetchTime = LogTime.getLogTime();
    while (!isCancelled && generator != null && !generator.startNext()) {
      stage = getNextStage(stage);
      generator = getNextGenerator();

      if (stage == Stage.SOURCE) {
        runReason = RunReason.SWITCH_TO_SOURCE_SERVICE;
        callback.reschedule(this);
        return;
      }
    }
    // We've run out of stages and generators, give up.
    if (stage == null) {
      callback.onLoadFailed();
    }
    // Otherwise a generator started a new load and we expect to be called back in
    // onDataFetcherReady.
  }

  private Stage getNextStage(Stage current) {
    if (current == null) {
      return null;
    }
    DiskCacheStrategy strategy = requestContext.getDiskCacheStrategy();
    switch (current) {
      case INITIALIZE:
        return strategy.decodeCachedResource()
            ? Stage.RESOURCE_CACHE : getNextStage(Stage.RESOURCE_CACHE);
      case RESOURCE_CACHE:
        return strategy.decodeCachedData()
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
      callback.onResourceReady(resource);
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
    LoadPath<Data, ?, R> path = requestContext.getLoadPath((Class<Data>) data.getClass());
    if (path != null) {
      return runLoadPath(data, dataSource, path);
    } else {
      return null;
    }
  }

  private <Data, ResourceType> Resource<R> runLoadPath(Data data, DataSource dataSource,
      LoadPath<Data, ResourceType, R> path) {
    return path.load(data, requestContext, width, height,
        new DecodeCallback<ResourceType>(dataSource));
  }

  private void logWithTimeAndKey(String message, long startTime) {
    logWithTimeAndKey(message, startTime, null /*extraArgs*/);
  }

  private void logWithTimeAndKey(String message, long startTime, String extraArgs) {
    Logs.log(Log.VERBOSE, message + " in " + LogTime.getElapsedMillis(startTime)
        + ", load key: " + loadKey
        + (extraArgs != null ? ", " + extraArgs : "")
        + ", thread: " + Thread.currentThread().getName());
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
        appliedTransformation = requestContext.getTransformation(resourceSubClass);
        transformed = appliedTransformation.transform(decoded, width, height);
      }
      // TODO: Make this the responsibility of the Transformation.
      if (!decoded.equals(transformed)) {
        decoded.recycle();
      }

      final EncodeStrategy encodeStrategy;
      final ResourceEncoder<Z> encoder;
      if (requestContext.isResourceEncoderAvailable(transformed)) {
        encoder = requestContext.getResultEncoder(transformed);
        encodeStrategy = encoder.getEncodeStrategy(requestContext.getOptions());
      } else {
        encoder = null;
        encodeStrategy = EncodeStrategy.NONE;
      }

      long startEncodeTime = LogTime.getLogTime();
      DiskCacheStrategy diskCacheStrategy = requestContext.getDiskCacheStrategy();
      boolean isFromAlternateCacheKey = !requestContext.isSourceKey(currentSourceKey);
      if (diskCacheStrategy.isResourceCacheable(isFromAlternateCacheKey, dataSource,
          encodeStrategy)) {
        if (encoder == null) {
          throw new Registry.NoResultEncoderAvailableException(transformed.get().getClass());
        }
        final Key key;
        if (encodeStrategy == EncodeStrategy.SOURCE) {
          key = new DataCacheKey(currentSourceKey, requestContext.getSignature());
        } else if (encodeStrategy == EncodeStrategy.TRANSFORMED) {
          key = new ResourceCacheKey(currentSourceKey, requestContext.getSignature(), width, height,
              appliedTransformation, resourceSubClass, requestContext.getOptions());
        } else {
          throw new IllegalArgumentException("Unknown strategy: " + encodeStrategy);
        }

        diskCacheProvider.getDiskCache().put(key, new DataCacheWriter<>(encoder, transformed,
            requestContext.getOptions()));
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
