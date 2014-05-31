package com.bumptech.glide.request.target;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;
import com.bumptech.glide.Resource;

/**
 * A target wrapping an ImageView. Obtains the runtime dimensions of the ImageView.
 */
public class BitmapImageViewTarget extends ViewTarget<ImageView, Bitmap> {
    private final ImageView view;

    public BitmapImageViewTarget(ImageView view) {
        super(view);
        this.view = view;
    }

    @Override
    public void onResourceReady(Resource<Bitmap> resource) {
        view.setImageBitmap(resource.get());
    }

    @Override
    public void setPlaceholder(Drawable placeholder) {
        view.setImageDrawable(placeholder);
    }
}
