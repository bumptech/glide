package com.bumptech.glide.load.resource.gif;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Logs;
import com.bumptech.glide.gifdecoder.GifDecoder;
import com.bumptech.glide.gifdecoder.GifHeader;
import com.bumptech.glide.gifdecoder.GifHeaderParser;
import com.bumptech.glide.load.Option;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.engine.bitmap_recycle.ByteArrayPool;
import com.bumptech.glide.load.resource.UnitTransformation;
import com.bumptech.glide.load.resource.bitmap.ImageHeaderParser;
import com.bumptech.glide.load.resource.bitmap.ImageHeaderParser.ImageType;
import com.bumptech.glide.util.LogTime;
import com.bumptech.glide.util.Util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Queue;

/**
 * An {@link com.bumptech.glide.load.ResourceDecoder} that decodes {@link
 * com.bumptech.glide.load.resource.gif.GifDrawable} from {@link java.io.InputStream} data.
 */
public class ByteBufferGifDecoder implements ResourceDecoder<ByteBuffer, GifDrawable> {

  /**
   *  If set to {@code true}, disables this decoder
   *  ({@link #handles(ByteBuffer, Options)} will return {@code false}). Defaults to
   * {@code false}.
   */
  public static final Option<Boolean> DISABLE_ANIMATION = Option.memory(
      "com.bumptech.glide.load.resource.gif.ByteBufferGifDecoder.DisableAnimation", false);

  private static final GifHeaderParserPool PARSER_POOL = new GifHeaderParserPool();
  private static final GifDecoderPool DECODER_POOL = new GifDecoderPool();

  private final Context context;
  private final GifHeaderParserPool parserPool;
  private final BitmapPool bitmapPool;
  private final ByteArrayPool byteArrayPool;
  private final GifDecoderPool decoderPool;
  private final GifBitmapProvider provider;

  public ByteBufferGifDecoder(Context context) {
    this(context, Glide.get(context).getBitmapPool(), Glide.get(context).getByteArrayPool());
  }

  public ByteBufferGifDecoder(Context context, BitmapPool bitmapPool, ByteArrayPool byteArrayPool) {
    this(context, bitmapPool, byteArrayPool, PARSER_POOL, DECODER_POOL);
  }

  // Visible for testing.
  ByteBufferGifDecoder(Context context, BitmapPool bitmapPool, ByteArrayPool byteArrayPool,
      GifHeaderParserPool parserPool, GifDecoderPool decoderPool) {
    this.context = context;
    this.bitmapPool = bitmapPool;
    this.byteArrayPool = byteArrayPool;
    this.decoderPool = decoderPool;
    this.provider = new GifBitmapProvider(bitmapPool);
    this.parserPool = parserPool;
  }

  @Override
  public boolean handles(ByteBuffer source, Options options) throws IOException {
    return !options.get(DISABLE_ANIMATION)
        && new ImageHeaderParser(source, byteArrayPool).getType() == ImageType.GIF;
  }

  @Override
  public GifDrawableResource decode(ByteBuffer source, int width, int height, Options options) {
    final GifHeaderParser parser = parserPool.obtain(source);
    final GifDecoder decoder = decoderPool.obtain(provider);
    try {
      return decode(source, width, height, parser, decoder);
    } finally {
      parserPool.release(parser);
      decoderPool.release(decoder);
    }
  }

  private GifDrawableResource decode(ByteBuffer byteBuffer, int width, int height,
      GifHeaderParser parser, GifDecoder decoder) {
    long startTime = LogTime.getLogTime();
    final GifHeader header = parser.parseHeader();
    if (header.getNumFrames() <= 0 || header.getStatus() != GifDecoder.STATUS_OK) {
      // If we couldn't decode the GIF, we will end up with a frame count of 0.
      return null;
    }

    Bitmap firstFrame = decodeFirstFrame(decoder, header, byteBuffer);
    if (firstFrame == null) {
      return null;
    }

    Transformation<Bitmap> unitTransformation = UnitTransformation.get();

    GifDrawable gifDrawable =
        new GifDrawable(context, provider, bitmapPool, byteBuffer, unitTransformation, width,
            height, header, firstFrame);

    if (Logs.isEnabled(Log.VERBOSE)) {
      Logs.log(Log.VERBOSE, "Decoded gif from stream in " + LogTime.getElapsedMillis(startTime));
    }

    return new GifDrawableResource(gifDrawable);
  }

  private Bitmap decodeFirstFrame(GifDecoder decoder, GifHeader header, ByteBuffer buffer) {
    decoder.setData(header, buffer);
    decoder.advance();
    return decoder.getNextFrame();
  }

  // Visible for testing.
  static class GifDecoderPool {
    private final Queue<GifDecoder> pool = Util.createQueue(0);

    public synchronized GifDecoder obtain(GifDecoder.BitmapProvider bitmapProvider) {
      GifDecoder result = pool.poll();
      if (result == null) {
        result = new GifDecoder(bitmapProvider);
      }
      return result;
    }

    public synchronized void release(GifDecoder decoder) {
      decoder.clear();
      pool.offer(decoder);
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
