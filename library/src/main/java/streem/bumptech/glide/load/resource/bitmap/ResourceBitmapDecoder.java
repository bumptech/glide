package com.bumptech.glide.load.resource.bitmap;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.drawable.ResourceDrawableDecoder;
import com.bumptech.glide.request.target.Target;

/**
 * Decodes {@link Bitmap}s from resource ids.
 *
 * <p>The framework will decode some resources as {@link Drawable}s that do not wrap {@link
 * Bitmap}s. This decoder will attempt to return a {@link Bitmap} for those {@link Drawable}s anyway
 * by drawing the {@link Drawable} to a {@link Canvas}s using the {@link Drawable}'s intrinsic
 * bounds or the dimensions provided to {@link #decode(Object, int, int, Options)}.
 *
 * <p>For non-{@link Bitmap} {@link Drawable}s that return <= 0 for {@link
 * Drawable#getIntrinsicWidth()} and/or {@link Drawable#getIntrinsicHeight()}, this decoder will
 * fail if the width and height provided to {@link #decode(Object, int, int, Options)} are {@link
 * Target#SIZE_ORIGINAL}.
 */
public class ResourceBitmapDecoder implements ResourceDecoder<Uri, Bitmap> {

  private final ResourceDrawableDecoder drawableDecoder;
  private final BitmapPool bitmapPool;

  public ResourceBitmapDecoder(ResourceDrawableDecoder drawableDecoder, BitmapPool bitmapPool) {
    this.drawableDecoder = drawableDecoder;
    this.bitmapPool = bitmapPool;
  }

  @Override
  public boolean handles(@NonNull Uri source, @NonNull Options options) {
    return ContentResolver.SCHEME_ANDROID_RESOURCE.equals(source.getScheme());
  }

  @Nullable
  @Override
  public Resource<Bitmap> decode(
      @NonNull Uri source, int width, int height, @NonNull Options options) {
    Resource<Drawable> drawableResource = drawableDecoder.decode(source, width, height, options);
    if (drawableResource == null) {
      return null;
    }
    Drawable drawable = drawableResource.get();
    return DrawableToBitmapConverter.convert(bitmapPool, drawable, width, height);
  }
}
