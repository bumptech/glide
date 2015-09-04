package com.bumptech.glide.load.engine.bitmap_recycle;

/**
 * A fixed size LruByteArrayPool that evicts arrays using an LRU strategy to keep the pool under
 * the maximum byte size.
 *
 * TODO: update Glide to use ArrayPool<byte[]> instead.
 */
public final class LruByteArrayPool implements ByteArrayPool {
  // 4MB.
  static final int DEFAULT_SIZE = 4 * 1024 * 1024;

  private final ArrayPool arrayPool;

  public LruByteArrayPool() {
    this(DEFAULT_SIZE);
  }

  public LruByteArrayPool(int maxSize) {
    this.arrayPool = new LruArrayPool(maxSize);
  }

  @Override
  public synchronized void put(byte[] bytes) {
    arrayPool.put(bytes, byte[].class);
  }

  @Override
  public byte[] get(int size) {
    return arrayPool.get(size, byte[].class);
  }
}
