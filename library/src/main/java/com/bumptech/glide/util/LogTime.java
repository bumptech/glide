package com.bumptech.glide.util;

import android.os.SystemClock;

/** A class for logging elapsed real time in millis. */
public final class LogTime {
  private static final double MILLIS_MULTIPLIER = 1d / Math.pow(10, 6);

  private LogTime() {
    // Utility class.
  }

  /** Returns the current time in nanos to be used with {@link #getElapsedMillis(long)}. */
  public static long getLogTime() {
    return SystemClock.elapsedRealtimeNanos();
  }

  /**
   * Returns the time elapsed since the given logTime in millis.
   *
   * @param logTime The start time of the event.
   */
  public static double getElapsedMillis(long logTime) {
    return (getLogTime() - logTime) * MILLIS_MULTIPLIER;
  }
}
