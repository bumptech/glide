package com.bumptech.glide.request.bitmap;

import com.bumptech.glide.request.target.Target;

/**
 * A class for monitoring the status of a request while images load.
 *
 * @param <T> The type of the model being loaded
 */
public interface RequestListener<T> {

    /**
     * Called when an exception occurs during a load. Will only be called if we currently want to display an image
     * for the given model in the given target. It is recommended to create a single instance per activity/fragment
     * rather than instantiate a new object for each call to {@code Glide.load()} to avoid object churn.
     *
     * <p>
     *     It is safe to reload this or a different model or change what is displayed in the target at this point.
     *     For example:
     * <pre>
     * <code>
     *     public void onException(Exception e, ModelType model, Target target) {
     *         target.setPlaceholder(R.drawable.a_specific_error_for_my_exception);
     *         Glide.load(model).into(target);
     *     }
     * </code>
     * </pre>
     * </p>
     *
     * <p>
     *     Note - if you want to reload this or any other model after an exception, you will need to include all
     *     relevant builder calls (like centerCrop, placeholder etc).
     * </p>
     *
     * @param e The exception, or null
     * @param model The model we were trying to load when the exception occurred
     * @param target The {@link Target} we were trying to load the image into
     */
    public abstract void onException(Exception e, T model, Target target);

    /**
     * Called when a load completes successfully, immediately after
     * {@link Target#onImageReady(android.graphics.Bitmap)}.
     *
     * @param model The specific model that was used to load the image.
     * @param target The target the model was loaded into.
     */
    public abstract void onImageReady(T model, Target target, boolean isFromMemoryCache, boolean isAnyImageSet);
}
