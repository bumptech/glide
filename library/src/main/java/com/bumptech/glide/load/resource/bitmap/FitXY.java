package com.bumptech.glide.load.resource.bitmap;

import android.graphics.Bitmap;
import androidx.annotation.NonNull;
import com.bumptech.glide.load.MultiTransformation;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import java.security.MessageDigest;

/**
 * Scale the image so that fill the given dimension. usually used with make bitmap RoundCorners
 * use as a part of {@link MultiTransformation} with {@link GranularRoundedCorners} or {@link RoundedCorners}
 */
public class FitXY extends BitmapTransformation {
  private static final String ID = "com.bumptech.glide.load.resource.bitmap.FitXY";
  private static final byte[] ID_BYTES = ID.getBytes(CHARSET);

  @Override
  protected Bitmap transform(
      @NonNull BitmapPool pool, @NonNull Bitmap toTransform, int outWidth, int outHeight) {
    return TransformationUtils.fitXY(pool, toTransform, outWidth, outHeight);
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof FitXY;
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
