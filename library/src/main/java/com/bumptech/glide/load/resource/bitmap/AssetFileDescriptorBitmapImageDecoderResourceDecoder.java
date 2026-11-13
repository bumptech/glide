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
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.ArrayPool;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;

/**
 * Decodes {@link Bitmap}s from {@link AssetFileDescriptor}s using {@link ImageDecoder} on Android
 * Q+ (API 29+).
 */
@RequiresApi(Build.VERSION_CODES.Q)
public final class AssetFileDescriptorBitmapImageDecoderResourceDecoder
    implements ResourceDecoder<AssetFileDescriptor, Bitmap> {

  private final BitmapImageDecoderResourceDecoder wrapped =
      new BitmapImageDecoderResourceDecoder(/* floorTargetSizeToOnePixel= */ true);
  private final List<ImageHeaderParser> parsers;
  private final ExifOrientationOptionsApplier exifOrientationOptionsApplier;

  public AssetFileDescriptorBitmapImageDecoderResourceDecoder(
      @NonNull List<ImageHeaderParser> parsers, @NonNull ArrayPool arrayPool) {
    this.parsers = parsers;
    this.exifOrientationOptionsApplier = new ExifOrientationOptionsApplier(parsers, arrayPool);
  }

  /**
   * Returns {@code true} if {@code source} contains a still image that {@link ImageDecoder} can
   * decode.
   *
   * @throws IOException if the image header cannot be read from {@code source}.
   */
  @Override
  public boolean handles(@NonNull AssetFileDescriptor source, @NonNull Options options)
      throws IOException {
    ParcelFileDescriptor pfd = source.getParcelFileDescriptor();
    if (pfd == null
        || ImageDecoderSupport.hasExplicitVideoFrame(options)
        || !ImageDecoderSupport.isSafeToTryDecoding(pfd)) {
      return false;
    }
    ImageType type = ImageHeaderParserUtils.getType(parsers, mapHeader(source));
    return ImageDecoderSupport.isSupportedImageType(type);
  }

  /**
   * Returns a read-only {@link ByteBuffer} memory-mapped over the first {@link
   * ImageHeaderParserUtils#MARK_READ_LIMIT} bytes of the region described by {@code
   * assetFileDescriptor}.
   *
   * <p>The mapping reads the file in place, so the header is parsed without copying it to the Java
   * heap and without moving the file offset of the shared file descriptor. The {@link
   * FileInputStream} is only used to obtain a {@link FileChannel} and is deliberately not closed,
   * because closing it would close the file descriptor owned by {@code assetFileDescriptor}.
   *
   * @throws IOException if the file descriptor cannot be memory-mapped, for example if it refers to
   *     a pipe or socket rather than a regular file.
   */
  @NonNull
  private static ByteBuffer mapHeader(@NonNull AssetFileDescriptor assetFileDescriptor)
      throws IOException {
    FileChannel channel = new FileInputStream(assetFileDescriptor.getFileDescriptor()).getChannel();
    long startOffset = assetFileDescriptor.getStartOffset();
    long declaredLength = assetFileDescriptor.getDeclaredLength();
    long length =
        declaredLength == AssetFileDescriptor.UNKNOWN_LENGTH
            ? channel.size() - startOffset
            : declaredLength;
    return channel.map(
        FileChannel.MapMode.READ_ONLY,
        startOffset,
        Math.max(0, Math.min(length, ImageHeaderParserUtils.MARK_READ_LIMIT)));
  }

  /**
   * Decodes {@code assetFileDescriptor} with {@link ImageDecoder}, applying EXIF orientation from
   * the image header.
   *
   * @return the decoded {@link Bitmap}, or {@code null} if {@link ImageDecoder} fails to decode it.
   * @throws IOException if the image header cannot be read from {@code assetFileDescriptor}.
   */
  @Nullable
  @Override
  public Resource<Bitmap> decode(
      @NonNull AssetFileDescriptor assetFileDescriptor,
      int width,
      int height,
      @NonNull Options options)
      throws IOException {
    options = exifOrientationOptionsApplier.apply(mapHeader(assetFileDescriptor), options);
    Source source =
        ImageDecoder.createSource(() ->
            new AssetFileDescriptor(
                assetFileDescriptor.getParcelFileDescriptor().dup(),
                assetFileDescriptor.getStartOffset(),
                assetFileDescriptor.getDeclaredLength()));
    return wrapped.decode(source, width, height, options);
  }
}
