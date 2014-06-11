package com.bumptech.glide.load.resource.gif;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.engine.cache.MemorySizeCalculator;
import com.bumptech.glide.load.resource.bitmap.StreamBitmapDecoder;
import com.bumptech.glide.request.target.SimpleTarget;

import java.util.UUID;

class GifFrameManager {
    static final long MIN_FRAME_DELAY = 16;
    private final String id;
    private final MemorySizeCalculator calculator;
    private BitmapPool bitmapPool;
    private final Handler mainHandler;
    private Transformation<Bitmap> transformation;
    private Context context;
    private DelayTarget current;
    private DelayTarget next;

    public interface FrameCallback {
        public void onFrameRead(Bitmap frame);
    }

    public GifFrameManager(Context context, Transformation<Bitmap> transformation) {
        this(context, UUID.randomUUID().toString(), Glide.get(context).getBitmapPool(),
                new Handler(Looper.getMainLooper()), transformation);
    }

    public GifFrameManager(Context context, String id, BitmapPool bitmapPool, Handler mainHandler,
            Transformation<Bitmap> transformation) {
        this.context = context;
        this.id = id;
        this.bitmapPool = bitmapPool;
        this.mainHandler = mainHandler;
        this.transformation = transformation;
        calculator = new MemorySizeCalculator(context);
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
                .using(new GifFrameLoader(id), GifDecoder.class)
                .load(decoder)
                .as(Bitmap.class)
                .decoder(new GifFrameResourceDecoder())
                .cacheDecoder(new StreamBitmapDecoder(bitmapPool))
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
