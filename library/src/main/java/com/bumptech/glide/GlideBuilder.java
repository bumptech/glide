package com.bumptech.glide;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.util.Log;

import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.Engine;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPoolAdapter;
import com.bumptech.glide.load.engine.bitmap_recycle.ByteArrayPool;
import com.bumptech.glide.load.engine.bitmap_recycle.LruBitmapPool;
import com.bumptech.glide.load.engine.bitmap_recycle.LruByteArrayPool;
import com.bumptech.glide.load.engine.cache.DiskCache;
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory;
import com.bumptech.glide.load.engine.cache.LruResourceCache;
import com.bumptech.glide.load.engine.cache.MemoryCache;
import com.bumptech.glide.load.engine.cache.MemorySizeCalculator;
import com.bumptech.glide.load.engine.executor.FifoPriorityThreadPoolExecutor;

import java.util.Collections;
import java.util.concurrent.ExecutorService;

/**
 * A builder class for setting default structural classes for Glide to use.
 */
public class GlideBuilder {
  private static final String TAG = "Glide";
  private final Context context;

  private Engine engine;
  private BitmapPool bitmapPool;
  private ByteArrayPool byteArrayPool;
  private MemoryCache memoryCache;
  private ExecutorService sourceService;
  private ExecutorService diskCacheService;
  private DecodeFormat decodeFormat;
  private DiskCache.Factory diskCacheFactory;
  private MemorySizeCalculator memorySizeCalculator;

  public GlideBuilder(Context context) {
    this.context = context.getApplicationContext();
  }

  /**
   * Sets the {@link com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool} implementation to use
   * to store and retrieve reused {@link android.graphics.Bitmap}s.
   *
   * @param bitmapPool The pool to use.
   * @return This builder.
   */
  public GlideBuilder setBitmapPool(BitmapPool bitmapPool) {
    this.bitmapPool = bitmapPool;
    return this;
  }

  /**
   * Sets the {@link ByteArrayPool} implementation to allow variable sized byte arrays to be stored
   * and retrieved as needed.
   *
   * @param byteArrayPool The pool to use.
   * @return This builder.
   */
  public GlideBuilder setByteArrayPool(ByteArrayPool byteArrayPool) {
    this.byteArrayPool = byteArrayPool;
    return this;
  }

  /**
   * Sets the {@link com.bumptech.glide.load.engine.cache.MemoryCache} implementation to store
   * {@link com.bumptech.glide.load.engine.Resource}s that are not currently in use.
   *
   * @param memoryCache The cache to use.
   * @return This builder.
   */
  public GlideBuilder setMemoryCache(MemoryCache memoryCache) {
    this.memoryCache = memoryCache;
    return this;
  }

  /**
   * Sets the {@link com.bumptech.glide.load.engine.cache.DiskCache} implementation to use to store
   * {@link com.bumptech.glide.load.engine.Resource} data and thumbnails.
   *
   * @param diskCache The disk cache to use.
   * @return This builder.
   * @deprecated Creating a disk cache directory on the main thread causes strict mode violations,
   * use {@link #setDiskCache(com.bumptech.glide.load.engine.cache.DiskCache.Factory)} instead.
   * Scheduled to be removed in Glide 4.0.
   */
  @Deprecated
  public GlideBuilder setDiskCache(final DiskCache diskCache) {
    return setDiskCache(new DiskCache.Factory() {
      @Override
      public DiskCache build() {
        return diskCache;
      }
    });
  }

  /**
   * Sets the {@link com.bumptech.glide.load.engine.cache.DiskCache.Factory} implementation to use
   * to construct the {@link com.bumptech.glide.load.engine.cache.DiskCache} to use to store {@link
   * com.bumptech.glide.load.engine.Resource} data on disk.
   *
   * @param diskCacheFactory The disk cche factory to use.
   * @return This builder.
   */
  public GlideBuilder setDiskCache(DiskCache.Factory diskCacheFactory) {
    this.diskCacheFactory = diskCacheFactory;
    return this;
  }

  /**
   * Sets the {@link java.util.concurrent.ExecutorService} implementation to use when retrieving
   * {@link com.bumptech.glide.load.engine.Resource}s that are not already in the cache.
   *
   * <p> Any implementation must order requests based on their {@link com.bumptech.glide.Priority}
   * for thumbnail requests to work properly. </p>
   *
   * @param service The ExecutorService to use.
   * @return This builder.
   * @see #setDiskCacheService(java.util.concurrent.ExecutorService)
   * @see com.bumptech.glide.load.engine.executor.FifoPriorityThreadPoolExecutor
   */
  public GlideBuilder setResizeService(ExecutorService service) {
    this.sourceService = service;
    return this;
  }

