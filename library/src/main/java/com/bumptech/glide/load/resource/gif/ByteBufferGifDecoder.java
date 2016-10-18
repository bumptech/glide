package com.bumptech.glide.load.resource.gif;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import com.bumptech.glide.Glide;
import com.bumptech.glide.gifdecoder.GifDecoder;
import com.bumptech.glide.gifdecoder.GifHeader;
import com.bumptech.glide.gifdecoder.GifHeaderParser;
import com.bumptech.glide.gifdecoder.StandardGifDecoder;
import com.bumptech.glide.load.ImageHeaderParser;
import com.bumptech.glide.load.ImageHeaderParser.ImageType;
import com.bumptech.glide.load.ImageHeaderParserUtils;
import com.bumptech.glide.load.Option;
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

  /**
   *  If set to {@code true}, disables this decoder
   *  ({@link #handles(ByteBuffer, Options)} will return {@code false}). Defaults to
   * {@code false}.
   */
  public static final Option<Boolean> DISABLE_ANIMATION = Option.memory(
      "com.bumptech.glide.load.resource.gif.ByteBufferGifDecoder.DisableAnimation", false);

  private static final GifHeaderParserPool PARSER_POOL = new GifHeaderParserPool();

  private final Context context;
  private final List<ImageHeaderParser> parsers;
  private final GifHeaderParserPool parserPool;
  private final BitmapPool bitmapPool;
  private final GifDecoderFactory gifDecoderFactory;
  private final GifBitmapProvider provider;

  public ByteBufferGifDecoder(Context context) {
    this(context, Glide.get(context).getRegistry().getImageHeaderParsers(),
        Glide.get(context).getBitmapPool(), Glide.get(context).getArrayPool());
  }

  public ByteBufferGifDecoder(
      Context context, List<ImageHeaderParser> parsers, BitmapPool bitmapPool,
      ArrayPool arrayPool) {
    this(context, parsers, bitmapPool, arrayPool, PARSER_POOL, GIF_DECODER_FACTORY);
  }

  // Visible for testing.
  ByteBufferGifDecoder(
      Context context,
      List<ImageHeaderParser> parsers,
      BitmapPool bitmapPool,
      ArrayPool arrayPool,
      GifHeaderParserPool parserPool,
      GifDecoderFactory gifDecoderFactory) {
    this.context = context.getApplicationContext();
    this.parsers = parsers;
    this.bitmapPool = bitmapPool;
    this.gifDecoderFactory = gifDecoderFactory;
    this.provider = new GifBitmapProvider(bitmapPool, arrayPool);
    this.parserPool = parserPool;
  }

  @Override
  public boolean handles(ByteBuffer source, Options options) throws IOException {
    return !options.get(DISABLE_ANIMATION)
        && ImageHeaderParserUtils.getType(parsers, source) == ImageType.GIF;
  }

  @Override
  public GifDrawableResource decode(ByteBuffer source, int width, int height, Options options) {
    final GifHeaderParser parser = parserPool.obtain(source);
    try {
      return decode(source, width, height, parser);
    } finally {
      parserPool.release(parser);
    }
  }

  private GifDrawableResource decode(ByteBuffer byteBuffer, int width, int height,
      GifHeaderParser parser) {
    long startTime = LogTime.getLogTime();
    final GifHeader header = parser.parseHeader();
    if (header.getNumFrames() <= 0 || header.getStatus() != GifDecoder.STATUS_OK) {
      // If we couldn't decode the GIF, we will end up with a frame count of 0.
      return null;
    }


    int sampleSize = getSampleSize(header, width, height);
    GifDecoder gifDecoder = gifDecoderFactory.build(provider, header, byteBuffer, sampleSize);
    gifDecoder.advance();
    Bitmap firstFrame = gifDecoder.getNextFrame();
    if (firstFrame == null) {
      return null;
    }

    Transformation<Bitmap> unitTransformation = UnitTransformation.get();

    GifDrawable gifDrawable =
        new GifDrawable(context, gifDecoder, bitmapPool, unitTransformation, width, height,
            firstFrame);

    if (Log.isLoggable(TAG, Log.VERBOSE)) {
      Log.v(TAG, "Decoded GIF from stream in " + LogTime.getElapsedMillis(startTime));
    }

    return new GifDrawableResource(gifDrawable);
  }

  private static int getSampleSize(GifHeader gifHeader, int targetWidth, int targetHeight) {
    int exactSampleSize = Math.min(gifHeader.getHeight() / targetHeight,
        gifHeader.getWidth() / targetWidth);
    int powerOfTwoSampleSize = exactSampleSize == 0 ? 0 : Integer.highestOneBit(exactSampleSize);
    // Although functionally equivalent to 0 for BitmapFactory, 1 is a safer default for our code
    // than 0.
    int sampleSize = Math.max(1, powerOfTwoSampleSize);
    if (Log.isLoggable(TAG, Log.VERBOSE)) {
      Log.v(TAG, "Downsampling GIF"
          + ", sampleSize: " + sampleSize
          + ", target dimens: [" + targetWidth + "x" + targetHeight + "]"
          + ", actual dimens: [" + gifHeader.getWidth() + "x" + gifHeader.getHeight() + "]");
    }
    return sampleSize;
  }

  // Visible for testing.
  static class GifDecoderFactory {
    public GifDecoder build(GifDecoder.BitmapProvider provider, GifHeader header,
        ByteBuffer data, int sampleSize) {
      return new StandardGifDecoder(provider, header, data, sampleSize);
    }
  }

  // Visible for testing.
  static class GifHeaderParserPool {
    private final Queue<GifHeaderParser> pool = Util.createQueue(0);

    public synchronized GifHeaderParser obtain(ByteBuffer buffer) {
      GifHeaderParser result = pool.poll();
      if (result == null) {
        result = new GifHeaderParser();
      }
      return result.setData(buffer);
    }

    public synchronized void release(GifHeaderParser parser) {
      parser.clear();
      pool.offer(parser);
    }
  }
}
