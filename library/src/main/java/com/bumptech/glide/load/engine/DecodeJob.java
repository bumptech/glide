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
import com.bumptech.glide.load.data.DataFetcherSet;
import com.bumptech.glide.load.engine.cache.DiskCache;
import com.bumptech.glide.util.LogTime;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
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
class DecodeJob<R> implements DataFetcher.Callback<Object> {
  private static final FileOpener DEFAULT_FILE_OPENER = new FileOpener();

  private final RequestContext<R> requestContext;
  private final EngineKey loadKey;
  private final int width;
  private final int height;
  private final DiskCacheProvider diskCacheProvider;
  private final FileOpener fileOpener;

  private volatile boolean isCancelled;

  public DecodeJob(RequestContext<R> requestContext, EngineKey loadKey, int width, int height,
      DiskCacheProvider diskCacheProvider) {
    this(requestContext, loadKey, width, height, diskCacheProvider, DEFAULT_FILE_OPENER);
  }

  // Visible for testing.
  DecodeJob(RequestContext<R> requestContext, EngineKey loadKey, int width, int height,
      DiskCacheProvider diskCacheProvider, FileOpener fileOpener) {
    this.requestContext = requestContext;
    this.loadKey = loadKey;
    this.width = width;
    this.height = height;
    this.diskCacheProvider = diskCacheProvider;
    this.fileOpener = fileOpener;
  }

  /**
   * Returns a transcoded resource decoded from transformed resource data in the disk cache, or null
   * if no such resource exists.
   */
  public Resource<R> decodeFromCachedResource() {
    if (!requestContext.getDiskCacheStrategy().decodeCachedResource()) {
      return null;
    }

    long startTime = LogTime.getLogTime();
    List<Class<?>> resourceClasses = requestContext.getRegisteredResourceClasses();
    Resource<R> result = null;
    for (Class<?> registeredResourceClass : resourceClasses) {
      Transformation<?> transformation =
          requestContext.getTransformation(registeredResourceClass);
      Key key = loadKey.getResultKey(transformation, registeredResourceClass);
      result = decodeFromCache(key, DataSource.RESOURCE_DISK_CACHE);
      if (result != null) {
        break;
      }
    }
    if (Logs.isEnabled(Log.VERBOSE)) {
      logWithTimeAndKey("Finished decode from cached resource", startTime);
    }
    return result;
  }

  /**
   * Returns a transformed and transcoded resource decoded from source data in the disk cache, or
   * null if no such resource exists.
   */
  public Resource<R> decodeFromCachedData() {
    if (!requestContext.getDiskCacheStrategy().decodeCachedData()) {
      return null;
    }
    long startTime = LogTime.getLogTime();
    Resource<R> result = decodeFromCache(loadKey.getOriginalKey(), DataSource.DATA_DISK_CACHE);
    if (Logs.isEnabled(Log.VERBOSE)) {
      logWithTimeAndKey("Finished decode from cached data", startTime);
    }
    return result;
  }

  /**
   * Returns a transformed and transcoded resource decoded from source data, or null if no source
   * data could be obtained or no resource could be decoded. <p> Depending on the {@link
   * com.bumptech.glide.load.engine.DiskCacheStrategy} used, source data is either decoded directly
   * or first written to the disk cache and then decoded from the disk cache. </p>
   */
  public Resource<R> decodeFromSource() {
    long startTime = LogTime.getLogTime();
    DiskCacheStrategy diskCacheStrategy = requestContext.getDiskCacheStrategy();
    Resource<R> result = null;
    for (DataFetcher<?> fetcher : requestContext.getDataFetchers()) {
      if (fetcher == null) {
        continue;
      }

      DataSource dataSource = fetcher.getDataSource();
      if (diskCacheStrategy.cacheSource(dataSource)) {
        result = cacheAndDecodeSource(fetcher);
      } else {
        result = decodeFromFetcher(fetcher, fetcher.getDataSource());
      }

      if (result != null) {
        break;
      }
    }

    if (Logs.isEnabled(Log.VERBOSE)) {
      logWithTimeAndKey("Decoded from source completed", startTime);
    }

    return result;
  }

