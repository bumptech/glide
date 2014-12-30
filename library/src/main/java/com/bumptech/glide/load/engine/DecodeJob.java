package com.bumptech.glide.load.engine;

import android.util.Log;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.Encoder;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.data.DataFetcherSet;
import com.bumptech.glide.load.data.DataRewinder;
import com.bumptech.glide.load.engine.cache.DiskCache;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;
import com.bumptech.glide.request.RequestContext;
import com.bumptech.glide.request.RequestContext.NoDecoderAvailableException;
import com.bumptech.glide.request.RequestContext.NoResultEncoderAvailableException;
import com.bumptech.glide.request.RequestContext.NoSourceEncoderAvailableException;
import com.bumptech.glide.util.LogTime;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

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

    private final EngineKey resultKey;
    private final int width;
    private final int height;
    private final DiskCacheProvider diskCacheProvider;
    private final DataFetcherSet fetchers;
    private final Class<Z> resourceClass;
    private final RequestContext requestContext;
    private final Transformation<Z> transformation;
    private final ResourceTranscoder<Z, R> transcoder;
    private final DiskCacheStrategy diskCacheStrategy;
    private final Priority priority;
    private final FileOpener fileOpener;
    private volatile boolean isCancelled;

    public DecodeJob(Class<Z> resourceClass, EngineKey resultKey, int width, int height, DataFetcherSet fetchers,
            RequestContext requestContext, Transformation<Z> transformation, ResourceTranscoder<Z, R> transcoder,
            DiskCacheProvider diskCacheProvider, DiskCacheStrategy diskCacheStrategy, Priority priority) {
        this(resourceClass, resultKey, width, height, fetchers, requestContext, transformation, transcoder,
                diskCacheProvider, diskCacheStrategy, priority, DEFAULT_FILE_OPENER);
    }

    // Visible for testing.
    DecodeJob(Class<Z> resourceClass, EngineKey resultKey, int width, int height, DataFetcherSet fetchers,
            RequestContext requestContext, Transformation<Z> transformation, ResourceTranscoder<Z, R> transcoder,
            DiskCacheProvider diskCacheProvider, DiskCacheStrategy diskCacheStrategy, Priority priority, FileOpener
            fileOpener) {
        this.resultKey = resultKey;
        this.width = width;
        this.height = height;
        this.fetchers = fetchers;
        this.resourceClass = resourceClass;
        this.requestContext = requestContext;
        this.transformation = transformation;
        this.transcoder = transcoder;
        this.diskCacheProvider = diskCacheProvider;
        this.diskCacheStrategy = diskCacheStrategy;
        this.priority = priority;
        this.fileOpener = fileOpener;
    }

    /**
     * Returns a transcoded resource decoded from transformed resource data in the disk cache, or null if no such
     * resource exists.
     *
     * @throws Exception
     */
    public Resource<R> decodeResultFromCache() throws Exception {
        if (!diskCacheStrategy.cacheResult()) {
            return null;
        }

        long startTime = LogTime.getLogTime();
        Resource<Z> transformed = loadFromCache(resultKey);
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            logWithTimeAndKey("Decoded transformed from cache", startTime);
        }
        startTime = LogTime.getLogTime();
        Resource<R> result = transcode(transformed);
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            logWithTimeAndKey("Transcoded transformed from cache", startTime);
        }
        return result;
    }

    /**
     * Returns a transformed and transcoded resource decoded from source data in the disk cache, or null if no such
     * resource exists.
     *
     * @throws Exception
     */
    public Resource<R> decodeSourceFromCache() throws Exception {
        if (!diskCacheStrategy.cacheSource()) {
            return null;
        }

        long startTime = LogTime.getLogTime();
        Resource<Z> decoded = loadFromCache(resultKey.getOriginalKey());
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            logWithTimeAndKey("Decoded source from cache", startTime);
        }
        return transformEncodeAndTranscode(decoded);
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
        for (DataFetcher<?> fetcher : fetchers.getFetchers()) {
            try {
                Resource<Z> decoded = decodeSource(fetcher);
                if (decoded == null) {
                    continue;
                }
                return transformEncodeAndTranscode(decoded);
            } catch (NoDecoderAvailableException e) {
                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log.v(TAG, "Failed to find encoder/decoder for data type, continuing", e);
                }
            } catch (NoSourceEncoderAvailableException e) {
                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log.v(TAG, "Failed to find source encoder for data type, continuing", e);
                }
            } catch (NoResultEncoderAvailableException e) {
                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log.v(TAG, "Failed to find result encoder for resource type, continuing", e);
                }
            } finally {
                fetcher.cleanup();
            }
        }
        throw new IllegalStateException("Load failed, unable to obtain or unable to decode data into requested"
                + " resource type");
    }

    public void cancel() {
        fetchers.cancel();
        isCancelled = true;
    }

    private Resource<R> transformEncodeAndTranscode(Resource<Z> decoded) throws NoResultEncoderAvailableException {
        long startTime = LogTime.getLogTime();
        Resource<Z> transformed = transform(decoded);
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            logWithTimeAndKey("Transformed resource from source", startTime);
        }

        writeTransformedToCache(transformed);

        startTime = LogTime.getLogTime();
        Resource<R> result = transcode(transformed);
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            logWithTimeAndKey("Transcoded transformed from source", startTime);
        }
        return result;
    }

    private void writeTransformedToCache(Resource<Z> transformed) throws NoResultEncoderAvailableException {
        if (transformed == null || !diskCacheStrategy.cacheResult()) {
            return;
        }
        long startTime = LogTime.getLogTime();
        SourceWriter<Resource<Z>> writer = new SourceWriter<Resource<Z>>(requestContext.getResultEncoder(transformed),
                transformed);
        diskCacheProvider.getDiskCache().put(resultKey, writer);
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            logWithTimeAndKey("Wrote transformed from source to cache", startTime);
        }
    }

    private <T> Resource<Z> decodeSource(DataFetcher<T> fetcher) throws Exception {
        long startTime = LogTime.getLogTime();
        final T data = fetcher.loadData(priority);
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            logWithTimeAndKey("Fetched data", startTime);
        }
        if (isCancelled) {
            return null;
        }
        DataRewinder<T> rewinder = requestContext.getRewinder(data);
        try {
            return decodeFromSourceData(rewinder);
        } finally {
            rewinder.cleanup();
        }
    }

    private <T> Resource<Z> decodeFromSourceData(DataRewinder<T> rewinder) throws IOException,
            NoDecoderAvailableException,
            NoSourceEncoderAvailableException {
        final Resource<Z> decoded;
        if (diskCacheStrategy.cacheSource()) {
            decoded = cacheAndDecodeSourceData(rewinder);
        } else {
            ResourceDecoder<T, Z> decoder = requestContext.getDecoder(rewinder, resourceClass);
            long startTime = LogTime.getLogTime();
            T data = rewinder.rewindAndGet();
            decoded = decoder.decode(data, width, height);
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                logWithTimeAndKey("Decoded from source", startTime);
            }
        }
        return decoded;
    }

    private <T> Resource<Z> cacheAndDecodeSourceData(DataRewinder<T> rewinder) throws IOException,
            NoSourceEncoderAvailableException,
            NoDecoderAvailableException {
        long startTime = LogTime.getLogTime();

        T data = rewinder.rewindAndGet();
        Encoder<T> encoder = requestContext.getSourceEncoder(data);
        SourceWriter<T> writer = new SourceWriter<T>(encoder, data);
        diskCacheProvider.getDiskCache().put(resultKey.getOriginalKey(), writer);
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            logWithTimeAndKey("Wrote source to cache", startTime);
        }

        startTime = LogTime.getLogTime();
        Resource<Z> result = loadFromCache(resultKey.getOriginalKey());
        if (Log.isLoggable(TAG, Log.VERBOSE) && result != null) {
            logWithTimeAndKey("Decoded source from cache", startTime);
        }
        return result;
    }

    private Resource<Z> loadFromCache(Key key) throws IOException, NoDecoderAvailableException {
        File cacheFile = diskCacheProvider.getDiskCache().get(key);
        if (cacheFile == null) {
            return null;
        }
        DataRewinder<File> rewinder = requestContext.getRewinder(cacheFile);
        Resource<Z> result = null;
        try {
            ResourceDecoder<File, Z> decoder = requestContext.getDecoder(rewinder, resourceClass);
            result = decoder.decode(rewinder.rewindAndGet(), width, height);
        } finally {
            if (result == null) {
                diskCacheProvider.getDiskCache().delete(key);
            }
            rewinder.cleanup();
        }
        return result;
    }

    private Resource<Z> transform(Resource<Z> decoded) {
        if (decoded == null) {
            return null;
        }

        Resource<Z> transformed = transformation.transform(decoded, width, height);
        if (!decoded.equals(transformed)) {
            decoded.recycle();
        }
        return transformed;
    }

    private Resource<R> transcode(Resource<Z> transformed) {
        if (transformed == null) {
            return null;
        }
        return transcoder.transcode(transformed);
    }

    private void logWithTimeAndKey(String message, long startTime) {
        Log.v(TAG, message + " in " + LogTime.getElapsedMillis(startTime) + resultKey);
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
