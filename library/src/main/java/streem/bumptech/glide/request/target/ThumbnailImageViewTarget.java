package com.bumptech.glide.request.target;

import android.graphics.drawable.Drawable;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.Nullable;

/**
 * Avoids extra calls to {@link android.view.View#requestLayout} when loading more than once image
 * into an {@link android.widget.ImageView} with fixed dimensions.
 *
 * <p>Typically it makes sense to use this class when loading multiple images with the {@link
 * com.bumptech.glide.RequestBuilder#thumbnail(com.bumptech.glide.RequestBuilder)} API into views in
 * a scrolling list like ListView, GridView, or RecyclerView.
 *
 * <p>{@link FixedSizeDrawable} may cause skewing or other undesirable behavior depending on your
 * images, views, and scaling. If this occurs, consider {@link DrawableImageViewTarget} or {@link
 * BitmapImageViewTarget} as alternatives.
 *
 * @param <T> The type of resource that will be displayed in the ImageView.
 */
// Public API.
@SuppressWarnings("WeakerAccess")
public abstract class ThumbnailImageViewTarget<T> extends ImageViewTarget<T> {

  public ThumbnailImageViewTarget(ImageView view) {
    super(view);
  }

  /** @deprecated Use {@link #waitForLayout()} insetad. */
  @Deprecated
  @SuppressWarnings({"deprecation"})
  public ThumbnailImageViewTarget(ImageView view, boolean waitForLayout) {
    super(view, waitForLayout);
  }

  @Override
  protected void setResource(@Nullable T resource) {
    ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
    Drawable result = getDrawable(resource);
    if (layoutParams != null && layoutParams.width > 0 && layoutParams.height > 0) {
      result = new FixedSizeDrawable(result, layoutParams.width, layoutParams.height);
    }

    view.setImageDrawable(result);
  }

  protected abstract Drawable getDrawable(T resource);
}
