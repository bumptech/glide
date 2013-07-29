/*
 * Copyright (c) 2012 Bump Technologies Inc. All rights reserved.
 */
package com.bumptech.glide.resize;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.ExifInterface;
import android.os.Build;
import com.bumptech.glide.resize.bitmap_recycle.BitmapPool;
import com.bumptech.glide.resize.bitmap_recycle.BitmapPoolAdapter;
import com.bumptech.glide.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.Queue;

/**
 * A class for synchronously resizing bitmaps with or without Bitmaps to reuse
 */
public class ImageResizer {
    private static final int TEMP_BYTES_SIZE = 16 * 1024; //16kb
    private static final int MARK_POSITION = 1024 * 1024; //1mb
    private static final boolean CAN_RECYCLE = Build.VERSION.SDK_INT >= 11;
    private final Queue<byte[]> tempQueue = new LinkedList<byte[]>();
    private final BitmapPool bitmapPool;

    private final BitmapFactory.Options defaultOptions;

    @TargetApi(11)
    public static BitmapFactory.Options getDefaultOptions() {
       BitmapFactory.Options decodeBitmapOptions = new BitmapFactory.Options();
       decodeBitmapOptions.inDither = false;
       decodeBitmapOptions.inScaled = false;
       decodeBitmapOptions.inPreferredConfig = Bitmap.Config.RGB_565;
       decodeBitmapOptions.inSampleSize = 1;
       if (CAN_RECYCLE)  {
           decodeBitmapOptions.inMutable = true;
       }
       return decodeBitmapOptions;
    }

    /**
     * Creates a new resizer that will not recycle Bitmaps
     */
    @SuppressWarnings("unused")
    public ImageResizer() {
        this(null, null);
    }

    @SuppressWarnings("unused")
    public ImageResizer(BitmapPool bitmapPool) {
        this(bitmapPool, null);
    }

    @SuppressWarnings("unused")
    public ImageResizer(BitmapFactory.Options options) {
        this(null, options);
    }

    private abstract class ImageDownsampler {
        public Bitmap load(InputStream is, int width, int height) {
            byte[] bytes = getTempBytes();
            RecyclableBufferedInputStream bis = new RecyclableBufferedInputStream(is, bytes);
            final int[] inDimens = getDimens(bis, width, height);

            // inSampleSize prefers multiples of 2, but we prefer to prioritize memory savings
            final int sampleSize = getSampleSize(inDimens[0], inDimens[1], width, height);

            final BitmapFactory.Options decodeBitmapOptions;
            if (sampleSize > 1) {
                decodeBitmapOptions = getOptions();
                decodeBitmapOptions.inSampleSize = sampleSize;
            } else {
                decodeBitmapOptions = getOptions(getRecycled(inDimens));
            }

            Bitmap result = decodeStream(bis, decodeBitmapOptions);
            releaseTempBytes(bytes);
            return result;
        }

        protected abstract int getSampleSize(int inWidth, int inHeight, int outWidth, int outHeight);

        protected int[] getDimens(RecyclableBufferedInputStream bis, int inWidth, int inHeight) {
            return getDimensions(bis);
        }
    }

    private final ImageDownsampler atLeastDownsampler = new ImageDownsampler() {

        @Override
        protected int getSampleSize(int inWidth, int inHeight, int outWidth, int outHeight) {
            // inSampleSize prefers multiples of 2, but we prefer to prioritize memory savings
            return Math.min(inHeight / outHeight, inWidth / outWidth);
        }
    };

    private final ImageDownsampler atMostDownsampler = new ImageDownsampler() {
        @Override
        protected int getSampleSize(int inWidth, int inHeight, int outWidth, int outHeight) {
            return Math.max(inHeight / outHeight, inWidth / outWidth);
        }
    };

    private final ImageDownsampler asIsDownsampler = new ImageDownsampler() {
        @Override
        protected int getSampleSize(int inWidth, int inHeight, int outWidth, int outHeight) {
            return 0;
        }
    };

