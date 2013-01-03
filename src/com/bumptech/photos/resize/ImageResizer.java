/*
 * Copyright (c) 2012 Bump Technologies Inc. All rights reserved.
 */
package com.bumptech.photos.resize;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.ExifInterface;
import android.os.Build;
import com.bumptech.photos.resize.cache.SizedBitmapCache;
import com.bumptech.photos.util.Log;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author sam
 *
 */
public class ImageResizer {

    private final SizedBitmapCache bitmapCache;

    public ImageResizer() {
        this(null);
    }

    public ImageResizer(SizedBitmapCache bitmapCache){
        this.bitmapCache = bitmapCache;
    }

    public Bitmap resizeCenterCrop(final String path, final int width, final int height){
        final Bitmap streamed = streamIn(path, width, height);

        if (streamed.getWidth() == width && streamed.getHeight() == height) {
            return streamed;
        }

        return centerCrop(getRecycled(width, height), streamed, width, height);
    }

    public Bitmap fitInSpace(final String path, final int width, final int height){
        final Bitmap streamed = streamIn(path, width > height ? 1 : width, height > width ? 1 : height);
        return fitInSpace(streamed, width, height);
    }

    public Bitmap loadApproximate(final String path, final int width, final int height){
        return streamIn(path, width, height);
    }

