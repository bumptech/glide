package com.bumptech.glide.integration.avif;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.util.Log;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.ImageHeaderParser;
import com.bumptech.glide.load.ImageHeaderParser.ImageType;
import com.bumptech.glide.load.ImageHeaderParserUtils;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapResource;
import com.bumptech.glide.load.resource.bitmap.DefaultImageHeaderParser;
import com.bumptech.glide.load.resource.bitmap.Downsampler;
import com.bumptech.glide.util.Preconditions;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import org.aomedia.avif.android.AvifDecoder;
import org.aomedia.avif.android.AvifDecoder.Info;

/** A Glide {@link ResourceDecoder} capable of decoding Avif images. */
public final class AvifByteBufferBitmapDecoder implements ResourceDecoder<ByteBuffer, Bitmap> {
  private static final String TAG = "AvifBitmapDecoder";

  private final List<ImageHeaderParser> parsers;
  private final BitmapPool bitmapPool;

  public AvifByteBufferBitmapDecoder(BitmapPool bitmapPool) {
    this(Collections.singletonList(new DefaultImageHeaderParser()), bitmapPool);
  }

  public AvifByteBufferBitmapDecoder(List<ImageHeaderParser> parsers, BitmapPool bitmapPool) {
    this.parsers = Preconditions.checkNotNull(parsers);
    this.bitmapPool = Preconditions.checkNotNull(bitmapPool);
  }

  private ByteBuffer maybeCopyBuffer(ByteBuffer source) {
    // Native calls can only access ByteBuffer if isDirect() is true and read from base address 0.
    // Slice direct buffers with non-zero position so JNI receives the offset base address, and copy
    // remaining bytes for non-direct buffers.
    if (source.isDirect()) {
      return source.position() == 0 ? source : source.slice();
    }
    ByteBuffer sourceCopy = ByteBuffer.allocateDirect(source.remaining());
    sourceCopy.put(source.slice());
    sourceCopy.flip();
    return sourceCopy;
  }

  @Override
  @Nullable
  public Resource<Bitmap> decode(ByteBuffer source, int width, int height, Options options) {
    ByteBuffer sourceCopy = maybeCopyBuffer(source);
    Info info = new Info();
    if (!AvifDecoder.getInfo(sourceCopy, sourceCopy.remaining(), info)) {
      if (Log.isLoggable(TAG, Log.ERROR)) {
        Log.e(TAG, "Requested to decode byte buffer which cannot be handled by AvifDecoder");
      }
      return null;
    }
    Bitmap.Config config;
    if (options.get(Downsampler.DECODE_FORMAT) == DecodeFormat.PREFER_RGB_565) {
      config = Config.RGB_565;
    } else {
      config = (info.depth == 8) ? Config.ARGB_8888 : Config.RGBA_F16;
    }
    Bitmap bitmap = bitmapPool.get(info.width, info.height, config);
    if (!AvifDecoder.decode(sourceCopy, sourceCopy.remaining(), bitmap)) {
      if (Log.isLoggable(TAG, Log.ERROR)) {
        Log.e(TAG, "Failed to decode ByteBuffer as Avif.");
      }
      bitmapPool.put(bitmap);
      return null;
    }
    return BitmapResource.obtain(bitmap, bitmapPool);
  }

  @Override
  public boolean handles(ByteBuffer source, Options options) {
    ImageType type;
    try {
      type = ImageHeaderParserUtils.getType(parsers, source.slice());
    } catch (IOException e) {
      return false;
    }
    return type.equals(ImageType.AVIF) || type.equals(ImageType.ANIMATED_AVIF);
  }
}
