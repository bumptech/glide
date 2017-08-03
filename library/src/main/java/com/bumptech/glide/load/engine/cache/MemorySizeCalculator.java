package com.bumptech.glide.load.engine.cache;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.text.format.Formatter;
import android.util.DisplayMetrics;
import android.util.Log;
import com.bumptech.glide.util.Preconditions;

/**
 * A calculator that tries to intelligently determine cache sizes for a given device based on some
 * constants and the devices screen density, width, and height.
 */
public final class MemorySizeCalculator {
  private static final String TAG = "MemorySizeCalculator";
  // Visible for testing.
  static final int BYTES_PER_ARGB_8888_PIXEL = 4;
  static final int LOW_MEMORY_BYTE_ARRAY_POOL_DIVISOR = 2;

  private final int bitmapPoolSize;
  private final int memoryCacheSize;
  private final Context context;
  private final int arrayPoolSize;

  interface ScreenDimensions {
    int getWidthPixels();
    int getHeightPixels();
  }

  MemorySizeCalculator(Context context, ActivityManager activityManager,
      ScreenDimensions screenDimensions, float memoryCacheScreens, float bitmapPoolScreens,
      int targetArrayPoolSize, float maxSizeMultiplier, float lowMemoryMaxSizeMultiplier) {
    this.context = context;
    arrayPoolSize =
        isLowMemoryDevice(activityManager)
            ? targetArrayPoolSize / LOW_MEMORY_BYTE_ARRAY_POOL_DIVISOR
            : targetArrayPoolSize;
    final int maxSize = getMaxSize(activityManager, maxSizeMultiplier, lowMemoryMaxSizeMultiplier);

    final int screenSize = screenDimensions.getWidthPixels() * screenDimensions.getHeightPixels()
        * BYTES_PER_ARGB_8888_PIXEL;

    int targetPoolSize = Math.round(screenSize * bitmapPoolScreens);
    int targetMemoryCacheSize = Math.round(screenSize * memoryCacheScreens);
    int availableSize = maxSize - arrayPoolSize;

    if (targetMemoryCacheSize + targetPoolSize <= availableSize) {
      memoryCacheSize = targetMemoryCacheSize;
      bitmapPoolSize = targetPoolSize;
    } else {
      float part = availableSize / (bitmapPoolScreens + memoryCacheScreens);
      memoryCacheSize = Math.round(part * memoryCacheScreens);
      bitmapPoolSize = Math.round(part * bitmapPoolScreens);
    }

    if (Log.isLoggable(TAG, Log.DEBUG)) {
      Log.d(
          TAG,
          "Calculation complete"
              + ", Calculated memory cache size: "
              + toMb(memoryCacheSize)
              + ", pool size: "
              + toMb(bitmapPoolSize)
              + ", byte array size: "
              + toMb(arrayPoolSize)
              + ", memory class limited? "
              + (targetMemoryCacheSize + targetPoolSize > maxSize)
              + ", max size: "
              + toMb(maxSize)
              + ", memoryClass: "
              + activityManager.getMemoryClass()
              + ", isLowMemoryDevice: "
              + isLowMemoryDevice(activityManager));
    }
  }

  /**
   * Returns the recommended memory cache size for the device it is run on in bytes.
   */
  public int getMemoryCacheSize() {
    return memoryCacheSize;
  }

  /**
   * Returns the recommended bitmap pool size for the device it is run on in bytes.
   */
  public int getBitmapPoolSize() {
    return bitmapPoolSize;
  }

  /**
   * Returns the recommended array pool size for the device it is run on in bytes.
   */
  public int getArrayPoolSizeInBytes() {
    return arrayPoolSize;
  }

  private static int getMaxSize(ActivityManager activityManager, float maxSizeMultiplier,
      float lowMemoryMaxSizeMultiplier) {
    final int memoryClassBytes = activityManager.getMemoryClass() * 1024 * 1024;
    final boolean isLowMemoryDevice = isLowMemoryDevice(activityManager);
    return Math.round(memoryClassBytes * (isLowMemoryDevice ? lowMemoryMaxSizeMultiplier
        : maxSizeMultiplier));
  }

  private String toMb(int bytes) {
    return Formatter.formatFileSize(context, bytes);
  }

