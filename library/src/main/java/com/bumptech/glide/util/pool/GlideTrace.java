package com.bumptech.glide.util.pool;

import androidx.core.os.TraceCompat;

/** Systracing utilities for Glide. */
public final class GlideTrace {

  // Enable this locally to see tracing statements.
  private static final boolean TRACING_ENABLED = false;

  /** Maximum length of a systrace tag. */
  private static final int MAX_LENGTH = 127;

  private GlideTrace() {
    // Utility class.
  }

  private static String truncateTag(String tag) {
    if (tag.length() > MAX_LENGTH) {
      return tag.substring(0, MAX_LENGTH - 1);
    }
    return tag;
  }

  public static void beginSection(String tag) {
    if (TRACING_ENABLED) {
      TraceCompat.beginSection(truncateTag(tag));
    }
  }

  public static void beginSectionFormat(String format, Object arg1) {
    if (TRACING_ENABLED) {
      TraceCompat.beginSection(truncateTag(String.format(format, arg1)));
    }
  }

  public static void beginSectionFormat(String format, Object arg1, Object arg2) {
    if (TRACING_ENABLED) {
      TraceCompat.beginSection(truncateTag(String.format(format, arg1, arg2)));
    }
  }

  public static void beginSectionFormat(String format, Object arg1, Object arg2, Object arg3) {
    if (TRACING_ENABLED) {
      TraceCompat.beginSection(truncateTag(String.format(format, arg1, arg2, arg3)));
    }
  }

  public static void endSection() {
    if (TRACING_ENABLED) {
      TraceCompat.endSection();
    }
  }
}
