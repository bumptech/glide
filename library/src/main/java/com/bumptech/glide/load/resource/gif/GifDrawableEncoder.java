package com.bumptech.glide.load.resource.gif;

import android.util.Log;

import com.bumptech.glide.load.EncodeStrategy;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.util.ByteBufferUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

/**
 * Writes the original bytes of a {@link com.bumptech.glide.load.resource.gif.GifDrawable} to an
 * {@link java.io.OutputStream}.
 */
public class GifDrawableEncoder implements ResourceEncoder<GifDrawable> {
  private static final String TAG = "GifEncoder";

  @Override
  public EncodeStrategy getEncodeStrategy(Map<String, Object> options) {
    return EncodeStrategy.SOURCE;
  }

  @Override
  public boolean encode(Resource<GifDrawable> data, OutputStream os, Map<String, Object> options) {
    GifDrawable drawable = data.get();
    try {
      ByteBufferUtil.encode(drawable.getBuffer(), os);
    } catch (IOException e) {
      if (Log.isLoggable(TAG, Log.WARN)) {
        Log.w(TAG, "Failed to encode gif drawable data", e);
      }
      return false;
    }
    return true;
  }
}
