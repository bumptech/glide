package com.bumptech.glide.load.resource.bitmap;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.Log;
import androidx.annotation.GuardedBy;
import androidx.annotation.VisibleForTesting;
import java.io.File;

/**
 * State and constants for interacting with {@link android.graphics.Bitmap.Config#HARDWARE} on
 * Android O+.
 */
final class HardwareConfigState {
  /**
   * The minimum size in pixels a {@link Bitmap} must be in both dimensions to be created with the
   * {@link Bitmap.Config#HARDWARE} configuration.
   *
   * <p>This is a quick check that lets us skip wasting FDs (see {@link #FD_SIZE_LIST}) on small
   * {@link Bitmap}s with relatively low memory costs.
   *
   * @see #FD_SIZE_LIST
   */
  @VisibleForTesting static final int MIN_HARDWARE_DIMENSION = 128;

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

  /**
   * 700 with an error of 50 Bitmaps in between at two FDs each lets us use up to 800 FDs for
   * hardware Bitmaps.
   */
  private static final int MAXIMUM_FDS_FOR_HARDWARE_CONFIGS = 700;

  private static volatile HardwareConfigState instance;

  private final boolean isHardwareConfigAllowedByDeviceModel;

  @GuardedBy("this")
  private int decodesSinceLastFdCheck;

  @GuardedBy("this")
  private boolean isFdSizeBelowHardwareLimit = true;

  static HardwareConfigState getInstance() {
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
    isHardwareConfigAllowedByDeviceModel = isHardwareConfigAllowedByDeviceModel();
    // Singleton constructor.
  }

  @TargetApi(Build.VERSION_CODES.O)
  boolean setHardwareConfigIfAllowed(
      int targetWidth,
      int targetHeight,
      BitmapFactory.Options optionsWithScaling,
      boolean isHardwareConfigAllowed,
      boolean isExifOrientationRequired) {
    if (!isHardwareConfigAllowed
        || !isHardwareConfigAllowedByDeviceModel
        || Build.VERSION.SDK_INT < Build.VERSION_CODES.O
        || isExifOrientationRequired) {
      return false;
    }

    boolean result =
        targetWidth >= MIN_HARDWARE_DIMENSION
            && targetHeight >= MIN_HARDWARE_DIMENSION
            // Make sure to call isFdSizeBelowHardwareLimit last because it has side affects.
            && isFdSizeBelowHardwareLimit();

    if (result) {
      optionsWithScaling.inPreferredConfig = Bitmap.Config.HARDWARE;
      optionsWithScaling.inMutable = false;
    }
    return result;
  }

  private static boolean isHardwareConfigAllowedByDeviceModel() {
    switch (Build.MODEL.substring(0, 7)) {
      case "SM-N935":
        // Fall through
      case "SM-J720":
        // Fall through
      case "SM-G960":
        // Fall through
      case "SM-G965":
        // Fall through
      case "SM-G935":
        // Fall through
      case "SM-G930":
        // Fall through
      case "SM-A520":
        // Fall through
        return Build.VERSION.SDK_INT != Build.VERSION_CODES.O;
      default:
        return true;
    }
  }

  private synchronized boolean isFdSizeBelowHardwareLimit() {
    if (++decodesSinceLastFdCheck >= MINIMUM_DECODES_BETWEEN_FD_CHECKS) {
      decodesSinceLastFdCheck = 0;
      int currentFds = FD_SIZE_LIST.list().length;
      isFdSizeBelowHardwareLimit = currentFds < MAXIMUM_FDS_FOR_HARDWARE_CONFIGS;

      if (!isFdSizeBelowHardwareLimit && Log.isLoggable(Downsampler.TAG, Log.WARN)) {
        Log.w(
            Downsampler.TAG,
            "Excluding HARDWARE bitmap config because we're over the file descriptor limit"
                + ", file descriptors "
                + currentFds
                + ", limit "
                + MAXIMUM_FDS_FOR_HARDWARE_CONFIGS);
      }
    }

    return isFdSizeBelowHardwareLimit;
  }
}
