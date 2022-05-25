package com.bumptech.glide.load.resource.bitmap;

import android.graphics.Bitmap;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;
import java.io.IOException;

/** Decodes {@link Bitmap}s from {@link ParcelFileDescriptor}s. */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public final class ParcelFileDescriptorBitmapDecoder
    implements ResourceDecoder<ParcelFileDescriptor, Bitmap> {

  // 512MB. While I don't have data on the number of valid image files > 512mb, I have determined
  // that virtually all crashes related to Huawei/Honor's DRM checker go away when we don't attempt
  // to decode files larger than this. We could increase this to 1GB safely, but it seems like 512MB
  // might be a little better from a crash reduction perspective. See b/201464175.
  private static final int MAXIMUM_FILE_BYTE_SIZE_FOR_FILE_DESCRIPTOR_DECODER = 512 * 1024 * 1024;

  private final Downsampler downsampler;

  public ParcelFileDescriptorBitmapDecoder(Downsampler downsampler) {
    this.downsampler = downsampler;
  }

  @Override
  public boolean handles(@NonNull ParcelFileDescriptor source, @NonNull Options options) {
    return isSafeToTryDecoding(source) && downsampler.handles(source);
  }

  private boolean isSafeToTryDecoding(@NonNull ParcelFileDescriptor source) {
    if ("HUAWEI".equalsIgnoreCase(Build.MANUFACTURER)
        || "HONOR".equalsIgnoreCase(Build.MANUFACTURER)) {
      return source.getStatSize() <= MAXIMUM_FILE_BYTE_SIZE_FOR_FILE_DESCRIPTOR_DECODER;
    }
    return true;
  }

  @Nullable
  @Override
  public Resource<Bitmap> decode(
      @NonNull ParcelFileDescriptor source, int width, int height, @NonNull Options options)
      throws IOException {
    return downsampler.decode(source, width, height, options);
  }
}
