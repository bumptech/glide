package com.bumptech.glide.load.resource.gif;

import android.content.Context;
import android.graphics.Bitmap;
import com.bumptech.glide.gifdecoder.GifDecoder;
import com.bumptech.glide.gifdecoder.GifHeader;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.resource.UnitTransformation;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;

import java.util.ArrayList;
import java.util.List;

/**
 * A container for data related to a particular GIF image that includes a partially decoded header and the bytes of
 * the compressed image that can be used together to decode individual frames of the GIF.
 */
public class GifData {
    private final Context context;
    private final GifHeader header;
    private final byte[] data;
    private final GifDecoderBitmapProvider bitmapProvider;
    private final String gifId;
    private final int targetWidth;
    private final int targetHeight;
    private final List<GifDrawable> drawables = new ArrayList<GifDrawable>();
    private Transformation<Bitmap> frameTransformation;

    public GifData(Context context, BitmapPool bitmapPool, String gifId, GifHeader header, byte[] data,
            int targetWidth, int targetHeight) {
        this.context = context;
        this.header = header;
        this.data = data;
        this.gifId = gifId;
        this.targetWidth = targetWidth;
        this.targetHeight = targetHeight;
        bitmapProvider = new GifDecoderBitmapProvider(bitmapPool);
    }

    /**
     * Returns the current non null frame {@link com.bumptech.glide.load.Transformation}.
     */
    @SuppressWarnings("unchecked")
    public Transformation<Bitmap> getFrameTransformation() {
        if (frameTransformation != null) {
            return frameTransformation;
        } else {
            return UnitTransformation.get();
        }
    }

    /**
     * Sets a {@link com.bumptech.glide.load.Transformation} that will be applied to each frame in the animation
     * individually.
     *
     * <p>
     *     Note - The frame transformations are not permanent in that they do not modify the underlying data,
     *     but only each frame as they are decoded. As a result these frame transformations cannot be cached and must
     *     be applied to the GifData whenever one is decoded, regardless of whether it came from the cache or not.
     * </p>
     *
     * @param transformation The transformation to apply.
     */
    public void setFrameTransformation(Transformation<Bitmap> transformation) {
        this.frameTransformation = transformation;
    }

    /**
     * Returns the size in bytes of the original compressed GIF image.
     */
    public int getByteSize() {
        return data.length;
    }

    /**
     * Returns the bytes of the original compressed GIF image.
     */
    public byte[] getData() {
        return data;
    }

    /**
     * Returns a new {@link com.bumptech.glide.load.resource.gif.GifDrawable} that can animate between the frames of
     * the wrapped GIF.
     */
    public GifDrawable getDrawable() {
        GifDrawable result = new GifDrawable(gifId, header, data, context, getFrameTransformation(), targetWidth,
                targetHeight, bitmapProvider);
        drawables.add(result);
        return result;
    }

    /**
     * Recycles the resources used by any {@link com.bumptech.glide.load.resource.gif.GifDrawable}s returned from
     * {@link #getDrawable()}.
     */
    public void recycle() {
        for (GifDrawable drawable : drawables) {
            drawable.stop();
            drawable.recycle();
        }
    }

    private static class GifDecoderBitmapProvider implements GifDecoder.BitmapProvider {
        private BitmapPool bitmapPool;

        public GifDecoderBitmapProvider(BitmapPool bitmapPool) {
            this.bitmapPool = bitmapPool;
        }

        @Override
        public Bitmap obtain(int width, int height, Bitmap.Config config) {
            return bitmapPool.get(width, height, config);
        }
    }
}
