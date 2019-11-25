package com.bumptech.glide.load.resource.bitmap;

import android.graphics.Bitmap;
import androidx.annotation.NonNull;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.util.Util;
import java.nio.ByteBuffer;
import java.security.MessageDigest;

/** A {@link BitmapTransformation} which has a different radius for each corner of a bitmap. */
public final class GranularRoundedCorners extends BitmapTransformation {
  private static final String ID = "com.bumptech.glide.load.resource.bitmap.GranularRoundedCorners";
  private static final byte[] ID_BYTES = ID.getBytes(CHARSET);

  private final float topLeft;
  private final float topRight;
  private final float bottomRight;
  private final float bottomLeft;

  /** Provide the radii to round the corners of the bitmap. */
  public GranularRoundedCorners(
      float topLeft, float topRight, float bottomRight, float bottomLeft) {
    this.topLeft = topLeft;
    this.topRight = topRight;
    this.bottomRight = bottomRight;
    this.bottomLeft = bottomLeft;
  }

  @Override
  protected Bitmap transform(
      @NonNull BitmapPool pool, @NonNull Bitmap toTransform, int outWidth, int outHeight) {
    return TransformationUtils.roundedCorners(
        pool, toTransform, topLeft, topRight, bottomRight, bottomLeft);
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof GranularRoundedCorners) {
      GranularRoundedCorners other = (GranularRoundedCorners) o;
      return topLeft == other.topLeft
          && topRight == other.topRight
          && bottomRight == other.bottomRight
          && bottomLeft == other.bottomLeft;
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hashCode = Util.hashCode(ID.hashCode(), Util.hashCode(topLeft));
    hashCode = Util.hashCode(topRight, hashCode);
    hashCode = Util.hashCode(bottomRight, hashCode);
    return Util.hashCode(bottomLeft, hashCode);
  }

  @Override
  public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {
    messageDigest.update(ID_BYTES);

    byte[] radiusData =
        ByteBuffer.allocate(16)
            .putFloat(topLeft)
            .putFloat(topRight)
            .putFloat(bottomRight)
            .putFloat(bottomLeft)
            .array();
    messageDigest.update(radiusData);
  }
}
