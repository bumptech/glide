package com.bumptech.glide.load.resource.gif;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import com.bumptech.glide.Glide;
import com.bumptech.glide.gifdecoder.GifDecoder;
import com.bumptech.glide.load.Encoder;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.engine.cache.MemorySizeCalculator;
import com.bumptech.glide.load.resource.NullEncoder;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.util.Util;

class GifFrameManager {
    /** 60fps is {@value #MIN_FRAME_DELAY}ms per frame. */
    private static final long MIN_FRAME_DELAY = 1000 / 60;
    private final MemorySizeCalculator calculator;
    private final GifFrameModelLoader frameLoader;
    private final GifFrameResourceDecoder frameResourceDecoder;
    private final GifDecoder decoder;
    private final Handler mainHandler;
    private final Context context;
    private final Encoder<GifDecoder> sourceEncoder;
    private final Transformation<Bitmap>[] transformation;
    private final int targetWidth;
    private final int targetHeight;
    private final int totalFrameSize;
    private DelayTarget current;
    private DelayTarget next;

    public interface FrameCallback {
        void onFrameRead(Bitmap frame, int index);
    }

    public GifFrameManager(Context context, GifDecoder decoder, Transformation<Bitmap> transformation, int targetWidth,
            int targetHeight, int frameWidth, int frameHeight) {
        this(context, Glide.get(context).getBitmapPool(), decoder, new Handler(Looper.getMainLooper()), transformation,
                targetWidth, targetHeight, frameWidth, frameHeight);
    }

    @SuppressWarnings("unchecked")
    public GifFrameManager(Context context, BitmapPool bitmapPool, GifDecoder decoder, Handler mainHandler,
            Transformation<Bitmap> transformation, int targetWidth, int targetHeight, int frameWidth, int frameHeight) {
        if (transformation == null) {
            throw new NullPointerException("Transformation must not be null");
        }

        this.context = context;
        this.frameResourceDecoder = new GifFrameResourceDecoder(bitmapPool);
        this.decoder = decoder;
        this.mainHandler = mainHandler;
        this.transformation = new Transformation[] {transformation};
        this.targetWidth = targetWidth;
        this.targetHeight = targetHeight;
        this.totalFrameSize = Util.getBitmapByteSize(frameWidth, frameHeight, Bitmap.Config.ARGB_8888);

        this.calculator = new MemorySizeCalculator(context);
        this.frameLoader = new GifFrameModelLoader();
        this.sourceEncoder = NullEncoder.get();
    }

    Transformation<Bitmap> getTransformation() {
        return transformation[0];
    }

    public void getNextFrame(FrameCallback cb) {
        decoder.advance();

        // We don't want to blow out the entire memory cache with frames of gifs, so try to set some
        // maximum size beyond which we will always just decode one frame at a time.
        boolean skipCache = totalFrameSize > calculator.getMemoryCacheSize() / 2;

        long targetTime = SystemClock.uptimeMillis() + Math.max(MIN_FRAME_DELAY, decoder.getNextDelay());
        next = new DelayTarget(cb, targetTime);
        next.setFrameIndex(decoder.getCurrentFrameIndex());

        Glide.with(context)
                .using(frameLoader, GifDecoder.class)
                .load(decoder)
                .as(Bitmap.class)
                .sourceEncoder(sourceEncoder)
                .decoder(frameResourceDecoder)
                .transform(transformation)
                .skipMemoryCache(skipCache)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .into(next);
    }

    public void clear() {
        if (current != null) {
            mainHandler.removeCallbacks(current);
            Glide.clear(current);
        }
        if (next != null) {
            mainHandler.removeCallbacks(next);
            Glide.clear(next);
        }
    }

    class DelayTarget extends SimpleTarget<Bitmap> implements Runnable {
        private final FrameCallback cb;
        private final long targetTime;
        private Bitmap resource;
        private int index;

        public DelayTarget(FrameCallback cb, long targetTime) {
            super(targetWidth, targetHeight);
            this.cb = cb;
            this.targetTime = targetTime;
        }

        public void setFrameIndex(int index) {
            this.index = index;
        }

        @Override
        public void onResourceReady(final Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
            this.resource = resource;
            mainHandler.postAtTime(this, targetTime);
        }

        @Override
        public void run() {
            cb.onFrameRead(resource, index);
            if (current != null) {
                Glide.clear(current);
            }
            current = this;
        }
    }
}
