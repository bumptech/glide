package com.bumptech.glide.load.resource.gif;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.cache.MemorySizeCalculator;
import com.bumptech.glide.load.resource.NullCacheDecoder;
import com.bumptech.glide.load.resource.gif.decoder.GifDecoder;
import com.bumptech.glide.request.target.SimpleTarget;

class GifFrameManager {
    static final long MIN_FRAME_DELAY = 16;
    private final MemorySizeCalculator calculator;
    private final GifFrameLoader frameLoader;
    private final GifFrameResourceDecoder frameResourceDecoder;
    private final NullCacheDecoder<Bitmap> cacheDecoder;
    private final Handler mainHandler;
    private Transformation<Bitmap> transformation;
    private Context context;
    private DelayTarget current;
    private DelayTarget next;

    public interface FrameCallback {
        public void onFrameRead(Bitmap frame);
    }

    public GifFrameManager(Context context, Transformation<Bitmap> transformation) {
        this(context, new Handler(Looper.getMainLooper()), transformation);
    }

    public GifFrameManager(Context context, Handler mainHandler,
            Transformation<Bitmap> transformation) {
        this.context = context;
        this.mainHandler = mainHandler;
        this.transformation = transformation;
        calculator = new MemorySizeCalculator(context);
        frameLoader = new GifFrameLoader();
        frameResourceDecoder = new GifFrameResourceDecoder();
        cacheDecoder = NullCacheDecoder.get();
    }

    Transformation<Bitmap> getTransformation() {
        return transformation;
    }

    public void getNextFrame(final GifDecoder decoder, FrameCallback cb) {
        decoder.advance();
        boolean skipCache = decoder.getDecodedFrameByteSize() > calculator.getMemoryCacheSize() / 2;

        long targetTime = SystemClock.uptimeMillis() + (Math.min(MIN_FRAME_DELAY, decoder.getNextDelay()));
        next = new DelayTarget(decoder, cb, targetTime, mainHandler);
        Glide.with(context)
                .using(frameLoader, GifDecoder.class)
                .load(decoder)
                .as(Bitmap.class)
                .decoder(frameResourceDecoder)
                .cacheDecoder(cacheDecoder)
                .transform(transformation)
                .skipMemoryCache(skipCache)
                .skipDiskCache(true)
                .into(next);
    }

    public void clear() {
        if (current != null) {
            Glide.clear(current);
            mainHandler.removeCallbacks(current);
        }
        if (next != null) {
            Glide.clear(next);
            mainHandler.removeCallbacks(next);
        }
    }

    class DelayTarget extends SimpleTarget<Bitmap> implements Runnable {
        private FrameCallback cb;
        private long targetTime;
        private Handler mainHandler;
        private Bitmap resource;

        public DelayTarget(GifDecoder decoder, FrameCallback cb, long targetTime, Handler mainHandler) {
            super(decoder.getWidth(), decoder.getHeight());
            this.cb = cb;
            this.targetTime = targetTime;
            this.mainHandler = mainHandler;
        }

        @Override
        public void onResourceReady(final Bitmap resource) {
            this.resource = resource;
            mainHandler.postAtTime(this, targetTime);
            if (current != null) {
                Glide.clear(current);
            }
            current = next;
            next = null;
        }

        @Override
        public void run() {
            cb.onFrameRead(resource);
        }
    }
}
