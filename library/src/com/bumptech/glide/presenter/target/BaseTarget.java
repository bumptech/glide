package com.bumptech.glide.presenter.target;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.animation.Animation;
import com.bumptech.glide.Glide;
import com.bumptech.glide.presenter.ImagePresenter;
import com.bumptech.glide.presenter.Presenter;

/**
 * A base {@link Target} for loading {@link Bitmap}s that provides basic or empty implementations for most methods.
 *
 * <p>
 *     For maximum efficiency, clear this target when you have finished using or displaying the {@link Bitmap} loaded
 *     into it using {@link Glide#clear(Target)}.
 * </p>
 *
 * <p>
 *     For loading {@link Bitmap}s into {@link View}s, {@link ViewTarget} is preferable to this class.
 * </p>
 */
public abstract class BaseTarget implements Target {

    private Presenter presenter;

    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public Presenter getPresenter() {
        return presenter;
    }

    @Override
    public void setPlaceholder(Drawable placeholder) { }

    @Override
    public void startAnimation(Animation animation) { }
}
