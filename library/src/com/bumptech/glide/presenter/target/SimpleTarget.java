package com.bumptech.glide.presenter.target;

import android.graphics.drawable.Drawable;
import android.view.animation.Animation;
import com.bumptech.glide.presenter.ImagePresenter;

/**
 * A simpler interface for targets with default (usually noop) implementations of non essential methods.
 */
public abstract class SimpleTarget extends BaseTarget {

    @Override
    public void setPlaceholder(Drawable placeholder) { }

    @Override
    public void startAnimation(Animation animation) { }

    /**
     * A default implementation that calls {@link com.bumptech.glide.presenter.target.Target.SizeReadyCallback}
     * synchronously with {@link #getSize()}
     *
     * @param cb The callback that must be called when the size of the target has been determined
     */
    @Override
    public void getSize(SizeReadyCallback cb) {
        final int[] size = getSize();
        cb.onSizeReady(size[0], size[1]);
    }

    /**
     * Synchronously return the dimensions of this target as [width, height]
     *
     * @return The dimensions of this target
     */
    protected abstract int[] getSize();
}
