package com.bumptech.glide.load.resource.bitmap;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.util.Log;
import androidx.annotation.ChecksSdkIntAtLeast;
import androidx.annotation.GuardedBy;
import androidx.annotation.VisibleForTesting;
import com.bumptech.glide.util.Util;
import java.io.File;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * State and constants for interacting with {@link android.graphics.Bitmap.Config#HARDWARE} on
 * Android O+.
 */
public final class HardwareConfigState {
  private static final String TAG = "HardwareConfig";

  /**
   * Force the state to wait until a call to allow hardware Bitmaps to be used when they'd otherwise
   * be eligible to work around a framework issue pre Q that can cause a native crash when
   * allocating a hardware Bitmap in this specific circumstance. See b/126573603#comment12 for
   * details.
   */
  public static final boolean BLOCK_HARDWARE_BITMAPS_WHEN_GL_CONTEXT_MIGHT_NOT_BE_INITIALIZED =
      Build.VERSION.SDK_INT < Build.VERSION_CODES.Q;

  /** Support for the hardware bitmap config was added in Android O. */
  @ChecksSdkIntAtLeast(api = VERSION_CODES.P)
  public static final boolean HARDWARE_BITMAPS_SUPPORTED =
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.P;

  /**
   * Allows us to check to make sure we're not exceeding the FD limit for a process with hardware
   * {@link Bitmap}s.
   *
   * <p>{@link Bitmap.Config#HARDWARE} {@link Bitmap}s require two FDs (depending on the driver).
   * Processes have an FD limit of 1024 (at least on O). With sufficiently small {@link Bitmap}s
   * and/or a sufficiently large {@link com.bumptech.glide.load.engine.cache.MemoryCache}, we can
   * end up with enough {@link Bitmap}s in memory that we blow through the FD limit, which causes
   * graphics errors, Binder errors, and a variety of crashes.
   *
   * <p>Calling list.size() should be relatively efficient (hopefully < 1ms on average) because
   * /proc is an in-memory FS.
   */
  private static final File FD_SIZE_LIST = new File("/proc/self/fd");

  /**
   * Each FD check takes 1-2ms, so to avoid overhead, only check every N decodes. 50 is more or less
   * arbitrary.
   */
  private static final int MINIMUM_DECODES_BETWEEN_FD_CHECKS = 50;
  // 20k.
  private static final int MAXIMUM_FDS_FOR_HARDWARE_CONFIGS_P = 20000;

  /**
   * Some P devices seem to have a more O like FD count, so we'll manually reduce the number of FDs
   * we use for hardware bitmaps. See b/139097735.
   */
  private static final int REDUCED_MAX_FDS_FOR_HARDWARE_CONFIGS_P = 500;

  /**
   * @deprecated This constant is unused and will be removed in a future version, avoid using it.
   */
  @Deprecated public static final int NO_MAX_FD_COUNT = -1;

  private static volatile HardwareConfigState instance;

  private final int sdkBasedMaxFdCount;
  @GuardedBy("this")
  private int decodesSinceLastFdCheck;

  @GuardedBy("this")
  private boolean isFdSizeBelowHardwareLimit = true;

  /**
   * Only mutated on the main thread. Read by any number of background threads concurrently.
   *
   * <p>Defaults to {@code false} because we need to wait for the GL context to be initialized and
   * it defaults to not initialized (https://b.corp.google.com/issues/126573603#comment12).
   */
  private final AtomicBoolean isHardwareConfigAllowedByAppState = new AtomicBoolean(false);

  public static HardwareConfigState getInstance() {
    if (instance == null) {
      synchronized (HardwareConfigState.class) {
        if (instance == null) {
          instance = new HardwareConfigState();
        }
      }
    }
    return instance;
  }

  @VisibleForTesting
  HardwareConfigState() {
    sdkBasedMaxFdCount = MAXIMUM_FDS_FOR_HARDWARE_CONFIGS_P;
  }

  public void blockHardwareBitmaps() {
    Util.assertMainThread();
    isHardwareConfigAllowedByAppState.set(false);
  }

  public void unblockHardwareBitmaps() {
    Util.assertMainThread();
    isHardwareConfigAllowedByAppState.set(true);
  }

  public boolean isHardwareConfigAllowed(
      int targetWidth,
      int targetHeight,
      boolean isHardwareConfigAllowed,
      boolean isExifOrientationRequired) {
    if (!isHardwareConfigAllowed) {
      if (Log.isLoggable(TAG, Log.VERBOSE)) {
        Log.v(TAG, "Hardware config disallowed by caller");
      }
      return false;
    }
    if (!HARDWARE_BITMAPS_SUPPORTED) {
      if (Log.isLoggable(TAG, Log.VERBOSE)) {
        Log.v(TAG, "Hardware config disallowed by sdk");
      }
      return false;
    }
    if (areHardwareBitmapsBlockedByAppState()) {
      if (Log.isLoggable(TAG, Log.VERBOSE)) {
        Log.v(TAG, "Hardware config disallowed by app state");
      }
      return false;
    }
    if (isExifOrientationRequired) {
      if (Log.isLoggable(TAG, Log.VERBOSE)) {
        Log.v(TAG, "Hardware config disallowed because exif orientation is required");
      }
      return false;
    }
    if (targetWidth < 0 || targetHeight < 0) {
      if (Log.isLoggable(TAG, Log.VERBOSE)) {
        Log.v(TAG, "Hardware config disallowed because of invalid dimensions");
      }
      return false;
    }
    // Make sure to call isFdSizeBelowHardwareLimit last because it has side affects.
    if (!isFdSizeBelowHardwareLimit()) {
      if (Log.isLoggable(TAG, Log.VERBOSE)) {
        Log.v(TAG, "Hardware config disallowed because there are insufficient FDs");
      }
      return false;
    }

    return true;
  }

  private boolean areHardwareBitmapsBlockedByAppState() {
    return BLOCK_HARDWARE_BITMAPS_WHEN_GL_CONTEXT_MIGHT_NOT_BE_INITIALIZED
        && !isHardwareConfigAllowedByAppState.get();
  }

  @TargetApi(Build.VERSION_CODES.O)
  boolean setHardwareConfigIfAllowed(
      int targetWidth,
      int targetHeight,
      BitmapFactory.Options optionsWithScaling,
      boolean isHardwareConfigAllowed,
      boolean isExifOrientationRequired) {
    boolean result =
        isHardwareConfigAllowed(
            targetWidth, targetHeight, isHardwareConfigAllowed, isExifOrientationRequired);
    if (result) {
      optionsWithScaling.inPreferredConfig = Bitmap.Config.HARDWARE;
      optionsWithScaling.inMutable = false;
    }
    return result;
  }

  private static boolean isHardwareBitmapCountReducedOnApi28ByB139097735() {
    if (Build.VERSION.SDK_INT != Build.VERSION_CODES.P) {
      return false;
    }
    for (String prefixOrModelName :
        Arrays.asList(
            "GM1900",
            "GM1901",
            "GM1903",
            "GM1911",
            "GM1915",
            "ONEPLUS A3000",
            "ONEPLUS A3010",
            "ONEPLUS A5010",
            "ONEPLUS A5000",
            "ONEPLUS A3003",
            "ONEPLUS A6000",
            "ONEPLUS A6003",
            "ONEPLUS A6010",
            "ONEPLUS A6013")) {
      if (Build.MODEL.startsWith(prefixOrModelName)) {
        return true;
      }
    }
    return false;
  }

  private int getMaxFdCount() {
    if (isHardwareBitmapCountReducedOnApi28ByB139097735()) {
      return REDUCED_MAX_FDS_FOR_HARDWARE_CONFIGS_P;
    }
    return sdkBasedMaxFdCount;
  }

  private synchronized boolean isFdSizeBelowHardwareLimit() {
    if (++decodesSinceLastFdCheck >= MINIMUM_DECODES_BETWEEN_FD_CHECKS) {
      decodesSinceLastFdCheck = 0;
      int currentFds = FD_SIZE_LIST.list().length;
      long maxFdCount = getMaxFdCount();
      isFdSizeBelowHardwareLimit = currentFds < maxFdCount;

      if (!isFdSizeBelowHardwareLimit && Log.isLoggable(Downsampler.TAG, Log.WARN)) {
        Log.w(
            Downsampler.TAG,
            "Excluding HARDWARE bitmap config because we're over the file descriptor limit"
                + ", file descriptors "
                + currentFds
                + ", limit "
                + maxFdCount);
      }
    }

    return isFdSizeBelowHardwareLimit;
  }
}
