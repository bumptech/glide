package com.bumptech.glide.load.resource.bitmap;

import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.graphics.ImageDecoder.Source;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import com.bumptech.glide.load.ImageHeaderParser;
import com.bumptech.glide.load.ImageHeaderParserUtils;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.ArrayPool;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;

/**
 * {@link ByteBuffer} specific implementation of {@link
 * ByteBufferBitmapImageDecoderResourceDecoder}.
 */
@RequiresApi(api = 28)
public final class ByteBufferBitmapImageDecoderResourceDecoder
    implements ResourceDecoder<ByteBuffer, Bitmap> {
  private final List<ImageHeaderParser> parsers;
  @Nullable private final ArrayPool arrayPool;
  private final boolean respectExifOrientation;
  private final BitmapImageDecoderResourceDecoder wrapped;

  public ByteBufferBitmapImageDecoderResourceDecoder() {
    this(
        Collections.<ImageHeaderParser>emptyList(),
        /* arrayPool= */ null,
        /* respectExifOrientation= */ false);
  }

  public ByteBufferBitmapImageDecoderResourceDecoder(@NonNull List<ImageHeaderParser> parsers) {
    this(parsers, /* arrayPool= */ null, /* respectExifOrientation= */ false);
  }

  public ByteBufferBitmapImageDecoderResourceDecoder(
      @NonNull List<ImageHeaderParser> parsers,
      @Nullable ArrayPool arrayPool,
      boolean respectExifOrientation) {
    this.parsers = parsers;
    this.arrayPool = arrayPool;
    this.respectExifOrientation = respectExifOrientation;
    this.wrapped =
        new BitmapImageDecoderResourceDecoder(
            /* clampTargetSizeToOnePixel= */ respectExifOrientation);
  }

  @Override
  public boolean handles(@NonNull ByteBuffer source, @NonNull Options options) throws IOException {
    return true;
  }

  @Override
  public Resource<Bitmap> decode(
      @NonNull ByteBuffer buffer, int width, int height, @NonNull Options options)
      throws IOException {
    if (respectExifOrientation && parsers != null && !parsers.isEmpty() && arrayPool != null) {
      int orientation = ImageHeaderParserUtils.getOrientation(parsers, buffer, arrayPool);
      if (TransformationUtils.isExifOrientationRequired(orientation)) {
        Options optionsWithExif = new Options();
        optionsWithExif.putAll(options);
        optionsWithExif.set(Downsampler.IS_EXIF_ORIENTATION_REQUIRED, true);
        options = optionsWithExif;
      }
    }
    Source source = ImageDecoder.createSource(buffer);
    return wrapped.decode(source, width, height, options);
  }
}
