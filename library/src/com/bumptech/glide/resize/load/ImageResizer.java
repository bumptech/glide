/*
 * Copyright (c) 2012 Bump Technologies Inc. All rights reserved.
 */
package com.bumptech.glide.resize.load;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Bitmap.Config;
import android.graphics.PorterDuff.Mode;
import android.media.ExifInterface;
import android.os.Build;
import android.util.Log;
import com.bumptech.glide.resize.RecyclableBufferedInputStream;
import com.bumptech.glide.resize.bitmap_recycle.BitmapPool;
import com.bumptech.glide.resize.bitmap_recycle.BitmapPoolAdapter;

import java.io.InputStream;
import java.util.LinkedList;
import java.util.Queue;

/**
 * A class for synchronously resizing bitmaps with or without Bitmaps to reuse
 */
public class ImageResizer {
    public static final int PAINT_FLAGS = Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG | Paint.FILTER_BITMAP_FLAG;

    private static final String TAG = "ImageResizer";
    private static final int TEMP_BYTES_SIZE = 16 * 1024; //16kb
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
    @Deprecated
    public ImageResizer(BitmapFactory.Options options) {
        this(null, options);
    }

    /**
     * Creates a new resizer that will attempt to recycle {@link android.graphics.Bitmap}s if any are available in the given dimensions
     *
     * @param bitmapPool The cache to try to recycle {@link android.graphics.Bitmap}s from
     */
    @Deprecated
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

    public Bitmap load(InputStream is) {
        return load(is, -1, -1, Downsampler.NONE, Transformation.NONE);
    }

    public Bitmap load(InputStream is, int outWidth, int outHeight) {
        return load(is, outWidth, outHeight, Transformation.NONE);
    }

    public Bitmap load(InputStream is, int outWidth, int outHeight, Transformation transformation) {
        return load(is, outWidth, outHeight, Downsampler.AT_LEAST, transformation);
    }

    public Bitmap load(InputStream is, int outWidth, int outHeight, Downsampler downsampler) {
        return load(is, outWidth, outHeight, downsampler, Transformation.NONE);
    }

    public Bitmap load(InputStream is, int outWidth, int outHeight, Downsampler downsampler, Transformation transformation) {
        byte[] tempBytesForBis = getTempBytes();
        byte[] tempBytesForOptions = getTempBytes();

        BitmapFactory.Options options = getOptions();
        options.inTempStorage = tempBytesForOptions;

        RecyclableBufferedInputStream bis = new RecyclableBufferedInputStream(is, tempBytesForBis);

        final Bitmap initial = downsampler.downsample(bis, options, bitmapPool, outWidth, outHeight);
        final Bitmap result = transformation.transform(initial, bitmapPool, outWidth, outHeight);

        if (initial != result && !bitmapPool.put(initial)) {
            initial.recycle();
        }

        releaseTempBytes(tempBytesForBis);
        releaseTempBytes(tempBytesForOptions);

        return result;
    }

    private BitmapFactory.Options getOptions() {
        BitmapFactory.Options result = new BitmapFactory.Options();
        copyOptions(defaultOptions, result);
        return result;
    }

