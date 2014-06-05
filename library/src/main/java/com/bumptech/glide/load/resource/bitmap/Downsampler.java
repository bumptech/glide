package com.bumptech.glide.load.resource.bitmap;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.Log;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.util.ByteArrayPool;

import java.io.IOException;
import java.io.InputStream;
import java.util.EnumSet;
import java.util.Set;

/**
 * A base class with methods for loading and decoding images from InputStreams.
 */
public abstract class Downsampler implements BitmapDecoder<InputStream> {
    private static final String TAG = "Downsampler";

    private static final boolean CAN_RECYCLE = Build.VERSION.SDK_INT >= 11;
    private static final Set<ImageHeaderParser.ImageType> TYPES_THAT_USE_POOL = EnumSet.of(
            ImageHeaderParser.ImageType.JPEG, ImageHeaderParser.ImageType.PNG_A, ImageHeaderParser.ImageType.PNG);

    @TargetApi(11)
    private static BitmapFactory.Options getDefaultOptions() {
       BitmapFactory.Options decodeBitmapOptions = new BitmapFactory.Options();
       decodeBitmapOptions.inDither = false;
       decodeBitmapOptions.inScaled = false;
       decodeBitmapOptions.inSampleSize = 1;
       if (CAN_RECYCLE)  {
           decodeBitmapOptions.inMutable = true;
       }
       return decodeBitmapOptions;
    }

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

        @Override
        public String getId() {
            return "AT_LEAST.com.bumptech.glide.load.data.bitmap";
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

        @Override
        public String getId() {
            return "AT_MOST.com.bumptech.glide.load.data.bitmap";
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

        @Override
        public String getId() {
            return "NONE.com.bumptech.glide.load.data.bitmap";
        }
    };

    // 5MB. This is the max image header size we can handle, we preallocate a much smaller buffer but will resize up to
    // this amount if necessary.
    private static final int MARK_POSITION = 5 * 1024 * 1024;


    /**
     * Load the image for the given InputStream. If a recycled Bitmap whose dimensions exactly match those of the image
     * for the given InputStream is available, the operation is much less expensive in terms of memory.
     *
     * Note - this method will throw an exception of a Bitmap with dimensions not matching those of the image for the
     * given InputStream is provided.
     *
     * @param is An InputStream to the data for the image
     * @param pool A pool of recycled bitmaps
     * @param outWidth The width the final image should be close to
     * @param outHeight The height the final image should be close to
     * @return A new bitmap containing the image from the given InputStream, or recycle if recycle is not null
     */
    @Override
    public Bitmap decode(InputStream is, BitmapPool pool, int outWidth, int outHeight, DecodeFormat decodeFormat) {
        final ByteArrayPool byteArrayPool = ByteArrayPool.get();
        byte[] bytesForOptions = byteArrayPool.getBytes();
        byte[] bytesForStream = byteArrayPool.getBytes();
        RecyclableBufferedInputStream bis = new RecyclableBufferedInputStream(is, bytesForStream);
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

        final BitmapFactory.Options options = getDefaultOptions();
        options.inTempStorage = bytesForOptions;

        final int[] inDimens = getDimensions(bis, options);
        final int inWidth = inDimens[0];
        final int inHeight = inDimens[1];

        final int degreesToRotate = TransformationUtils.getExifOrientationDegrees(orientation);
        final int sampleSize;
        if (degreesToRotate == 90 || degreesToRotate == 270) {
            // If we're rotating the image +-90 degrees, we need to downsample accordingly so the image width is
            // decreased to near our target's height and the image height is decreased to near our target width.
            sampleSize = getSampleSize(inHeight, inWidth, outWidth, outHeight);
        } else {
            sampleSize = getSampleSize(inWidth, inHeight, outWidth, outHeight);
        }

        final Bitmap downsampled = downsampleWithSize(bis, options, pool, inWidth, inHeight, sampleSize, decodeFormat);

        Bitmap rotated = null;
        if (downsampled != null) {
            rotated = TransformationUtils.rotateImageExif(downsampled, pool, orientation);

            if (downsampled != rotated && !pool.put(downsampled)) {
                downsampled.recycle();
            }
        }

        byteArrayPool.releaseBytes(bytesForOptions);
        byteArrayPool.releaseBytes(bytesForStream);
        return rotated;
    }

    protected Bitmap downsampleWithSize(RecyclableBufferedInputStream bis, BitmapFactory.Options options,
            BitmapPool pool, int inWidth, int inHeight, int sampleSize, DecodeFormat decodeFormat) {
        // Prior to KitKat, the inBitmap size must exactly match the size of the bitmap we're decoding.
        Bitmap.Config config = getConfig(bis, decodeFormat);
        options.inSampleSize = sampleSize;
        options.inPreferredConfig = config;
        if (options.inSampleSize == 1 || Build.VERSION.SDK_INT >= 19) {
            if (shouldUsePool(bis)) {
                setInBitmap(options, pool.get(inWidth, inHeight, config));
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
            final ImageHeaderParser.ImageType type = new ImageHeaderParser(bis).getType();
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

    private Bitmap.Config getConfig(RecyclableBufferedInputStream bis, DecodeFormat format) {
        if (format == DecodeFormat.ALWAYS_ARGB_8888) {
            return Bitmap.Config.ARGB_8888;
        }

        boolean hasAlpha = false;
        bis.mark(1024); //we probably only need 25, but this is safer (particularly since the buffer size is > 1024)
        try {
            hasAlpha = new ImageHeaderParser(bis).hasAlpha();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                bis.reset();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return hasAlpha ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565;
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
