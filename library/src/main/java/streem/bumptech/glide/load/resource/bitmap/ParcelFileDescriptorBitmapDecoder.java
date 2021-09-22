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

  private final Downsampler downsampler;

  public ParcelFileDescriptorBitmapDecoder(Downsampler downsampler) {
    this.downsampler = downsampler;
  }

  @Override
  public boolean handles(@NonNull ParcelFileDescriptor source, @NonNull Options options) {
    return downsampler.handles(source);
  }

  @Nullable
  @Override
  public Resource<Bitmap> decode(
      @NonNull ParcelFileDescriptor source, int width, int height, @NonNull Options options)
      throws IOException {
    return downsampler.decode(source, width, height, options);
  }
}
