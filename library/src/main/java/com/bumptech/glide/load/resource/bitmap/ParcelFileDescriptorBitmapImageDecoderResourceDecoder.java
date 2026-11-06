package com.bumptech.glide.load.resource.bitmap;

import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.graphics.ImageDecoder.Source;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import com.bumptech.glide.load.ImageHeaderParser;
import com.bumptech.glide.load.ImageHeaderParser.ImageType;
import com.bumptech.glide.load.ImageHeaderParserUtils;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.data.ParcelFileDescriptorRewinder;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.ArrayPool;
import java.io.IOException;
import java.util.List;

/**
 * Decodes {@link Bitmap}s from {@link ParcelFileDescriptor}s using {@link ImageDecoder} on Android
 * Q+ (API 29+).
 */
@RequiresApi(Build.VERSION_CODES.Q)
public final class ParcelFileDescriptorBitmapImageDecoderResourceDecoder
    implements ResourceDecoder<ParcelFileDescriptor, Bitmap> {

  private final BitmapImageDecoderResourceDecoder wrapped =
      new BitmapImageDecoderResourceDecoder(/* floorTargetSizeToOnePixel= */ true);
  private final List<ImageHeaderParser> parsers;
  private final ArrayPool arrayPool;
  private final ExifOrientationOptionsApplier exifOrientationOptionsApplier;

  public ParcelFileDescriptorBitmapImageDecoderResourceDecoder(
      @NonNull List<ImageHeaderParser> parsers, @NonNull ArrayPool arrayPool) {
    this.parsers = parsers;
    this.arrayPool = arrayPool;
    this.exifOrientationOptionsApplier = new ExifOrientationOptionsApplier(parsers, arrayPool);
  }

  @Override
  public boolean handles(@NonNull ParcelFileDescriptor source, @NonNull Options options)
      throws IOException {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
        || ImageDecoderSupport.hasExplicitVideoFrame(options)
        || !ImageDecoderSupport.isSafeToTryDecoding(source)) {
      return false;
    }
    ImageType type =
        ImageHeaderParserUtils.getType(
            parsers, new ParcelFileDescriptorRewinder(source), arrayPool);
    return ImageDecoderSupport.isSupportedImageType(type);
  }

  @Nullable
  @Override
  public Resource<Bitmap> decode(
      @NonNull ParcelFileDescriptor parcelFileDescriptor,
      int width,
      int height,
      @NonNull Options options)
      throws IOException {
    options = exifOrientationOptionsApplier.apply(parcelFileDescriptor, options);
    Source source =
        ImageDecoder.createSource(
            () ->
                new AssetFileDescriptor(
                    parcelFileDescriptor.dup(), 0, AssetFileDescriptor.UNKNOWN_LENGTH));
    return wrapped.decode(source, width, height, options);
  }
}