    public Bitmap loadAsIs(final InputStream is1, final InputStream is2) {
        int[] dimens = new int[] {-1, -1};
        try {
            dimens = getDimension(is1);
        } finally {
            try {
                is1.close();
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
        Bitmap resized = null;
        try {
            resized = load(is2, getRecycled(dimens));
        } finally {
            try {
                is2.close();
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
        return resized;
    }

    public Bitmap loadAsIs(final String path, final int width, final int height) {
        return load(path, getRecycled(width, height));
    }

    public Bitmap loadAsIs(final String path){
        int[] dimens = getDimensions(path);
        return load(path, getRecycled(dimens));
    }

    private Bitmap getRecycled(int[] dimens) {
        return getRecycled(dimens[0], dimens[1]);
    }

    private Bitmap getRecycled(int width, int height) {
        Bitmap result = null;
        if (bitmapCache != null) {
            result = bitmapCache.get(width, height);
        }
        return result;
    }

      public static Bitmap centerCrop(Bitmap toCrop, int width, int height) {
        return centerCrop(null, toCrop, width, height);
    }

    public static Bitmap centerCrop(Bitmap recycled, Bitmap toCrop, int width, int height) {
        if (toCrop.getWidth() == width && toCrop.getHeight() == height) {
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
        Bitmap result = recycled != null ? recycled : Bitmap.createBitmap(width, height, toCrop.getConfig());
        Canvas canvas = new Canvas(result);
        Paint paint = new Paint();
        //only if scaling up
        paint.setFilterBitmap(false);
        paint.setAntiAlias(true);
        canvas.drawBitmap(toCrop, m, paint);
        return result;
    }

    public static Bitmap cropToWidth(Bitmap toCrop, int width) {
        Bitmap cropped = toCrop;
        if (toCrop.getWidth() > width) {
            int extraWidth = toCrop.getWidth() - width;
            cropped = Bitmap.createBitmap(toCrop, extraWidth / 2, 0, width, toCrop.getHeight());
            toCrop.recycle();
        }
        return cropped;
    }

    //crops a section height pixels tall in the center of the image with equal
    //amounts discarded above and below
    public static Bitmap cropToHeight(Bitmap toCrop, int height){
        Bitmap cropped = toCrop;
        if (toCrop.getHeight() > height){
            int extraHeight = toCrop.getHeight() - height;
            cropped = Bitmap.createBitmap(toCrop, 0, extraHeight / 2, toCrop.getWidth(), height);
            toCrop.recycle();
        }
        return cropped;
    }

    //shrinks to the given width, shrinking the height to maintain the original image proportions
    public static Bitmap shrinkToWidth(Bitmap toShrink, int width){
        //get exactly the right width
        float widthPercent = ((float) width/toShrink.getWidth());
        int shrunkImageHeight = Math.round(widthPercent * toShrink.getHeight());
        Bitmap shrunk = Bitmap.createScaledBitmap(toShrink, width, shrunkImageHeight, true);
        toShrink.recycle();
        return shrunk;
    }

    public static Bitmap shrinkToHeight(Bitmap toShrink, int height){
        float heightPercent = ((float) height/toShrink.getHeight());
        int shrunkImageWidth = Math.round(heightPercent * toShrink.getWidth());
        Bitmap shrunk = Bitmap.createScaledBitmap(toShrink, shrunkImageWidth, height, true);
        toShrink.recycle();
        return shrunk;
    }

    public static Bitmap fitInSpace(Bitmap toFit, int width, int height){
        if (height > width){
            return shrinkToWidth(toFit, width);
        } else {
            return shrinkToHeight(toFit, height);
        }
    }

    public static Bitmap load(String path) {
        return load(path, null);
    }

    public static Bitmap load(String path, Bitmap recycle) {
        Bitmap result = null;
        try {
            result = load(new FileInputStream(path), recycle);
        } catch (FileNotFoundException e) {
            Log.d("PSR: file not found loading bitmap at: " + path);
            e.printStackTrace();
        }
        return result == null ? null : orientImage(path, result);
    }

    public static Bitmap load(InputStream is) {
        return load(is, null);
    }

    public static Bitmap load(InputStream is, Bitmap recycle){
        final BitmapFactory.Options decodeBitmapOptions = new BitmapFactory.Options();
        decodeBitmapOptions.inSampleSize = 1;
        decodeBitmapOptions.inPreferredConfig = Bitmap.Config.RGB_565;
        decodeBitmapOptions.inDither = false;
        if (Build.VERSION.SDK_INT >= 11) {
            decodeBitmapOptions.inMutable = true; //required or next attempt to recycle will fail
            if (recycle != null) {
                decodeBitmapOptions.inBitmap = recycle; //we can load photo without a bitmap to recycle,
                                                        //its just less efficient
            }
        }
        InputStream stream;
        Bitmap result = null;
        try {
            stream = new BufferedInputStream(is);
            result = BitmapFactory.decodeStream(stream, null, decodeBitmapOptions);
            stream.close();
        } catch (IOException e) {
            Log.d("PSR: io exception: " + e + " loading bitmap");
        }
        return result;
    }

    public static int[] getDimensions(String path) {
        int[] dimens = new int[]{-1, -1};
        try {
            InputStream is = new BufferedInputStream(new FileInputStream(path));
            dimens = getDimension(is);
            is.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return dimens;
    }

    public static int[] getDimension(InputStream is) {
        final BitmapFactory.Options decodeBoundsOptions = new BitmapFactory.Options();
        decodeBoundsOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(is, null, decodeBoundsOptions); //doesn't load, just sets the decodeBounds
        return new int[] { decodeBoundsOptions.outWidth, decodeBoundsOptions.outHeight };
    }
    //from http://stackoverflow.com/questions/7051025/how-do-i-scale-a-streaming-bitmap-in-place-without-reading-the-whole-image-first
    //streams in to near, but not exactly at the desired width and height.
    public static Bitmap streamIn(String path, int width, int height) {
        int orientation = getOrientation(path);
        if(orientation == 90 || orientation == 270) {
            //Swap width and height for initial downsample calculation if its oriented so.
            //The image will then be rotated back to normal.
            int w = width;
            width = height;
            height = w;
        }

        Bitmap result = null;
        try {
            final int[] dimens = getDimensions(path);
            final int originalWidth = dimens[0];
            final int originalHeight = dimens[1];

            // inSampleSize prefers multiples of 2, but we prefer to prioritize memory savings
            int sampleSize = Math.min(originalHeight / height, originalWidth / width);

            final BitmapFactory.Options decodeBitmapOptions = new BitmapFactory.Options();
            // For further memory savings, you may want to consider using this option
            decodeBitmapOptions.inPreferredConfig = Bitmap.Config.RGB_565; // Uses 2-bytes instead of default 4 per pixel
            decodeBitmapOptions.inDither = false;

            InputStream is = new BufferedInputStream(new FileInputStream(path), 16384);

            decodeBitmapOptions.inSampleSize = sampleSize;
            if (Build.VERSION.SDK_INT > 11) {
                decodeBitmapOptions.inMutable = true;
            }
            result = BitmapFactory.decodeStream(is, null, decodeBitmapOptions);
            if (orientation != 0) {
                result = rotateImage(result, orientation);
            }
            is.close();
        } catch (Exception e){
            Log.d("PSR: error decoding image: " + e);
        } catch (OutOfMemoryError e){
            Log.d("PSR: not enough memory to resize image at " + path);
            Log.d(e.toString());
        }
        return result;
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
