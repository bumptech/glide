package com.bumptech.glide.load.resource.bitmap;

import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.graphics.ImageDecoder.Source;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * {@link ByteBuffer} specific implementation of {@link
 * ByteBufferBitmapImageDecoderResourceDecoder}.
 */
@RequiresApi(api = 28)
public final class ByteBufferBitmapImageDecoderResourceDecoder
    implements ResourceDecoder<ByteBuffer, Bitmap> {
  private final BitmapImageDecoderResourceDecoder wrapped = new BitmapImageDecoderResourceDecoder();

  @Override
  public boolean handles(@NonNull ByteBuffer source, @NonNull Options options) throws IOException {
    return true;
  }

  @Override
  public Resource<Bitmap> decode(
      @NonNull ByteBuffer buffer, int width, int height, @NonNull Options options)
      throws IOException {
    Source source = ImageDecoder.createSource(buffer);
    return wrapped.decode(source, width, height, options);
  }
}
