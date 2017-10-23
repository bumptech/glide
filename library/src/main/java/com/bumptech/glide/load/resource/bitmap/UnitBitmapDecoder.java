package com.bumptech.glide.load.resource.bitmap;

import android.graphics.Bitmap;
import android.support.annotation.Nullable;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPoolAdapter;
import java.io.IOException;

/**
 * Passes through a (hopefully) non-owned {@link Bitmap} as a {@link Bitmap} based {@link Resource}
 * so that the given {@link Bitmap} is not recycled.
 */
public final class UnitBitmapDecoder implements ResourceDecoder<Bitmap, Bitmap> {
  private static final BitmapPoolAdapter BITMAP_POOL = new BitmapPoolAdapter();

  @Override
  public boolean handles(Bitmap source, Options options) throws IOException {
    return true;
  }

  @Nullable
  @Override
  public Resource<Bitmap> decode(Bitmap source, int width, int height, Options options)
      throws IOException {
    return new BitmapResource(source, BITMAP_POOL);
  }
}
