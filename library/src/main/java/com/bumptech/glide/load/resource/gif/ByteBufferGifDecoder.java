package com.bumptech.glide.load.resource.gif;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.bumptech.glide.Glide;
import com.bumptech.glide.gifdecoder.GifDecoder;
import com.bumptech.glide.gifdecoder.GifHeader;
import com.bumptech.glide.gifdecoder.GifHeaderParser;
import com.bumptech.glide.gifdecoder.StandardGifDecoder;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.ImageHeaderParser;
import com.bumptech.glide.load.ImageHeaderParser.ImageType;
import com.bumptech.glide.load.ImageHeaderParserUtils;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.bitmap_recycle.ArrayPool;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.UnitTransformation;
import com.bumptech.glide.util.LogTime;
import com.bumptech.glide.util.Util;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Queue;

/**
 * An {@link com.bumptech.glide.load.ResourceDecoder} that decodes {@link
 * com.bumptech.glide.load.resource.gif.GifDrawable} from {@link java.io.InputStream} data.
 */
public class ByteBufferGifDecoder implements ResourceDecoder<ByteBuffer, GifDrawable> {
  private static final String TAG = "BufferGifDecoder";
  private static final GifDecoderFactory GIF_DECODER_FACTORY = new GifDecoderFactory();
  private static final GifHeaderParserPool PARSER_POOL = new GifHeaderParserPool();

  private final Context context;
  private final List<ImageHeaderParser> parsers;
  private final GifHeaderParserPool parserPool;
  private final GifDecoderFactory gifDecoderFactory;
  private final GifBitmapProvider provider;

  // Public API.
  @SuppressWarnings("unused")
  public ByteBufferGifDecoder(Context context) {
    this(
        context,
        Glide.get(context).getRegistry().getImageHeaderParsers(),
        Glide.get(context).getBitmapPool(),
        Glide.get(context).getArrayPool());
  }

  public ByteBufferGifDecoder(
      Context context,
      List<ImageHeaderParser> parsers,
      BitmapPool bitmapPool,
      ArrayPool arrayPool) {
    this(context, parsers, bitmapPool, arrayPool, PARSER_POOL, GIF_DECODER_FACTORY);
  }

  @VisibleForTesting
  ByteBufferGifDecoder(
      Context context,
      List<ImageHeaderParser> parsers,
      BitmapPool bitmapPool,
      ArrayPool arrayPool,
      GifHeaderParserPool parserPool,
      GifDecoderFactory gifDecoderFactory) {
    this.context = context.getApplicationContext();
    this.parsers = parsers;
    this.gifDecoderFactory = gifDecoderFactory;
    this.provider = new GifBitmapProvider(bitmapPool, arrayPool);
    this.parserPool = parserPool;
  }

  @Override
  public boolean handles(@NonNull ByteBuffer source, @NonNull Options options) throws IOException {
    return !options.get(GifOptions.DISABLE_ANIMATION)
        && ImageHeaderParserUtils.getType(parsers, source) == ImageType.GIF;
  }

  @Override
  public GifDrawableResource decode(
      @NonNull ByteBuffer source, int width, int height, @NonNull Options options) {
    final GifHeaderParser parser = parserPool.obtain(source);
    try {
      return decode(source, width, height, parser, options);
    } finally {
      parserPool.release(parser);
    }
  }

  @Nullable
  private GifDrawableResource decode(
      ByteBuffer byteBuffer, int width, int height, GifHeaderParser parser, Options options) {
    long startTime = LogTime.getLogTime();
    try {
      final GifHeader header = parser.parseHeader();
      if (header.getNumFrames() <= 0 || header.getStatus() != GifDecoder.STATUS_OK) {
        // If we couldn't decode the GIF, we will end up with a frame count of 0.
        return null;
      }

      Bitmap.Config config =
          options.get(GifOptions.DECODE_FORMAT) == DecodeFormat.PREFER_RGB_565
              ? Bitmap.Config.RGB_565
              : Bitmap.Config.ARGB_8888;

      int sampleSize = getSampleSize(header, width, height);
      GifDecoder gifDecoder = gifDecoderFactory.build(provider, header, byteBuffer, sampleSize);
      gifDecoder.setDefaultBitmapConfig(config);
      gifDecoder.advance();
      Bitmap firstFrame = gifDecoder.getNextFrame();
      if (firstFrame == null) {
        return null;
      }

      Transformation<Bitmap> unitTransformation = UnitTransformation.get();

      GifDrawable gifDrawable =
          new GifDrawable(context, gifDecoder, unitTransformation, width, height, firstFrame);

      return new GifDrawableResource(gifDrawable);
    } finally {
      if (Log.isLoggable(TAG, Log.VERBOSE)) {
        Log.v(TAG, "Decoded GIF from stream in " + LogTime.getElapsedMillis(startTime));
      }
    }
  }

  private static int getSampleSize(GifHeader gifHeader, int targetWidth, int targetHeight) {
    int exactSampleSize =
        Math.min(gifHeader.getHeight() / targetHeight, gifHeader.getWidth() / targetWidth);
    int powerOfTwoSampleSize = exactSampleSize == 0 ? 0 : Integer.highestOneBit(exactSampleSize);
    // Although functionally equivalent to 0 for BitmapFactory, 1 is a safer default for our code
    // than 0.
    int sampleSize = Math.max(1, powerOfTwoSampleSize);
    if (Log.isLoggable(TAG, Log.VERBOSE) && sampleSize > 1) {
      Log.v(
          TAG,
          "Downsampling GIF"
              + ", sampleSize: "
              + sampleSize
              + ", target dimens: ["
              + targetWidth
              + "x"
              + targetHeight
              + "]"
              + ", actual dimens: ["
              + gifHeader.getWidth()
              + "x"
              + gifHeader.getHeight()
              + "]");
    }
    return sampleSize;
  }

  @VisibleForTesting
  static class GifDecoderFactory {
    GifDecoder build(
        GifDecoder.BitmapProvider provider, GifHeader header, ByteBuffer data, int sampleSize) {
      return new StandardGifDecoder(provider, header, data, sampleSize);
    }
  }

  @VisibleForTesting
  static class GifHeaderParserPool {
    private final Queue<GifHeaderParser> pool = Util.createQueue(0);

    synchronized GifHeaderParser obtain(ByteBuffer buffer) {
      GifHeaderParser result = pool.poll();
      if (result == null) {
        result = new GifHeaderParser();
      }
      return result.setData(buffer);
    }

    synchronized void release(GifHeaderParser parser) {
      parser.clear();
      pool.offer(parser);
    }
  }
}
