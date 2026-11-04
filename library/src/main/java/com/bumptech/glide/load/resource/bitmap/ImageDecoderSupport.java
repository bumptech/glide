package com.bumptech.glide.load.resource.bitmap;

import android.os.Build;
import android.os.ParcelFileDescriptor;
import androidx.annotation.NonNull;
import com.bumptech.glide.load.ImageHeaderParser.ImageType;
import com.bumptech.glide.load.Options;

/** Shared helper methods for file-descriptor and {@link android.graphics.ImageDecoder} decoders. */
final class ImageDecoderSupport {

  /**
   * Maximum file size (512MB) that is decoded from a file descriptor on Honor devices.
   *
   * <p>While I don't have data on the number of valid image files > 512mb, I have determined that
   * virtually all crashes related to Honor's DRM checker go away when we don't attempt to decode
   * files larger than this. We could increase this to 1GB safely, but it seems like 512MB might be
   * a little better from a crash reduction perspective. See b/201464175.
   */
  private static final int MAXIMUM_FILE_BYTE_SIZE_FOR_FILE_DESCRIPTOR_DECODER = 512 * 1024 * 1024;

  /**
   * Returns {@code true} if {@link VideoDecoder#TARGET_FRAME} is set to a strictly positive frame
   * timestamp ({@code > 0}).
   */
  static boolean hasExplicitVideoFrame(@NonNull Options options) {
    Long targetFrame = options.get(VideoDecoder.TARGET_FRAME);
    return targetFrame != null && targetFrame > 0;
  }

  /**
   * Returns {@code true} if the given {@link ParcelFileDescriptor} is within the safe file size
   * bound for file-descriptor decoding on Honor devices.
   */
  static boolean isSafeToTryDecoding(@NonNull ParcelFileDescriptor source) {
    if ("HONOR".equalsIgnoreCase(Build.MANUFACTURER)) {
      return source.getStatSize() <= MAXIMUM_FILE_BYTE_SIZE_FOR_FILE_DESCRIPTOR_DECODER;
    }
    return true;
  }

  /**
   * Returns {@code true} if the given {@link ImageType} is a recognized image format supported by
   * {@link android.graphics.ImageDecoder} (rejecting {@link ImageType#UNKNOWN}, such as video
   * containers).
   */
  static boolean isSupportedImageType(@NonNull ImageType type) {
    return type != ImageType.UNKNOWN;
  }

  private ImageDecoderSupport() {}
}
