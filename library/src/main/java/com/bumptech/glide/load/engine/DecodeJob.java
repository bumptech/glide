package com.bumptech.glide.load.engine;

import android.util.Log;

import com.bumptech.glide.load.Encoder;
import com.bumptech.glide.load.Key;
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
 * A class responsible for decoding resources either from cached data or from the original source and applying
 * transformations and transcodes.
 *
 * @param <Z> The type of resource that will be decoded.
 * @param <R> The type of resource that will be transcoded from the decoded and transformed resource.
 */
class DecodeJob<Z, R> {
    private static final String TAG = "DecodeJob";
    private static final FileOpener DEFAULT_FILE_OPENER = new FileOpener();

    private final RequestContext<Z, R> requestContext;
    private final EngineKey loadKey;
    private final int width;
    private final int height;
    private final DiskCacheProvider diskCacheProvider;
    private final FileOpener fileOpener;

    private volatile boolean isCancelled;

    public DecodeJob(RequestContext<Z, R> requestContext, EngineKey loadKey, int width, int height,
            DiskCacheProvider diskCacheProvider) {
        this(requestContext, loadKey, width, height, diskCacheProvider, DEFAULT_FILE_OPENER);
    }

    // Visible for testing.
    DecodeJob(RequestContext<Z, R> requestContext, EngineKey loadKey, int width, int height,
            DiskCacheProvider diskCacheProvider, FileOpener fileOpener) {
        this.requestContext = requestContext;
        this.loadKey = loadKey;
        this.width = width;
        this.height = height;
        this.diskCacheProvider = diskCacheProvider;
        this.fileOpener = fileOpener;
    }

    /**
     * Returns a transcoded resource decoded from transformed resource data in the disk cache, or null if no such
     * resource exists.
     *
     * @throws Exception
     */
    public Resource<R> decodeResultFromCache() throws Exception {
        if (!requestContext.getDiskCacheStrategy().cacheResult()) {
            return null;
        }
        List<Class<?>> resourceClasses = requestContext.getRegisteredResourceClasses();
        for (Class<?> registeredResourceClass : resourceClasses) {
            Transformation<?> transformation = requestContext.getTransformation(registeredResourceClass);
            Key key = loadKey.getResultKey(transformation, registeredResourceClass);
            try {
                return decodeFromCache(loadKey, false /*cacheResult*/, false /*transformResult*/);
            } catch (Exception e) {
                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log.v(TAG, "Failed to find or decode resource from cache with key=" + key);
                }
            }
        }
        return null;
    }

    /**
     * Returns a transformed and transcoded resource decoded from source data in the disk cache, or null if no such
     * resource exists.
     *
     * @throws Exception
     */
    public Resource<R> decodeSourceFromCache() throws Exception {
        if (!requestContext.getDiskCacheStrategy().cacheSource()) {
            return null;
        }
        return decodeFromCache(loadKey.getOriginalKey(), requestContext.getDiskCacheStrategy().cacheResult(),
                true /*transformResult*/);
    }

    private Resource<R> decodeFromCache(Key key, boolean cacheResult, boolean transformResult) throws Exception {
        long startTime = LogTime.getLogTime();
        final Resource<R> result;
        File cacheFile = diskCacheProvider.getDiskCache().get(key);
        if (cacheFile != null) {
            result = decodeFromPaths(
                    requestContext.getDataFetchers(cacheFile, width, height),
                    requestContext.getSourceCacheLoadPaths(cacheFile), cacheResult, transformResult);
        } else {
            result = null;
        }
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            logWithTimeAndKey("Decoded from cache", startTime);
        }
        return result;
    }

    /**
     * Returns a transformed and transcoded resource decoded from source data, or null if no source data could be
     * obtained or no resource could be decoded.
     *
     * <p>
     *     Depending on the {@link com.bumptech.glide.load.engine.DiskCacheStrategy} used, source data is either decoded
     *     directly or first written to the disk cache and then decoded from the disk cache.
     * </p>
     *
     * @throws Exception
     */
    public Resource<R> decodeFromSource() throws Exception {
        if (requestContext.getDiskCacheStrategy().cacheSource()) {
            return cacheAndDecodeSource();
        } else {
            return decodeFromPaths(requestContext.getDataFetchers(), requestContext.getLoadPaths(),
                    requestContext.getDiskCacheStrategy().cacheResult(), true /*transformResult*/);
        }
    }

    private Resource<R> cacheAndDecodeSource() throws Exception {
        for (DataFetcher<?> fetcher : requestContext.getDataFetchers()) {
            if (fetcher == null) {
                continue;
            }
            Object data = fetcher.loadData(requestContext.getPriority());
            try {
                if (data == null) {
                    if (Log.isLoggable(TAG, Log.VERBOSE)) {
                        Log.v(TAG, "Failed to fetch data from: " + fetcher);
                    }
                    continue;
                }
                Encoder<Object> encoder = requestContext.getSourceEncoder(data);
                SourceWriter<Object> writer = new SourceWriter<Object>(encoder, data);
                diskCacheProvider.getDiskCache().put(loadKey.getOriginalKey(), writer);
                return decodeFromCache(loadKey.getOriginalKey(),
                        requestContext.getDiskCacheStrategy().cacheResult(), true /*transformResult*/);
            } finally {
                fetcher.cleanup();
            }
        }
        throw new IllegalStateException("No path found to load " + requestContext.getResourceClass() + " and "
                + requestContext.getTranscodeClass() + " from a cached File");
    }

    private Resource<R> decodeFromPaths(DataFetcherSet<?> fetchers, List<LoadPath<?, Z, R>> loadPaths, final boolean
            cacheResult, final
            boolean transformResult) throws Exception {
        for (LoadPath<?, Z, R> path : loadPaths) {
            Resource<R> result = runLoadPath(fetchers, path, cacheResult, transformResult);
            if (result != null) {
                return result;
            }
            if (isCancelled) {
                break;
            }
        }
        return null;
    }

    private <T> Resource<R> runLoadPath(DataFetcherSet<?> fetchers, LoadPath<T, Z, R> path, final boolean cacheResult,
            final boolean transformResult) throws Exception {
        DataFetcher<T> fetcher = fetchers.getFetcher(path.getDataClass());
        return path.load(fetcher, requestContext, width, height, new DecodePath.DecodeCallback<Z>() {
                @Override
                public Resource<Z> onResourceDecoded(final Resource<Z> resource) {
                    Resource<Z> result = resource;
                    Class<Z> resourceSubClass = (Class<Z>) result.get().getClass();
                    Transformation<Z> appliedTransformation = null;
                    if (transformResult) {
                        appliedTransformation = requestContext.getTransformation(resourceSubClass);
                        result = appliedTransformation.transform(resource, width, height);
                    }
                    if (cacheResult) {
                        Key resultCacheKey = loadKey.getResultKey(appliedTransformation, resourceSubClass);
                        diskCacheProvider.getDiskCache().put(resultCacheKey,
                                new SourceWriter<Resource<Z>>(requestContext.getResultEncoder(result), result));
                    }
                    return result;
                }
            });
    }

    public void cancel() {
        requestContext.getDataFetchers().cancel();
        isCancelled = true;
    }

    private void logWithTimeAndKey(String message, long startTime) {
        Log.v(TAG, message + " in " + LogTime.getElapsedMillis(startTime) + loadKey);
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
                success = encoder.encode(data, os);
            } catch (FileNotFoundException e) {
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "Failed to find file to write to disk cache", e);
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
            return success;
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
