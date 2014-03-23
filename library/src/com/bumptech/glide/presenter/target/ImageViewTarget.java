package com.bumptech.glide.presenter.target;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.widget.ImageView;

import java.lang.ref.WeakReference;

import static android.view.ViewGroup.LayoutParams;

/**
 * A target wrapping an ImageView. Obtains the runtime dimensions of the ImageView.
 */
public class ImageViewTarget extends ViewTarget<ImageView> {
    private final ImageView view;

    public ImageViewTarget(ImageView view) {
        super(view);
        this.view = view;
    }

    @Override
    public void onImageReady(Bitmap bitmap) {
        view.setImageBitmap(bitmap);
    }

    @Override
    public void setPlaceholder(Drawable placeholder) {
        view.setImageDrawable(placeholder);
    }
}
