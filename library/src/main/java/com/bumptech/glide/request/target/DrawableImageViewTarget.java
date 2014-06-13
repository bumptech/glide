package com.bumptech.glide.request.target;

import android.graphics.drawable.Drawable;
import android.widget.ImageView;

public class DrawableImageViewTarget extends ViewTarget<ImageView, Drawable> {
    private final ImageView view;

    public DrawableImageViewTarget(ImageView view) {
        super(view);
        this.view = view;
    }

    @Override
    public void onResourceReady(Drawable resource) {
        view.setImageDrawable(resource);
    }

    @Override
    public void setPlaceholder(Drawable placeholder) {
        view.setImageDrawable(placeholder);
    }
}
