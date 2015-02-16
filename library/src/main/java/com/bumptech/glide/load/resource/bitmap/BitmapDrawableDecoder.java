package com.bumptech.glide.load.resource.bitmap;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;

import java.io.IOException;
import java.util.Map;

/**
 * Decodes an {@link android.graphics.drawable.BitmapDrawable} for a data type.
 *
 * @param <DataType> The type of data that will be decoded.
 */
public class BitmapDrawableDecoder<DataType> implements ResourceDecoder<DataType, BitmapDrawable> {

  private final Resources resources;
  private final BitmapPool bitmapPool;
  private final ResourceDecoder<DataType, Bitmap> wrapped;

  public BitmapDrawableDecoder(Context context, ResourceDecoder<DataType, Bitmap> decoder) {
    this(context.getResources(), Glide.get(context).getBitmapPool(), decoder);
  }

  public BitmapDrawableDecoder(Resources resources, BitmapPool bitmapPool,
      ResourceDecoder<DataType, Bitmap> wrapped) {
    this.resources = resources;
    this.bitmapPool = bitmapPool;
    this.wrapped = wrapped;
  }

  @Override
  public boolean handles(DataType source) throws IOException {
    return wrapped.handles(source);
  }

  @Override
  public Resource<BitmapDrawable> decode(DataType source, int width, int height,
      Map<String, Object> options) throws IOException {
    Resource<Bitmap> bitmapResource = wrapped.decode(source, width, height, options);
    if (bitmapResource == null) {
      return null;
    }

    BitmapDrawable drawable = new BitmapDrawable(resources, bitmapResource.get());
    return new BitmapDrawableResource(drawable, bitmapPool);
  }
}
