package com.bumptech.glide.request.target;

import android.graphics.Bitmap;
import android.widget.ImageView;

/**
 * A {@link com.bumptech.glide.request.target.Target} that can display an {@link
 * android.graphics.Bitmap} in an {@link android.widget.ImageView}.
 */
public class BitmapImageViewTarget extends ImageViewTarget<Bitmap> {
  // Public API.
  @SuppressWarnings("WeakerAccess")
  public BitmapImageViewTarget(ImageView view) {
    super(view);
  }

  /**
   * @deprecated Use {@link #waitForLayout()} instead.
   */
  // Public API.
  @SuppressWarnings({"unused", "deprecation"})
  @Deprecated
  public BitmapImageViewTarget(ImageView view, boolean waitForLayout) {
    super(view, waitForLayout);
  }

  /**
   * Sets the {@link android.graphics.Bitmap} on the view using {@link
   * android.widget.ImageView#setImageBitmap(android.graphics.Bitmap)}.
   *
   * @param resource The bitmap to display.
   */
  @Override
  protected void setResource(Bitmap resource) {
    view.setImageBitmap(resource);
  }
}
