package com.bumptech.glide.load.resource.drawable;

import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapDrawableResource;

/**
 * Handles generic {@link Drawable} types where we may be uncertain of their size or type.
 */
final class DrawableResourceImpl extends DrawableResource<Drawable> {

  @SuppressWarnings("unchecked")
  public static Resource<Drawable> newInstance(Drawable drawable, BitmapPool bitmapPool) {
    if (drawable instanceof BitmapDrawable) {
      BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
      return (Resource<Drawable>) (Resource<? extends Drawable>)
          new BitmapDrawableResource(bitmapDrawable, bitmapPool);
    }
    return new DrawableResourceImpl(drawable);
  }

  private DrawableResourceImpl(Drawable drawable) {
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
