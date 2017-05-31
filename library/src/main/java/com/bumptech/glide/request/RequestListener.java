package com.bumptech.glide.request;

import android.support.annotation.Nullable;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.target.Target;

/**
 * A class for monitoring the status of a request while images load.
 *
 * @param <R> The type of resource being loaded.
 */
public interface RequestListener<R> {

  /**
   * Called when an exception occurs during a load. Will only be called if we currently want to
   * display an image for the given model in the given target. It is recommended to create a single
   * instance per activity/fragment rather than instantiate a new object for each call to {@code
   * Glide.load()} to avoid object churn.
   *
   * <p> It is safe to reload this or a different model or change what is displayed in the target at
   * this point. For example:
   * <pre>
   * {@code
   * public void onLoadFailed(Exception e, T model, Target target, boolean isFirstResource) {
   *     target.setPlaceholder(R.drawable.a_specific_error_for_my_exception);
   *     Glide.load(model).into(target);
   * }
   * }
   * </pre>
   * </p>
   *
   * <p> Note - if you want to reload this or any other model after an exception, you will need to
   * include all relevant builder calls (like centerCrop, placeholder etc). </p>
   *
   * @param e               The maybe {@code null} exception containing information about why the
   *                        request failed.
   * @param model           The model we were trying to load when the exception occurred.
   * @param target          The {@link Target} we were trying to load the image into.
   * @param isFirstResource {@code true} if this exception is for the first resource to load.
   * @return {@code true} if the listener has handled updating the target for the given exception,
   *         {@code false} to allow Glide's request to update the target.
   */
  boolean onLoadFailed(@Nullable GlideException e, Object model, Target<R> target,
      boolean isFirstResource);

  /**
   * Called when a load completes successfully, immediately after {@link
   * Target#onResourceReady(Object, com.bumptech.glide.request.transition.Transition)}.
   *
   * @param resource          The resource that was loaded for the target.
   * @param model             The specific model that was used to load the image.
   * @param target            The target the model was loaded into.
   * @param dataSource        The {@link DataSource} the resource was loaded from.
   * @param isFirstResource   {@code true} if this is the first resource to in this load to be
   *                          loaded into the target. For example when loading a thumbnail and a
   *                          full-sized image, this will be {@code true} for the first image to
   *                          load and {@code false} for the second.
   * @return {@code true} if the listener has handled setting the resource on the target,
   *         {@code false} to allow Glide's request to update the target.
   *         Setting the resource includes handling animations, be sure to take that into account.
   */
  boolean onResourceReady(R resource, Object model, Target<R> target, DataSource dataSource,
      boolean isFirstResource);
}
