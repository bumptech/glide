package com.bumptech.glide.load.resource.gif;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import com.bumptech.glide.GenericRequestBuilder;
import com.bumptech.glide.Glide;
import com.bumptech.glide.gifdecoder.GifDecoder;
import com.bumptech.glide.load.Encoder;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.NullEncoder;
import com.bumptech.glide.load.resource.UnitTransformation;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.util.UUID;

class GifFrameManager {
    private final GifDecoder decoder;
    private final Handler mainHandler;
    private final int targetWidth;
    private final int targetHeight;
    private final FrameSignature signature;
    private final GenericRequestBuilder<GifDecoder, GifDecoder, Bitmap, Bitmap> requestBuilder;
    private boolean isLoadInProgress;
    private DelayTarget current;
    private DelayTarget next;
    private Transformation<Bitmap> transformation = UnitTransformation.get();

    public interface FrameCallback {
        void onFrameRead(int index);
    }

    public GifFrameManager(Context context, GifDecoder decoder, int targetWidth, int targetHeight) {
        this(context, Glide.get(context).getBitmapPool(), decoder, new Handler(Looper.getMainLooper()), targetWidth,
                targetHeight);
    }

    public GifFrameManager(Context context, BitmapPool bitmapPool, GifDecoder decoder, Handler mainHandler,
                           int targetWidth, int targetHeight) {

        this.decoder = decoder;
        this.mainHandler = mainHandler;
        this.targetWidth = targetWidth;
        this.targetHeight = targetHeight;
        this.signature = new FrameSignature();

        GifFrameResourceDecoder frameResourceDecoder = new GifFrameResourceDecoder(bitmapPool);
        GifFrameModelLoader frameLoader = new GifFrameModelLoader();
        Encoder<GifDecoder> sourceEncoder = NullEncoder.get();

        requestBuilder = Glide.with(context)
                .using(frameLoader, GifDecoder.class)
                .from(GifDecoder.class)
                .as(Bitmap.class)
                .signature(signature)
                .sourceEncoder(sourceEncoder)
                .decoder(frameResourceDecoder)
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.NONE);
    }

    public void setFrameTransformation(Transformation<Bitmap> transformation) {
        if (transformation == null) {
            throw new NullPointerException("Transformation must not be null");
        }
        this.transformation = transformation;
    }

    @SuppressWarnings("unchecked")
    public void getNextFrame(FrameCallback cb) {
        if (isLoadInProgress) {
            return;
        }
        isLoadInProgress = true;

        decoder.advance();

        long targetTime = SystemClock.uptimeMillis() + decoder.getNextDelay();
        next = new DelayTarget(cb, targetTime);
        next.setFrameIndex(decoder.getCurrentFrameIndex());

        // Use an incrementing signature to make sure we never hit an active resource that matches one of our frames.
        signature.increment();
        requestBuilder
                .load(decoder)
                .transform(transformation)
                .into(next);
    }

    public Bitmap getCurrentFrame() {
        return current != null ? current.resource : null;
    }

    public void clear() {
        isLoadInProgress = false;
        if (current != null) {
            Glide.clear(current);
            mainHandler.removeCallbacks(current);
            current = null;
        }
        if (next != null) {
            Glide.clear(next);
            mainHandler.removeCallbacks(next);
            next = null;
        }

        decoder.resetFrameIndex();
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
            isLoadInProgress = false;
            cb.onFrameRead(index);
            if (current != null) {
                // TODO: figure out why this is necessary and fix it. See issue #219.
                final DelayTarget recycleCurrent = current;
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Glide.clear(recycleCurrent);
                    }
                });
            }
            current = this;
        }

        @Override
        public void onLoadCleared(Drawable placeholder) {
            resource = null;
        }
    }

    private static class FrameSignature implements Key {
        private final UUID uuid;
        private int id;

        public FrameSignature() {
            this.uuid = UUID.randomUUID();
        }

        public void increment() {
            id++;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof FrameSignature) {
                FrameSignature other = (FrameSignature) o;
                return other.uuid.equals(uuid) && id == other.id;
            }
            return false;
        }

        @Override
        public int hashCode() {
            int result = uuid.hashCode();
            result = 31 * result + id;
            return result;
        }

        @Override
        public void updateDiskCacheKey(MessageDigest messageDigest) throws UnsupportedEncodingException {
            throw new UnsupportedOperationException("Not implemented");
        }
    }
}
