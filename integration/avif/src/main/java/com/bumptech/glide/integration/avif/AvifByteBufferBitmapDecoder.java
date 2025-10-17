package com.bumptech.glide.integration.avif;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.util.Log;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapResource;
import com.bumptech.glide.load.resource.bitmap.Downsampler;
import com.bumptech.glide.util.Preconditions;
import java.nio.ByteBuffer;
import javax.annotation.Nullable;
import org.aomedia.avif.android.AvifDecoder;
import org.aomedia.avif.android.AvifDecoder.Info;

/** A Glide {@link ResourceDecoder} capable of decoding Avif images. */
public final class AvifByteBufferBitmapDecoder implements ResourceDecoder<ByteBuffer, Bitmap> {
  private static final String TAG = "AvifBitmapDecoder";

  private final BitmapPool bitmapPool;

  public AvifByteBufferBitmapDecoder(BitmapPool bitmapPool) {
    this.bitmapPool = Preconditions.checkNotNull(bitmapPool);
  }

  private ByteBuffer maybeCopyBuffer(ByteBuffer source) {
    // Native calls can only access ByteBuffer if isDirect() is true. Otherwise, we would have to
    // make a copy into a direct ByteBuffer.
    if (source.isDirect()) {
      return source;
    }
    ByteBuffer sourceCopy = ByteBuffer.allocateDirect(source.remaining());
    sourceCopy.put(source);
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
    return AvifDecoder.isAvifImage(maybeCopyBuffer(source));
  }
}
