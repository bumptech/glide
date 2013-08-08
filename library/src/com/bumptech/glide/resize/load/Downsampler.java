package com.bumptech.glide.resize.load;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import com.bumptech.glide.resize.RecyclableBufferedInputStream;
import com.bumptech.glide.resize.bitmap_recycle.BitmapPool;

import java.io.IOException;

/**
 * A base class with methods for loading and decoding images from InputStreams.
 */
public abstract class Downsampler {
    private final String id = getClass().toString();

    /**
     * Load and scale the image uniformly (maintaining the image's aspect ratio) so that the dimensions of the image
     * will be greater than or equal to the given width and height.
     *
     */
    public static Downsampler AT_LEAST = new Downsampler() {
        @Override
        protected int getSampleSize(int inWidth, int inHeight, int outWidth, int outHeight) {
            return Math.min(inHeight / outHeight, inWidth / outWidth);
        }
    };

    /**
     * Load and scale the image uniformly (maintaining the image's aspect ratio) so that the dimensions of the image
     * will be less than or equal to the given width and height.
     *
     */
    public static Downsampler AT_MOST = new Downsampler() {
        @Override
        protected int getSampleSize(int inWidth, int inHeight, int outWidth, int outHeight) {
            return Math.max(inHeight / outHeight, inWidth / outWidth);
        }
    };


    /**
     * Load the image at its original size
     *
     */
    public static Downsampler NONE = new Downsampler() {
        @Override
        protected int getSampleSize(int inWidth, int inHeight, int outWidth, int outHeight) {
            return 0;
        }
    };

    private static final int MARK_POSITION = 1024 * 1024; //1mb

    /**
     * Load the image for the given InputStream. If a recycled Bitmap whose dimensions exactly match those of the image
     * for the given InputStream is available, the operation is much less expensive in terms of memory.
     *
     * Note - this method will throw an exception of a Bitmap with dimensions not matching those of the image for the
     * given InputStream is provided.
     *
     * @param bis An InputStream to the data for the image
     * @param options The options to pass to {@link BitmapFactory#decodeStream(java.io.InputStream, android.graphics.Rect, android.graphics.BitmapFactory.Options)}
     * @param pool A pool of recycled bitmaps
     * @param outWidth The width the final image should be close to
     * @param outHeight The height the final image should be close to
     * @return A new bitmap containing the image from the given InputStream, or recycle if recycle is not null
     */
    public Bitmap downsample(RecyclableBufferedInputStream bis, BitmapFactory.Options options, BitmapPool pool, int outWidth, int outHeight) {
        final int[] inDimens = getDimensions(bis, options);
        final int inWidth = inDimens[0];
        final int inHeight = inDimens[1];

        final int sampleSize = getSampleSize(inWidth, inHeight, outWidth, outHeight);

        //sample sizes <= 1 do nothing
        if (sampleSize > 1) {
            options.inSampleSize = sampleSize;
        } else {
            setInBitmap(options, pool.get(inWidth, inHeight));
        }

        return decodeStream(bis, options);
    }

    /**
     * Get some id that uniquely identifies the downsample for use as part of a cache key
     * @return A unique String
     */
    public String getId() {
        return id;
    }

    /**
     * Determine the amount of downsampling to use for a load given the dimensions of the image to be downsampled and
     * the dimensions of the view/target the image will be displayed in.
     *
     * @see BitmapFactory.Options#inSampleSize
     *
     * @param inWidth The width of the image to be downsampled
     * @param inHeight The height of the image to be downsampled
     * @param outWidth The width of the view/target the image will be displayed in
     * @param outHeight The height of the view/target the imag will be displayed in
     * @return An integer to pass in to {@link BitmapFactory#decodeStream(java.io.InputStream, android.graphics.Rect, android.graphics.BitmapFactory.Options)}
     */
    protected abstract int getSampleSize(int inWidth, int inHeight, int outWidth, int outHeight);

    /**
     * A method for getting the dimensions of an image from the given InputStream
     *
     * @param bis The InputStream representing the image
     * @param options The options to pass to {@link BitmapFactory#decodeStream(java.io.InputStream, android.graphics.Rect, android.graphics.BitmapFactory.Options)}
     * @return an array containing the dimensions of the image in the form {width, height}
     */
    public int[] getDimensions(RecyclableBufferedInputStream bis, BitmapFactory.Options options) {
        options.inJustDecodeBounds = true;
        decodeStream(bis, options);
        options.inJustDecodeBounds = false;
        return new int[] { options.outWidth, options.outHeight };
    }


    private Bitmap decodeStream(RecyclableBufferedInputStream bis, BitmapFactory.Options options) {
         if (options.inJustDecodeBounds) {
             bis.mark(MARK_POSITION); //this is large, but jpeg headers are not size bounded so we need
                         //something large enough to minimize the possibility of not being able to fit
                         //enough of the header in the buffer to get the image size so that we don't fail
                         //to load images. The BufferedInputStream will create a new buffer of 2x the
                         //original size each time we use up the buffer space without passing the mark so
                         //this is a maximum bound on the buffer size, not a default. Most of the time we
                         //won't go past our pre-allocated 16kb
         }

        final Bitmap result = BitmapFactory.decodeStream(bis, null, options);

        try {
            if (options.inJustDecodeBounds) {
                bis.reset();
                bis.clearMark();
            } else {
                bis.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    @TargetApi(11)
    private static void setInBitmap(BitmapFactory.Options options, Bitmap recycled) {
        if (Build.VERSION.SDK_INT >= 11) {
            options.inBitmap = recycled;
        }
    }
}
