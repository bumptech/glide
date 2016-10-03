package com.bumptech.glide.integration.gifencoder;

import android.graphics.Bitmap;
import android.util.Log;
import com.bumptech.glide.gifdecoder.GifDecoder;
import com.bumptech.glide.gifdecoder.GifHeader;
import com.bumptech.glide.gifdecoder.GifHeaderParser;
import com.bumptech.glide.gifdecoder.StandardGifDecoder;
import com.bumptech.glide.gifencoder.AnimatedGifEncoder;
import com.bumptech.glide.load.EncodeStrategy;
import com.bumptech.glide.load.Option;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.UnitTransformation;
import com.bumptech.glide.load.resource.bitmap.BitmapResource;
import com.bumptech.glide.load.resource.gif.GifBitmapProvider;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.util.ByteBufferUtil;
import com.bumptech.glide.util.LogTime;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.security.MessageDigest;

/**
 * An {@link com.bumptech.glide.load.ResourceEncoder} that can write
 * {@link com.bumptech.glide.load.resource.gif.GifDrawable} to cache.
 */
public class ReEncodingGifResourceEncoder implements ResourceEncoder<GifDrawable> {

  private static final String KEY_ENCODE_TRANSFORMATION =
      "com.bumptech.glide.load.resource.gif.GifResourceEncoder.EncodeTransformation";
   /**
   * A boolean option that, if set to <code>true</code>, causes the fully transformed
   * GIF to be written to cache.
   *
   * <p> Warning - encoding GIFs is slow and often produces larger and less efficient GIFs than
   * the originals. Re-encoding may be worth it to decrease the size of very large GIFs. </p>
   *
   * <p> Defaults to <code>false</code>. </p>
   */
  public static final Option<Boolean> ENCODE_TRANSFORMATION =
      Option.disk(KEY_ENCODE_TRANSFORMATION, false, new Option.CacheKeyUpdater<Boolean>() {
        @Override
        public void update(byte[] keyBytes, Boolean value, MessageDigest messageDigest) {
          if (value) {
            messageDigest.update(keyBytes);
          }
        }
      });

  private static final Factory FACTORY = new Factory();
  private static final String TAG = "GifEncoder";
  private final GifDecoder.BitmapProvider provider;
  private final BitmapPool bitmapPool;
  private final Factory factory;

  public ReEncodingGifResourceEncoder(BitmapPool bitmapPool) {
    this(bitmapPool, FACTORY);
  }

  // Visible for testing.
  ReEncodingGifResourceEncoder(BitmapPool bitmapPool, Factory factory) {
    this.bitmapPool = bitmapPool;
    provider = new GifBitmapProvider(bitmapPool);
    this.factory = factory;
  }

  @Override
  public EncodeStrategy getEncodeStrategy(Options options) {
    Boolean encodeTransformation = options.get(ENCODE_TRANSFORMATION);
    return encodeTransformation != null && encodeTransformation
        ? EncodeStrategy.TRANSFORMED : EncodeStrategy.SOURCE;
  }

  @Override
  public boolean encode(Resource<GifDrawable> resource, File file, Options options) {
    GifDrawable drawable = resource.get();
    Transformation<Bitmap> transformation = drawable.getFrameTransformation();
    boolean isTransformed = !(transformation instanceof UnitTransformation);
    if (isTransformed && options.get(ENCODE_TRANSFORMATION)) {
      return encodeTransformedToFile(drawable, file);
    } else {
      return writeDataDirect(drawable.getBuffer(), file);
    }
  }

  private boolean encodeTransformedToFile(GifDrawable drawable, File file) {
    long startTime = LogTime.getLogTime();
    OutputStream os = null;
    boolean success = false;
    try {
      os = new BufferedOutputStream(new FileOutputStream(file));
      success = encodeTransformedToStream(drawable, os);
      os.close();
    } catch (IOException e) {
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, "Failed to encode GIF", e);
      }
    } finally {
      if (os != null) {
        try {
          os.close();
        } catch (IOException e) {
          // Ignored.
        }
      }

    }
    if (Log.isLoggable(TAG, Log.VERBOSE)) {
      Log.v(TAG, "Re-encoded GIF with " + drawable.getFrameCount() + " frames and "
          + drawable.getBuffer().limit() + " bytes in " + LogTime.getElapsedMillis(startTime)
          + " ms");
    }

    return success;
  }

  private boolean encodeTransformedToStream(GifDrawable drawable, OutputStream os) {
    Transformation<Bitmap> transformation = drawable.getFrameTransformation();
    GifDecoder decoder = decodeHeaders(drawable.getBuffer());
    AnimatedGifEncoder encoder = factory.buildEncoder();
    if (!encoder.start(os)) {
      return false;
    }

    for (int i = 0; i < decoder.getFrameCount(); i++) {
      Bitmap currentFrame = decoder.getNextFrame();
      Resource<Bitmap> transformedResource =
          getTransformedFrame(currentFrame, transformation, drawable);
      try {
        if (!encoder.addFrame(transformedResource.get())) {
          return false;
        }
        int currentFrameIndex = decoder.getCurrentFrameIndex();
        int delay = decoder.getDelay(currentFrameIndex);
        encoder.setDelay(delay);

        decoder.advance();
      } finally {
        transformedResource.recycle();
      }
    }

    return encoder.finish();
  }

  private boolean writeDataDirect(ByteBuffer data, File file) {
    try {
      ByteBufferUtil.toFile(data, file);
    } catch (IOException e) {
      if (Log.isLoggable(TAG, Log.WARN)) {
        Log.w(TAG, "Failed to write GIF data", e);
      }
      return false;
    }
    return true;
  }

  private GifDecoder decodeHeaders(ByteBuffer data) {
    GifHeaderParser parser = factory.buildParser();
    parser.setData(data);
    GifHeader header = parser.parseHeader();

    GifDecoder decoder = factory.buildDecoder(provider);
    decoder.setData(header, data);
    decoder.advance();

    return decoder;
  }

  private Resource<Bitmap> getTransformedFrame(Bitmap currentFrame,
      Transformation<Bitmap> transformation, GifDrawable drawable) {
    // TODO: what if current frame is null?
    Resource<Bitmap> bitmapResource = factory.buildFrameResource(currentFrame, bitmapPool);
    Resource<Bitmap> transformedResource = transformation
        .transform(bitmapResource, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
    if (!bitmapResource.equals(transformedResource)) {
      bitmapResource.recycle();
    }
    return transformedResource;
  }

  // Visible for testing.
  static class Factory {

    public GifDecoder buildDecoder(GifDecoder.BitmapProvider bitmapProvider) {
      return new StandardGifDecoder(bitmapProvider);
    }

    public GifHeaderParser buildParser() {
      return new GifHeaderParser();
    }

    public AnimatedGifEncoder buildEncoder() {
      return new AnimatedGifEncoder();
    }

    public Resource<Bitmap> buildFrameResource(Bitmap bitmap, BitmapPool bitmapPool) {
      return new BitmapResource(bitmap, bitmapPool);
    }
  }
}
