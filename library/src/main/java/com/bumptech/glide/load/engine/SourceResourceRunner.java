package com.bumptech.glide.load.engine;

import android.os.SystemClock;
import android.util.Log;
import com.bumptech.glide.load.CacheLoader;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.Encoder;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.engine.cache.DiskCache;
import com.bumptech.glide.load.engine.executor.Prioritized;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;
import com.bumptech.glide.request.ResourceCallback;

import java.io.InputStream;
import java.io.OutputStream;

/**
 *
 * @param <T> The type of the data the resource will be decoded from.
 * @param <Z> The type of the resource that will be decoded.
 * @param <R> The type of the resource that will be transcoded to from the decoded resource.
 */
public class SourceResourceRunner<T, Z, R> implements Runnable, DiskCache.Writer, Prioritized {
    private static final String TAG = "SourceRunner";
    private final EngineKey key;
    private final int width;
    private final int height;
    private final CacheLoader cacheLoader;
    private final ResourceDecoder<InputStream, Z> cacheDecoder;
    private final DataFetcher<T> fetcher;
    private final boolean cacheSource;
    private final Encoder<T> sourceEncoder;
    private final ResourceDecoder<T, Z> decoder;
    private final Transformation<Z> transformation;
    private final ResourceEncoder<Z> encoder;
    private final ResourceTranscoder<Z, R> transcoder;
    private final DiskCache diskCache;
    private final Priority priority;
    private final ResourceCallback cb;

    private Resource<Z> result;
    private volatile boolean isCancelled;

    public SourceResourceRunner(EngineKey key, int width, int height, CacheLoader cacheLoader,
            ResourceDecoder<InputStream, Z> cacheDecoder, DataFetcher<T> dataFetcher, boolean cacheSource,
            Encoder<T> sourceEncoder, ResourceDecoder<T, Z> decoder, Transformation<Z> transformation,
            ResourceEncoder<Z> encoder, ResourceTranscoder<Z, R> transcoder, DiskCache diskCache, Priority priority,
            ResourceCallback cb) {
        this.key = key;
        this.width = width;
        this.height = height;
        this.cacheLoader = cacheLoader;
        this.cacheDecoder = cacheDecoder;
        this.fetcher = dataFetcher;
        this.cacheSource = cacheSource;
        this.sourceEncoder = sourceEncoder;
        this.decoder = decoder;
        this.transformation = transformation;
        this.encoder = encoder;
        this.transcoder = transcoder;
        this.diskCache = diskCache;
        this.priority = priority;
        this.cb = cb;
    }

    public void cancel() {
        isCancelled = true;
        if (fetcher != null) {
            fetcher.cancel();
        }
    }

    @Override
    public void run() {
        if (isCancelled) {
            return;
        }

        try {
            long start = SystemClock.currentThreadTimeMillis();
            Resource<Z> decoded = cacheLoader.load(key.getOriginalKey(), cacheDecoder, width, height);

            if (decoded == null) {
                decoded = decodeFromSource();
                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log.v(TAG, "Decoded from source in " + (SystemClock.currentThreadTimeMillis() - start) + " cache");
                    start = SystemClock.currentThreadTimeMillis();
                }
            }

            if (decoded != null) {
                Resource<Z> transformed = transformation.transform(decoded, width, height);
                if (decoded != transformed) {
                    decoded.recycle();
                }
                result = transformed;
            }
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "transformed in " + (SystemClock.currentThreadTimeMillis() - start));
            }

            if (result != null) {
                diskCache.put(key, this);
                start = SystemClock.currentThreadTimeMillis();
                Resource<R> transcoded = transcoder.transcode(result);
                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log.d(TAG, "transcoded in " + (SystemClock.currentThreadTimeMillis() - start));
                }
                cb.onResourceReady(transcoded);
            } else {
                cb.onException(null);
            }

        } catch (Exception e) {
            cb.onException(e);
        }
    }

    private Resource<Z> encodeSourceAndDecodeFromCache(final T data) {
        diskCache.put(key.getOriginalKey(), new DiskCache.Writer() {
            @Override
            public boolean write(OutputStream os) {
                return sourceEncoder.encode(data, os);
            }
        });
        return cacheLoader.load(key.getOriginalKey(), cacheDecoder, width, height);
    }

    private Resource<Z> decodeFromSource() throws Exception {
        try {
            final T data = fetcher.loadData(priority);
            if (data != null) {
                if (cacheSource) {
                    return encodeSourceAndDecodeFromCache(data);
                } else {
                    return decoder.decode(data, width, height);
                }
            }
        } finally {
            fetcher.cleanup();
        }

        return null;
    }

    @Override
    public boolean write(OutputStream os) {
        long start = SystemClock.currentThreadTimeMillis();
        boolean success = encoder.encode(result, os);
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "wrote to disk cache in " + (SystemClock.currentThreadTimeMillis() - start));
        }
        return success;
    }

    @Override
    public int getPriority() {
        return priority.ordinal();
    }
}
