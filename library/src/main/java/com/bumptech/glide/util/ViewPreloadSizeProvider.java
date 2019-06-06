package com.bumptech.glide.util;

import android.graphics.drawable.Drawable;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.ListPreloader;
import com.bumptech.glide.request.target.CustomViewTarget;
import com.bumptech.glide.request.target.SizeReadyCallback;
import com.bumptech.glide.request.transition.Transition;
import java.util.Arrays;

/**
 * A {@link com.bumptech.glide.ListPreloader.PreloadSizeProvider} that will extract the preload size
 * from a given {@link android.view.View}.
 *
 * @param <T> The type of the model the size should be provided for.
 */
public class ViewPreloadSizeProvider<T>
    implements ListPreloader.PreloadSizeProvider<T>, SizeReadyCallback {
  private int[] size;
  // We need to keep a strong reference to the Target so that it isn't garbage collected due to a
  // weak reference
  // while we're waiting to get its size.
  @SuppressWarnings("unused")
  private SizeViewTarget viewTarget;

  /**
   * Constructor that does nothing by default and requires users to call {@link
   * #setView(android.view.View)} when a View is available to registerComponents the dimensions
   * returned by this class.
   */
  public ViewPreloadSizeProvider() {
    // This constructor is intentionally empty. Nothing special is needed here.
  }

  /**
   * Constructor that will extract the preload size from a given {@link android.view.View}.
   *
   * @param view A not null View the size will be extracted from async using an {@link
   *     android.view.ViewTreeObserver .OnPreDrawListener}
   */
  // Public API.
  @SuppressWarnings("WeakerAccess")
  public ViewPreloadSizeProvider(@NonNull View view) {
    viewTarget = new SizeViewTarget(view);
    viewTarget.getSize(this);
  }

  @Nullable
  @Override
  public int[] getPreloadSize(@NonNull T item, int adapterPosition, int itemPosition) {
    if (size == null) {
      return null;
    } else {
      return Arrays.copyOf(size, size.length);
    }
  }

  @Override
  public void onSizeReady(int width, int height) {
    size = new int[] {width, height};
    viewTarget = null;
  }

  /**
   * Sets the {@link android.view.View} the size will be extracted.
   *
   * <p>Note - only the first call to this method will be obeyed, subsequent requests will be
   * ignored.
   *
   * @param view A not null View the size will be extracted async with an {@link
   *     android.view.ViewTreeObserver .OnPreDrawListener}
   */
  public void setView(@NonNull View view) {
    if (size != null || viewTarget != null) {
      return;
    }
    viewTarget = new SizeViewTarget(view);
    viewTarget.getSize(this);
  }

  static final class SizeViewTarget extends CustomViewTarget<View, Object> {
    SizeViewTarget(@NonNull View view) {
      super(view);
    }

    @Override
    protected void onResourceCleared(@Nullable Drawable placeholder) {}

    @Override
    public void onLoadFailed(@Nullable Drawable errorDrawable) {}

    @Override
    public void onResourceReady(
        @NonNull Object resource, @Nullable Transition<? super Object> transition) {}
  }
}
