package com.bumptech.glide.request.target;

import android.graphics.drawable.Drawable;
import android.widget.ImageView;

/**
 * Efficiently displays multiple Drawables loaded serially into a single {@link android.view.View}.
 */
public class DrawableThumbnailImageViewTarget extends ThumbnailImageViewTarget<Drawable> {
  public DrawableThumbnailImageViewTarget(ImageView view) {
    super(view);
  }

  @Override
  protected Drawable getDrawable(Drawable resource) {
    return resource;
  }
}
