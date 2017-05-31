package com.bumptech.glide.load.resource.bitmap;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.util.Preconditions;
import java.io.IOException;

/**
 * Decodes an {@link android.graphics.drawable.BitmapDrawable} for a data type.
 *
 * @param <DataType> The type of data that will be decoded.
 */
public class BitmapDrawableDecoder<DataType> implements ResourceDecoder<DataType, BitmapDrawable> {

  private final ResourceDecoder<DataType, Bitmap> decoder;
  private final Resources resources;
  private final BitmapPool bitmapPool;

  public BitmapDrawableDecoder(Context context, ResourceDecoder<DataType, Bitmap> decoder) {
    this(context.getResources(), Glide.get(context).getBitmapPool(), decoder);
  }

  public BitmapDrawableDecoder(Resources resources, BitmapPool bitmapPool,
      ResourceDecoder<DataType, Bitmap> decoder) {
    this.resources = Preconditions.checkNotNull(resources);
    this.bitmapPool = Preconditions.checkNotNull(bitmapPool);
    this.decoder = Preconditions.checkNotNull(decoder);
  }

  @Override
  public boolean handles(DataType source, Options options) throws IOException {
    return decoder.handles(source, options);
  }

  @Override
  public Resource<BitmapDrawable> decode(DataType source, int width, int height, Options options)
      throws IOException {
    Resource<Bitmap> bitmapResource = decoder.decode(source, width, height, options);
    if (bitmapResource == null) {
      return null;
    }

    return LazyBitmapDrawableResource.obtain(resources, bitmapPool, bitmapResource.get());
  }
}
