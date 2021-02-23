package com.bumptech.glide.integration.piex;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.DisplayMetrics;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.ArrayPool;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapResource;
import com.bumptech.glide.load.resource.bitmap.Downsampler;
import com.bumptech.glide.load.resource.bitmap.StreamBitmapDecoder;
import com.bumptech.glide.load.resource.bitmap.TransformationUtils;
import com.google.photos.editing.raw.android.libraries.piex.Piex;
import com.google.photos.editing.raw.android.libraries.piex.Piex.PreviewImageData;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

/**
 * A simple {@link com.bumptech.glide.load.ResourceDecoder} that gets the preview image from RAW
 * files using Piex.
 */
public final class PiexResourceDecoder implements ResourceDecoder<InputStream, Bitmap> {
  private final BitmapPool bitmapPool;
  private final StreamBitmapDecoder jpegBitmapDecoder;

  /**
   * Constructor for a new {@link load.resource.bitmap.StreamBitmapDecoder} using the given {@link
   * load.engine.bitmap_recycle.BitmapPool}.
   */
  public PiexResourceDecoder(Context context, BitmapPool bitmapPool, ArrayPool arrayPool) {
    this.bitmapPool = bitmapPool;

    DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
    this.jpegBitmapDecoder =
        new StreamBitmapDecoder(
            new Downsampler(Collections.emptyList(), displayMetrics, bitmapPool, arrayPool),
            arrayPool);
  }

  /**
   * Constructor for a new {@link load.resource.bitmap.StreamBitmapDecoder} using the given {@link
   * load.engine.bitmap_recycle.BitmapPool}.
   */
  public PiexResourceDecoder(Glide glide) {
    this(glide.getContext(), glide.getBitmapPool(), glide.getArrayPool());
  }

  /**
   * Finds out if the given {@link java.io.InputStream} contains a RAW image using {@link
   * com.google.photos.editing.raw.android.libraries.piex.Piex}.
   *
   * @throws IOException if the strem can not be marked or reset.
   */
  @Override
  public boolean handles(InputStream source, Options options) throws IOException {
    return Piex.isRaw(source);
  }

  /**
   * Returns a {@link load.resource.bitmap.BitmapResource} in the desired width and height from an
   * {@link java.io.InputStream}.
   *
   * @throws IOException if the Jpeg data is invalid.
   */
  @Override
  public Resource<Bitmap> decode(InputStream source, int width, int height, Options options)
      throws IOException {
    PreviewImageData previewImageData = Piex.getPreviewImageData(source);
    ByteArrayInputStream jpegPreview = new ByteArrayInputStream(previewImageData.previewImage);
    Resource<Bitmap> resource = jpegBitmapDecoder.decode(jpegPreview, width, height, options);

    // TODO(b/19834399): We need to apply color correction to the bitmap.
    Bitmap bitmap = resource.get();

    Bitmap rotated =
        TransformationUtils.rotateImageExif(
            bitmapPool,
            bitmap,
            TransformationUtils.getExifOrientationDegrees((int) previewImageData.exifOrientation));
    if (!bitmap.equals(rotated)) {
      resource.recycle();
    }

    return BitmapResource.obtain(rotated, bitmapPool);
  }
}
