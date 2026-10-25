package com.bumptech.glide.load.resource.bitmap;

import android.graphics.Bitmap;
import androidx.annotation.NonNull;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.util.Util;

/**
 * A {@link Resource} that wraps a {@link Bitmap} but does not recycle it.
 *
 * <p>This is useful when you want to pass a {@link Bitmap} to a component that would otherwise
 * recycle it, but you want to keep the {@link Bitmap} alive for another component to use.
 */
public final class NonOwnedBitmapResource implements Resource<Bitmap> {

  private final Bitmap bitmap;

  public NonOwnedBitmapResource(@NonNull Bitmap bitmap) {
    this.bitmap = bitmap;
  }

  @NonNull
  @Override
  public Class<Bitmap> getResourceClass() {
    return Bitmap.class;
  }

  @NonNull
  @Override
  public Bitmap get() {
    return bitmap;
  }

  @Override
  public int getSize() {
    return Util.getBitmapByteSize(bitmap);
  }

  @Override
  public void recycle() {
    // Do nothing.
  }
}
