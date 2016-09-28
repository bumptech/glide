package com.bumptech.glide.load.resource.bitmap;

import static android.os.Build.ID;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.FloatRange;

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;

import java.security.MessageDigest;

/**
 * Scale the image so that either the width of the image matches the given width and the height of
 * the image is greater than the given height or vice versa, and then crop the larger dimension to
 * match the given dimension.
 *
 * Using percentages the crop area can be adjusted
 *
 * Example usage:
 *
 * Crop top-left: new PositionedCrop(context, 0, 0);
 *
 * Crop top-right: new PositionedCrop(context, 0, 1);
 *
 * Crop bottom-right: new PositionedCrop(context, 1, 1);
 *
 * Crop top-center: new PositionedCrop(context, 0,5f, 0);
 *
 * Does not maintain the image's aspect ratio
 */
public class PositionedCrop extends BitmapTransformation {
  private static final byte[] ID_BYTES = ID.getBytes(CHARSET);

  private float xPercentage = 0.5f;
  private float yPercentage = 0.5f;

  public PositionedCrop(Context context, @FloatRange(from = 0.0, to = 1.0) float xPercentage, @FloatRange(from = 0.0, to = 1.0) float yPercentage) {
    super(context);
    this.xPercentage = xPercentage;
    this.yPercentage = yPercentage;
  }

  public PositionedCrop(BitmapPool bitmapPool, @FloatRange(from = 0.0, to = 1.0) float xPercentage, @FloatRange(from = 0.0, to = 1.0) float yPercentage) {
    super(bitmapPool);
    this.xPercentage = xPercentage;
    this.yPercentage = yPercentage;
  }

  // Bitmap doesn't implement equals, so == and .equals are equivalent here.
  @SuppressWarnings("PMD.CompareObjectsWithEquals")
  @Override
  protected Bitmap transform(BitmapPool pool, Bitmap toTransform, int outWidth, int outHeight) {

    return TransformationUtils.cropPosition(pool, toTransform, outWidth, outHeight, xPercentage, yPercentage);
  }

  public String getId() {
    return "PositionedCrop.com.bumptech.glide.load.resource.bitmap.x:" + xPercentage + ".y:" + yPercentage;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof PositionedCrop) {
      return ((PositionedCrop) o).xPercentage == xPercentage && ((PositionedCrop) o).yPercentage == yPercentage;
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return getId().hashCode();
  }

  @Override
  public void updateDiskCacheKey(MessageDigest messageDigest) {
    messageDigest.update(ID_BYTES);
  }
}
