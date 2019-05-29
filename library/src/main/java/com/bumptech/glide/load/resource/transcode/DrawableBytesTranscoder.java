package com.bumptech.glide.load.resource.transcode;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapResource;
import com.bumptech.glide.load.resource.gif.GifDrawable;

/**
 * Obtains {@code byte[]} from {@link BitmapDrawable}s by delegating to a {@link ResourceTranscoder}
 * for {@link Bitmap}s to {@code byte[]}s.
 */
public final class DrawableBytesTranscoder implements ResourceTranscoder<Drawable, byte[]> {
  private final BitmapPool bitmapPool;
  private final ResourceTranscoder<Bitmap, byte[]> bitmapBytesTranscoder;
  private final ResourceTranscoder<GifDrawable, byte[]> gifDrawableBytesTranscoder;

  public DrawableBytesTranscoder(
      @NonNull BitmapPool bitmapPool,
      @NonNull ResourceTranscoder<Bitmap, byte[]> bitmapBytesTranscoder,
      @NonNull ResourceTranscoder<GifDrawable, byte[]> gifDrawableBytesTranscoder) {
    this.bitmapPool = bitmapPool;
    this.bitmapBytesTranscoder = bitmapBytesTranscoder;
    this.gifDrawableBytesTranscoder = gifDrawableBytesTranscoder;
  }

  @Nullable
  @Override
  public Resource<byte[]> transcode(
      @NonNull Resource<Drawable> toTranscode, @NonNull Options options) {
    Drawable drawable = toTranscode.get();
    if (drawable instanceof BitmapDrawable) {
      return bitmapBytesTranscoder.transcode(
          BitmapResource.obtain(((BitmapDrawable) drawable).getBitmap(), bitmapPool), options);
    } else if (drawable instanceof GifDrawable) {
      return gifDrawableBytesTranscoder.transcode(toGifDrawableResource(toTranscode), options);
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  @NonNull
  private static Resource<GifDrawable> toGifDrawableResource(@NonNull Resource<Drawable> resource) {
    return (Resource<GifDrawable>) (Resource<?>) resource;
  }
}
