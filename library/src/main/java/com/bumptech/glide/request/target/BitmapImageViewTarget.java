package com.bumptech.glide.request.target;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;
import com.bumptech.glide.request.animation.GlideAnimation;

/**
 * A {@link com.bumptech.glide.request.target.Target} that can display an {@link android.graphics.Bitmap} in an
 * {@link android.widget.ImageView}.
 *
 * @see com.bumptech.glide.request.target.DrawableImageViewTarget
 */
public class BitmapImageViewTarget extends ViewTarget<ImageView, Bitmap> {
    private final ImageView view;

    public BitmapImageViewTarget(ImageView view) {
        super(view);
        this.view = view;
    }

    /**
     * {@inheritDoc}
     * If no {@link com.bumptech.glide.request.animation.GlideAnimation} is given or if the animation does not set the
     * {@link Bitmap} on the view, the bitmap is set using
     * {@link android.widget.ImageView#setImageBitmap(android.graphics.Bitmap)}.
     *
     * @param resource {@inheritDoc}
     * @param glideAnimation {@inheritDoc}
     */
    @Override
    public void onResourceReady(Bitmap resource, GlideAnimation<Bitmap> glideAnimation) {
        if (glideAnimation == null || !glideAnimation.animate(view.getDrawable(), resource, view)) {
            view.setImageBitmap(resource);
        }
    }


    /**
     * Sets the given {@link android.graphics.drawable.Drawable} on the view using
     * {@link android.widget.ImageView#setImageDrawable(android.graphics.drawable.Drawable)}.
     *
     * @param placeholder {@inheritDoc}
     */
    @Override
    public void setPlaceholder(Drawable placeholder) {
        view.setImageDrawable(placeholder);
    }
}
