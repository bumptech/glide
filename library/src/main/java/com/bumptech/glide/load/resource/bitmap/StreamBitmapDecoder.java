package com.bumptech.glide.load.resource.bitmap;

import android.content.Context;
import android.graphics.Bitmap;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;

import java.io.InputStream;
import java.util.Map;

/**
 * An {@link com.bumptech.glide.load.ResourceDecoder} that uses an {@link
 * com.bumptech.glide.load.resource.bitmap.Downsampler} to decode an {@link android.graphics.Bitmap}
 * from an {@link java.io.InputStream}.
 */
public class StreamBitmapDecoder implements ResourceDecoder<InputStream, Bitmap> {
  public static final String KEY_DOWNSAMPLE_STRATEGY =
      "com.bumptech.glide.load.resource.bitmap.StreamBitmapDecoder.DownsampleStrategy";

  private final BitmapPool bitmapPool;

  public StreamBitmapDecoder(Context context) {
    this(Glide.get(context).getBitmapPool());
  }

  public StreamBitmapDecoder(BitmapPool bitmapPool) {
    this.bitmapPool = bitmapPool;
  }

  @Override
  public boolean handles(InputStream source) {
    // TODO: we should probably consider options here.
    return true;
  }

  @Override
  public Resource<Bitmap> decode(InputStream source, int width, int height,
      Map<String, Object> options) {
    DownsampleStrategy strategy =
        options.containsKey(KEY_DOWNSAMPLE_STRATEGY) ? (DownsampleStrategy) options
            .get(KEY_DOWNSAMPLE_STRATEGY) : DownsampleStrategy.DEFAULT;
    Bitmap bitmap = strategy.downsampler.decode(source, bitmapPool, width, height, options);
    return BitmapResource.obtain(bitmap, bitmapPool);
  }
}
