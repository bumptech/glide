package com.bumptech.glide.request.target;

import android.graphics.drawable.Drawable;
import android.widget.ImageView;

/**
 * A target for display {@link Drawable} objects in {@link ImageView}s.
 */
public class DrawableImageViewTarget extends ImageViewTarget<Drawable> {
    public DrawableImageViewTarget(ImageView view) {
        super(view);
    }

    @Override
    protected void setResource(Drawable resource) {
       view.setImageDrawable(resource);
    }
}
