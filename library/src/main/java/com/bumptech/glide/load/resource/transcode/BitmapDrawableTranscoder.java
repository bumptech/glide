package com.bumptech.glide.load.resource.transcode;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.LazyBitmapDrawableResource;
import com.bumptech.glide.util.Preconditions;

/**
 * An {@link com.bumptech.glide.load.resource.transcode.ResourceTranscoder} that converts {@link
 * android.graphics.Bitmap}s into {@link android.graphics.drawable.BitmapDrawable}s.
 */
public class BitmapDrawableTranscoder implements ResourceTranscoder<Bitmap, BitmapDrawable> {
  private final Resources resources;
  private final BitmapPool bitmapPool;

  public BitmapDrawableTranscoder(Context context) {
    this(context.getResources(), Glide.get(context).getBitmapPool());
  }

  public BitmapDrawableTranscoder(Resources resources, BitmapPool bitmapPool) {
    this.resources = Preconditions.checkNotNull(resources);
    this.bitmapPool = Preconditions.checkNotNull(bitmapPool);
  }

  @Override
  public Resource<BitmapDrawable> transcode(Resource<Bitmap> toTranscode) {
    return LazyBitmapDrawableResource.obtain(resources, bitmapPool, toTranscode.get());
  }
}
