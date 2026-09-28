package com.bumptech.glide.load.resource.bitmap;

import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.graphics.ImageDecoder.Source;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;
import java.io.File;
import java.io.IOException;

/** Decodes {@link Bitmap}s directly from {@link File}s using {@link ImageDecoder}. */
@RequiresApi(api = 28)
public final class FileBitmapImageDecoderResourceDecoder implements ResourceDecoder<File, Bitmap> {
  private final BitmapImageDecoderResourceDecoder wrapped = new BitmapImageDecoderResourceDecoder();

  @Override
  public boolean handles(@NonNull File file, @NonNull Options options) {
    return true;
  }

  @Nullable
  @Override
  public Resource<Bitmap> decode(
      @NonNull File file, int width, int height, @NonNull Options options) throws IOException {
    Source source = ImageDecoder.createSource(file);
    return wrapped.decode(source, width, height, options);
  }
}
