package com.bumptech.glide.load.resource.bitmap;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.Log;
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
  public static final boolean HARDWARE_BITMAPS_SUPPORTED =
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;

  /**
   * The minimum size in pixels a {@link Bitmap} must be in both dimensions to be created with the
   * {@link Bitmap.Config#HARDWARE} configuration.
   *
   * <p>This is a quick check that lets us skip wasting FDs (see {@link #FD_SIZE_LIST}) on small
   * {@link Bitmap}s with relatively low memory costs.
   *
   * @see #FD_SIZE_LIST
   */
  @VisibleForTesting static final int MIN_HARDWARE_DIMENSION_O = 128;

  private static final int MIN_HARDWARE_DIMENSION_P = 0;

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
   *
   * <p>Prior to P, the limit per process was 1024 FDs. In P, the limit was updated to 32k FDs per
   * process.
   *
   * <p>Access to this variable will be removed in a future version without deprecation.
   */
  private static final int MAXIMUM_FDS_FOR_HARDWARE_CONFIGS_O = 700;
  // 20k.
  private static final int MAXIMUM_FDS_FOR_HARDWARE_CONFIGS_P = 20000;

  /** This constant will be removed in a future version without deprecation, avoid using it. */
  public static final int NO_MAX_FD_COUNT = -1;

  private static volatile HardwareConfigState instance;
  private static volatile int manualOverrideMaxFdCount = NO_MAX_FD_COUNT;

  private final boolean isHardwareConfigAllowedByDeviceModel;
  private final int sdkBasedMaxFdCount;
  private final int minHardwareDimension;

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
    isHardwareConfigAllowedByDeviceModel = isHardwareConfigAllowedByDeviceModel();
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      sdkBasedMaxFdCount = MAXIMUM_FDS_FOR_HARDWARE_CONFIGS_P;
      minHardwareDimension = MIN_HARDWARE_DIMENSION_P;
    } else {
      sdkBasedMaxFdCount = MAXIMUM_FDS_FOR_HARDWARE_CONFIGS_O;
      minHardwareDimension = MIN_HARDWARE_DIMENSION_O;
    }
  }

  public boolean areHardwareBitmapsBlocked() {
    Util.assertMainThread();
    return !isHardwareConfigAllowedByAppState.get();
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
    if (!isHardwareConfigAllowedByDeviceModel) {
      if (Log.isLoggable(TAG, Log.VERBOSE)) {
        Log.v(TAG, "Hardware config disallowed by device model");
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
    if (targetWidth < minHardwareDimension) {
      if (Log.isLoggable(TAG, Log.VERBOSE)) {
        Log.v(TAG, "Hardware config disallowed because width is too small");
      }
      return false;
    }
    if (targetHeight < minHardwareDimension) {
      if (Log.isLoggable(TAG, Log.VERBOSE)) {
        Log.v(TAG, "Hardware config disallowed because height is too small");
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

  private static boolean isHardwareConfigAllowedByDeviceModel() {
    return !isHardwareConfigDisallowedByB112551574() && !isHardwareConfigDisallowedByB147430447();
  }

  private static boolean isHardwareConfigDisallowedByB147430447() {
    if (Build.VERSION.SDK_INT != Build.VERSION_CODES.O_MR1) {
      return false;
    }
    // This method will only be called once, so simple iteration is reasonable.
    return Arrays.asList(
            "LG-M250",
            "LG-M320",
            "LG-Q710AL",
            "LG-Q710PL",
            "LGM-K121K",
            "LGM-K121L",
            "LGM-K121S",
            "LGM-X320K",
            "LGM-X320L",
            "LGM-X320S",
            "LGM-X401L",
            "LGM-X401S",
            "LM-Q610.FG",
            "LM-Q610.FGN",
            "LM-Q617.FG",
            "LM-Q617.FGN",
            "LM-Q710.FG",
            "LM-Q710.FGN",
            "LM-X220PM",
            "LM-X220QMA",
            "LM-X410PM")
        .contains(Build.MODEL);
  }

  private static boolean isHardwareConfigDisallowedByB112551574() {
    if (Build.VERSION.SDK_INT != Build.VERSION_CODES.O) {
      return false;
    }
    // This method will only be called once, so simple iteration is reasonable.
    for (String prefixOrModelName :
        // This is sadly a list of prefixes, not models. We no longer have the data that shows us
        // all the explicit models, so we have to live with the prefixes.
        Arrays.asList(
            // Samsung
            "SC-04J",
            "SM-N935",
            "SM-J720",
            "SM-G570F",
            "SM-G570M",
            "SM-G960",
            "SM-G965",
            "SM-G935",
            "SM-G930",
            "SM-A520",
            "SM-A720F",
            // Moto
            "moto e5",
            "moto e5 play",
            "moto e5 plus",
            "moto e5 cruise",
            "moto g(6) forge",
            "moto g(6) play")) {
      if (Build.MODEL.startsWith(prefixOrModelName)) {
        return true;
      }
    }
    return false;
  }

  private int getMaxFdCount() {
    return manualOverrideMaxFdCount != NO_MAX_FD_COUNT
        ? manualOverrideMaxFdCount
        : sdkBasedMaxFdCount;
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
