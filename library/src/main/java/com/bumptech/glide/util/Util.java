package com.bumptech.glide.util;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.load.model.Model;
import com.bumptech.glide.request.BaseRequestOptions;
import com.bumptech.glide.request.target.Target;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;

/** A collection of assorted utility classes. */
public final class Util {
  private static final int HASH_MULTIPLIER = 31;
  private static final int HASH_ACCUMULATOR = 17;
  private static final char[] HEX_CHAR_ARRAY = "0123456789abcdef".toCharArray();
  // 32 bytes from sha-256 -> 64 hex chars.
  private static final char[] SHA_256_CHARS = new char[64];
  @Nullable private static volatile Handler mainThreadHandler;

  private Util() {
    // Utility class.
  }

  /** Returns the hex string of the given byte array representing a SHA256 hash. */
  @NonNull
  public static String sha256BytesToHex(@NonNull byte[] bytes) {
    synchronized (SHA_256_CHARS) {
      return bytesToHex(bytes, SHA_256_CHARS);
    }
  }

  // Taken from:
  // http://stackoverflow.com/questions/9655181/convert-from-byte-array-to-hex-string-in-java
  // /9655275#9655275
  @SuppressWarnings("PMD.UseVarargs")
  @NonNull
  private static String bytesToHex(@NonNull byte[] bytes, @NonNull char[] hexChars) {
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
   *     removed in Glide 4.0.
   */
  @Deprecated
  public static int getSize(@NonNull Bitmap bitmap) {
    return getBitmapByteSize(bitmap);
  }

  /** Returns the in memory size of the given {@link Bitmap} in bytes. */
  @TargetApi(Build.VERSION_CODES.KITKAT)
  public static int getBitmapByteSize(@NonNull Bitmap bitmap) {
    // The return value of getAllocationByteCount silently changes for recycled bitmaps from the
    // internal buffer size to row bytes * height. To avoid random inconsistencies in caches, we
    // instead assert here.
    if (bitmap.isRecycled()) {
      throw new IllegalStateException(
          "Cannot obtain size for recycled Bitmap: "
              + bitmap
              + "["
              + bitmap.getWidth()
              + "x"
              + bitmap.getHeight()
              + "] "
              + bitmap.getConfig());
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      // Workaround for KitKat initial release NPE in Bitmap, fixed in MR1. See issue #148.
      try {
        return bitmap.getAllocationByteCount();
      } catch (
          @SuppressWarnings("PMD.AvoidCatchingNPE")
          NullPointerException e) {
        // Do nothing.
      }
    }
    return bitmap.getHeight() * bitmap.getRowBytes();
  }

  /**
   * Returns the in memory size of {@link android.graphics.Bitmap} with the given width, height, and
   * {@link android.graphics.Bitmap.Config}.
   */
  public static int getBitmapByteSize(int width, int height, @Nullable Bitmap.Config config) {
    return width * height * getBytesPerPixel(config);
  }

  /**
   * Returns the number of bytes required to store each pixel of a {@link Bitmap} with the given
   * {@code config}.
   *
   * <p>Defaults to {@link Bitmap.Config#ARGB_8888} if {@code config} is {@code null}.
   */
  public static int getBytesPerPixel(@Nullable Bitmap.Config config) {
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
      case RGBA_F16:
        bytesPerPixel = 8;
        break;
      case ARGB_8888:
      default:
        bytesPerPixel = 4;
        break;
    }
    return bytesPerPixel;
  }

  /**
   * Returns {@code true} if {@code width} and {@code height} are both {@code > 0} and/or equal to
   * {@link Target#SIZE_ORIGINAL}.
   */
  public static boolean isValidDimensions(int width, int height) {
    return isValidDimension(width) && isValidDimension(height);
  }

  public static boolean isValidDimension(int dimen) {
    return dimen > 0 || dimen == Target.SIZE_ORIGINAL;
  }

