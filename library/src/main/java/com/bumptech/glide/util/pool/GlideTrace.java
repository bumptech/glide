package com.bumptech.glide.util.pool;

import androidx.tracing.Trace;
import java.util.concurrent.atomic.AtomicInteger;

/** Systracing utilities for Glide. */
public final class GlideTrace {
  // Enable this locally to see tracing statements.
  private static final boolean TRACING_ENABLED = false;

  private static final AtomicInteger COOKIE_CREATOR = TRACING_ENABLED ? new AtomicInteger() : null;

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
      Trace.beginSection(truncateTag(tag));
    }
  }

  public static void beginSectionFormat(String format, Object arg1) {
    if (TRACING_ENABLED) {
      Trace.beginSection(truncateTag(String.format(format, arg1)));
    }
  }

  public static void beginSectionFormat(String format, Object arg1, Object arg2) {
    if (TRACING_ENABLED) {
      Trace.beginSection(truncateTag(String.format(format, arg1, arg2)));
    }
  }

  public static void beginSectionFormat(String format, Object arg1, Object arg2, Object arg3) {
    if (TRACING_ENABLED) {
      Trace.beginSection(truncateTag(String.format(format, arg1, arg2, arg3)));
    }
  }

  public static int beginSectionAsync(String tag) {
    if (TRACING_ENABLED) {
      int cookie = COOKIE_CREATOR.incrementAndGet();
      Trace.beginAsyncSection(truncateTag(tag), cookie);
      return cookie;
    }
    return -1;
  }

  public static void endSectionAsync(String tag, int cookie) {
    if (TRACING_ENABLED) {
      Trace.endAsyncSection(tag, cookie);
    }
  }

  public static void endSection() {
    if (TRACING_ENABLED) {
      Trace.endSection();
    }
  }
}
