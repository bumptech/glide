package com.bumptech.glide.load.resource.bitmap;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import java.security.MessageDigest;

/**
 * Scale the image so that either the width of the image matches the given width and the height of
 * the image is greater than the given height or vice versa, and then crop the larger dimension to
 * match the given dimension.
 *
 * Does not maintain the image's aspect ratio
 */
public class CenterCrop extends BitmapTransformation {
  private static final String ID = "com.bumptech.glide.load.resource.bitmap.CenterCrop";
  private static final byte[] ID_BYTES = ID.getBytes(CHARSET);

  public CenterCrop(Context context) {
    super(context);
  }

  public CenterCrop(BitmapPool bitmapPool) {
    super(bitmapPool);
  }

  // Bitmap doesn't implement equals, so == and .equals are equivalent here.
  @SuppressWarnings("PMD.CompareObjectsWithEquals")
  @Override
  protected Bitmap transform(@NonNull BitmapPool pool, @NonNull Bitmap toTransform, int outWidth,
      int outHeight) {
    return TransformationUtils.centerCrop(pool, toTransform, outWidth, outHeight);
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof CenterCrop;
  }

  @Override
  public int hashCode() {
    return ID.hashCode();
  }

  @Override
  public void updateDiskCacheKey(MessageDigest messageDigest) {
    messageDigest.update(ID_BYTES);
  }
}
