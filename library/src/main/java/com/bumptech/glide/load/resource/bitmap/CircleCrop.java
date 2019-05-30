package com.bumptech.glide.load.resource.bitmap;

import android.graphics.Bitmap;
import androidx.annotation.NonNull;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import java.security.MessageDigest;

/**
 * A Glide {@link BitmapTransformation} to circle crop an image. Behaves similar to a {@link
 * FitCenter} transform, but the resulting image is masked to a circle.
 *
 * <p>Uses a PorterDuff blend mode, see http://ssp.impulsetrain.com/porterduff.html.
 */
public class CircleCrop extends BitmapTransformation {
  // The version of this transformation, incremented to correct an error in a previous version.
  // See #455.
  private static final int VERSION = 1;
  private static final String ID = "com.bumptech.glide.load.resource.bitmap.CircleCrop." + VERSION;
  private static final byte[] ID_BYTES = ID.getBytes(CHARSET);

  // Bitmap doesn't implement equals, so == and .equals are equivalent here.
  @SuppressWarnings("PMD.CompareObjectsWithEquals")
  @Override
  protected Bitmap transform(
      @NonNull BitmapPool pool, @NonNull Bitmap toTransform, int outWidth, int outHeight) {
    return TransformationUtils.circleCrop(pool, toTransform, outWidth, outHeight);
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof CircleCrop;
  }

  @Override
  public int hashCode() {
    return ID.hashCode();
  }

  @Override
  public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {
    messageDigest.update(ID_BYTES);
  }
}
