package com.bumptech.glide.request.animation;

import android.graphics.drawable.Drawable;
import android.widget.ImageView;

/**
 * An interface that allows a transformation to be applied to {@link android.view.View}s in
 * {@link com.bumptech.glide.request.target.Target}s in across resource types. Targets that wrap views will be able to
 * provide all of the necessary arguments and start the animation. Those that do not will be unable to provide the
 * necessary arguments and will therefore be forced to ignore the animation. This interface is a compromise that
 * allows view animations in Glide's complex world of arbitrary resource types and arbitrary target types.
 *
 * @param <R> The type of the resource that should be animated to.
 */
public interface GlideAnimation<R> {
    /**
     * Animates from the previous {@link android.graphics.drawable.Drawable} that is currently being displayed in the
     * given view, if not null, to the new resource that should be displayed in the view.
     *
     * @param previous The {@link android.graphics.drawable.Drawable} currently displayed in the given view.
     * @param current The new resource that will be displayed in the view.
     * @param view The view.
     * @return True if int he process of running the animation the new resource was set on the view, false if the caller
     * needs to manually set the current resource on the view.
     */
    public boolean animate(Drawable previous, R current, ImageView view);
}
