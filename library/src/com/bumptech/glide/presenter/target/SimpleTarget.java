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
     * synchronously with {@link #getWidth()} and {@link #getHeight()}
     *
     * @param cb The callback that must be called when the size of the target has been determined
     */
    @Override
    public void getSize(SizeReadyCallback cb) {
        cb.onSizeReady(getWidth(), getHeight());
    }

    /**
     * @return The height of this target, which will be used to determine how to load and crop the Bitmap.
     */
    protected abstract int getWidth();

    /**
     * @return The width of this target, which will be used to determine how to load and crop the Bitmap.
     */
    protected abstract int getHeight();
}
