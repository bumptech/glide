package com.bumptech.glide.request.target;

import android.graphics.drawable.Drawable;
import android.widget.ImageView;
import com.bumptech.glide.request.GlideAnimation;

public class DrawableImageViewTarget extends ViewTarget<ImageView, Drawable> {
    private final ImageView view;

    public DrawableImageViewTarget(ImageView view) {
        super(view);
        this.view = view;
    }

    @Override
    public void onResourceReady(Drawable resource, GlideAnimation<Drawable> animation) {
        if (animation == null || !animation.animate(view.getDrawable(), resource, view, this)) {
            view.setImageDrawable(resource);
        }
    }

    @Override
    public void setPlaceholder(Drawable placeholder) {
        view.setImageDrawable(placeholder);
    }
}
