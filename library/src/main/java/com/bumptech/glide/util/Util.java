package com.bumptech.glide.util;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Looper;
import com.bumptech.glide.request.target.Target;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;

/**
 * A collection of assorted utility classes.
 */
public final class Util {
  private static final char[] HEX_CHAR_ARRAY = "0123456789abcdef".toCharArray();
  // 32 bytes from sha-256 -> 64 hex chars.
  private static final char[] SHA_256_CHARS = new char[64];

  private Util() {
    // Utility class.
  }

  /**
   * Returns the hex string of the given byte array representing a SHA256 hash.
   */
  public static String sha256BytesToHex(byte[] bytes) {
    synchronized (SHA_256_CHARS) {
      return bytesToHex(bytes, SHA_256_CHARS);
    }
  }

  // Taken from:
  // http://stackoverflow.com/questions/9655181/convert-from-byte-array-to-hex-string-in-java
  // /9655275#9655275
  @SuppressWarnings("PMD.UseVarargs")
  private static String bytesToHex(byte[] bytes, char[] hexChars) {
    int v;
    for (int j = 0; j < bytes.length; j++) {
      v = bytes[j] & 0xFF;
      hexChars[j * 2] = HEX_CHAR_ARRAY[v >>> 4];
      hexChars[j * 2 + 1] = HEX_CHAR_ARRAY[v & 0x0F];
    }
    return new String(hexChars);
  }

  /**
   * Returns the allocated byte size of the given bitmap.
   *
   * @see #getBitmapByteSize(android.graphics.Bitmap)
   * @deprecated Use {@link #getBitmapByteSize(android.graphics.Bitmap)} instead. Scheduled to be
   * removed in Glide 4.0.
   */
  @Deprecated
  public static int getSize(Bitmap bitmap) {
    return getBitmapByteSize(bitmap);
  }

  /**
   * Returns the in memory size of the given {@link Bitmap} in bytes.
   */
  @TargetApi(Build.VERSION_CODES.KITKAT)
  public static int getBitmapByteSize(Bitmap bitmap) {
    // The return value of getAllocationByteCount silently changes for recycled bitmaps from the
    // internal buffer size to row bytes * height. To avoid random inconsistencies in caches, we
    // instead assert here.
    if (bitmap.isRecycled()) {
      throw new IllegalStateException("Cannot obtain size for recycled Bitmap: " + bitmap
          + "[" + bitmap.getWidth() + "x" + bitmap.getHeight() + "] " + bitmap.getConfig());
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      // Workaround for KitKat initial release NPE in Bitmap, fixed in MR1. See issue #148.
      try {
        return bitmap.getAllocationByteCount();
      } catch (NullPointerException e) {
        // Do nothing.
      }
    }
    return bitmap.getHeight() * bitmap.getRowBytes();
  }

  /**
   * Returns the in memory size of {@link android.graphics.Bitmap} with the given width, height, and
   * {@link android.graphics.Bitmap.Config}.
   */
  public static int getBitmapByteSize(int width, int height, Bitmap.Config config) {
    return width * height * getBytesPerPixel(config);
  }

  private static int getBytesPerPixel(Bitmap.Config config) {
    // A bitmap by decoding a GIF has null "config" in certain environments.
    if (config == null) {
      config = Bitmap.Config.ARGB_8888;
    }

    int bytesPerPixel;
    switch (config) {
      case ALPHA_8:
        bytesPerPixel = 1;
        break;
      case RGB_565:
      case ARGB_4444:
        bytesPerPixel = 2;
        break;
      case ARGB_8888:
      default:
        bytesPerPixel = 4;
        break;
    }
    return bytesPerPixel;
  }

  /**
   * Returns true if width and height are both > 0 and/or equal to {@link Target#SIZE_ORIGINAL}.
   */
  public static boolean isValidDimensions(int width, int height) {
    return isValidDimension(width) && isValidDimension(height);
  }

  private static boolean isValidDimension(int dimen) {
    return dimen > 0 || dimen == Target.SIZE_ORIGINAL;
  }

  /**
   * Throws an {@link java.lang.IllegalArgumentException} if called on a thread other than the main
   * thread.
   */
  public static void assertMainThread() {
    if (!isOnMainThread()) {
      throw new IllegalArgumentException("You must call this method on the main thread");
    }
  }

  /**
   * Throws an {@link java.lang.IllegalArgumentException} if called on the main thread.
   */
  public static void assertBackgroundThread() {
    if (!isOnBackgroundThread()) {
      throw new IllegalArgumentException("You must call this method on a background thread");
    }
  }

  /**
   * Returns {@code true} if called on the main thread, {@code false} otherwise.
   */
  public static boolean isOnMainThread() {
    return Looper.myLooper() == Looper.getMainLooper();
  }

  /**
   * Returns {@code true} if called on a background thread, {@code false} otherwise.
   */
  public static boolean isOnBackgroundThread() {
    return !isOnMainThread();
  }

  /**
   * Creates a {@link java.util.Queue} of the given size using Glide's preferred implementation.
   */
  public static <T> Queue<T> createQueue(int size) {
    return new ArrayDeque<>(size);
  }

  /**
   * Returns a copy of the given list that is safe to iterate over and perform actions that may
   * modify the original list.
   *
   * <p> See #303 and #375. </p>
   */
  public static <T> List<T> getSnapshot(Collection<T> other) {
    // toArray creates a new ArrayList internally and this way we can guarantee entries will not
    // be null. See #322.
    List<T> result = new ArrayList<T>(other.size());
    for (T item : other) {
      result.add(item);
    }
    return result;
  }

  /**
   * Null-safe equivalent of {@code a.equals(b)}.
   *
   * @see java.util.Objects#equals
   */
  public static boolean bothNullOrEqual(Object a, Object b) {
    return a == null ? b == null : a.equals(b);
  }
}
