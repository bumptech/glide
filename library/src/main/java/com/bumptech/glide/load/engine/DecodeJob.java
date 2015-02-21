package com.bumptech.glide.load.engine;

import android.util.Log;

import com.bumptech.glide.Logs;
import com.bumptech.glide.Registry;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.EncodeStrategy;
import com.bumptech.glide.load.Encoder;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.engine.cache.DiskCache;
import com.bumptech.glide.load.engine.executor.Prioritized;
import com.bumptech.glide.util.LogTime;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;

/**
 * A class responsible for decoding resources either from cached data or from the original source
 * and applying transformations and transcodes.
 *
 * TODO: handle multiple different IDs from different DataFetchers.
 *
 * @param <R> The type of resource that will be transcoded from the decoded and transformed
 *            resource.
 */
class DecodeJob<R> implements DataFetcher.DataCallback<Object>,
    Runnable,
    Prioritized {
  private static final FileOpener DEFAULT_FILE_OPENER = new FileOpener();
  private static final Resource<?> DECODE_FROM_CACHED_SOURCE_SIGNAL = new Resource<Object>() {
    @Override
    public Class<Object> getResourceClass() {
      return null;
    }

    @Override
    public Object get() {
      return null;
    }

    @Override
    public int getSize() {
      return 0;
    }

    @Override
    public void recycle() {
      // Do nothing.
    }
  };

  private final RequestContext<R> requestContext;
  private final EngineKey loadKey;
  private final int width;
  private final int height;
  private final DiskCacheProvider diskCacheProvider;
  private final Callback<R> callback;
  private final FileOpener fileOpener;

  private Stage stage = Stage.RESOURCE_CACHE;
  private Thread currentThread;
  private Iterator<DataFetcher<?>> sourceFetchers;
  private Iterator<DataFetcher<?>> fetchers;
  private Object data;
  private DataFetcher<?> fetcher;

  private volatile boolean isCancelled;

  public DecodeJob(RequestContext<R> requestContext, EngineKey loadKey, int width, int height,
      DiskCacheProvider diskCacheProvider, Callback<R> callback) {
    this(requestContext, loadKey, width, height, diskCacheProvider, callback, DEFAULT_FILE_OPENER);
  }

  // Visible for testing.
  DecodeJob(RequestContext<R> requestContext, EngineKey loadKey, int width, int height,
      DiskCacheProvider diskCacheProvider, Callback<R> callback, FileOpener fileOpener) {
    this.requestContext = requestContext;
    this.loadKey = loadKey;
    this.width = width;
    this.height = height;
    this.diskCacheProvider = diskCacheProvider;
    this.callback = callback;
    this.fileOpener = fileOpener;
  }

  private enum Stage {
    /** Decode from a cached resource. */
    RESOURCE_CACHE,
    /** Decode from cached source data. */
    DATA_CACHE,
    /** Decode from retrieved source. */
    SOURCE,
    /** Decode from retrieved source that was written directly to cache. */
    SOURCE_FROM_CACHE,
  }

  @Override
  public int getPriority() {
    return requestContext.getPriority().ordinal();
  }

  /**
   * Attempts to decode a request with any provided information and the current set of registered
   * components.
   *
   * <p> Each load runs through following stages:
   * 1. Check for any available transformed resource data encoded in the disk cache.
   * 2. Check for any available original source data encoded in the disk cache.
   * 3. Fetch and decode source data. </p>
   *
   * <p> If any stage provides a valid resource, we break out of the process early. </p>
   *
   * <p> For each stage:
   * 1. Obtain a set of DataFetchers that can obtain data
   * 2. For each obtained fetcher:
   * 3. Call the fetcher to load data (and post any result back to our executor)
   * 4. Attempt to decode the retrieved data. </p>
   *
   * <p> When decoding from source data, for any available
   * {@link com.bumptech.glide.load.data.DataFetcher} the
   * {@link com.bumptech.glide.load.engine.DiskCacheStrategy} may indicate that data should be first
   * written to disk before being decoded. In that case, we pause our iteration over
   * source fetchers and begin to iterate over fetchers to retrieve data from the newly cached
   * source data. If a resource is successfully decoded, we return early. If not, we resume
   * iterating over the source fetchers. </p>
   */
  @Override
  public void run() {
    // 4. We've retrieved some data from a single DataFetcher, try to decode it.
    if (fetcher != null) {
      Resource<R> resource = decodeFromData(fetcher, data);
      if (DECODE_FROM_CACHED_SOURCE_SIGNAL.equals(resource)) {
        sourceFetchers = fetchers;
        fetchers = null;
        stage = Stage.SOURCE_FROM_CACHE;
      } else if (resource != null) {
        callback.onResourceReady(resource);
        return;
      }

      fetcher = null;
      data = null;
    }

    // Once we've retrieved data, allow the decode to continue, but don't allow any subsequent
    // fetches or decodes.
    if (isCancelled) {
      return;
    }

    if (fetchers != null) {
      // 2. We have a set of fetchers we're iterating over.
      if (startNextFetch()) {
        // 3. We've started a fetch for the next DataFetcher in the set.
        return;
      } else {
        // There are no more fetchers, on to the next stage.
        fetchers = null;
      }
    } else {
      // 1. We just started a new stage and need to obtain a set of data fetchers for that stage.
       if (stage == Stage.RESOURCE_CACHE) {
        fetchers = getResourceCacheFetchers();
      } else if (stage == Stage.DATA_CACHE || stage == Stage.SOURCE_FROM_CACHE) {
        fetchers = getDataCacheFetchers();
      } else if (stage == Stage.SOURCE) {
        fetchers = requestContext.getDataFetchers().iterator();
      }
    }

    if (fetchers == null) {
      // We've just finished a stage (unsuccessfully) and need to move on to the next one.
      if (stage == Stage.RESOURCE_CACHE) {
        stage = Stage.DATA_CACHE;
      } else if (stage == Stage.DATA_CACHE) {
        stage = Stage.SOURCE;
      } else if (stage == Stage.SOURCE_FROM_CACHE) {
        stage = Stage.SOURCE;
        fetchers = sourceFetchers;
        sourceFetchers = null;
      } else {
        stage = null;
      }

      if (stage != null) {
        // Make sure we run on our source thread pool if we're decoding from source.
        if (stage == Stage.SOURCE) {
          callback.reschedule(this);
        } else {
          run();
        }
      } else {
        callback.onLoadFailed();
      }
    } else {
      // We've just started a new stage and obtained a set of fetchers and should iterate.
      run();
    }
  }

  @Override
  public void onDataReady(Object data) {
    this.data = data;
    if (!Thread.currentThread().equals(currentThread)) {
      callback.reschedule(this);
    } else {
      run();
    }
  }

  public void cancel() {
    requestContext.getDataFetchers().cancel();
    isCancelled = true;
  }

  private Iterator<DataFetcher<?>> getResourceCacheFetchers() {
    if (!requestContext.getDiskCacheStrategy().decodeCachedResource()) {
      return null;
    }

    long startTime = LogTime.getLogTime();
    Iterator<DataFetcher<?>> cacheFetchers = null;
    List<Class<?>> resourceClasses = requestContext.getRegisteredResourceClasses();
    for (Class<?> registeredResourceClass : resourceClasses) {
      Transformation<?> transformation =
          requestContext.getTransformation(registeredResourceClass);
      Key key = loadKey.getResultKey(transformation, registeredResourceClass);
      File cacheFile = diskCacheProvider.getDiskCache().get(key);
      if (cacheFile != null) {
        cacheFetchers = requestContext.getDataFetchers(cacheFile, width, height).iterator();
        break;
      }
    }
    if (Logs.isEnabled(Log.VERBOSE)) {
      logWithTimeAndKey("Retrieved resource cache fetchers", startTime);
    }
    return cacheFetchers;
  }

  private Iterator<DataFetcher<?>> getDataCacheFetchers() {
    if (!requestContext.getDiskCacheStrategy().decodeCachedData()) {
      return null;
    }

    long startTime = LogTime.getLogTime();
    File cacheFile = diskCacheProvider.getDiskCache().get(loadKey.getOriginalKey());
    Iterator<DataFetcher<?>> result = null;
    if (cacheFile != null) {
      result =  requestContext.getDataFetchers(cacheFile, width, height).iterator();
    }
    if (Logs.isEnabled(Log.VERBOSE)) {
      logWithTimeAndKey("Retrieved data cache fetchers", startTime);
    }
    return result;
  }

  private boolean startNextFetch() {
    while (fetchers.hasNext()) {
      DataFetcher<?> next = fetchers.next();
      if (next == null) {
        continue;
      }

      long startTime = LogTime.getLogTime();
      LoadPath<?, ?, R> path = requestContext.getLoadPath(next.getDataClass());
      if (path == null) {
        continue;
      }

      try {
        currentThread = Thread.currentThread();
        fetcher = next;
        next.loadData(requestContext.getPriority(), this);
        if (Logs.isEnabled(Log.VERBOSE)) {
          logWithTimeAndKey("started fetch/decode for data", startTime);
        }
        return true;
      } catch (IOException e) {
        if (Logs.isEnabled(Log.DEBUG)) {
          Logs.log(Log.DEBUG, "Fetcher failed: " + next, e);
        }
      }
    }
    return false;
  }

  private DataSource getDataSource(DataFetcher<?> fetcher) {
    switch (stage) {
      case RESOURCE_CACHE:
        return DataSource.RESOURCE_DISK_CACHE;
      case DATA_CACHE:
        return DataSource.DATA_DISK_CACHE;
      default:
        return fetcher.getDataSource();
    }
  }

  private <Data> Resource<R> decodeFromData(DataFetcher<?> fetcher, Data data) {
    try {
      if (data == null) {
        return null;
      }
      long startTime = LogTime.getLogTime();
      DataSource dataSource = getDataSource(fetcher);

      if (stage == Stage.SOURCE && requestContext.getDiskCacheStrategy().cacheSource(dataSource)) {
        Encoder<Data> encoder = requestContext.getSourceEncoder(data);
        SourceWriter<Data> writer = new SourceWriter<>(encoder, data);
        diskCacheProvider.getDiskCache().put(loadKey.getOriginalKey(), writer);
        if (Logs.isEnabled(Log.VERBOSE)) {
          logWithTimeAndKey("Finished encode source to cache", startTime);
        }
        return getDecodeFromCachedSourceSignal();
      } else {
        Resource<R> result = decodeFromFetcher(data, dataSource);
        if (Logs.isEnabled(Log.VERBOSE)) {
          logWithTimeAndKey("Decoded result " + result, startTime);
        }
        return result;
      }
    } finally {
      fetcher.cleanup();
    }
  }

  @SuppressWarnings("unchecked")
  private static <R> Resource<R> getDecodeFromCachedSourceSignal() {
    return (Resource<R>) DECODE_FROM_CACHED_SOURCE_SIGNAL;
  }

  @SuppressWarnings("unchecked")
  private <Data> Resource<R> decodeFromFetcher(Data data,
      DataSource dataSource) {
    LoadPath<Data, ?, R> path = requestContext.getLoadPath((Class<Data>) data.getClass());
    if (path != null) {
      return runLoadPath(data, dataSource, path);
    } else {
      return null;
    }
  }

  private <Data, ResourceType> Resource<R> runLoadPath(Data data,
      DataSource dataSource, LoadPath<Data, ResourceType, R> path) {
    return path.load(data, requestContext, width, height,
        new DecodeCallback<ResourceType>(dataSource));
  }

  private void logWithTimeAndKey(String message, long startTime) {
    Logs.log(Log.VERBOSE, message + " in " + LogTime.getElapsedMillis(startTime) + ", key: "
        + loadKey + " thread: " + Thread.currentThread().getName());
  }

  class SourceWriter<DataType> implements DiskCache.Writer {

    private final Encoder<DataType> encoder;
    private final DataType data;

    public SourceWriter(Encoder<DataType> encoder, DataType data) {
      this.encoder = encoder;
      this.data = data;
    }

    @Override
    public boolean write(File file) {
      boolean success = false;
      OutputStream os = null;
      try {
        os = fileOpener.open(file);
        success = encoder.encode(data, os, requestContext.getOptions());
      } catch (FileNotFoundException e) {
        if (Logs.isEnabled(Log.DEBUG)) {
          Logs.log(Log.DEBUG, "Failed to find file to write to disk cache", e);
        }
      } finally {
        if (os != null) {
          try {
            os.close();
          } catch (IOException e) {
            // Do nothing.
          }
        }
      }
      if (!success && Logs.isEnabled(Log.DEBUG)) {
        Logs.log(Log.DEBUG, "Failed to write to cache");
      }
      return success;
    }
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
      if (requestContext.getDiskCacheStrategy().cacheResult(dataSource, encodeStrategy)) {
        if (encoder == null) {
          throw new Registry.NoResultEncoderAvailableException(transformed.get().getClass());
        }
        final Key key;
        if (encodeStrategy == EncodeStrategy.SOURCE) {
          key = loadKey.getOriginalKey();
        } else if (encodeStrategy == EncodeStrategy.TRANSFORMED) {
          key = loadKey.getResultKey(appliedTransformation, resourceSubClass);
        } else {
          throw new IllegalArgumentException("Unknown strategy: " + encodeStrategy);
        }

        diskCacheProvider.getDiskCache().put(key, new SourceWriter<>(encoder, transformed));
        if (Logs.isEnabled(Log.VERBOSE)) {
          logWithTimeAndKey("Encoded resource to cache", startEncodeTime);
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

  static class FileOpener {
    public OutputStream open(File file) throws FileNotFoundException {
      return new BufferedOutputStream(new FileOutputStream(file));
    }
  }
}
