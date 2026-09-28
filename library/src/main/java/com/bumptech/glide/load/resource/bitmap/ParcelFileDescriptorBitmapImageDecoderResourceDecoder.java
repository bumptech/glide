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
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.data.ParcelFileDescriptorRewinder;
import com.bumptech.glide.load.engine.Resource;
import java.io.IOException;

/** Decodes {@link Bitmap}s from {@link ParcelFileDescriptor}s using {@link ImageDecoder}. */
@RequiresApi(Build.VERSION_CODES.Q)
public final class ParcelFileDescriptorBitmapImageDecoderResourceDecoder
    implements ResourceDecoder<ParcelFileDescriptor, Bitmap> {

  private static final int MAXIMUM_FILE_BYTE_SIZE_FOR_FILE_DESCRIPTOR_DECODER = 512 * 1024 * 1024;
  private final BitmapImageDecoderResourceDecoder wrapped = new BitmapImageDecoderResourceDecoder();

  @Override
  public boolean handles(@NonNull ParcelFileDescriptor source, @NonNull Options options) {
    if ("HUAWEI".equalsIgnoreCase(Build.MANUFACTURER)
        || "HONOR".equalsIgnoreCase(Build.MANUFACTURER)) {
      return source.getStatSize() <= MAXIMUM_FILE_BYTE_SIZE_FOR_FILE_DESCRIPTOR_DECODER;
    }
    return ParcelFileDescriptorRewinder.isSupported();
  }

  @Nullable
  @Override
  public Resource<Bitmap> decode(
      @NonNull ParcelFileDescriptor source, int width, int height, @NonNull Options options)
      throws IOException {
    Source imageDecoderSource =
        ImageDecoder.createSource(
            () -> new AssetFileDescriptor(source.dup(), 0, AssetFileDescriptor.UNKNOWN_LENGTH));
    return wrapped.decode(imageDecoderSource, width, height, options);
  }
}