  private static boolean isLowMemoryDevice(ActivityManager activityManager) {
    // Explicitly check with an if statement, on some devices both parts of boolean expressions
    // can be evaluated even if we'd normally expect a short circuit.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      return activityManager.isLowRamDevice();
    } else {
      return false;
    }
  }

  /**
   * Constructs an {@link MemorySizeCalculator} with reasonable defaults that can be optionally
   * overridden.
   */
  public static final class Builder {
    // Visible for testing.
    static final int MEMORY_CACHE_TARGET_SCREENS = 2;
    static final int BITMAP_POOL_TARGET_SCREENS = 4;
    static final float MAX_SIZE_MULTIPLIER = 0.4f;
    static final float LOW_MEMORY_MAX_SIZE_MULTIPLIER = 0.33f;
    // 4MB.
    static final int ARRAY_POOL_SIZE_BYTES = 4 * 1024 * 1024;

    private final Context context;

    // Modifiable for testing.
    private ActivityManager activityManager;
    private ScreenDimensions screenDimensions;

    private float memoryCacheScreens = MEMORY_CACHE_TARGET_SCREENS;
    private float bitmapPoolScreens = BITMAP_POOL_TARGET_SCREENS;
    private float maxSizeMultiplier = MAX_SIZE_MULTIPLIER;
    private float lowMemoryMaxSizeMultiplier = LOW_MEMORY_MAX_SIZE_MULTIPLIER;
    private int arrayPoolSizeBytes = ARRAY_POOL_SIZE_BYTES;

    public Builder(Context context) {
      this.context = context;
      activityManager =
          (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
      screenDimensions =
          new DisplayMetricsScreenDimensions(context.getResources().getDisplayMetrics());
    }

    /**
     * Sets the number of device screens worth of pixels the
     * {@link com.bumptech.glide.load.engine.cache.MemoryCache} should be able to hold and
     * returns this Builder.
     */
    public Builder setMemoryCacheScreens(float memoryCacheScreens) {
      Preconditions.checkArgument(bitmapPoolScreens >= 0,
          "Memory cache screens must be greater than or equal to 0");
      this.memoryCacheScreens = memoryCacheScreens;
      return this;
    }

    /**
     * Sets the number of device screens worth of pixels the
     * {@link com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool} should be able to hold and
     * returns this Builder.
     */
    public Builder setBitmapPoolScreens(float bitmapPoolScreens) {
      Preconditions.checkArgument(bitmapPoolScreens >= 0,
          "Bitmap pool screens must be greater than or equal to 0");
      this.bitmapPoolScreens = bitmapPoolScreens;
      return this;
    }

    /**
     * Sets the maximum percentage of the device's memory class for standard devices that can be
     * taken up by Glide's {@link com.bumptech.glide.load.engine.cache.MemoryCache} and
     * {@link com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool} put together, and returns
     * this builder.
     */
    public Builder setMaxSizeMultiplier(float maxSizeMultiplier) {
      Preconditions.checkArgument(maxSizeMultiplier >= 0 && maxSizeMultiplier <= 1,
          "Size multiplier must be between 0 and 1");
      this.maxSizeMultiplier = maxSizeMultiplier;
      return this;
    }

    /**
     * Sets the maximum percentage of the device's memory class for low ram devices that can be
     * taken up by Glide's {@link com.bumptech.glide.load.engine.cache.MemoryCache} and
     * {@link com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool} put together, and returns
     * this builder.
     *
     * @see ActivityManager#isLowRamDevice()
     */
    public Builder setLowMemoryMaxSizeMultiplier(float lowMemoryMaxSizeMultiplier) {
      Preconditions.checkArgument(
          lowMemoryMaxSizeMultiplier >= 0 && lowMemoryMaxSizeMultiplier <= 1,
              "Low memory max size multiplier must be between 0 and 1");
      this.lowMemoryMaxSizeMultiplier = lowMemoryMaxSizeMultiplier;
      return this;
    }

    /**
     * Sets the size in bytes of the {@link
     * com.bumptech.glide.load.engine.bitmap_recycle.ArrayPool} to use to store temporary
     * arrays while decoding data and returns this builder.
     *
     * <p>This number will be halved on low memory devices that return {@code true} from
     * {@link ActivityManager#isLowRamDevice()}.
     */
    public Builder setArrayPoolSize(int arrayPoolSizeBytes) {
      this.arrayPoolSizeBytes = arrayPoolSizeBytes;
      return this;
    }

    // Visible for testing.
    Builder setActivityManager(ActivityManager activityManager) {
      this.activityManager = activityManager;
      return this;
    }

    // Visible for testing.
    Builder setScreenDimensions(ScreenDimensions screenDimensions) {
      this.screenDimensions = screenDimensions;
      return this;
    }

    public MemorySizeCalculator build() {
      return new MemorySizeCalculator(context, activityManager, screenDimensions,
          memoryCacheScreens, bitmapPoolScreens, arrayPoolSizeBytes, maxSizeMultiplier,
          lowMemoryMaxSizeMultiplier);
      }
  }

  private static final class DisplayMetricsScreenDimensions implements ScreenDimensions {
    private final DisplayMetrics displayMetrics;

    public DisplayMetricsScreenDimensions(DisplayMetrics displayMetrics) {
      this.displayMetrics = displayMetrics;
    }

    @Override
    public int getWidthPixels() {
      return displayMetrics.widthPixels;
    }

    @Override
    public int getHeightPixels() {
      return displayMetrics.heightPixels;
    }
  }
}
