package com.bumptech.glide.resize.load;

import static com.bumptech.glide.resize.load.ImageHeaderParser.ImageType;
import static com.bumptech.glide.resize.load.ImageHeaderParser.ImageType.PNG_A;
import static com.bumptech.glide.resize.load.ImageHeaderParser.ImageType.JPEG;
import static com.bumptech.glide.resize.load.ImageHeaderParser.ImageType.PNG;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.Log;
import com.bumptech.glide.resize.RecyclableBufferedInputStream;
import com.bumptech.glide.resize.bitmap_recycle.BitmapPool;
import java.io.IOException;
import java.util.EnumSet;
import java.util.Set;

/**
 * A base class with methods for loading and decoding images from InputStreams.
 */
public abstract class Downsampler {
    private static final String TAG = "Downsampler";

    private static final Set<ImageType> TYPES_THAT_USE_POOL = EnumSet.of(JPEG, PNG_A, PNG);
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

    private static final int MARK_POSITION = 5 * 1024 * 1024; //5mb, max possible, not preallocated

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
        bis.mark(MARK_POSITION);
        int orientation = 0;
        try {
            orientation = new ImageHeaderParser(bis).getOrientation();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            bis.reset();
        } catch (IOException e) {
            e.printStackTrace();
        }

        final int[] inDimens = getDimensions(bis, options);
        final int inWidth = inDimens[0];
        final int inHeight = inDimens[1];

        final int degreesToRotate = ImageResizer.getExifOrientationDegrees(orientation);
        final int sampleSize;
        if (degreesToRotate == 90 || degreesToRotate == 270) {
            //if we're rotating the image +-90 degrees, we need to downsample accordingly so the image width is
            //decreased to near our target's height and the image height is decreased to near our target width
            sampleSize = getSampleSize(inHeight, inWidth, outWidth, outHeight);
        } else {
            sampleSize = getSampleSize(inWidth, inHeight, outWidth, outHeight);
        }

        final Bitmap downsampled = downsampleWithSize(bis, options, pool, inWidth, inHeight, sampleSize);
        if (downsampled == null) {
            throw new IllegalArgumentException("Unable to decode image sample size: " + sampleSize + " inWidth: "
                    + inWidth + " inHeight: " + inHeight);
        }
        final Bitmap rotated = ImageResizer.rotateImageExif(downsampled, pool, orientation);

        if (downsampled != rotated && !pool.put(downsampled)) {
            downsampled.recycle();
        }

        return rotated;
    }

    protected Bitmap downsampleWithSize(RecyclableBufferedInputStream bis, BitmapFactory.Options options,
            BitmapPool pool, int inWidth, int inHeight, int sampleSize) {
        // Prior to KitKat, the inBitmap size must exactly match the size of the bitmap we're decoding.
        options.inSampleSize = sampleSize;
        if (options.inSampleSize == 1 || Build.VERSION.SDK_INT >= 19) {
            if (shouldUsePool(bis)) {
                setInBitmap(options, pool.get(inWidth, inHeight, getConfig(bis)));
            }
        }
        return decodeStream(bis, options);
    }

    private boolean shouldUsePool(RecyclableBufferedInputStream bis) {
        // On KitKat+, any bitmap can be used to decode any other bitmap.
        if (Build.VERSION.SDK_INT >= 19) {
            return true;
        }

        bis.mark(1024);
        try {
            final ImageType type = new ImageHeaderParser(bis).getType();
            // cannot reuse bitmaps when decoding images that are not PNG or JPG.
            // look at : https://groups.google.com/forum/#!msg/android-developers/Mp0MFVFi1Fo/e8ZQ9FGdWdEJ
            return TYPES_THAT_USE_POOL.contains(type);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                bis.reset();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private Bitmap.Config getConfig(RecyclableBufferedInputStream bis) {
        Bitmap.Config result = Bitmap.Config.RGB_565;
        bis.mark(1024); //we probably only need 25, but this is safer (particularly since the buffer size is > 1024)
        try {
            result = new ImageHeaderParser(bis).hasAlpha() ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                bis.reset();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
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
            if (Log.isLoggable(TAG, Log.ERROR)) {
                Log.e(TAG, "Exception loading inDecodeBounds=" + options.inJustDecodeBounds
                        + " sample=" + options.inSampleSize, e);
            }
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
