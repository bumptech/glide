package com.bumptech.glide.load.resource.bitmap;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import androidx.annotation.NonNull;
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

  // Public API.
  @SuppressWarnings({"unused", "WeakerAccess"})
  public BitmapDrawableDecoder(Context context, ResourceDecoder<DataType, Bitmap> decoder) {
    this(context.getResources(), decoder);
  }

  /**
   * @deprecated Use {@link #BitmapDrawableDecoder(Context, ResourceDecoder)}, {@code bitmapPool} is
   *     ignored.
   */
  @Deprecated
  public BitmapDrawableDecoder(
      Resources resources,
      @SuppressWarnings("unused") BitmapPool bitmapPool,
      ResourceDecoder<DataType, Bitmap> decoder) {
    this(resources, decoder);
  }

  public BitmapDrawableDecoder(
      @NonNull Resources resources, @NonNull ResourceDecoder<DataType, Bitmap> decoder) {
    this.resources = Preconditions.checkNotNull(resources);
    this.decoder = Preconditions.checkNotNull(decoder);
  }

  @Override
  public boolean handles(@NonNull DataType source, @NonNull Options options) throws IOException {
    return decoder.handles(source, options);
  }

  @Override
  public Resource<BitmapDrawable> decode(
      @NonNull DataType source, int width, int height, @NonNull Options options)
      throws IOException {
    Resource<Bitmap> bitmapResource = decoder.decode(source, width, height, options);
    return LazyBitmapDrawableResource.obtain(resources, bitmapResource);
  }
}
