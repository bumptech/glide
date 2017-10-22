package com.bumptech.glide.load.resource.bitmap;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import java.security.MessageDigest;

/**
 * Applies a {@link Bitmap} {@link Transformation} to {@link Drawable}s by first attempting to
 * convert the {@link Drawable} to a {@link Bitmap} and then running the {@link Transformation}
 * on the converted {@link Bitmap}.
 *
 * <p>This class is relatively efficient for {@link BitmapDrawable} where the {@link Bitmap} is
 * readily accessible. For non-{@link Bitmap} based {@link Drawable}s, this class must first try to
 * draw the {@link Drawable} to a {@link Bitmap} using {@link android.graphics.Canvas}, which is
 * less efficient. {@link Drawable}s that implement {@link android.graphics.drawable.Animatable}
 * will fail with an exception. {@link Drawable}s that return <= 0 for
 * {@link Drawable#getIntrinsicHeight()} and/or {@link Drawable#getIntrinsicWidth()} will fail
 * with an exception if the requested size is
 * {@link com.bumptech.glide.request.target.Target#SIZE_ORIGINAL}. {@link Drawable}s without
 * intrinsic dimensions are drawn using the dimensions provided in
 * {@link Transformation#transform(Context, Resource, int, int)}. As a result, they may be
 * transformed incorrectly or in unexpected ways.
 */
public class DrawableTransformation implements Transformation<Drawable> {

  private final Transformation<Bitmap> wrapped;

  public DrawableTransformation(Transformation<Bitmap> wrapped) {
    this.wrapped = wrapped;
  }

  @SuppressWarnings("unchecked")
  public Transformation<BitmapDrawable> asBitmapDrawable() {
   return (Transformation<BitmapDrawable>) (Transformation<?>) this;
  }

  @Override
  public Resource<Drawable> transform(Context context, Resource<Drawable> resource, int outWidth,
      int outHeight) {
    BitmapPool bitmapPool = Glide.get(context).getBitmapPool();
    Drawable drawable = resource.get();
    Bitmap bitmap = DrawableToBitmapConverter.convert(bitmapPool, drawable, outWidth, outHeight);
    if (bitmap == null) {
      throw new IllegalArgumentException("Unable to convert " + drawable + " to a Bitmap");
    }
    Resource<Bitmap> bitmapResourceToTransform = new BitmapResource(bitmap, bitmapPool);
    Resource<Bitmap> transformedBitmapResource =
        wrapped.transform(context, bitmapResourceToTransform, outWidth, outHeight);

    if (transformedBitmapResource.equals(bitmapResourceToTransform)) {
      transformedBitmapResource.recycle();
      // If we extracted the Bitmap from a StateListDrawable, or a BitmapDrawable then we can't
      // recycle it here. If on the other hand, we drew the Drawable to a Canvas, we can recycle
      // the resulting unused Bitmap.
      if (DrawableToBitmapConverter.willDraw(drawable)) {
        bitmapResourceToTransform.recycle();
      }
      return resource;
    } else {
      return newDrawableResource(context, transformedBitmapResource.get());
    }
  }

  @SuppressWarnings("unchecked")
  private Resource<Drawable> newDrawableResource(
      Context context, Bitmap transformed) {
    Resource<? extends Drawable> result =
        LazyBitmapDrawableResource.obtain(context, transformed);
    return (Resource<Drawable>) result;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof DrawableTransformation) {
      DrawableTransformation other = (DrawableTransformation) o;
      return wrapped.equals(other.wrapped);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return wrapped.hashCode();
  }

  @Override
  public void updateDiskCacheKey(MessageDigest messageDigest) {
    wrapped.updateDiskCacheKey(messageDigest);
  }
}