  private <Data> Resource<R> cacheAndDecodeSource(DataFetcher<Data> fetcher) {
    Data data = null;
    try {
      data = fetcher.loadData(requestContext.getPriority());
    } catch (IOException e) {
      if (Logs.isEnabled(Log.VERBOSE)) {
        Logs.log(Log.VERBOSE, "fetcher threw obtaining data", e);
      }
    }

    try {
      if (data == null) {
        return null;
      }

      Encoder<Data> encoder = requestContext.getSourceEncoder(data);
      SourceWriter<Data> writer = new SourceWriter<>(encoder, data);
      diskCacheProvider.getDiskCache().put(loadKey.getOriginalKey(), writer);
      return decodeFromCache(loadKey.getOriginalKey(), fetcher.getDataSource());
    } finally {
      fetcher.cleanup();
    }
  }

  private Resource<R> decodeFromCache(Key key, DataSource dataSource) {
    File cacheFile = diskCacheProvider.getDiskCache().get(key);
    if (cacheFile == null) {
      return null;
    }
    return decodeFromCacheFile(cacheFile, dataSource);
  }

  private Resource<R> decodeFromCacheFile(File cacheFile, DataSource dataSource) {
    DataFetcherSet<?> fetchers = requestContext.getDataFetchers(cacheFile, width, height);
    Resource<R> result = null;
    for (DataFetcher<?> fetcher : fetchers) {
      if (fetcher == null) {
        continue;
      }

      result = decodeFromFetcher(fetcher, dataSource);
      if (result != null || isCancelled) {
        break;
      }
    }
    return result;
  }

  private <Data> Resource<R> decodeFromFetcher(DataFetcher<Data> fetcher, DataSource dataSource) {
    LoadPath<Data, ?, R> path = requestContext.getLoadPath(fetcher.getDataClass());
    if (path != null) {
      return runLoadPath(fetcher, dataSource, path);
    } else {
      return null;
    }
  }

  private <Data, ResourceType> Resource<R> runLoadPath(DataFetcher<Data> fetcher,
      DataSource dataSource, LoadPath<Data, ResourceType, R> path) {
    Resource<R> result = null;
    try {
      Data data = fetcher.loadData(requestContext.getPriority());
      if (data == null) {
        return null;
      }
      result = path.load(data, requestContext, width, height,
          new DecodeCallback<ResourceType>(dataSource));
    } catch (IOException e) {
      if (Logs.isEnabled(Log.VERBOSE)) {
        Logs.log(Log.VERBOSE, "Fetcher failed: " + fetcher, e);
      }
    } finally {
      fetcher.cleanup();
    }
    return result;
  }

  public void cancel() {
    requestContext.getDataFetchers().cancel();
    isCancelled = true;
  }

  private void logWithTimeAndKey(String message, long startTime) {
    Logs.log(Log.VERBOSE, message + " in " + LogTime.getElapsedMillis(startTime) + ", key: "
        + loadKey);
  }

  @Override
  public void onDataReady(Object data) {

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
      if (!success && Logs.isEnabled(Log.VERBOSE)) {
        Logs.log(Log.VERBOSE, "Failed to write to cache");
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
          Logs.log(Log.VERBOSE, "Encoded resource to cache"
              + ", key: " + key
              + ", encode strategy: " + encodeStrategy
              + ", disk cache strategy: " + requestContext.getDiskCacheStrategy()
              + ", source: " + dataSource);
        }
      }
      return transformed;
    }

    @SuppressWarnings("unchecked")
    private Class<Z> getResourceClass(Resource<Z> resource) {
      return (Class<Z>) resource.get().getClass();
    }
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
