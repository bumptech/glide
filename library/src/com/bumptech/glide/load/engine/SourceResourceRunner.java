package com.bumptech.glide.load.engine;

import com.bumptech.glide.Priority;
import com.bumptech.glide.Resource;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.data.transcode.ResourceTranscoder;
import com.bumptech.glide.load.engine.cache.DiskCache;
import com.bumptech.glide.load.engine.executor.Prioritized;
import com.bumptech.glide.load.resource.ResourceFetcher;
import com.bumptech.glide.request.ResourceCallback;

import java.io.OutputStream;

/**
 *
 * @param <T> The type of the data the resource will be decoded from.
 * @param <Z> The type of the resource that will be decoded.
 * @param <R> The type of the resource that will be transcoded to from the decoded resource.
 */
public class SourceResourceRunner<T, Z, R> implements Runnable, DiskCache.Writer, Prioritized {
    private final Key key;
    private final int width;
    private final int height;
    private final ResourceFetcher<T> fetcher;
    private final ResourceDecoder<T, Z> decoder;
    private Transformation<Z> transformation;
    private final ResourceEncoder<Z> encoder;
    private ResourceTranscoder<Z, R> transcoder;
    private DiskCache diskCache;
    private Priority priority;
    private ResourceCallback cb;
    private Resource<Z> result;
    private volatile boolean isCancelled;

    public SourceResourceRunner(Key key, int width, int height, ResourceFetcher<T> resourceFetcher,
            ResourceDecoder<T, Z> decoder, Transformation<Z> transformation, ResourceEncoder<Z> encoder,
            ResourceTranscoder<Z, R> transcoder, DiskCache diskCache, Priority priority, ResourceCallback cb) {
        this.key = key;
        this.width = width;
        this.height = height;
        this.fetcher = resourceFetcher;
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
        fetcher.cancel();
    }

    @Override
    public void run() {
        if (isCancelled) {
            return;
        }

        try {
            T toDecode = fetcher.loadResource(priority);
            if (toDecode != null) {
                Resource<Z> decoded = null;
                try {
                    decoded = decoder.decode(toDecode, width, height);
                } finally {
                    fetcher.cleanup();
                }
                if (decoded != null) {
                    Resource<Z> transformed = transformation.transform(decoded, width, height);
                    if (decoded != transformed) {
                        decoded.recycle();
                    }
                    result = transformed;
                }
            }
            if (result != null) {
                diskCache.put(key, this);
                Resource<R> transcoded = transcoder.transcode(result);
                cb.onResourceReady(transcoded);
            } else {
                cb.onException(null);
            }

        } catch (Exception e) {
            cb.onException(e);
        }
    }

    @Override
    public void write(OutputStream os) {
        encoder.encode(result, os);
    }

    @Override
    public int getPriority() {
        return priority.ordinal();
    }
}