  /**
   * Sets the {@link java.util.concurrent.ExecutorService} implementation to use when retrieving
   * {@link com.bumptech.glide.load.engine.Resource}s that are currently in cache.
   *
   * <p> Any implementation must order requests based on their {@link com.bumptech.glide.Priority}
   * for thumbnail requests to work properly. </p>
   *
   * @param service The ExecutorService to use.
   * @return This builder.
   * @see #setResizeService(java.util.concurrent.ExecutorService)
   * @see com.bumptech.glide.load.engine.executor.FifoPriorityThreadPoolExecutor
   */
  public GlideBuilder setDiskCacheService(ExecutorService service) {
    this.diskCacheService = service;
    return this;
  }

  /**
   * Sets the {@link com.bumptech.glide.load.DecodeFormat} that will be the default format for all
   * the default decoders that can change the {@link android.graphics.Bitmap.Config} of the {@link
   * android.graphics.Bitmap}s they decode.
   *
   * <p> Decode format is always a suggestion, not a requirement. See {@link
   * com.bumptech.glide.load.DecodeFormat} for more details. </p>
   *
   * <p> If you instantiate and use a custom decoder, it will use {@link
   * com.bumptech.glide.load.DecodeFormat#DEFAULT} as its default. </p>
   *
   * <p> Calls to this method are ignored on KitKat and Lollipop. See #301. </p>
   *
   * @param decodeFormat The format to use.
   * @return This builder.
   */
  public GlideBuilder setDecodeFormat(DecodeFormat decodeFormat) {
    if (DecodeFormat.REQUIRE_ARGB_8888 && decodeFormat != DecodeFormat.PREFER_ARGB_8888) {
      this.decodeFormat = DecodeFormat.PREFER_ARGB_8888;
      if (Log.isLoggable(TAG, Log.WARN)) {
        Log.w(TAG, "Unsafe to use RGB_565 on KitKat or Lollipop, ignoring setDecodeFormat");
      }
    } else {
      this.decodeFormat = decodeFormat;
    }
    return this;
  }

  /**
   * Sets the {@link MemorySizeCalculator} to use to calculate maximum sizes for default
   * {@link MemoryCache MemoryCaches} and/or default {@link BitmapPool BitmapPools}.
   *
   * @see #setMemorySizeCalculator(MemorySizeCalculator)
   *
   * @param builder The builder to use (will not be modified).
   * @return This builder.
   */
  public GlideBuilder setMemorySizeCalculator(MemorySizeCalculator.Builder builder) {
    return setMemorySizeCalculator(builder.build());
  }

  /**
   * Sets the {@link MemorySizeCalculator} to use to calculate maximum sizes for default
   * {@link MemoryCache MemoryCaches} and/or default {@link BitmapPool BitmapPools}.
   *
   * <p>The given {@link MemorySizeCalculator} will not affect custom pools or caches provided
   * via {@link #setBitmapPool(BitmapPool)} or {@link #setMemoryCache(MemoryCache)}.
   *
   * @param calculator The calculator to use.
   * @return This builder.
   */
  public GlideBuilder setMemorySizeCalculator(MemorySizeCalculator calculator) {
    this.memorySizeCalculator = calculator;
    return this;
  }

  // For testing.
  GlideBuilder setEngine(Engine engine) {
    this.engine = engine;
    return this;
  }

  Glide createGlide() {
    if (sourceService == null) {
      final int cores = Math.max(1, Runtime.getRuntime().availableProcessors());
      sourceService = new FifoPriorityThreadPoolExecutor("source", cores);
    }
    if (diskCacheService == null) {
      diskCacheService = new FifoPriorityThreadPoolExecutor("disk-cache", 1);
    }

    if (memorySizeCalculator == null) {
      memorySizeCalculator = new MemorySizeCalculator.Builder(context).build();
    }

    if (bitmapPool == null) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
        int size = memorySizeCalculator.getBitmapPoolSize();
        if (DecodeFormat.REQUIRE_ARGB_8888) {
          bitmapPool = new LruBitmapPool(size, Collections.singleton(Bitmap.Config.ARGB_8888));
        } else {
          bitmapPool = new LruBitmapPool(size);
        }
      } else {
        bitmapPool = new BitmapPoolAdapter();
      }
    }

    if (byteArrayPool == null) {
      byteArrayPool = new LruByteArrayPool();
    }

    if (memoryCache == null) {
      memoryCache = new LruResourceCache(memorySizeCalculator.getMemoryCacheSize());
    }

    if (diskCacheFactory == null) {
      diskCacheFactory = new InternalCacheDiskCacheFactory(context, Glide.DEFAULT_DISK_CACHE_SIZE);
    }

    if (engine == null) {
      engine = new Engine(memoryCache, diskCacheFactory, diskCacheService, sourceService);
    }

    if (decodeFormat == null) {
      decodeFormat = DecodeFormat.DEFAULT;
    }

    return new Glide(engine, memoryCache, bitmapPool, byteArrayPool, context, decodeFormat);
  }
}