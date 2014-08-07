package com.bumptech.glide.load.resource.gif;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import com.bumptech.glide.Glide;
import com.bumptech.glide.gifdecoder.GifDecoder;
import com.bumptech.glide.load.Encoder;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.engine.cache.MemorySizeCalculator;
import com.bumptech.glide.load.resource.NullDecoder;
import com.bumptech.glide.load.resource.NullEncoder;
import com.bumptech.glide.load.resource.NullResourceEncoder;
import com.bumptech.glide.load.resource.bitmap.BitmapEncoder;
import com.bumptech.glide.load.resource.bitmap.StreamBitmapDecoder;
import com.bumptech.glide.load.resource.file.FileToStreamDecoder;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;

import java.io.File;

class GifFrameManager {
    // 16ms per frame = 60fps
    static final long MIN_FRAME_DELAY = 16;
    private final MemorySizeCalculator calculator;
    private final GifFrameModelLoader frameLoader;
    private final GifFrameResourceDecoder frameResourceDecoder;
    private final ResourceDecoder<File, Bitmap> cacheDecoder;
    private final GifDecoder decoder;
    private final Handler mainHandler;
    private final ResourceEncoder<Bitmap> encoder;
    private final Context context;
    private final Encoder<GifDecoder> sourceEncoder;
    private final Transformation<Bitmap> transformation;
    private final int targetWidth;
    private final int targetHeight;
    private final int totalFrameSize;
    private DelayTarget current;
    private DelayTarget next;

    public interface FrameCallback {
        public void onFrameRead(Bitmap frame, int index);
    }

    public GifFrameManager(Context context, GifDecoder decoder, Transformation<Bitmap> transformation, int targetWidth,
            int targetHeight, int frameWidth, int frameHeight) {
        this(context, Glide.get(context).getBitmapPool(), decoder, new Handler(Looper.getMainLooper()), transformation,
                targetWidth, targetHeight, frameWidth, frameHeight);
    }

    public GifFrameManager(Context context, BitmapPool bitmapPool, GifDecoder decoder, Handler mainHandler,
            Transformation<Bitmap> transformation, int targetWidth, int targetHeight, int frameWidth, int frameHeight) {
        this.context = context;
        this.decoder = decoder;
        this.mainHandler = mainHandler;
        this.transformation = transformation;
        this.targetWidth = targetWidth;
        this.targetHeight = targetHeight;
        this.totalFrameSize = frameWidth * frameHeight * (decoder.isTransparent() ? 4 : 2);
        calculator = new MemorySizeCalculator(context);
        frameLoader = new GifFrameModelLoader();
        frameResourceDecoder = new GifFrameResourceDecoder(bitmapPool);
        sourceEncoder = NullEncoder.get();

        if (transformation == null) {
            throw new NullPointerException("Transformation must not be null");
        }

        if (!decoder.isTransparent()) {
            // For non transparent gifs, we can beat the performance of our gif decoder for each frame by decoding jpegs
            // from disk.
            cacheDecoder = new FileToStreamDecoder<Bitmap>(new StreamBitmapDecoder(context));
            encoder = new BitmapEncoder();
        } else {
            // For transparent gifs, we would have to encode as pngs which is actually slower than our gif decoder so we
            // avoid writing frames to the disk cache entirely.
            cacheDecoder = NullDecoder.get();
            encoder = NullResourceEncoder.get();
        }
    }

    Transformation<Bitmap> getTransformation() {
        return transformation;
    }

    public void getNextFrame(FrameCallback cb) {
        decoder.advance();

        /**
         * Note - Using the disk cache can potentially cause frames to be decoded incorrectly because the decoder is
         * sequential. If earlier frames are evicted for some reason, later ones may then not be decoded correctly.
         */

        // We don't want to blow out the entire memory cache with frames of gifs, so try to set some
        // maximum size beyond which we will always just decode one frame at a time.
        boolean skipCache = totalFrameSize > calculator.getMemoryCacheSize() / 2;
        // We can decode non transparent (cached as jpegs) frames more quickly from cache, but transparent
        // (cached as png) frames more quickly from the gif data.
        boolean skipDiskCache = decoder.isTransparent();

        long targetTime = SystemClock.uptimeMillis() + (Math.max(MIN_FRAME_DELAY, decoder.getNextDelay()));
        next = new DelayTarget(cb, targetTime);
        next.setFrameIndex(decoder.getCurrentFrameIndex());

        Glide.with(context)
                .using(frameLoader, GifDecoder.class)
                .load(decoder)
                .as(Bitmap.class)
                .sourceEncoder(sourceEncoder)
                .decoder(frameResourceDecoder)
                .cacheDecoder(cacheDecoder)
                .encoder(encoder)
                .transform(transformation)
                .skipMemoryCache(skipCache)
                .diskCacheStrategy(skipDiskCache ? DiskCacheStrategy.NONE : DiskCacheStrategy.RESULT)
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
        private FrameCallback cb;
        private long targetTime;
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
