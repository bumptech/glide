package com.bumptech.glide.load.resource.gif;

import android.util.Log;

import com.bumptech.glide.Logs;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.resource.bitmap.ImageHeaderParser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Map;

/**
 * A relatively inefficient decoder for {@link com.bumptech.glide.load.resource.gif.GifDrawable}
 * that converts {@link java.io.InputStream}s to {@link java.nio.ByteBuffer}s and then passes
 * the buffer to a wrapped decoder.
 */
public class StreamGifDecoder implements ResourceDecoder<InputStream, GifDrawable> {
  /**
   * A Key for an {@link Boolean} option that if set to {@code true}, disables this decoder
   * ({@link #handles(java.io.InputStream, java.util.Map)} will return {@code false}). Defaults to
   * {@code false}.
   */
  public static final String KEY_DISABLE_ANIMATION =
      "com.bumptech.glide.load.resource.gif.ByteBufferGifDecoder.DisableAnimation";

  private final ResourceDecoder<ByteBuffer, GifDrawable> byteBufferDecoder;

  public StreamGifDecoder(ResourceDecoder<ByteBuffer, GifDrawable> byteBufferDecoder) {
    this.byteBufferDecoder = byteBufferDecoder;
  }

  @Override
  public boolean handles(InputStream source, Map<String, Object> options) throws IOException {
    boolean isDisabled = options.containsKey(KEY_DISABLE_ANIMATION)
        && (boolean) options.get(KEY_DISABLE_ANIMATION);
    return !isDisabled
        && new ImageHeaderParser(source).getType() == ImageHeaderParser.ImageType.GIF;
  }

  @Override
  public Resource<GifDrawable> decode(InputStream source, int width, int height,
      Map<String, Object> options) throws IOException {
    byte[] data = inputStreamToBytes(source);
    if (data == null) {
      return null;
    }
    ByteBuffer byteBuffer = ByteBuffer.wrap(data);
    return byteBufferDecoder.decode(byteBuffer, width, height, options);
  }

  private static byte[] inputStreamToBytes(InputStream is) {
    final int bufferSize = 16384;
    ByteArrayOutputStream buffer = new ByteArrayOutputStream(bufferSize);
    try {
      int nRead;
      byte[] data = new byte[bufferSize];
      while ((nRead = is.read(data)) != -1) {
        buffer.write(data, 0, nRead);
      }
      buffer.flush();
    } catch (IOException e) {
      if (Logs.isEnabled(Log.WARN)) {
        Logs.log(Log.WARN, "Error reading data from stream", e);
      }
      return null;
    }
    return buffer.toByteArray();
  }
}
