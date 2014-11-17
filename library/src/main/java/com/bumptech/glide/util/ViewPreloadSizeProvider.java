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
 * @param <T>
 */
public class ViewPreloadSizeProvider<T> implements ListPreloader.PreloadSizeProvider<T>, SizeReadyCallback {
    private int[] size = null;
    private SizeViewTarget viewTarget;

    public ViewPreloadSizeProvider() {
        // This constructor is intentionally empty. Nothing special is needed here.
    }

    /**
     * A {@link com.bumptech.glide.ListPreloader.PreloadSizeProvider} that will extract the preload size from a given
     * {@link android.view.View}.
     * @param view A not null View the size will be extracted async with an {@link android.view.ViewTreeObserver
     *             .OnPreDrawListener}
     */
    public ViewPreloadSizeProvider(View view) {
        setView(view);
    }

    @Override
    public int[] getPreloadSize(T item, int position) {
        if (size == null) {
            return null;
        } else {
            return Arrays.copyOf(this.size, this.size.length);
        }
    }

    @Override
    public void onSizeReady(int width, int height) {
        this.size = new int[]{width, height};
        this.viewTarget = null;
    }

    /**
     * Set the {@link android.view.View} the size will be extracted.
     * @param view A not null View the size will be extracted async with an {@link android.view.ViewTreeObserver
     *             .OnPreDrawListener}
     */
    public void setView(View view) {
        if (this.viewTarget == null) {
            this.viewTarget = new SizeViewTarget(view, this);
        }
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
