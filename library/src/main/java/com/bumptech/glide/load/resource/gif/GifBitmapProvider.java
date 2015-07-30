package com.bumptech.glide.load.resource.gif;


import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bumptech.glide.gifdecoder.GifDecoder;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.engine.bitmap_recycle.ByteArrayPool;

/**
 * Implements {@link com.bumptech.glide.gifdecoder.GifDecoder.BitmapProvider} by wrapping Glide's
 * {@link com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool}.
 */
public final class GifBitmapProvider implements GifDecoder.BitmapProvider {
  private final BitmapPool bitmapPool;
  @Nullable private final ByteArrayPool byteArrayPool;

  /**
   * Constructs an instance without a shared byte array pool. Byte arrays will be always constructed
   * when requested.
   */
  public GifBitmapProvider(BitmapPool bitmapPool) {
    this(bitmapPool, null /*byteArrayPool*/);
  }

  /**
   * Constructs an instance with a shared byte array pool. Byte arrays will be reused where
   * possible.
   */
  public GifBitmapProvider(BitmapPool bitmapPool, @Nullable ByteArrayPool byteArrayPool) {
    this.bitmapPool = bitmapPool;
    this.byteArrayPool = byteArrayPool;
  }

  @NonNull
  @Override
  public Bitmap obtain(int width, int height, Bitmap.Config config) {
    return bitmapPool.getDirty(width, height, config);
  }

  @Override
  public void release(Bitmap bitmap) {
    bitmapPool.put(bitmap);
  }

  @Override
  public byte[] obtainByteArray(int size) {
    if (byteArrayPool == null) {
      return new byte[size];
    }
    return byteArrayPool.get(size);
  }

  @Override
  public void release(byte[] bytes) {
    if (byteArrayPool == null) {
      return;
    }
    byteArrayPool.put(bytes);
  }
}
