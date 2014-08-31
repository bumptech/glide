package com.bumptech.glide.load.engine;

import android.os.SystemClock;
import android.util.Log;

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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * A background {@link java.lang.Runnable} responsible for loading a resource from its original data if the resource is
 * not in cache.
 *
 * @param <T> The type of the data the resource will be decoded from.
 * @param <Z> The type of the resource that will be decoded.
 * @param <R> The type of the resource that will be transcoded to from the decoded resource.
 */
class SourceResourceRunner<T, Z, R> implements Runnable, Prioritized {
    private static final WriterFactory DEFAULT_WRITER_FACTORY = new DefaultWriterFactory();

    private static final String TAG = "SourceRunner";
    private final EngineKey key;
    private final int width;
    private final int height;
    private final CacheLoader cacheLoader;
    private final ResourceDecoder<File, Z> cacheDecoder;
    private final DataFetcher<T> fetcher;
    private final Encoder<T> sourceEncoder;
    private final ResourceDecoder<T, Z> decoder;
    private final Transformation<Z> transformation;
    private final ResourceEncoder<Z> encoder;
    private final ResourceTranscoder<Z, R> transcoder;
    private final DiskCache diskCache;
    private final Priority priority;
    private DiskCacheStrategy diskCacheStrategy;
    private final ResourceCallback cb;
    private WriterFactory writerFactory;

    private volatile boolean isCancelled;

    public SourceResourceRunner(EngineKey key,
                         int width,
                         int height,
                         CacheLoader cacheLoader,
                         ResourceDecoder<File, Z> cacheDecoder,
                         DataFetcher<T> dataFetcher,
                         Encoder<T> sourceEncoder,
                         ResourceDecoder<T, Z> decoder,
                         Transformation<Z> transformation,
                         ResourceEncoder<Z> encoder,
                         ResourceTranscoder<Z, R> transcoder,
                         DiskCache diskCache,
                         Priority priority,
                         DiskCacheStrategy diskCacheStrategy,
                         ResourceCallback cb) {
        this(key, width, height, cacheLoader, cacheDecoder, dataFetcher, sourceEncoder, decoder,
                transformation, encoder, transcoder, diskCache, priority, diskCacheStrategy, cb,
                DEFAULT_WRITER_FACTORY);
    }

    SourceResourceRunner(EngineKey key,
                         int width,
                         int height,
                         CacheLoader cacheLoader,
                         ResourceDecoder<File, Z> cacheDecoder,
                         DataFetcher<T> dataFetcher,
                         Encoder<T> sourceEncoder,
                         ResourceDecoder<T, Z> decoder,
                         Transformation<Z> transformation,
                         ResourceEncoder<Z> encoder,
                         ResourceTranscoder<Z, R> transcoder,
                         DiskCache diskCache,
                         Priority priority,
                         DiskCacheStrategy diskCacheStrategy,
                         ResourceCallback cb,
                         WriterFactory writerFactory) {
        this.key = key;
        this.width = width;
        this.height = height;
        this.cacheLoader = cacheLoader;
        this.cacheDecoder = cacheDecoder;
        this.fetcher = dataFetcher;
        this.sourceEncoder = sourceEncoder;
        this.decoder = decoder;
        this.transformation = transformation;
        this.encoder = encoder;
        this.transcoder = transcoder;
        this.diskCache = diskCache;
        this.priority = priority;
        this.diskCacheStrategy = diskCacheStrategy;
        this.cb = cb;
        this.writerFactory = writerFactory;
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

        Resource<R> result = null;
        try {
            result = runWrapped();
        } catch (Exception e) {
            cb.onException(e);
        }

        if (result == null) {
            cb.onException(null);
        } else {
            cb.onResourceReady(result);
        }
    }

    private Resource<R> runWrapped() throws Exception {
        long start = SystemClock.currentThreadTimeMillis();
        Resource<Z> decoded = null;
        if (diskCacheStrategy.cacheSource()) {
            decoded = cacheLoader.load(key.getOriginalKey(), cacheDecoder, width, height);
        }

        if (decoded == null) {
            decoded = decodeFromSource();
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "Decoded from source in " + (SystemClock.currentThreadTimeMillis() - start) + " cache");
                start = SystemClock.currentThreadTimeMillis();
            }
        }

        Resource<Z> transformed = null;
        if (decoded != null) {
            transformed = transformation.transform(decoded, width, height);
            if (decoded != transformed) {
                decoded.recycle();
            }
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "transformed in " + (SystemClock.currentThreadTimeMillis() - start));
            }
        }

        Resource<R> transcoded = null;
        if (transformed != null) {
            if (diskCacheStrategy.cacheResult()) {
                diskCache.put(key, writerFactory.build(encoder, transformed));
            }
            start = SystemClock.currentThreadTimeMillis();
            transcoded = transcoder.transcode(transformed);
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.d(TAG, "transcoded in " + (SystemClock.currentThreadTimeMillis() - start));
            }
        }
        return transcoded;
    }

    private Resource<Z> decodeFromSource() throws Exception {
        try {
            final T data = fetcher.loadData(priority);
            if (data != null) {
                if (diskCacheStrategy.cacheSource()) {
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

    private Resource<Z> encodeSourceAndDecodeFromCache(final T data) {
        diskCache.put(key.getOriginalKey(), writerFactory.build(sourceEncoder, data));
        return cacheLoader.load(key.getOriginalKey(), cacheDecoder, width, height);
    }

    @Override
    public int getPriority() {
        return priority.ordinal();
    }

    private static class DefaultWriterFactory implements WriterFactory {

        @Override
        public <T> SourceWriter<T> build(Encoder<T> encoder, T data) {
            return new SourceWriter<T>(encoder, data);
        }
    }

    interface WriterFactory {
        public <T> SourceWriter<T> build(Encoder<T> encoder, T data);
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
                os = new FileOutputStream(file);
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
