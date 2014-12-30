package com.bumptech.glide.request.target;

import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import com.bumptech.glide.request.animation.GlideAnimation;

/**
 * A target for display {@link Drawable} objects in {@link ImageView}s.
 */
public class DrawableImageViewTarget extends ImageViewTarget<Drawable> {
    private Drawable resource;

    public DrawableImageViewTarget(ImageView view) {
        super(view);
    }

    @Override
    protected void setResource(Drawable resource) {
        this.resource = resource;
        view.setImageDrawable(resource);
    }

    @Override
    public void onResourceReady(Drawable resource, GlideAnimation<? super Drawable> glideAnimation) {
        super.onResourceReady(resource, glideAnimation);
        if (resource instanceof Animatable) {
            ((Animatable) resource).start();
        }
    }

    @Override
    public void onStart() {
        if (resource instanceof Animatable) {
            ((Animatable) resource).start();
        }
    }

    @Override
    public void onStop() {
        if (resource instanceof Animatable) {
            ((Animatable) resource).stop();
        }
    }
}
