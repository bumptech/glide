package com.bumptech.glide.request;

import com.bumptech.glide.request.target.Target;

/**
 * A class for monitoring the status of a request while images load.
 *
 * @param <T> The type of the model being loaded.
 * @param <R> The type of resource being loaded.
 */
public interface RequestListener<T, R> {

    /**
     * Called when an exception occurs during a load. Will only be called if we currently want to display an image
     * for the given model in the given target. It is recommended to create a single instance per activity/fragment
     * rather than instantiate a new object for each call to {@code Glide.load()} to avoid object churn.
     *
     * <p>
     *     It is safe to reload this or a different model or change what is displayed in the target at this point.
     *     For example:
     * <pre>
     * {@code
     * public void onException(Exception e, T model, Target target, boolean isFirstResource) {
     *     target.setPlaceholder(R.drawable.a_specific_error_for_my_exception);
     *     Glide.load(model).into(target);
     * }
     * }
     * </pre>
     * </p>
     *
     * <p>
     *     Note - if you want to reload this or any other model after an exception, you will need to include all
     *     relevant builder calls (like centerCrop, placeholder etc).
     * </p>
     *
     * @param e The exception, or null.
     * @param model The model we were trying to load when the exception occurred.
     * @param target The {@link Target} we were trying to load the image into.
     * @param isFirstResource True if this exception is for the first resource to load.
     * @return True if the listener has handled updating the target for the given exception, false to allow
     *         Glide's request to update the target.
     */
    boolean onException(Exception e, T model, Target<R> target, boolean isFirstResource);

    /**
     * Called when a load completes successfully, immediately after
     * {@link Target#onResourceReady(Object, com.bumptech.glide.request.animation.GlideAnimation)}.
     *
     * @param resource The resource that was loaded for the target.
     * @param model The specific model that was used to load the image.
     * @param target The target the model was loaded into.
     * @param isFromMemoryCache True if the load completed synchronously (useful for determining whether or not to
     *                          animate)
     * @param isFirstResource True if this is the first resource to in this load to be loaded into the target. For
     *                        example when loading a thumbnail and a fullsize image, this will be true for the first
     *                        image to load and false for the second.
     * @return True if the listener has handled setting the resource on the target (including any animations), false to
     *         allow Glide's request to update the target (again including animations).
     */
    boolean onResourceReady(R resource, T model, Target<R> target, boolean isFromMemoryCache, boolean isFirstResource);
}
