package com.bumptech.glide.request.target;

import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.manager.LifecycleListener;
import com.bumptech.glide.request.Request;
import com.bumptech.glide.request.transition.Transition;

/**
 * An interface that Glide can load a resource into and notify of relevant lifecycle events during a
 * load.
 *
 * <p>The lifecycle events in this class are as follows:
 *
 * <ul>
 *   <li>onLoadStarted
 *   <li>onResourceReady
 *   <li>onLoadCleared
 *   <li>onLoadFailed
 * </ul>
 *
 * The typical lifecycle is onLoadStarted -> onResourceReady or onLoadFailed -> onLoadCleared.
 * However, there are no guarantees. onLoadStarted may not be called if the resource is in memory or
 * if the load will fail because of a null model object. onLoadCleared similarly may never be called
 * if the target is never cleared. See the docs for the individual methods for details.
 *
 * @param <R> The type of resource the target can display.
 */
public interface Target<R> extends LifecycleListener {
  /** Indicates that we want the resource in its original unmodified width and/or height. */
  int SIZE_ORIGINAL = Integer.MIN_VALUE;

  /**
   * A lifecycle callback that is called when a load is started.
   *
   * <p>Note - This may not be called for every load, it is possible for example for loads to fail
   * before the load starts (when the model object is null).
   *
   * <p>Note - This method may be called multiple times before any other lifecycle method is called.
   * Loads can be paused and restarted due to lifecycle or connectivity events and each restart may
   * cause a call here.
   *
   * @param placeholder The placeholder drawable to optionally show, or null.
   */
  void onLoadStarted(@Nullable Drawable placeholder);

  /**
   * A <b>mandatory</b> lifecycle callback that is called when a load fails.
   *
   * <p>Note - This may be called before {@link #onLoadStarted(android.graphics.drawable.Drawable) }
   * if the model object is null.
   *
   * <p>You <b>must</b> ensure that any current Drawable received in {@link #onResourceReady(Object,
   * Transition)} is no longer used before redrawing the container (usually a View) or changing its
   * visibility.
   *
   * @param errorDrawable The error drawable to optionally show, or null.
   */
  void onLoadFailed(@Nullable Drawable errorDrawable);

  /**
   * The method that will be called when the resource load has finished.
   *
   * @param resource the loaded resource.
   */
  void onResourceReady(@NonNull R resource, @Nullable Transition<? super R> transition);

  /**
   * A <b>mandatory</b> lifecycle callback that is called when a load is cancelled and its resources
   * are freed.
   *
   * <p>You <b>must</b> ensure that any current Drawable received in {@link #onResourceReady(Object,
   * Transition)} is no longer used before redrawing the container (usually a View) or changing its
   * visibility.
   *
   * @param placeholder The placeholder drawable to optionally show, or null.
   */
  void onLoadCleared(@Nullable Drawable placeholder);

  /**
   * A method to retrieve the size of this target.
   *
   * @param cb The callback that must be called when the size of the target has been determined
   */
  void getSize(@NonNull SizeReadyCallback cb);

  /**
   * Removes the given callback from the pending set if it's still retained.
   *
   * @param cb The callback to remove.
   */
  void removeCallback(@NonNull SizeReadyCallback cb);

  /** Sets the current request for this target to retain, should not be called outside of Glide. */
  void setRequest(@Nullable Request request);

  /** Retrieves the current request for this target, should not be called outside of Glide. */
  @Nullable
  Request getRequest();
}
