package com.bumptech.glide.load.resource.drawable;

import android.graphics.drawable.Drawable;
import com.bumptech.glide.load.engine.Resource;

/**
 * Handles generic {@link Drawable} types where we may be uncertain of their size or type and
 * where we don't know that it's safe for us to recycle or re-use the Drawable.
 */
final class NonOwnedDrawableResource extends DrawableResource<Drawable> {

  @SuppressWarnings("unchecked")
  public static Resource<Drawable> newInstance(Drawable drawable) {
    return new NonOwnedDrawableResource(drawable);
  }

  private NonOwnedDrawableResource(Drawable drawable) {
    super(drawable);
  }

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
