package com.bumptech.glide.request.target;

import android.graphics.drawable.Drawable;
import android.view.animation.Animation;
import com.bumptech.glide.request.Request;

/**
 * An interface that Glide can load an image into
 *
 * @param <Z> The type of resource the target can display.
 */
public interface Target<Z> {

    /**
     * A callback that must be called when the target has determined its size. For fixed size targets it can
     * be called synchronously.
     */
    public interface SizeReadyCallback {
        public void onSizeReady(int width, int height);
    }

    /**
     * The method that will be called when the image load has finished
     * @param resource the loaded resource.
     */
    public void onResourceReady(Z resource);

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

    public void setRequest(Request request);

    public Request getRequest();
}
