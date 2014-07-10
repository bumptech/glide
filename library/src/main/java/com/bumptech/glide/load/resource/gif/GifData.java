package com.bumptech.glide.load.resource.gif;

import android.content.Context;
import android.graphics.Bitmap;

import com.bumptech.glide.gifdecoder.GifDecoder;
import com.bumptech.glide.gifdecoder.GifHeader;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.UnitTransformation;
import com.bumptech.glide.load.resource.bitmap.BitmapResource;

import java.util.ArrayList;
import java.util.List;

/**
 * A container for data related to a particular GIF image that includes a partially decoded header and the bytes of
 * the compressed image that can be used together to decode individual frames of the GIF.
 */
public class GifData {
    private static final GifDrawableFactory GIF_DRAWABLE_FACTORY = new DefaultGifDrawableFactory();
    private final Context context;
    private final BitmapPool bitmapPool;
    private final GifHeader header;
    private final byte[] data;
    private final GifDecoderBitmapProvider bitmapProvider;
    private final String gifId;
    /** The target dimensions we should pass to Glide to use when loading individual frames */
    private final int targetWidth;
    private final int targetHeight;
    private final GifDrawableFactory factory;
    private final List<GifDrawable> drawables = new ArrayList<GifDrawable>();

    private Transformation<Bitmap> frameTransformation;
    /** The final dimensions of the transformed frames */
    private int frameHeight;
    private int frameWidth;

    public GifData(Context context, BitmapPool bitmapPool, String gifId, GifHeader header, byte[] data,
            int targetWidth, int targetHeight) {
        this(context, bitmapPool, gifId, header, data, targetWidth, targetHeight, GIF_DRAWABLE_FACTORY);
    }

    GifData(Context context, BitmapPool bitmapPool, String gifId, GifHeader header, byte[] data,
            int targetWidth, int targetHeight, GifDrawableFactory factory) {
        this.context = context;
        this.bitmapPool = bitmapPool;
        this.header = header;
        this.data = data;
        this.gifId = gifId;
        this.targetWidth = targetWidth;
        this.targetHeight = targetHeight;
        this.factory = factory;
        bitmapProvider = new GifDecoderBitmapProvider(bitmapPool);

        frameWidth = header.getWidth();
        frameHeight = header.getHeight();
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

        // The drawable needs to be initialized with the correct width and height in order for a view displaying it
        // to end up with the right dimensions. Since our transformations may arbitrarily modify the dimensions of
        // our gif, here we create a stand in for a frame and pass it to the transformation to see what the final
        // transformed dimensions will be so that our drawable can report the correct intrinsict width and height.
        Bitmap toTest = bitmapPool.get(header.getWidth(), header.getHeight(), Bitmap.Config.RGB_565);
        if (toTest == null) {
            toTest = Bitmap.createBitmap(header.getWidth(), header.getHeight(), Bitmap.Config.RGB_565);
        }
        Resource<Bitmap> bitmapResource = new BitmapResource(toTest, bitmapPool);
        Resource<Bitmap> transformed = transformation.transform(bitmapResource, targetWidth, targetHeight);
        if (bitmapResource != transformed) {
            bitmapResource.recycle();
        }
        Bitmap bitmap = transformed.get();
        frameWidth = bitmap.getWidth();
        frameHeight = bitmap.getHeight();
        transformed.recycle();
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
        GifDrawable result = factory.build(context, bitmapProvider, getFrameTransformation(), targetWidth, targetHeight,
                gifId, header, data, frameWidth, frameHeight);
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

    interface GifDrawableFactory {
        public GifDrawable build(Context context, GifDecoder.BitmapProvider provider,
                Transformation<Bitmap> frameTransformation, int targetWidth, int targetHeight, String gifId,
                GifHeader header, byte[] data, int frameWidth, int frameHeight);
    }

    private static class DefaultGifDrawableFactory implements GifDrawableFactory {

        @Override
        public GifDrawable build(Context context, GifDecoder.BitmapProvider provider, Transformation<Bitmap>
                frameTransformation, int targetWidth, int targetHeight, String gifId, GifHeader header, byte[] data,
                int frameWidth, int frameHeight) {
            return new GifDrawable(context, provider, frameTransformation, targetWidth, targetHeight, gifId, header,
                    data, frameWidth, frameHeight);
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
