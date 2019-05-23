package com.bumptech.glide.request.target;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

/**
 * Efficiently displays multiple Bitmaps loaded serially into a single {@link android.view.View}.
 */
// Public API.
@SuppressWarnings("unused")
public class BitmapThumbnailImageViewTarget extends ThumbnailImageViewTarget<Bitmap> {
  public BitmapThumbnailImageViewTarget(ImageView view) {
    super(view);
  }

  /** @deprecated Use {@link #waitForLayout()} instead. */
  @SuppressWarnings("deprecation")
  @Deprecated
  public BitmapThumbnailImageViewTarget(ImageView view, boolean waitForLayout) {
    super(view, waitForLayout);
  }

  @Override
  protected Drawable getDrawable(Bitmap resource) {
    return new BitmapDrawable(view.getResources(), resource);
  }
}
