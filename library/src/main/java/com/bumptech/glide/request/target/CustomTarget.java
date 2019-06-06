package com.bumptech.glide.request.target;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.request.Request;
import com.bumptech.glide.request.transition.Transition;
import com.bumptech.glide.util.Util;

/**
 * A base {@link Target} for loading resources ({@link android.graphics.Bitmap}, {@link Drawable}
 * etc) that are used outside of {@link android.view.View}s.
 *
 * <p>If you're loading a resource into a {@link View}, use {@link
 * com.bumptech.glide.RequestBuilder#into(ImageView)}, a subclass of {@link ImageViewTarget}, or
 * {@link CustomViewTarget}. Using this class to load resources into {@link View}s can prevent Glide
 * from correctly cancelling any previous loads, which may result in incorrect images appearing in
 * the view, especially in scrolling views like {@link androidx.recyclerview.widget.RecyclerView}.
 *
 * <p>You <em>MUST</em> implement {@link #onLoadCleared(Drawable)} and ensure that all references to
 * any resource passed into the target in {@link #onResourceReady(Object, Transition)} are removed
 * before {@link #onLoadCleared(Drawable)} completes. Failing to do so can result in graphical
 * corruption, crashes caused by recycled {@link Bitmap}s, and other undefined behavior. It is never
 * safe to leave {@link #onLoadCleared(Drawable)} unimplemented or empty. Even if you do not
 * manually clear this {@link Target}, Glide may do so automatically after certain lifecycle events
 * in {@link androidx.fragment.app.Fragment}s and {@link android.app.Activity}s.
 *
 * <p>This class can only be used with {@link Target#SIZE_ORIGINAL} or when the desired resource
 * dimensions are known when the {@link Target} is created. If you'd like to run some asynchronous
 * process and make full use of {@link #getSize(SizeReadyCallback)} and {@link SizeReadyCallback},
 * extend {@link Target} directly instead of using this class.
 *
 * @param <T> The type of resource that will be loaded (e.g. {@link Bitmap}).
 */
public abstract class CustomTarget<T> implements Target<T> {

  private final int width;
  private final int height;

  @Nullable private Request request;

  /**
   * Creates a new {@link CustomTarget} that will attempt to load the resource in its original size.
   *
   * <p>This constructor can cause very memory inefficient loads if the resource is large and can
   * cause OOMs. It's provided as a convenience for when you'd like to specify dimensions with
   * {@link com.bumptech.glide.request.RequestOptions#override(int)}. In all other cases, prefer
   * {@link #CustomTarget(int, int)}.
   */
  public CustomTarget() {
    this(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL);
  }

  /**
   * Creates a new {@code CustomTarget} that will return the given {@code width} and {@code height}
   * as the requested size (unless overridden by {@link
   * com.bumptech.glide.request.RequestOptions#override(int)} in the request).
   *
   * @param width The requested width (> 0, or == Target.SIZE_ORIGINAL).
   * @param height The requested height (> 0, or == Target.SIZE_ORIGINAL).
   * @throws IllegalArgumentException if width/height doesn't meet (> 0, or == Target.SIZE_ORIGINAL)
   */
  public CustomTarget(int width, int height) {
    if (!Util.isValidDimensions(width, height)) {
      throw new IllegalArgumentException(
          "Width and height must both be > 0 or Target#SIZE_ORIGINAL, but given"
              + " width: "
              + width
              + " and height: "
              + height);
    }

    this.width = width;
    this.height = height;
  }

  @Override
  public void onStart() {
    // Intentionally empty, this can be optionally implemented by subclasses.
  }

  @Override
  public void onStop() {
    // Intentionally empty, this can be optionally implemented by subclasses.
  }

  @Override
  public void onDestroy() {
    // Intentionally empty, this can be optionally implemented by subclasses.
  }

  @Override
  public void onLoadStarted(@Nullable Drawable placeholder) {
    // Intentionally empty, this can be optionally implemented by subclasses.
  }

  @Override
  public void onLoadFailed(@Nullable Drawable errorDrawable) {
    // Intentionally empty, this can be optionally implemented by subclasses.
  }

  @Override
  public final void getSize(@NonNull SizeReadyCallback cb) {
    cb.onSizeReady(width, height);
  }

  @Override
  public final void removeCallback(@NonNull SizeReadyCallback cb) {
    // Do nothing, this class does not retain SizeReadyCallbacks.
  }

  @Override
  public final void setRequest(@Nullable Request request) {
    this.request = request;
  }

  @Nullable
  @Override
  public final Request getRequest() {
    return request;
  }
}
