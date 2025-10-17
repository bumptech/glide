package com.bumptech.glide.integration.avif;

import android.graphics.Bitmap;
import com.bumptech.glide.load.ImageHeaderParser;
import com.bumptech.glide.load.ImageHeaderParser.ImageType;
import com.bumptech.glide.load.ImageHeaderParserUtils;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.ArrayPool;
import com.bumptech.glide.util.ByteBufferUtil;
import com.bumptech.glide.util.Preconditions;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import javax.annotation.Nullable;

/** A Glide {@link ResourceDecoder} capable of decoding Avif Images. */
public final class AvifStreamBitmapDecoder implements ResourceDecoder<InputStream, Bitmap> {
  private static final String TAG = "AvifStreamBitmapDecoder";

  private final List<ImageHeaderParser> parsers;
  private final AvifByteBufferBitmapDecoder avifByteBufferDecoder;
  private final ArrayPool arrayPool;

  public AvifStreamBitmapDecoder(
      List<ImageHeaderParser> parsers,
      AvifByteBufferBitmapDecoder avifByteBufferDecoder,
      ArrayPool arrayPool) {
    this.parsers = parsers;
    this.avifByteBufferDecoder = Preconditions.checkNotNull(avifByteBufferDecoder);
    this.arrayPool = Preconditions.checkNotNull(arrayPool);
  }

  @Override
  @Nullable
  public Resource<Bitmap> decode(InputStream source, int width, int height, Options options)
      throws IOException {
    return avifByteBufferDecoder.decode(ByteBufferUtil.fromStream(source), width, height, options);
  }

  @Override
  public boolean handles(InputStream source, Options options) throws IOException {
    ImageType type = ImageHeaderParserUtils.getType(parsers, source, arrayPool);
    return type.equals(ImageType.AVIF) || type.equals(ImageType.ANIMATED_AVIF);
  }
}
