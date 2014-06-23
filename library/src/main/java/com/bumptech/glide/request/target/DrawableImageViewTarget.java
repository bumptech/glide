package com.bumptech.glide.request.target;

import android.graphics.drawable.Drawable;
import android.widget.ImageView;
import com.bumptech.glide.request.GlideAnimation;

public class DrawableImageViewTarget extends ViewTarget<ImageView, Drawable> {
    private static final float SQUARE_RATIO_MARGIN = 0.05f;
    private final ImageView view;

    public DrawableImageViewTarget(ImageView view) {
        super(view);
        this.view = view;
    }

    @Override
    public void onResourceReady(Drawable resource, GlideAnimation<Drawable> animation) {

        //TODO: Try to generalize this to other sizes/shapes.
        // This is a dirty hack that tries to make loading square thumbnails and then square full images less costly by
        // forcing both the smaller thumb and the larger version to have exactly the same intrinsic dimensions. If a
        // drawable is replaced in an ImageView by another drawable with different intrinsic dimensions, the ImageView
        // requests a layout. Scrolling rapidly while replacing thumbs with larger images triggers lots of these calls
        // and causes significant amounts of jank.
        float viewRatio = view.getWidth() / (float) view.getHeight();
        float drawableRatio = resource.getIntrinsicWidth() / (float) resource.getIntrinsicHeight();
        if (Math.abs(viewRatio - 1f) <= SQUARE_RATIO_MARGIN && Math.abs(drawableRatio - 1f) <= SQUARE_RATIO_MARGIN) {
            resource = new SquaringDrawable(resource, view.getWidth());
        }

        if (animation == null || !animation.animate(view.getDrawable(), resource, view, this)) {
            view.setImageDrawable(resource);
        }
    }

    @Override
    public void setPlaceholder(Drawable placeholder) {
        view.setImageDrawable(placeholder);
    }
}
