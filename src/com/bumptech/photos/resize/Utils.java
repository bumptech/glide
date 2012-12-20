package com.bumptech.photos.resize;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import com.bumptech.photos.util.Photo;
import com.bumptech.photos.util.Log;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created with IntelliJ IDEA.
 * User: sam
 * Date: 12/20/12
 * Time: 1:55 PM
 * To change this template use File | Settings | File Templates.
 */
public class Utils {

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
        return result == null ? null : Photo.orientImage(path, result);
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

    //from http://stackoverflow.com/questions/7051025/how-do-i-scale-a-streaming-bitmap-in-place-without-reading-the-whole-image-first
    //streams in to near, but not exactly at the desired width and height.
    public static Bitmap streamIn(String path, int width, int height) {
        int orientation = Photo.getOrientation(path);
        if(orientation == 90 || orientation == 270) {
            //Swap width and height for initial downsample calculation if its oriented so.
            //The image will then be rotated back to normal.
            int w = width;
            width = height;
            height = w;
        }

        Bitmap result = null;
        try {
            final BitmapFactory.Options decodeBitmapOptions = new BitmapFactory.Options();
            // For further memory savings, you may want to consider using this option
            decodeBitmapOptions.inPreferredConfig = Bitmap.Config.RGB_565; // Uses 2-bytes instead of default 4 per pixel
            decodeBitmapOptions.inDither = false;
            //avoid markInvalidated by creating two streams, rather than one and resetting it
            //readLimit would have to be size of entire photo, which can be huge
            InputStream first = new BufferedInputStream(new FileInputStream(path), 16384);

            //find the dimensions of the actual image
            final BitmapFactory.Options decodeBoundsOptions = new BitmapFactory.Options();
            decodeBoundsOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(first, null, decodeBoundsOptions); //doesn't load, just sets the decodeBounds
            first.close();

            final int originalWidth = decodeBoundsOptions.outWidth;
            final int originalHeight = decodeBoundsOptions.outHeight;

            // inSampleSize prefers multiples of 2, but we prefer to prioritize memory savings
            int sampleSize = Math.min(originalHeight / height, originalWidth / width);
            InputStream second = new BufferedInputStream(new FileInputStream(path), 16384);

            decodeBitmapOptions.inSampleSize = sampleSize;
            if (Build.VERSION.SDK_INT > 11) {
                decodeBitmapOptions.inMutable = true;
            }
            Log.d("PSR: Loading image with sample size: " + sampleSize);
            result = BitmapFactory.decodeStream(second, null, decodeBitmapOptions);
            if(orientation != 0) {
                result = Photo.rotateImage(result, orientation);
            }
            second.close();
        } catch (Exception e){
            Log.d("PSR: error decoding image: " + e);
        } catch (OutOfMemoryError e){
            Log.d("PSR: not enough memory to resize image at " + path);
            Log.d(e.toString());
        }
        return result;
    }
}
