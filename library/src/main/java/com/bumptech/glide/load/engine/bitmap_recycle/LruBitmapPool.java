package com.bumptech.glide.load.engine.bitmap_recycle;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.ComponentCallbacks2;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.util.Synthetic;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * An {@link com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool} implementation that uses an
 * {@link com.bumptech.glide.load.engine.bitmap_recycle.LruPoolStrategy} to bucket {@link Bitmap}s
 * and then uses an LRU eviction policy to evict {@link android.graphics.Bitmap}s from the least
 * recently used bucket in order to keep the pool below a given maximum size limit.
 */
public class LruBitmapPool implements BitmapPool {
  private static final String TAG = "LruBitmapPool";
  private static final Bitmap.Config DEFAULT_CONFIG = Bitmap.Config.ARGB_8888;

  private final LruPoolStrategy strategy;
  private final Set<Bitmap.Config> allowedConfigs;
  private final long initialMaxSize;
  private final BitmapTracker tracker;

  private long maxSize;
  private long currentSize;
  private int hits;
  private int misses;
  private int puts;
  private int evictions;

  // Exposed for testing only.
  LruBitmapPool(long maxSize, LruPoolStrategy strategy, Set<Bitmap.Config> allowedConfigs) {
    this.initialMaxSize = maxSize;
    this.maxSize = maxSize;
    this.strategy = strategy;
    this.allowedConfigs = allowedConfigs;
    this.tracker = new NullBitmapTracker();
  }

  /**
   * Constructor for LruBitmapPool.
   *
   * @param maxSize The initial maximum size of the pool in bytes.
   */
  public LruBitmapPool(long maxSize) {
    this(maxSize, getDefaultStrategy(), getDefaultAllowedConfigs());
  }

  /**
   * Constructor for LruBitmapPool.
   *
   * @param maxSize The initial maximum size of the pool in bytes.
   * @param allowedConfigs A white listed put of {@link android.graphics.Bitmap.Config} that are
   *     allowed to be put into the pool. Configs not in the allowed put will be rejected.
   */
  // Public API.
  @SuppressWarnings("unused")
  public LruBitmapPool(long maxSize, Set<Bitmap.Config> allowedConfigs) {
    this(maxSize, getDefaultStrategy(), allowedConfigs);
  }

  /** Returns the number of cache hits for bitmaps in the pool. */
  public long hitCount() {
    return hits;
  }

  /** Returns the number of cache misses for bitmaps in the pool. */
  public long missCount() {
    return misses;
  }

  /** Returns the number of bitmaps that have been evicted from the pool. */
  public long evictionCount() {
    return evictions;
  }

  /** Returns the current size of the pool in bytes. */
  public long getCurrentSize() {
    return currentSize;
  }

  @Override
  public long getMaxSize() {
    return maxSize;
  }

  @Override
  public synchronized void setSizeMultiplier(float sizeMultiplier) {
    maxSize = Math.round(initialMaxSize * sizeMultiplier);
    evict();
  }

  @Override
  public synchronized void put(Bitmap bitmap) {
    if (bitmap == null) {
      throw new NullPointerException("Bitmap must not be null");
    }
    if (bitmap.isRecycled()) {
      throw new IllegalStateException("Cannot pool recycled bitmap");
    }
    if (!bitmap.isMutable()
        || strategy.getSize(bitmap) > maxSize
        || !allowedConfigs.contains(bitmap.getConfig())) {
      if (Log.isLoggable(TAG, Log.VERBOSE)) {
        Log.v(
            TAG,
            "Reject bitmap from pool"
                + ", bitmap: "
                + strategy.logBitmap(bitmap)
                + ", is mutable: "
                + bitmap.isMutable()
                + ", is allowed config: "
                + allowedConfigs.contains(bitmap.getConfig()));
      }
      bitmap.recycle();
      return;
    }

    final int size = strategy.getSize(bitmap);
    strategy.put(bitmap);
    tracker.add(bitmap);

    puts++;
    currentSize += size;

    if (Log.isLoggable(TAG, Log.VERBOSE)) {
      Log.v(TAG, "Put bitmap in pool=" + strategy.logBitmap(bitmap));
    }
    dump();

    evict();
  }

  private void evict() {
    trimToSize(maxSize);
  }

  @Override
  @NonNull
  public Bitmap get(int width, int height, Bitmap.Config config) {
    Bitmap result = getDirtyOrNull(width, height, config);
    if (result != null) {
      // Bitmaps in the pool contain random data that in some cases must be cleared for an image
      // to be rendered correctly. we shouldn't force all consumers to independently erase the
      // contents individually, so we do so here. See issue #131.
      result.eraseColor(Color.TRANSPARENT);
    } else {
      result = createBitmap(width, height, config);
    }

    return result;
  }

  @NonNull
  @Override
  public Bitmap getDirty(int width, int height, Bitmap.Config config) {
    Bitmap result = getDirtyOrNull(width, height, config);
    if (result == null) {
      result = createBitmap(width, height, config);
    }
    return result;
  }

  @NonNull
  private static Bitmap createBitmap(int width, int height, @Nullable Bitmap.Config config) {
    return Bitmap.createBitmap(width, height, config != null ? config : DEFAULT_CONFIG);
  }

  @TargetApi(Build.VERSION_CODES.O)
  private static void assertNotHardwareConfig(Bitmap.Config config) {
    // Avoid short circuiting on sdk int since it breaks on some versions of Android.
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
      return;
    }