    private final ImageDownsampler fixedAsIsDownsampler = new ImageDownsampler() {
        @Override
        protected int getSampleSize(int inWidth, int inHeight, int outWidth, int outHeight) {
            return 0;
        }

        @Override
        protected int[] getDimens(RecyclableBufferedInputStream bis, int inWidth, int inHeight) {
            return new int[] { inWidth, inHeight };

        }
    };
    /**
     * Creates a new resizer that will attempt to recycle {@link android.graphics.Bitmap}s if any are available in the given dimensions
     *
     * @param bitmapPool The cache to try to recycle {@link android.graphics.Bitmap}s from
     */
    public ImageResizer(BitmapPool bitmapPool, BitmapFactory.Options defaultOptions){
        if (bitmapPool == null) {
            this.bitmapPool = new BitmapPoolAdapter();
        } else {
            this.bitmapPool = bitmapPool;
        }

        if (defaultOptions == null) {
            this.defaultOptions = getDefaultOptions();
        } else {
            this.defaultOptions = defaultOptions;
        }
    }

    /**
     * Scale the image so that either the width of the image matches the given width and the height of the image is
     * greater than the given height or vice versa, and then crop the larger dimension to match the given dimension.
     *
     * Does not maintain the image's aspect ratio
     *
     * @param is The InputStream for the image
     * @param width The minimum width of the image
     * @param height The minimum height of the image
     * @return The resized image
     */
    public Bitmap centerCrop(InputStream is, int width, int height) {
        final Bitmap streamed = loadAtLeast(is, width, height);
        return centerCrop(getRecycled(width, height), streamed, width, height);
    }

    /**
     * Scale the image uniformly (maintaining the image's aspect ratio) so that one of the dimensions of the image
     * will be equal to the given dimension and the other will be less than the given dimension
     *
     * @param is The InputStream for the image
     * @param width The maximum width of the image
     * @param height The maximum height of the image
     * @return The resized image
     */
    public Bitmap fitInSpace(InputStream is, int width, int height) {
        final Bitmap streamed = loadAtLeast(is, width > height ? 1 : width, height > width ? 1 : height);
        return fitInSpace(streamed, width, height);
    }

    /**
     * Scale the image uniformly (maintaining the image's aspect ratio) so that the dimensions of the image will be
     * greater than or equal to the given width and height.
     *
     * @param is An inputStream for the image
     * @param width The minimum width of the returned Bitmap
     * @param height The minimum height of the returned Bitmap
     * @return A Bitmap containing the image
     */
    public Bitmap loadAtLeast(InputStream is, int width, int height) {
        return atLeastDownsampler.load(is, width, height);
    }

    /**
     * Scale the image uniformly (maintaining the image's aspect ratio) so that the dimensions of the image will be
     * less than or equal to the given width and height. Unlike {@link #fitInSpace(android.graphics.Bitmap, int, int)},
     * one or both dimensions may be less than the given dimensions.
     *
     * @param is An InputStream for the image.
     * @param width The maximum width
     * @param height The maximum height
     * @return A bitmap containing the image
     */
    @SuppressWarnings("unused")
    public Bitmap loadAtMost(InputStream is, int width, int height) {
        return atMostDownsampler.load(is, width, height);
    }

    /**
     * Load the image at its original size
     *
     * @param is The InputStream for the image
     * @return The loaded image
     */
    public Bitmap loadAsIs(final InputStream is) {
        return asIsDownsampler.load(is, 0, 0);
    }

    /**
     * Load the image at its original size
     *
     * This is somewhat more efficient than {@link #loadAsIs(java.io.InputStream)} because it does not need to read
     * the image header to determine the image's width and height. Instead, it assumes the given width and height
     *
     * @param is The InputStream for the image
     * @param width The width of the image represented by the InputStream
     * @param height The height of the image represented by the InputStream
     * @return The loaded image
     */
    public Bitmap loadAsIs(InputStream is, int width, int height) {
        return fixedAsIsDownsampler.load(is, width, height);
    }

