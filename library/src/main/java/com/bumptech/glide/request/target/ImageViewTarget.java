package com.bumptech.glide.request.target;

import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import com.bumptech.glide.request.animation.GlideAnimation;

/**
 * A base {@link com.bumptech.glide.request.target.Target} for displaying resources in
 * {@link android.widget.ImageView}s.
 *
 * @param <Z> The type of resource that this target will display in the wrapped {@link android.widget.ImageView}.
 */
public abstract class ImageViewTarget<Z> extends ViewTarget<ImageView, Z> implements GlideAnimation.ViewAdapter {

    public ImageViewTarget(ImageView view) {
        super(view);
    }

    /**
     * Returns the current {@link android.graphics.drawable.Drawable} being displayed in the view using
     * {@link android.widget.ImageView#getDrawable()}.
     */
    @Override
    public Drawable getCurrentDrawable() {
        return view.getDrawable();
    }

    /**
     * Sets the given {@link android.graphics.drawable.Drawable} on the view using
     * {@link android.widget.ImageView#setImageDrawable(android.graphics.drawable.Drawable)}.
     *
     * @param drawable {@inheritDoc}
     */
    @Override
    public void setDrawable(Drawable drawable) {
        view.setImageDrawable(drawable);
    }

    /**
     * Sets the given {@link android.graphics.drawable.Drawable} on the view using
     * {@link android.widget.ImageView#setImageDrawable(android.graphics.drawable.Drawable)}.
     *
     * @param placeholder {@inheritDoc}
     */
    @Override
    public void onLoadStarted(Drawable placeholder) {
        view.setImageDrawable(placeholder);
    }

    /**
     * Sets the given {@link android.graphics.drawable.Drawable} on the view using
     * {@link android.widget.ImageView#setImageDrawable(android.graphics.drawable.Drawable)}.
     *
     * @param errorDrawable {@inheritDoc}
     */
    @Override
    public void onLoadFailed(Exception e, Drawable errorDrawable) {
        view.setImageDrawable(errorDrawable);
    }

    /**
     * Sets the given {@link android.graphics.drawable.Drawable} on the view using
     * {@link android.widget.ImageView#setImageDrawable(android.graphics.drawable.Drawable)}.
     *
     * @param placeholder {@inheritDoc}
     */
    @Override
    public void onLoadCleared(Drawable placeholder) {
        view.setImageDrawable(placeholder);
    }

    @Override
    public void onResourceReady(Z resource, GlideAnimation<? super Z> glideAnimation) {
        if (glideAnimation == null || !glideAnimation.animate(resource, this)) {
            setResource(resource);
        }
    }

    protected abstract void setResource(Z resource);

}

