package com.bumptech.glide.load.engine.executor;

import android.os.Build;
import android.os.StrictMode;
import android.os.StrictMode.ThreadPolicy;
import android.util.Log;
import java.io.File;
import java.io.FilenameFilter;
import java.util.regex.Pattern;

/** Compatibility methods for {@link java.lang.Runtime}. */
final class RuntimeCompat {
  private static final String TAG = "GlideRuntimeCompat";
  private static final String CPU_NAME_REGEX = "cpu[0-9]+";
  private static final String CPU_LOCATION = "/sys/devices/system/cpu/";

  private RuntimeCompat() {
    // Utility class.
  }

  /** Determines the number of cores available on the device. */
  static int availableProcessors() {
    int cpus = Runtime.getRuntime().availableProcessors();
    if (Build.VERSION.SDK_INT < 17) {
      cpus = Math.max(getCoreCountPre17(), cpus);
    }
    return cpus;
  }

  /**
   * Determines the number of cores available on the device (pre-v17).
   *
   * <p>Before Jellybean, {@link Runtime#availableProcessors()} returned the number of awake cores,
   * which may not be the number of available cores depending on the device's current state. See
   * https://stackoverflow.com/a/30150409.
   *
   * @return the maximum number of processors available to the VM; never smaller than one
   */
  @SuppressWarnings("PMD")
  private static int getCoreCountPre17() {
    // We override the current ThreadPolicy to allow disk reads.
    // This shouldn't actually do disk-IO and accesses a device file.
    // See: https://github.com/bumptech/glide/issues/1170
    File[] cpus = null;
    ThreadPolicy originalPolicy = StrictMode.allowThreadDiskReads();
    try {
      File cpuInfo = new File(CPU_LOCATION);
      final Pattern cpuNamePattern = Pattern.compile(CPU_NAME_REGEX);
      cpus =
          cpuInfo.listFiles(
              new FilenameFilter() {
                @Override
                public boolean accept(File file, String s) {
                  return cpuNamePattern.matcher(s).matches();
                }
              });
    } catch (Throwable t) {
      if (Log.isLoggable(TAG, Log.ERROR)) {
        Log.e(TAG, "Failed to calculate accurate cpu count", t);
      }
    } finally {
      StrictMode.setThreadPolicy(originalPolicy);
    }
    return Math.max(1, cpus != null ? cpus.length : 0);
  }
}
