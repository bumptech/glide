package com.bumptech.glide.load.resource.bitmap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.load.ImageHeaderParser;
import com.bumptech.glide.load.ImageHeaderParserUtils;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.engine.bitmap_recycle.ArrayPool;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

/**
 * Inspects the EXIF orientation of encoded image data and, when rotation is required, returns a
 * copy of the request {@link Options} with {@link Downsampler#IS_EXIF_ORIENTATION_REQUIRED} set.
 *
 * <p>Shared by the {@link android.graphics.ImageDecoder}-backed bitmap decoders so that {@link
 * com.bumptech.glide.load.resource.DefaultOnHeaderDecodedListener} can avoid hardware allocation
 * for images that will subsequently be rotated.
 */
final class ExifOrientationOptionsApplier {
  @Nullable private final List<ImageHeaderParser> parsers;
  @Nullable private final ArrayPool arrayPool;

  /**
   * @param parsers Parsers used to read the EXIF orientation. If {@code null} or empty, {@link
   *     #apply} is a no-op.
   * @param arrayPool Pool used by the parsers. If {@code null}, {@link #apply} is a no-op.
   */
  ExifOrientationOptionsApplier(
      @Nullable List<ImageHeaderParser> parsers, @Nullable ArrayPool arrayPool) {
    this.parsers = parsers;
    this.arrayPool = arrayPool;
  }

  /**
   * Returns {@code options} unchanged if EXIF inspection is not possible or the image does not
   * require rotation; otherwise returns a copy of {@code options} with {@link
   * Downsampler#IS_EXIF_ORIENTATION_REQUIRED} set to {@code true}.
   *
   * <p>The position of {@code buffer} is not modified.
   */
  @NonNull
  Options apply(@NonNull ByteBuffer buffer, @NonNull Options options) throws IOException {
    if (parsers == null || parsers.isEmpty() || arrayPool == null) {
      return options;
    }
    int orientation = ImageHeaderParserUtils.getOrientation(parsers, buffer, arrayPool);
    if (!TransformationUtils.isExifOrientationRequired(orientation)) {
      return options;
    }
    Options optionsWithExif = new Options();
    optionsWithExif.putAll(options);
    optionsWithExif.set(Downsampler.IS_EXIF_ORIENTATION_REQUIRED, true);
    return optionsWithExif;
  }
}
