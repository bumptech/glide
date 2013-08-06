package com.bumptech.glide.presenter.target;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.view.animation.Animation;
import com.bumptech.glide.presenter.ImagePresenter;

/**
 * An interface that Glide can load an image into
 */
public interface Target {

    /**
     * A callback that must be called when the target has determined its size. For fixed size targets it can
     * be called synchronously.
     */
    public interface SizeReadyCallback {
        public void onSizeReady(int width, int height);
    }

    /**
     * The method that will be called when the image load has finished
     * @param bitmap The loaded image
     */
    public void onImageReady(Bitmap bitmap);

    /**
     * A method that can optionally be implemented to set any placeholder that might have been passed to Glide to
     * display either while an image is loading or after the load has failed.
     *
     * @param placeholder The drawable to display
     */
    public void setPlaceholder(Drawable placeholder);

    /**
     * A method to retrieve the size of this target
     * @param cb The callback that must be called when the size of the target has been determined
     */
    public void getSize(SizeReadyCallback cb);

    /**
     * A method that can be optionally implemented to start any animation that might have been passed to Glide for this
     * target.
     *
     * @param animation The animation to display
     */
    public void startAnimation(Animation animation);

    /**
     * A method that is used to keep a reference to the ImagePresenter. Increases the efficiency of loads if it is
     * possible that this target object will be used more than once.
     *
     * @param imagePresenter The ImagePresenter to store
     */
    public void setImagePresenter(ImagePresenter imagePresenter);

    /**
     * A method for retrieving any current ImagePresenter. Increases the efficiency of loads if it is possible that this
     * target object will be used more than once.
     *
     * @return The current ImagePresenter or null
     */
    public ImagePresenter getImagePresenter();
}
