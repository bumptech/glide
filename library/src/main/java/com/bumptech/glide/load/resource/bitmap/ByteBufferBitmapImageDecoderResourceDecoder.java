package com.bumptech.glide.load.resource.bitmap;

import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.graphics.ImageDecoder.Source;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import com.bumptech.glide.load.ImageHeaderParser;
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
  @Nullable private final ExifOrientationOptionsApplier exifOrientationOptionsApplier;
  private final BitmapImageDecoderResourceDecoder wrapped;
  private final boolean rejectExplicitVideoFrames;

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
    this(parsers, arrayPool, respectExifOrientation, /* rejectExplicitVideoFrames= */ false);
  }

  /**
   * @param rejectExplicitVideoFrames {@code true} to return {@code false} from {@link #handles} for
   *     requests with a positive {@link VideoDecoder#TARGET_FRAME}. See {@link
   *     com.bumptech.glide.GlideBuilder#setFileDescriptorImageDecoderEnabled(boolean)}.
   */
  public ByteBufferBitmapImageDecoderResourceDecoder(
      @NonNull List<ImageHeaderParser> parsers,
      @Nullable ArrayPool arrayPool,
      boolean respectExifOrientation,
      boolean rejectExplicitVideoFrames) {
    this.rejectExplicitVideoFrames = rejectExplicitVideoFrames;
    this.exifOrientationOptionsApplier =
        respectExifOrientation ? new ExifOrientationOptionsApplier(parsers, arrayPool) : null;
    this.wrapped =
        new BitmapImageDecoderResourceDecoder(
            /* floorTargetSizeToOnePixel= */ respectExifOrientation);
  }

  @Override
  public boolean handles(@NonNull ByteBuffer source, @NonNull Options options) throws IOException {
    return !(rejectExplicitVideoFrames && ImageDecoderSupport.hasExplicitVideoFrame(options));
  }

  @Override
  public Resource<Bitmap> decode(
      @NonNull ByteBuffer buffer, int width, int height, @NonNull Options options)
      throws IOException {
    if (exifOrientationOptionsApplier != null) {
      options = exifOrientationOptionsApplier.apply(buffer, options);
    }
    Source source = ImageDecoder.createSource(buffer);
    return wrapped.decode(source, width, height, options);
  }
}