    private byte[] getTempBytes() {
        byte[] result;
        synchronized (tempQueue) {
            result = tempQueue.poll();
        }
        if (result == null) {
            result = new byte[TEMP_BYTES_SIZE];
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Created temp bytes");
            }
        }
        return result;
    }

    private void releaseTempBytes(byte[] bytes) {
        synchronized (tempQueue) {
            tempQueue.offer(bytes);
        }
    }

    private static void copyOptions(BitmapFactory.Options from, BitmapFactory.Options to) {
        if (Build.VERSION.SDK_INT >= 11) {
            copyOptionsHoneycomb(from, to);
        } else if (Build.VERSION.SDK_INT >= 10) {
            copyOptionsGingerbreadMr1(from, to);
        } else {
            copyOptionsFroyo(from, to);
        }
    }

    @TargetApi(11)
    private static void copyOptionsHoneycomb(BitmapFactory.Options from, BitmapFactory.Options to) {
        copyOptionsGingerbreadMr1(from, to);
        to.inMutable = from.inMutable;
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
        m.postTranslate((int) (dx + 0.5f), (int) (dy + 0.5f));
        final Bitmap result;
        if (recycled != null) {
            result = recycled;
        } else {
            result = Bitmap.createBitmap(width, height, toCrop.getConfig() == null ?
                                                            Bitmap.Config.ARGB_8888 : toCrop.getConfig());
        }
        Canvas canvas = new Canvas(result);
        Paint paint = new Paint(PAINT_FLAGS);
        canvas.drawBitmap(toCrop, m, paint);
        return result;
    }
    
    
    /**
     * Clip the given Bitmap with transparent circle
     *
     * @param recycled A mutable Bitmap with dimensions width and height that we can load the cropped portion of toCrop
     *                 into
     * @param toCrop The Bitmap to resize
     * @param width The width of the final Bitmap
     * @param height The height of the final Bitmap
     * @return The resized Bitmap (will be recycled if recycled is not null)
     */
    public static Bitmap circleCrop(Bitmap recycled, Bitmap toCrop, int width, int height) {
        if (toCrop == null) {
            return null;
        }
        final Bitmap result;
        if (recycled != null) {
            result = recycled;
        } else {
        	result = Bitmap.createBitmap(toCrop.getWidth(), toCrop.getHeight(), Config.ARGB_8888);
        }
	    Canvas canvas = new Canvas(result);
	
	    final int color = 0xff424242;
	    final Paint paint = new Paint();
	    final Rect rect = new Rect(0, 0, toCrop.getWidth(), toCrop.getHeight());
	    final RectF rectF = new RectF(rect);
	
	    paint.setAntiAlias(true);
	    canvas.drawARGB(0, 0, 0, 0);
	    paint.setColor(color);
	    canvas.drawRoundRect(rectF, toCrop.getWidth(), toCrop.getHeight(), paint);
	
	    paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
	    canvas.drawBitmap(toCrop, rect, rect, paint);
	
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
     * @return A new Bitmap shrunk to fit within the given dimensions, or toFit if toFit's width or height matches the
     * given dimensions and toFit fits within the given dimensions
     */
    @Deprecated
    public static Bitmap fitInSpace(Bitmap toFit, int width, int height){
        if (toFit == null) return null;

        if (height > width){
            return shrinkToWidth(toFit, width);
        } else {
            return shrinkToHeight(toFit, height);
        }
    }

    /**
     * An expensive operation to resize the given Bitmap down so that it fits within the given dimensions maintain
     * the original proportions.
     *
     * @param toFit The Bitmap to shrink.
     * @param pool The BitmapPool to try to reuse a bitmap from.
     * @param width The width the final image will fit within.
     * @param height The height the final image will fit within.
     * @return A new Bitmap shrunk to fit within the given dimensions, or toFit if toFit's width or height matches the
     * given dimensions and toFit fits within the given dimensions
     */
    public static Bitmap fitCenter(Bitmap toFit, BitmapPool pool, int width, int height) {
         final float shrinkPercentage;
        final int targetWidth;
        final int targetHeight;
        if (height > width) {
            shrinkPercentage = width / (float) toFit.getWidth();
            targetWidth = width;
            targetHeight = Math.round(shrinkPercentage * toFit.getHeight());
        } else {
            shrinkPercentage = height / (float) toFit.getHeight();
            targetWidth = Math.round(shrinkPercentage * toFit.getWidth());
            targetHeight = height;
        }

        Bitmap.Config config = toFit.getConfig() != null ? toFit.getConfig() : Bitmap.Config.ARGB_8888;
        Bitmap toReuse = pool.get(targetWidth, targetHeight, config);
        if (toReuse == null) {
            toReuse = Bitmap.createBitmap(targetWidth, targetHeight, config);
        }
        Canvas canvas = new Canvas(toReuse);
        Matrix matrix = new Matrix();
        matrix.setScale(shrinkPercentage, shrinkPercentage);
        Paint paint = new Paint(ImageResizer.PAINT_FLAGS);
        canvas.drawBitmap(toFit, matrix, paint);

        return toReuse;
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
        } catch (Exception e){
            if (Log.isLoggable(TAG, Log.ERROR)) {
                Log.e(TAG, "Unable to get orientation for image with path=" + pathToOriginal, e);
            }
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
            if (Log.isLoggable(TAG, Log.ERROR)) {
                Log.e(TAG, "Exception when trying to orient image", e);
            }
            e.printStackTrace();
        }
        return imageToOrient;
    }

    /**
     * Get the # of degrees an image must be rotated to match the given exif orientation.
     *
     * @param exifOrientation The exif orientation [1-8]
     * @return the number of degrees to rotate
     */
    public static int getExifOrientationDegrees(int exifOrientation) {
        final int degreesToRotate;
        switch (exifOrientation) {
            case ExifInterface.ORIENTATION_TRANSPOSE:
            case ExifInterface.ORIENTATION_ROTATE_90:
                degreesToRotate = 90;
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                degreesToRotate = 180;
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
            case ExifInterface.ORIENTATION_ROTATE_270:
                degreesToRotate = 270;
                break;
            default:
                degreesToRotate = 0;

        }
        return degreesToRotate;
    }

    /**
     * Rotate and/or flip the image to match the given exif orientation
     *
     * @param toOrient The bitmap to rotate/flip
     * @param pool A pool that may or may not contain an image of the necessary dimensions
     * @param exifOrientation the exif orientation [1-8]
     * @return The rotated and/or flipped image or toOrient if no rotation or flip was necessary
     */
    public static Bitmap rotateImageExif(Bitmap toOrient, BitmapPool pool, int exifOrientation) {
        final Matrix matrix = new Matrix();
        switch (exifOrientation) {
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.setScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.setRotate(180);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.setRotate(180);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                matrix.setRotate(90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.setRotate(90);
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                matrix.setRotate(-90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.setRotate(-90);
                break;
            default: //case ExifInterface.ORIENTATION_NORMAL
                return toOrient;
        }

        // From Bitmap.createBitmap.
        final RectF newRect = new RectF(0, 0, toOrient.getWidth(), toOrient.getHeight());
        matrix.mapRect(newRect);

        final int newWidth = Math.round(newRect.width());
        final int newHeight = Math.round(newRect.height());

        Bitmap result = pool.get(newWidth, newHeight, toOrient.getConfig());
        if (result == null) {
            result = Bitmap.createBitmap(newWidth, newHeight, toOrient.getConfig());
        }

        matrix.postTranslate(-newRect.left, -newRect.top);

        final Canvas canvas = new Canvas(result);
        final Paint paint = new Paint(PAINT_FLAGS);
        canvas.drawBitmap(toOrient, matrix, paint);

        return result;
    }
}