    /**
     * A potentially expensive operation to load the image for the given InputStream. If a recycled Bitmap whose
     * dimensions exactly match those of the image for the given InputStream is available, the operation is much less
     * expensive in terms of memory.
     *
     * Note - this method will throw an exception of a Bitmap with dimensions not matching those of the image for the
     * given InputStream is provided.
     *
     * @param is The InputStream representing the image data
     * @param recycle A Bitmap we can load the image into, or null
     * @return A new bitmap containing the image from the given InputStream, or recycle if recycle is not null
     */
    private Bitmap load(RecyclableBufferedInputStream is, Bitmap recycle){
        final BitmapFactory.Options decodeBitmapOptions = getOptions(recycle);
        return decodeStream(is, decodeBitmapOptions);
    }

    /**
     * A method for getting the dimensions of an image from the given InputStream
     *
     * @param is The InputStream representing the image
     * @return an array containing the dimensions of the image in the form {width, height}
     */
    private int[] getDimensions(RecyclableBufferedInputStream is) {
        final BitmapFactory.Options decodeBoundsOptions = getOptions();
        decodeBoundsOptions.inJustDecodeBounds = true;
        decodeStream(is, decodeBoundsOptions);
        return new int[] { decodeBoundsOptions.outWidth, decodeBoundsOptions.outHeight };
    }

