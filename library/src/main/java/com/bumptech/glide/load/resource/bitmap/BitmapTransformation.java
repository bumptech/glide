package com.bumptech.glide.load.resource.bitmap;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.util.Util;

/**
 * A simple {@link com.bumptech.glide.load.Transformation} for transforming
 * {@link android.graphics.Bitmap}s that abstracts away dealing with
 * {@link com.bumptech.glide.load.engine.Resource} objects for subclasses.
 *
 * Use cases will look something like this:
 * <pre>
 * <code>
 * public class FillSpace extends BaseBitmapTransformation {
 *     private static final String ID = "com.bumptech.glide.transformations.FillSpace";
 *     private static final String ID_BYTES = ID.getBytes(STRING_CHARSET_NAME);
 *
 *     {@literal @Override}
 *     public Bitmap transform(BitmapPool pool, Bitmap toTransform, int outWidth, int outHeight) {
 *         if (toTransform.getWidth() == outWidth && toTransform.getHeight() == outHeight) {
 *             return toTransform;
 *         }
 *
 *         return Bitmap.createScaledBitmap(toTransform, outWidth, outHeight, true);
 *     }
 *
 *     {@literal @Override}
 *     public void equals(Object o) {
 *       return o instanceof FillSpace;
 *     }
 *
 *     {@literal @Override}
 *     public int hashCode() {
 *       return ID.hashCode();
 *     }
 *
 *     {@literal @Override}
 *     public void updateDiskCacheKey(MessageDigest messageDigest)
 *         throws UnsupportedEncodingException {
 *       messageDigest.update(ID_BYTES);
 *     }
 * }
 * </code>
 * </pre>
 */
public abstract class BitmapTransformation implements Transformation<Bitmap> {

  private final BitmapPool bitmapPool;

  public BitmapTransformation(Context context) {
    this(Glide.get(context).getBitmapPool());
  }

  public BitmapTransformation(BitmapPool bitmapPool) {
    this.bitmapPool = bitmapPool;
  }

  @Override
  public final Resource<Bitmap> transform(Resource<Bitmap> resource, int outWidth, int outHeight) {
    if (!Util.isValidDimensions(outWidth, outHeight)) {
      throw new IllegalArgumentException(
          "Cannot apply transformation on width: " + outWidth + " or height: " + outHeight
              + " less than or equal to zero and not Target.SIZE_ORIGINAL");
    }
    Bitmap toTransform = resource.get();
    int targetWidth = outWidth == Target.SIZE_ORIGINAL ? toTransform.getWidth() : outWidth;
    int targetHeight = outHeight == Target.SIZE_ORIGINAL ? toTransform.getHeight() : outHeight;
    Bitmap transformed = transform(bitmapPool, toTransform, targetWidth, targetHeight);

    final Resource<Bitmap> result;
    if (toTransform.equals(transformed)) {
      result = resource;
    } else {
      result = BitmapResource.obtain(transformed, bitmapPool);
    }
    return result;
  }

  /**
   * Transforms the given {@link android.graphics.Bitmap} based on the given dimensions and returns
   * the transformed result.
   *
   * <p>The provided Bitmap, toTransform, should not be recycled or returned to the pool. Glide will
   * automatically recycle and/or reuse toTransform if the transformation returns a different
   * Bitmap. Similarly implementations should never recycle or return Bitmaps that are returned as
   * the result of this method. Recycling or returning the provided and/or the returned Bitmap to
   * the pool will lead to a variety of runtime exceptions and drawing errors. See #408 for an
   * example. If the implementation obtains and discards intermediate Bitmaps, they may safely be
   * returned to the BitmapPool and/or recycled.
   *
   * <p>outWidth and outHeight will never be
   * {@link com.bumptech.glide.request.target.Target#SIZE_ORIGINAL},
   * this class converts them to be the size of the Bitmap we're going to transform before calling
   * this method.
   *
   * @param pool        A {@link com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool} that can
   *                    be used to obtain and return intermediate {@link Bitmap}s used in this
   *                    transformation. For every {@link android.graphics.Bitmap} obtained from the
   *                    pool during this transformation, a {@link android.graphics.Bitmap} must also
   *                    be returned.
   * @param toTransform The {@link android.graphics.Bitmap} to transform.
   * @param outWidth    The ideal width of the transformed bitmap (the transformed width does not
   *                    need to match exactly).
   * @param outHeight   The ideal height of the transformed bitmap (the transformed height does not
   *                    need to match exactly).
   */
  protected abstract Bitmap transform(@NonNull BitmapPool pool, @NonNull Bitmap toTransform,
      int outWidth, int outHeight);
}