  /** Posts the given {@code runnable} to the UI thread using a shared {@link Handler}. */
  public static void postOnUiThread(Runnable runnable) {
    getUiThreadHandler().post(runnable);
  }

  /** Removes the given {@code runnable} from the UI threads queue if it is still queued. */
  public static void removeCallbacksOnUiThread(Runnable runnable) {
    getUiThreadHandler().removeCallbacks(runnable);
  }

  private static Handler getUiThreadHandler() {
    if (mainThreadHandler == null) {
      synchronized (Util.class) {
        if (mainThreadHandler == null) {
          mainThreadHandler = new Handler(Looper.getMainLooper());
        }
      }
    }
    return mainThreadHandler;
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

  /** Throws an {@link java.lang.IllegalArgumentException} if called on the main thread. */
  public static void assertBackgroundThread() {
    if (!isOnBackgroundThread()) {
      throw new IllegalArgumentException("You must call this method on a background thread");
    }
  }

  /** Returns {@code true} if called on the main thread, {@code false} otherwise. */
  public static boolean isOnMainThread() {
    return Looper.myLooper() == Looper.getMainLooper();
  }

  /** Returns {@code true} if called on a background thread, {@code false} otherwise. */
  public static boolean isOnBackgroundThread() {
    return !isOnMainThread();
  }

  /** Creates a {@link java.util.Queue} of the given size using Glide's preferred implementation. */
  @NonNull
  public static <T> Queue<T> createQueue(int size) {
    return new ArrayDeque<>(size);
  }

  /**
   * Returns a copy of the given list that is safe to iterate over and perform actions that may
   * modify the original list.
   *
   * <p>See #303, #375, #322, #2262.
   */
  @NonNull
  @SuppressWarnings("UseBulkOperation")
  public static <T> List<T> getSnapshot(@NonNull Collection<T> other) {
    // toArray creates a new ArrayList internally and does not guarantee that the values it contains
    // are non-null. Collections.addAll in ArrayList uses toArray internally and therefore also
    // doesn't guarantee that entries are non-null. WeakHashMap's iterator does avoid returning null
    // and is therefore safe to use. See #322, #2262.
    List<T> result = new ArrayList<>(other.size());
    for (T item : other) {
      if (item != null) {
        result.add(item);
      }
    }
    return result;
  }

  /**
   * Null-safe equivalent of {@code a.equals(b)}.
   *
   * @see java.util.Objects#equals
   */
  public static boolean bothNullOrEqual(@Nullable Object a, @Nullable Object b) {
    return a == null ? b == null : a.equals(b);
  }

  public static boolean bothModelsNullEquivalentOrEquals(@Nullable Object a, @Nullable Object b) {
    if (a == null) {
      return b == null;
    }
    if (a instanceof Model) {
      return ((Model) a).isEquivalentTo(b);
    }
    return a.equals(b);
  }

  public static boolean bothBaseRequestOptionsNullEquivalentOrEquals(
      @Nullable BaseRequestOptions<?> a,
      @Nullable BaseRequestOptions<?> b
  ) {
    if (a == null) {
      return b == null;
    }
    return a.isEquivalentTo(b);
  }

  public static int hashCode(int value) {
    return hashCode(value, HASH_ACCUMULATOR);
  }

  public static int hashCode(int value, int accumulator) {
    return accumulator * HASH_MULTIPLIER + value;
  }

  public static int hashCode(float value) {
    return hashCode(value, HASH_ACCUMULATOR);
  }

  public static int hashCode(float value, int accumulator) {
    return hashCode(Float.floatToIntBits(value), accumulator);
  }

  public static int hashCode(@Nullable Object object, int accumulator) {
    return hashCode(object == null ? 0 : object.hashCode(), accumulator);
  }

  public static int hashCode(boolean value, int accumulator) {
    return hashCode(value ? 1 : 0, accumulator);
  }

  public static int hashCode(boolean value) {
    return hashCode(value, HASH_ACCUMULATOR);
  }
}