    if (config == Bitmap.Config.HARDWARE) {
      throw new IllegalArgumentException(
          "Cannot create a mutable Bitmap with config: "
              + config
              + ". Consider setting Downsampler#ALLOW_HARDWARE_CONFIG to false in your"
              + " RequestOptions and/or in GlideBuilder.setDefaultRequestOptions");
    }
  }

  @Nullable
  private synchronized Bitmap getDirtyOrNull(
      int width, int height, @Nullable Bitmap.Config config) {
    assertNotHardwareConfig(config);
    // Config will be null for non public config types, which can lead to transformations naively
    // passing in null as the requested config here. See issue #194.
    final Bitmap result = strategy.get(width, height, config != null ? config : DEFAULT_CONFIG);
    if (result == null) {
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, "Missing bitmap=" + strategy.logBitmap(width, height, config));
      }
      misses++;
    } else {
      hits++;
      currentSize -= strategy.getSize(result);
      tracker.remove(result);
      normalize(result);
    }
    if (Log.isLoggable(TAG, Log.VERBOSE)) {
      Log.v(TAG, "Get bitmap=" + strategy.logBitmap(width, height, config));
    }
    dump();

    return result;
  }

  // Setting these two values provides Bitmaps that are essentially equivalent to those returned
  // from Bitmap.createBitmap.
  private static void normalize(Bitmap bitmap) {
    bitmap.setHasAlpha(true);
    maybeSetPreMultiplied(bitmap);
  }

  @TargetApi(Build.VERSION_CODES.KITKAT)
  private static void maybeSetPreMultiplied(Bitmap bitmap) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      bitmap.setPremultiplied(true);
    }
  }

  @Override
  public void clearMemory() {
    if (Log.isLoggable(TAG, Log.DEBUG)) {
      Log.d(TAG, "clearMemory");
    }
    trimToSize(0);
  }

  @SuppressLint("InlinedApi")
  @Override
  public void trimMemory(int level) {
    if (Log.isLoggable(TAG, Log.DEBUG)) {
      Log.d(TAG, "trimMemory, level=" + level);
    }
    if ((level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND)
        || ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            && (level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN))) {
      clearMemory();
    } else if ((level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN)
        || (level == ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL)) {
      trimToSize(getMaxSize() / 2);
    }
  }

  private synchronized void trimToSize(long size) {
    while (currentSize > size) {
      final Bitmap removed = strategy.removeLast();
      // TODO: This shouldn't ever happen, see #331.
      if (removed == null) {
        if (Log.isLoggable(TAG, Log.WARN)) {
          Log.w(TAG, "Size mismatch, resetting");
          dumpUnchecked();
        }
        currentSize = 0;
        return;
      }
      tracker.remove(removed);
      currentSize -= strategy.getSize(removed);
      evictions++;
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, "Evicting bitmap=" + strategy.logBitmap(removed));
      }
      dump();
      removed.recycle();
    }
  }

  private void dump() {
    if (Log.isLoggable(TAG, Log.VERBOSE)) {
      dumpUnchecked();
    }
  }

  private void dumpUnchecked() {
    Log.v(
        TAG,
        "Hits="
            + hits
            + ", misses="
            + misses
            + ", puts="
            + puts
            + ", evictions="
            + evictions
            + ", currentSize="
            + currentSize
            + ", maxSize="
            + maxSize
            + "\nStrategy="
            + strategy);
  }

  private static LruPoolStrategy getDefaultStrategy() {
    final LruPoolStrategy strategy;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      strategy = new SizeConfigStrategy();
    } else {
      strategy = new AttributeStrategy();
    }
    return strategy;
  }

  @TargetApi(Build.VERSION_CODES.O)
  private static Set<Bitmap.Config> getDefaultAllowedConfigs() {
    Set<Bitmap.Config> configs = new HashSet<>(Arrays.asList(Bitmap.Config.values()));
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      // GIFs, among other types, end up with a native Bitmap config that doesn't map to a java
      // config and is treated as null in java code. On KitKat+ these Bitmaps can be reconfigured
      // and are suitable for re-use.
      configs.add(null);
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      configs.remove(Bitmap.Config.HARDWARE);
    }
    return Collections.unmodifiableSet(configs);
  }

  private interface BitmapTracker {
    void add(Bitmap bitmap);

    void remove(Bitmap bitmap);
  }

  @SuppressWarnings("unused")
  // Only used for debugging
  private static class ThrowingBitmapTracker implements BitmapTracker {
    private final Set<Bitmap> bitmaps = Collections.synchronizedSet(new HashSet<Bitmap>());

    @Override
    public void add(Bitmap bitmap) {
      if (bitmaps.contains(bitmap)) {
        throw new IllegalStateException(
            "Can't add already added bitmap: "
                + bitmap
                + " ["
                + bitmap.getWidth()
                + "x"
                + bitmap.getHeight()
                + "]");
      }
      bitmaps.add(bitmap);
    }

    @Override
    public void remove(Bitmap bitmap) {
      if (!bitmaps.contains(bitmap)) {
        throw new IllegalStateException("Cannot remove bitmap not in tracker");
      }
      bitmaps.remove(bitmap);
    }
  }

  private static final class NullBitmapTracker implements BitmapTracker {

    @Synthetic
    NullBitmapTracker() {}

    @Override
    public void add(Bitmap bitmap) {
      // Do nothing.
    }

    @Override
    public void remove(Bitmap bitmap) {
      // Do nothing.
    }
  }
}
