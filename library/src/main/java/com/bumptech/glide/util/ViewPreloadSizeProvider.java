package com.bumptech.glide.util;

import android.view.View;

import com.bumptech.glide.ListPreloader;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SizeReadyCallback;
import com.bumptech.glide.request.target.ViewTarget;

import java.util.Arrays;

/**
 * A {@link com.bumptech.glide.ListPreloader.PreloadSizeProvider} that will extract the preload size from a given
 * {@link android.view.View}.
 *
 * @param <T> The type of the model the size should be provided for.
 */
public class ViewPreloadSizeProvider<T> implements ListPreloader.PreloadSizeProvider<T>, SizeReadyCallback {
    private int[] size;
    // We need to keep a strong reference to the Target so that it isn't garbage collected due to a weak reference
    // while we're waiting to get its size.
    @SuppressWarnings("unused")
    private SizeViewTarget viewTarget;

    /**
     * Constructor that does nothing by default and requires users to call {@link #setView(android.view.View)} when a
     * View is available to registerComponents the dimensions returned by this class.
     */
    public ViewPreloadSizeProvider() {
        // This constructor is intentionally empty. Nothing special is needed here.
    }

    /**
     * Constructor that will extract the preload size from a given {@link android.view.View}.
     *
     * @param view A not null View the size will be extracted from async using an {@link android.view.ViewTreeObserver
     *             .OnPreDrawListener}
     */
    public ViewPreloadSizeProvider(View view) {
        setView(view);
    }

    @Override
    public int[] getPreloadSize(T item, int adapterPosition, int itemPosition) {
        if (size == null) {
            return null;
        } else {
            return Arrays.copyOf(this.size, this.size.length);
        }
    }

    @Override
    public void onSizeReady(int width, int height) {
        this.size = new int[]{width, height};
        viewTarget = null;
    }

    /**
     * Sets the {@link android.view.View} the size will be extracted.
     *
     * <p>
     *     Note - only the first call to this method will be obeyed, subsequent requests will be ignored.
     * </p>
     *
     * @param view A not null View the size will be extracted async with an {@link android.view.ViewTreeObserver
     *             .OnPreDrawListener}
     */
    public void setView(View view) {
        if (this.size != null || viewTarget != null) {
            return;
        }
        this.viewTarget = new SizeViewTarget(view, this);
    }

    private static final class SizeViewTarget extends ViewTarget<View, Object> {

        public SizeViewTarget(View view, SizeReadyCallback callback) {
            super(view);
            getSize(callback);
        }

        @Override
        public void onResourceReady(Object resource, GlideAnimation glideAnimation) {
            // Do nothing
        }
    }
}
