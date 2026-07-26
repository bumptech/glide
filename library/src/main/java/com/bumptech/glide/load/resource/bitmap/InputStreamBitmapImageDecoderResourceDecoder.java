package com.bumptech.glide.load.resource.bitmap;

import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.graphics.ImageDecoder.Source;
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
import com.bumptech.glide.util.ByteBufferUtil;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;

/** {@link InputStream} specific implementation of {@link BitmapImageDecoderResourceDecoder}. */
@RequiresApi(api = 28)
public final class InputStreamBitmapImageDecoderResourceDecoder
    implements ResourceDecoder<InputStream, Bitmap> {
  private final BitmapImageDecoderResourceDecoder wrapped = new BitmapImageDecoderResourceDecoder();
  private final List<ImageHeaderParser> parsers;
  private final boolean useHeapBuffer;
  @Nullable private final ArrayPool arrayPool;
  private final boolean useArrayPool;

  public InputStreamBitmapImageDecoderResourceDecoder(
      List<ImageHeaderParser> parsers,
      boolean useHeapBuffer,
      @Nullable ArrayPool arrayPool,
      boolean useArrayPool) {
    this.parsers = parsers;
    this.useHeapBuffer = useHeapBuffer;
    this.arrayPool = arrayPool;
    this.useArrayPool = useArrayPool;
  }

  @Override
  public boolean handles(@NonNull InputStream source, @NonNull Options options) throws IOException {
    if (!useArrayPool) {
      return true;
    }
    if (arrayPool == null) {
      return false;
    }
    ImageType type = ImageHeaderParserUtils.getType(parsers, source, arrayPool);
    return type != ImageType.UNKNOWN;
  }

  @Override
  public Resource<Bitmap> decode(
      @NonNull InputStream stream, int width, int height, @NonNull Options options)
      throws IOException {
    ByteBuffer buffer =
        useArrayPool && arrayPool != null
            ? ByteBufferUtil.fromStream(stream, useHeapBuffer, arrayPool)
            : ByteBufferUtil.fromStream(stream, useHeapBuffer);
    Source source = ImageDecoder.createSource(buffer);
    return wrapped.decode(source, width, height, options);
  }
}
