package com.bumptech.glide.request.animation;

import android.graphics.drawable.Drawable;
import android.view.View;

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
     * An interface wrapping a view that exposes the necessary methods to run the various types of android animations
     * ({@link com.bumptech.glide.request.animation.ViewAnimation},
     * {@link com.bumptech.glide.request.animation.ViewPropertyAnimation} and animated
     * {@link android.graphics.drawable.Drawable}s).
     */
    interface ViewAdapter {
        /**
         * Returns the wrapped {@link android.view.View}.
         */
        View getView();

        /**
         * Returns the current drawable being displayed in the view, or null if no such drawable exists (or one cannot
         * be retrieved).
         */
        Drawable getCurrentDrawable();

        /**
         * Sets the current drawable (usually an animated drawable) to display in the wrapped view.
         *
         * @param drawable The drawable to display in the wrapped view.
         */
        void setDrawable(Drawable drawable);
    }

    /**
     * Animates from the previous {@link android.graphics.drawable.Drawable} that is currently being displayed in the
     * given view, if not null, to the new resource that should be displayed in the view.
     *
     * @param current The new resource that will be displayed in the view.
     * @param adapter The {@link com.bumptech.glide.request.animation.GlideAnimation.ViewAdapter} wrapping a view that
     *                can at least return an {@link android.view.View} from
     *                {@link com.bumptech.glide.request.animation.GlideAnimation.ViewAdapter#getView()}.
     * @return True if int he process of running the animation the new resource was set on the view, false if the caller
     * needs to manually set the current resource on the view.
     */
    boolean animate(R current, ViewAdapter adapter);
}
