package com.bumptech.glide.request.target;

import android.graphics.Bitmap;
import android.widget.ImageView;

/**
 * A {@link com.bumptech.glide.request.target.Target} that can display an {@link android.graphics.Bitmap} in an
 * {@link android.widget.ImageView}.
 *
 * @see com.bumptech.glide.request.target.DrawableImageViewTarget
 */
public class BitmapImageViewTarget extends ImageViewTarget<Bitmap> {
    private final ImageView view;

    public BitmapImageViewTarget(ImageView view) {
        super(view);
        this.view = view;
    }

    /**
     * Sets the {@link android.graphics.Bitmap} on the view using
     * {@link android.widget.ImageView#setImageBitmap(android.graphics.Bitmap)}.
     *
     * @param resource The bitmap to display.
     */
    @Override
    protected void setResource(Bitmap resource) {
        view.setImageBitmap(resource);
    }
}
