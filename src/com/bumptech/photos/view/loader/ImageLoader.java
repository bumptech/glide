package com.bumptech.photos.view.loader;

import android.graphics.Bitmap;

/**
 * Created with IntelliJ IDEA.
 * User: sam
 * Date: 1/1/13
 * Time: 2:21 PM
 * To change this template use File | Settings | File Templates.
 */
public interface ImageLoader<T> {

    public interface ImageReadyCallback {
        public boolean onImageReady(Bitmap image);
        public void onError(Exception e);
    }

    /**
     * Load the image at the given path represented by the given model
     *
     * @param path - the path to the image or null if the required information is contained in the model
     * @param model - the object taht represents or contains an image that can be displayed
     * @param width - the width of the view where the image will be displayed
     * @param height - the height of the view where the image will be displayed
     * @param cb - the callback to call when the bitmap is loaded into memory
     * @return A reference to the fetch (if needed because of a weak reference) or null
     */
    public Object fetchImage(String path, T model, int width, int height, ImageReadyCallback cb);

    public void clear();
}
