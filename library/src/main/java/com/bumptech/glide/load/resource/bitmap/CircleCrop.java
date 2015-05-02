package com.bumptech.glide.load.resource.bitmap;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;

import java.security.MessageDigest;

/**
 * A Glide {@link BitmapTransformation} to circle crop an image.  Behaves similar to a
 * {@link FitCenter} transform, but the resulting image is masked to a circle.
 *
 * <p> Uses a PorterDuff blend mode, see http://ssp.impulsetrain.com/porterduff.html. </p>
 */
public class CircleCrop extends BitmapTransformation {
  private static final String ID = "com.bumptech.glide.load.resource.bitmap.CircleCrop";
  private static final byte[] ID_BYTES = ID.getBytes(CHARSET);

  public CircleCrop(Context context) {
    super(context);
  }

  public CircleCrop(BitmapPool bitmapPool) {
    super(bitmapPool);
  }

  // Bitmap doesn't implement equals, so == and .equals are equivalent here.
  @SuppressWarnings("PMD.CompareObjectsWithEquals")
  @Override
  protected Bitmap transform(BitmapPool pool, Bitmap toTransform, int outWidth, int outHeight) {
    final Bitmap toReuse = pool.get(outWidth, outHeight,
        toTransform.getConfig() != null ? toTransform.getConfig() : Config.ARGB_8888);
    Bitmap transformed = TransformationUtils.circleCrop(toReuse, toTransform, outWidth, outHeight);
    if (toReuse != null && toReuse != transformed && !pool.put(toReuse)) {
      toReuse.recycle();
    }
    return transformed;
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
  public void updateDiskCacheKey(MessageDigest messageDigest) {
    messageDigest.update(ID_BYTES);
  }
}
