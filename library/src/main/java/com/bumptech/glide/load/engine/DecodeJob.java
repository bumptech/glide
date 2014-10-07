package com.bumptech.glide.load.engine;

import android.os.SystemClock;
import android.util.Log;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.Encoder;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.engine.cache.DiskCache;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;
import com.bumptech.glide.provider.DataLoadProvider;

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
 * @param <A> The type of the source data the resource can be decoded from.
 * @param <T> The type of resource that will be decoded.
 * @param <Z> The type of resource that will be transcoded from the decoded and transformed resource.
 */
class DecodeJob<A, T, Z> {
    private static final String TAG = "DecodeJob";

    private final EngineKey resultKey;
    private final int width;
    private final int height;
    private final DataFetcher<A> fetcher;
    private final DataLoadProvider<A, T> loadProvider;
    private final Transformation<T> transformation;
    private final ResourceTranscoder<T, Z> transcoder;
    private final DiskCacheStrategy diskCacheStrategy;
    private final DiskCache diskCache;
    private final Priority priority;
    private volatile boolean isCancelled;

    public DecodeJob(EngineKey resultKey, int width, int height, DataFetcher<A> fetcher,
            DataLoadProvider<A, T> loadProvider, Transformation<T> transformation, ResourceTranscoder<T, Z> transcoder,
            DiskCache diskCache, DiskCacheStrategy diskCacheStrategy, Priority priority) {
        this.resultKey = resultKey;
        this.width = width;
        this.height = height;
        this.fetcher = fetcher;
        this.loadProvider = loadProvider;
        this.transformation = transformation;
        this.transcoder = transcoder;
        this.diskCacheStrategy = diskCacheStrategy;
        this.diskCache = diskCache;
        this.priority = priority;
    }

    /**
     * Returns a transcoded resource decoded from transformed resource data in the disk cache, or null if no such
     * resource exists.
     *
     * @throws Exception
     */
    public Resource<Z> decodeResultFromCache() throws Exception {
        if (!diskCacheStrategy.cacheResult()) {
            return null;
        }

        Resource<T> transformed = loadFromCache(resultKey);
        return transcode(transformed);
    }

    /**
     * Returns a transformed and transcoded resource decoded from source data in the disk cache, or null if no such
     * resource exists.
     *
     * @throws Exception
     */
    public Resource<Z> decodeSourceFromCache() throws Exception {
        if (!diskCacheStrategy.cacheSource()) {
            return null;
        }

        Resource<T> decoded = loadFromCache(resultKey.getOriginalKey());
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
    public Resource<Z> decodeFromSource() throws Exception {
        Resource<T> decoded = decodeSource();
        return transformEncodeAndTranscode(decoded);
    }

    public void cancel() {
        fetcher.cancel();
        isCancelled = true;
    }

    private Resource<Z> transformEncodeAndTranscode(Resource<T> decoded) {
        Resource<T> transformed = transform(decoded);
        writeTransformedToCache(transformed);
        return transcode(transformed);
    }

    private void writeTransformedToCache(Resource<T> transformed) {
        if (transformed == null || !diskCacheStrategy.cacheResult()) {
            return;
        }
        SourceWriter<Resource<T>> writer = new SourceWriter<Resource<T>>(loadProvider.getEncoder(), transformed);
        diskCache.put(resultKey, writer);
    }

    private Resource<T> decodeSource() throws Exception {
        Resource<T> decoded = null;
        try {
            final A data = fetcher.loadData(priority);
            if (isCancelled) {
                return null;
            }
            decoded = decodeFromSourceData(data);
        } finally {
            fetcher.cleanup();
        }
        return decoded;
    }

    private Resource<T> decodeFromSourceData(A data) throws IOException {
        final Resource<T> decoded;
        if (diskCacheStrategy.cacheSource()) {
            decoded = cacheAndDecodeSourceData(data);
        } else {
            decoded = loadProvider.getSourceDecoder().decode(data, width, height);
        }
        return decoded;
    }

    private Resource<T> cacheAndDecodeSourceData(A data) throws IOException {
        SourceWriter<A> writer = new SourceWriter<A>(loadProvider.getSourceEncoder(), data);
        diskCache.put(resultKey.getOriginalKey(), writer);
        return loadFromCache(resultKey.getOriginalKey());
    }

    private Resource<T> loadFromCache(Key key) throws IOException {
        File cacheFile = diskCache.get(key);
        if (cacheFile == null) {
            return null;
        }

        Resource<T> result = null;
        try {
            result = loadProvider.getCacheDecoder().decode(cacheFile, width, height);
        } finally {
            if (result == null) {
                diskCache.delete(key);
            }
        }
        return result;
    }

    private Resource<T> transform(Resource<T> decoded) {
        if (decoded == null) {
            return null;
        }

        Resource<T> transformed = transformation.transform(decoded, width, height);
        if (!decoded.equals(transformed)) {
            decoded.recycle();
        }
        return transformed;
    }

    private Resource<Z> transcode(Resource<T> transformed) {
        if (transformed == null) {
            return null;
        }
        return transcoder.transcode(transformed);
    }

    static class SourceWriter<T> implements DiskCache.Writer {

        private final Encoder<T> encoder;
        private final T data;

        public SourceWriter(Encoder<T> encoder, T data) {
            this.encoder = encoder;
            this.data = data;
        }

        @Override
        public boolean write(File file) {
            long start = SystemClock.currentThreadTimeMillis();
            boolean success = false;
            OutputStream os = null;
            try {
                os = new BufferedOutputStream(new FileOutputStream(file));
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
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "wrote to disk cache in " + (SystemClock.currentThreadTimeMillis() - start));
            }
            return success;
        }
    }
}
