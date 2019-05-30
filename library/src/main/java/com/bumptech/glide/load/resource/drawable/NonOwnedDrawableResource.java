package com.bumptech.glide.load.resource.drawable;

import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.load.engine.Resource;

/**
 * Handles generic {@link Drawable} types where we may be uncertain of their size or type and where
 * we don't know that it's safe for us to recycle or re-use the Drawable.
 */
final class NonOwnedDrawableResource extends DrawableResource<Drawable> {

  @SuppressWarnings("unchecked")
  @Nullable
  static Resource<Drawable> newInstance(@Nullable Drawable drawable) {
    return drawable != null ? new NonOwnedDrawableResource(drawable) : null;
  }

  private NonOwnedDrawableResource(Drawable drawable) {
    super(drawable);
  }

  @NonNull
  @SuppressWarnings("unchecked")
  @Override
  public Class<Drawable> getResourceClass() {
    return (Class<Drawable>) drawable.getClass();
  }

  @Override
  public int getSize() {
    // 4 bytes per pixel for ARGB_8888 Bitmaps is something of a reasonable approximation. If
    // there are no intrinsic bounds, we can fall back just to 1.
    return Math.max(1, drawable.getIntrinsicWidth() * drawable.getIntrinsicHeight() * 4);
  }

  @Override
  public void recycle() {
    // Do nothing.
  }
}
