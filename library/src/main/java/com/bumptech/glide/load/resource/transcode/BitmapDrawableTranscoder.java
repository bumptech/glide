package com.bumptech.glide.load.resource.transcode;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.support.annotation.NonNull;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.LazyBitmapDrawableResource;

/**
 * An {@link com.bumptech.glide.load.resource.transcode.ResourceTranscoder} that converts {@link
 * android.graphics.Bitmap}s into {@link android.graphics.drawable.BitmapDrawable}s.
 */
public class BitmapDrawableTranscoder implements ResourceTranscoder<Bitmap, BitmapDrawable> {
  private final Resources resources;

  // Public API.
  @SuppressWarnings("unused")
  public BitmapDrawableTranscoder(@NonNull Context context) {
    this(context.getResources());
  }

  /**
   * @deprecated Use {@link #BitmapDrawableTranscoder(Resources)}, {@code bitmapPool} is unused.
   */
  @Deprecated
  public BitmapDrawableTranscoder(
      @NonNull Resources resources, @SuppressWarnings("unused") BitmapPool bitmapPool) {
    this(resources);
  }

  public BitmapDrawableTranscoder(@NonNull Resources resources) {
    this.resources = resources;
  }

  @Override
  public Resource<BitmapDrawable> transcode(Resource<Bitmap> toTranscode, Options options) {
    return LazyBitmapDrawableResource.obtain(resources, toTranscode);
  }
}
