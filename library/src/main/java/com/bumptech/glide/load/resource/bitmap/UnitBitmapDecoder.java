package com.bumptech.glide.load.resource.bitmap;

import android.graphics.Bitmap;
import android.support.annotation.Nullable;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import java.io.IOException;

public final class UnitBitmapDecoder implements ResourceDecoder<Bitmap, Bitmap> {

  private final BitmapPool bitmapPool;

  public UnitBitmapDecoder(BitmapPool bitmapPool) {
    this.bitmapPool = bitmapPool;
  }

  @Override
  public boolean handles(Bitmap source, Options options) throws IOException {
    return true;
  }

  @Nullable
  @Override
  public Resource<Bitmap> decode(Bitmap source, int width, int height, Options options)
      throws IOException {
    return new BitmapResource(source, bitmapPool);
  }
}