    private Bitmap decodeStream(RecyclableBufferedInputStream bis, BitmapFactory.Options decodeBitmapOptions) {
        decodeBitmapOptions.inTempStorage = getTempBytes();

        if (decodeBitmapOptions.inJustDecodeBounds) {
            bis.mark(MARK_POSITION); //this is large, but jpeg headers are not size bounded so we need
                                     //something large enough to minimize the possibility of not being able to fit
                                     //enough of the header in the buffer to get the image size so that we don't fail
                                     //to load images. The BufferedInputStream will create a new buffer of 2x the
                                     //original size each time we use up the buffer space without passing the mark so
                                     //this is a maximum bound on the buffer size, not a default. Most of the time we
                                     //won't go past our pre-allocated 16kb
        }
        final Bitmap result = BitmapFactory.decodeStream(bis, null, decodeBitmapOptions);
        try {
            if (decodeBitmapOptions.inJustDecodeBounds) {
                bis.reset();
                bis.clearMark();
            } else {
                bis.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            releaseTempBytes(decodeBitmapOptions.inTempStorage);
        }

        return result;
    }

    private BitmapFactory.Options getOptions() {
        return getOptions(null);
    }

    private BitmapFactory.Options getOptions(Bitmap recycle) {
        BitmapFactory.Options result = new BitmapFactory.Options();
        copyOptions(defaultOptions, result, recycle);
        return result;
    }

    private Bitmap getRecycled(int[] dimens) {
        return getRecycled(dimens[0], dimens[1]);
    }

    private Bitmap getRecycled(int width, int height) {
        return bitmapPool.get(width, height);
    }

    private byte[] getTempBytes() {
        byte[] result;
        synchronized (tempQueue) {
            result = tempQueue.poll();
        }
        if (result == null) {
            result = new byte[TEMP_BYTES_SIZE];
            Log.d("IR: created temp bytes");
        }
        return result;
    }

    private void releaseTempBytes(byte[] bytes) {
        synchronized (tempQueue) {
            tempQueue.offer(bytes);
        }
    }

    private static void copyOptions(BitmapFactory.Options from, BitmapFactory.Options to, Bitmap recycled) {
        if (Build.VERSION.SDK_INT >= 11) {
            copyOptionsHoneycomb(from, to, recycled);
        } else if (Build.VERSION.SDK_INT >= 10) {
            copyOptionsGingerbreadMr1(from, to);
        } else {
            copyOptionsFroyo(from, to);
        }
    }

    @TargetApi(11)
    private static void copyOptionsHoneycomb(BitmapFactory.Options from, BitmapFactory.Options to, Bitmap recycled) {
        copyOptionsGingerbreadMr1(from, to);
        to.inMutable = from.inMutable;
        to.inBitmap = recycled;
    }

    @TargetApi(10)
    private static void copyOptionsGingerbreadMr1(BitmapFactory.Options from, BitmapFactory.Options to) {
        copyOptionsFroyo(from, to);
        to.inPreferQualityOverSpeed = from.inPreferQualityOverSpeed;
    }

    private static void copyOptionsFroyo(BitmapFactory.Options from, BitmapFactory.Options to) {
        to.inDensity = from.inDensity;
        to.inDither = from.inDither;
        to.inInputShareable = from.inInputShareable;
        to.inPreferredConfig = from.inPreferredConfig;
        to.inPurgeable = from.inPurgeable;
        to.inSampleSize = from.inSampleSize;
        to.inScaled = from.inScaled;
        to.inScreenDensity = from.inScreenDensity;
        to.inTargetDensity = from.inTargetDensity;
    }

    /**
     * A potentially expensive operation to crop the given Bitmap so that it fills the given dimensions. This operation
     * is significantly less expensive in terms of memory if a mutable Bitmap with the given dimensions is passed in
     * as well.
     *
     * @param recycled A mutable Bitmap with dimensions width and height that we can load the cropped portion of toCrop
     *                 into
     * @param toCrop The Bitmap to resize
     * @param width The width of the final Bitmap
     * @param height The height of the final Bitmap
     * @return The resized Bitmap (will be recycled if recycled is not null)
     */
    public static Bitmap centerCrop(Bitmap recycled, Bitmap toCrop, int width, int height) {
        if (toCrop == null) {
            return null;
        } else if (toCrop.getWidth() == width && toCrop.getHeight() == height) {
            return toCrop;
        }
        //from ImageView/Bitmap.createScaledBitmap
        //https://android.googlesource.com/platform/frameworks/base/+/refs/heads/master/core/java/android/widget/ImageView.java
        //https://android.googlesource.com/platform/frameworks/base/+/refs/heads/master/graphics/java/android/graphics/Bitmap.java
        final float scale;
        float dx = 0, dy = 0;
        Matrix m = new Matrix();
        if (toCrop.getWidth() * height > width * toCrop.getHeight()) {
            scale = (float) height / (float) toCrop.getHeight();
            dx = (width - toCrop.getWidth() * scale) * 0.5f;
        } else {
            scale = (float) width / (float) toCrop.getWidth();
            dy = (height - toCrop.getHeight() * scale) * 0.5f;
        }

        m.setScale(scale, scale);
        m.postTranslate((int) dx + 0.5f, (int) dy + 0.5f);
        final Bitmap result;
        if (recycled != null) {
            result = recycled;
        } else {
            result = Bitmap.createBitmap(width, height, toCrop.getConfig() == null ?
                                                            Bitmap.Config.ARGB_8888 : toCrop.getConfig());
        }
        Canvas canvas = new Canvas(result);
        Paint paint = new Paint();
        //only if scaling up
        paint.setFilterBitmap(false);
        paint.setAntiAlias(true);
        canvas.drawBitmap(toCrop, m, paint);
        return result;
    }

    /**
     * An expensive operation to resize the given image, maintaining the original proportions, so that its width
     * matches the given width
     *
     * @param toShrink The Bitmap to shrink
     * @param width The width of the final Bitmap
     * @return A new Bitmap shrunk to the given width, or toShrink if toShrink's width is equivalent to the given width
     */
    public static Bitmap shrinkToWidth(Bitmap toShrink, int width){
        Bitmap shrunk = toShrink;
        float widthPercent = ((float) width/toShrink.getWidth());
        if (widthPercent != 1) {
            int shrunkImageHeight = Math.round(widthPercent * toShrink.getHeight());
            shrunk = Bitmap.createScaledBitmap(toShrink, width, shrunkImageHeight, true);
        }
        return shrunk;
    }

    /**
     * An expensive operation to resize the given image, maintaining the original proportions, so that its height
     * matches the given height
     *
     * @param toShrink The Bitmap to shrink
     * @param height The height of the final Bitmap
     * @return A new Bitmap shrunk to the given height, or toShrink if toShink's height is equivalent to the given
     *          height
     */
    public static Bitmap shrinkToHeight(Bitmap toShrink, int height){
        Bitmap shrunk = toShrink;
        float heightPercent = ((float) height/toShrink.getHeight());
        if (heightPercent != 1) {
            int shrunkImageWidth = Math.round(heightPercent * toShrink.getWidth());
            shrunk = Bitmap.createScaledBitmap(toShrink, shrunkImageWidth, height, true);
        }
        return shrunk;
    }

    /**
     * An expensive operation to resize the given Bitmap down so that it fits within the given dimensions maintaining
     * the original proportions
     *
     * @param toFit The Bitmap to shrink
     * @param width The width the final image will fit within
     * @param height The height the final image will fit within
     * @return A new Bitmap shrunk to fit within the given dimesions, or toFit if toFit's width or height matches the
     * given dimensions and toFit fits within the given dimensions
     */
    public static Bitmap fitInSpace(Bitmap toFit, int width, int height){
        if (toFit == null) return null;

        if (height > width){
            return shrinkToWidth(toFit, width);
        } else {
            return shrinkToHeight(toFit, height);
        }
    }

    /**
     * Returns a matrix with rotation set based on Exif orientation tag.
     * If the orientation is undefined or 0 null is returned.
     *
     * @param pathToOriginal Path to original image file that may have exif data.
     * @return  A rotation in degrees based on exif orientation
     */
    public static int getOrientation(String pathToOriginal) {
        int degreesToRotate = 0;
        try{
            ExifInterface exif = new ExifInterface(pathToOriginal);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
            if (orientation == ExifInterface.ORIENTATION_ROTATE_90){
                degreesToRotate = 90;
            } else if (orientation == ExifInterface.ORIENTATION_ROTATE_180){
                degreesToRotate = 180;
            } else if (orientation == ExifInterface.ORIENTATION_ROTATE_270){
                degreesToRotate = 270;
            }
        } catch (IOException e){
            Log.w("IOException for image with filePath=" + pathToOriginal);
        } catch (Exception e) {
            Log.w("Exception when trying to get image orientation matrix");
            e.printStackTrace();
        }
        return degreesToRotate;
    }

    /**
     * This is an expensive operation that copies the image in place with the pixels rotated.
     * If possible rather use getOrientationMatrix, and set that as the imageMatrix on an ImageView.
     *
     * @param pathToOriginal Path to original image file that may have exif data.
     * @param imageToOrient Image Bitmap to orient.
     * @return The oriented bitmap. May be the imageToOrient without modification, or a new Bitmap.
     */
    @SuppressWarnings("unused")
    public static Bitmap orientImage(String pathToOriginal, Bitmap imageToOrient){
        int degreesToRotate = getOrientation(pathToOriginal);
        return rotateImage(imageToOrient, degreesToRotate);
    }

    /**
     * This is an expensive operation that copies the image in place with the pixels rotated.
     * If possible rather use getOrientationMatrix, and set that as the imageMatrix on an ImageView.
     *
     * @param imageToOrient Image Bitmap to orient.
     * @param degreesToRotate number of degrees to rotate the image by. If zero the original image is returned unmodified.
     * @return The oriented bitmap. May be the imageToOrient without modification, or a new Bitmap.
     */
    public static Bitmap rotateImage(Bitmap imageToOrient, int degreesToRotate) {
        try{
            if(degreesToRotate != 0) {
                Matrix matrix = new Matrix();
                matrix.setRotate(degreesToRotate);
                imageToOrient = Bitmap.createBitmap(
                        imageToOrient,
                        0,
                        0,
                        imageToOrient.getWidth(),
                        imageToOrient.getHeight(),
                        matrix,
                        true);
            }
        } catch (Exception e) {
            Log.w("Exception when trying to orient image");
            e.printStackTrace();
        }
        return imageToOrient;
    }
}
