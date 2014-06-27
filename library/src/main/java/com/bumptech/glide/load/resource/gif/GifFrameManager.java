package com.bumptech.glide.load.resource.gif;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import com.bumptech.glide.Glide;
import com.bumptech.glide.gifdecoder.GifDecoder;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.SkipCache;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.engine.cache.MemorySizeCalculator;
import com.bumptech.glide.load.model.NullEncoder;
import com.bumptech.glide.load.resource.NullDecoder;
import com.bumptech.glide.load.resource.bitmap.BitmapEncoder;
import com.bumptech.glide.load.resource.bitmap.StreamBitmapDecoder;
import com.bumptech.glide.request.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;

import java.io.InputStream;

class GifFrameManager {
    // 16ms per frame = 60fps
    static final long MIN_FRAME_DELAY = 16;
    private final MemorySizeCalculator calculator;
    private final GifFrameModelLoader frameLoader;
    private final GifFrameResourceDecoder frameResourceDecoder;
    private final ResourceDecoder<InputStream, Bitmap> cacheDecoder;
    private final GifDecoder decoder;
    private final Handler mainHandler;
    private final ResourceEncoder<Bitmap> encoder;
    private final Context context;
    private final NullEncoder<GifDecoder> sourceEncoder;
    private Transformation<Bitmap> transformation;
    private final int targetWidth;
    private final int targetHeight;
    private DelayTarget current;
    private DelayTarget next;
    private int frameSize = -1;

    public interface FrameCallback {
        public void onFrameRead(Bitmap frame);
    }

    public GifFrameManager(Context context, GifDecoder decoder, Transformation<Bitmap> transformation, int targetWidth,
            int targetHeight) {
        this(context, Glide.get(context).getBitmapPool(), decoder, new Handler(Looper.getMainLooper()), transformation,
                targetWidth, targetHeight);
    }

    public GifFrameManager(Context context, BitmapPool bitmapPool, GifDecoder decoder, Handler mainHandler,
            Transformation<Bitmap> transformation, int targetWidth, int targetHeight) {
        this.context = context;
        this.decoder = decoder;
        this.mainHandler = mainHandler;
        this.transformation = transformation;
        this.targetWidth = targetWidth;
        this.targetHeight = targetHeight;
        calculator = new MemorySizeCalculator(context);
        frameLoader = new GifFrameModelLoader();
        frameResourceDecoder = new GifFrameResourceDecoder(bitmapPool);
        sourceEncoder = NullEncoder.get();

        if (!decoder.isTransparent()) {
            // For non transparent gifs, we can beat the performance of our gif decoder for each frame by decoding jpegs
            // from disk.
            cacheDecoder = new StreamBitmapDecoder(context);
            encoder = new BitmapEncoder(Bitmap.CompressFormat.JPEG, 70);
        } else {
            // For transparent gifs, we would have to encode as pngs which is actually slower than our gif decoder so we
            // avoid writing frames to the disk cache entirely.
            cacheDecoder = NullDecoder.get();
            encoder = SkipCache.get();
        }
    }

    Transformation<Bitmap> getTransformation() {
        return transformation;
    }

    private int getEstimatedTotalFrameSize() {
        if (frameSize == -1) {
            return decoder.getDecodedFramesByteSizeSum();
        } else {
            return frameSize * decoder.getFrameCount();
        }
    }

    public void getNextFrame(FrameCallback cb) {
        decoder.advance();
        // We don't want to blow out the entire memory cache with frames of gifs, so try to set some
        // maximum size beyond which we will always just decode one frame at a time.
        boolean skipCache = getEstimatedTotalFrameSize() > calculator.getMemoryCacheSize() / 2;

        long targetTime = SystemClock.uptimeMillis() + (Math.max(MIN_FRAME_DELAY, decoder.getNextDelay()));
        next = new DelayTarget(cb, targetTime);

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
        private Bitmap resource;

        public DelayTarget(FrameCallback cb, long targetTime) {
            super(targetWidth, targetHeight);
            this.cb = cb;
            this.targetTime = targetTime;
        }

        @Override
        public void onResourceReady(final Bitmap resource, GlideAnimation<Bitmap> glideAnimation) {
            // Ignore allocationByteSize, we only want the minimum frame size.
            frameSize = resource.getHeight() * resource.getRowBytes();
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
