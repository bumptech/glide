package com.bumptech.glide;

import android.util.Log;

/**
 * Uniform logs across Glide.
 */
public final class Logs {
  private static final String TAG = "Glide";

  private Logs() {
    // Utility class.
  }

  public static boolean isEnabled(int logLevel) {
    return Log.isLoggable(TAG, logLevel);
  }

  public static void log(int logLevel, String message) {
    log(logLevel, message, null);
  }

  public static void log(int logLevel, String message, Exception e) {
    switch (logLevel) {
      case Log.ERROR:
        Log.e(TAG, message, e);
        break;
      case Log.WARN:
        Log.w(TAG, message, e);
        break;
      case Log.INFO:
        Log.i(TAG, message, e);
        break;
      case Log.DEBUG:
        Log.d(TAG, message, e);
        break;
      case Log.VERBOSE:
        Log.v(TAG, message, e);
        break;
      default:
        throw new IllegalArgumentException("Unknown level: " + logLevel);
    }
  }
}
