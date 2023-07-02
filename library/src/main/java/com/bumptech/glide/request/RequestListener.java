package com.bumptech.glide.request;

import android.graphics.drawable.Drawable;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;

/**
 * A class for monitoring the status of a request while images load.
 *
 * <p>All methods in this interface will be called from a background thread if the {@code
 * RequestListener} is added to a request that is started with {@link RequestBuilder#submit()},
 * {@link RequestBuilder#submit(int, int)}, or {@link RequestBuilder#into(int, int)}. Those methods
 * no longer post results back to the main thread to avoid the unnecessary thread interactions and
 * corresponding latency. As a side affect though, listeners added to those requests are no longer
 * called on the main thread. {@code RequestListeners} added to requests started with {@link
 * RequestBuilder#into(Target)} or {@link RequestBuilder#into(ImageView)} will continue to be called
 * back on the main thread.
 *
 * @param <R> The type of resource being loaded.
 */
public interface RequestListener<R> {

  /**
   * Called when an exception occurs during a load, immediately before {@link
   * Target#onLoadFailed(Drawable)}. Will only be called if we currently want to display an image
   * for the given model in the given target. It is recommended to create a single instance per
   * activity/fragment rather than instantiate a new object for each call to {@code
   * Glide.with(fragment/activity).load()} to avoid object churn.
   *
   * <p>It is not safe to reload this or a different model in this callback. If you need to do so
   * use {@link com.bumptech.glide.RequestBuilder#error(RequestBuilder)} instead.
   *
   * <p>Although you can't start an entirely new load, it is safe to change what is displayed in the
   * {@link Target} at this point, as long as you return {@code true} from the method to prevent
   * {@link Target#onLoadFailed(Drawable)} from being called.
   *
   * <p>For threading guarantees, see the class comment.
   *
   * <p>For example:
   *
   * <pre>{@code
   * public boolean onLoadFailed(Exception e, T model, Target target, boolean isFirstResource) {
   *     target.setPlaceholder(R.drawable.a_specific_error_for_my_exception);
   *     return true; // Prevent onLoadFailed from being called on the Target.
   * }
   * }</pre>
   *
   * @param e The maybe {@code null} exception containing information about why the request failed.
   * @param model The model we were trying to load when the exception occurred.
   * @param target The {@link Target} we were trying to load the image into.
   * @param isFirstResource {@code true} if this exception is for the first resource to load.
   * @return {@code true} to prevent {@link Target#onLoadFailed(Drawable)} from being called on
   *     {@code target}, typically because the listener wants to update the {@code target} or the
   *     object the {@code target} wraps itself or {@code false} to allow {@link
   *     Target#onLoadFailed(Drawable)} to be called on {@code target}.
   */
  boolean onLoadFailed(
      @Nullable GlideException e,
      @Nullable Object model,
      @NonNull Target<R> target,
      boolean isFirstResource);

  /**
   * Called when a load completes successfully, immediately before {@link
   * Target#onResourceReady(Object, com.bumptech.glide.request.transition.Transition)}.
   *
   * <p>For threading guarantees, see the class comment.
   *
   * @param resource The resource that was loaded for the target. Non-null because a null resource
   *     will result in a call to {@link #onLoadFailed(GlideException, Object, Target, boolean)}
   *     instead of this method.
   * @param model The specific model that was used to load the image. Non-null because a null model
   *     will result in a call to {@link #onLoadFailed(GlideException, Object, Target, boolean)}
   *     instead of this method.
   * @param target The target the model was loaded into.
   * @param dataSource The {@link DataSource} the resource was loaded from.
   * @param isFirstResource {@code true} if this is the first resource to in this load to be loaded
   *     into the target. For example when loading a thumbnail and a full-sized image, this will be
   *     {@code true} for the first image to load and {@code false} for the second.
   * @return {@code true} to prevent {@link Target#onResourceReady(Object, Transition)} from being
   *     called on {@code target}, typically because the listener wants to update the {@code target}
   *     or the object the {@code target} wraps itself or {@code false} to allow {@link
   *     Target#onResourceReady(Object, Transition)} to be called on {@code target}.
   */
  boolean onResourceReady(
      @NonNull R resource,
      @NonNull Object model,
      Target<R> target,
      @NonNull DataSource dataSource,
      boolean isFirstResource);
}
