package com.bumptech.glide.request.target;

import android.graphics.drawable.Drawable;
import android.widget.ImageView;

public class DrawableImageViewTarget extends ViewTarget<ImageView, Drawable> {
    public DrawableImageViewTarget(ImageView view) {
        super(view);
    }

    @Override
    public void onResourceReady(Drawable resource) {
        getView().setImageDrawable(resource);
    }

    @Override
    public void setPlaceholder(Drawable placeholder) {
        getView().setImageDrawable(placeholder);
    }
}
