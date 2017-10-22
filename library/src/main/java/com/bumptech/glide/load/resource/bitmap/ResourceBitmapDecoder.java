package com.bumptech.glide.load.resource.bitmap;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.Nullable;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.drawable.ResourceDrawableDecoder;
import com.bumptech.glide.request.target.Target;
import java.io.IOException;

/**
 * Decodes {@link Bitmap}s from resource ids.
 *
 * <p>The framework will decode some resources as {@link Drawable}s that do not wrap
 * {@link Bitmap}s. This decoder will attempt to return a {@link Bitmap} for those
 * {@link Drawable}s anyway by drawing the {@link Drawable} to a {@link Canvas}s using
 * the {@link Drawable}'s intrinsic bounds or the dimensions provided to
 * {@link #decode(Object, int, int, Options)}.
 *
 * <p>For non-{@link Bitmap} {@link Drawable}s that return <= 0 for
 * {@link Drawable#getIntrinsicWidth()} and/or {@link Drawable#getIntrinsicHeight()}, this
 * decoder will fail if the width and height provided to {@link #decode(Object, int, int, Options)}
 * are {@link Target#SIZE_ORIGINAL}.
 */
public class ResourceBitmapDecoder implements ResourceDecoder<Uri, Bitmap> {

  private final ResourceDrawableDecoder drawableDecoder;
  private final BitmapPool bitmapPool;

  public ResourceBitmapDecoder(ResourceDrawableDecoder drawableDecoder, BitmapPool bitmapPool) {
    this.drawableDecoder = drawableDecoder;
    this.bitmapPool = bitmapPool;
  }

  @Override
  public boolean handles(Uri source, Options options) throws IOException {
    return ContentResolver.SCHEME_ANDROID_RESOURCE.equals(source.getScheme());
  }

  @Nullable
  @Override
  public Resource<Bitmap> decode(Uri source, int width, int height, Options options)
      throws IOException {
    Resource<Drawable> drawableResource = drawableDecoder.decode(source, width, height, options);
    if (drawableResource == null) {
      return null;
    }

    // Handle DrawableContainer or StateListDrawables that may contain one or more BitmapDrawables.
    Drawable drawable = drawableResource.get().getCurrent();

    Bitmap result = null;
    if (drawable instanceof BitmapDrawable) {
      result = ((BitmapDrawable) drawable).getBitmap();
    } else if (!(drawable instanceof Animatable)) {
      result = drawToBitmap(drawable, width, height);
    }

    if (result == null) {
      return null;
    }
    return new BitmapResource(result, bitmapPool);
  }

  private Bitmap drawToBitmap(Drawable drawable, int width, int height) {
    if (width == Target.SIZE_ORIGINAL && drawable.getIntrinsicWidth() <= 0) {
      throw new IllegalArgumentException("Unable to draw " + drawable + " to Bitmap with "
          + "Target.SIZE_ORIGINAL because the Drawable has no intrinsic width");
    }
    if (height == Target.SIZE_ORIGINAL && drawable.getIntrinsicHeight()  <= 0) {
      throw new IllegalArgumentException("Unable to draw " + drawable + " to Bitmap with "
          + "Target.SIZE_ORIGINAL because the Drawable has no intrinsic height");
    }
    int targetWidth = drawable.getIntrinsicWidth() > 0 ? drawable.getIntrinsicWidth() : width;
    int targetHeight = drawable.getIntrinsicHeight() > 0 ? drawable.getIntrinsicHeight() : height;
    Bitmap result = Bitmap.createBitmap(targetWidth, targetHeight, Config.ARGB_8888);
    Canvas canvas = new Canvas(result);
    drawable.setBounds(0, 0, targetWidth, targetHeight);
    drawable.draw(canvas);
    canvas.setBitmap(null);
    return result;
  }
}
