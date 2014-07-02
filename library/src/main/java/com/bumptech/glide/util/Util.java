package com.bumptech.glide.util;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Looper;

public class Util {
    private static final char[] HEX_CHAR_ARRAY = "0123456789abcdef".toCharArray();
    // 32 bytes from sha-256 -> 64 hex chars.
    private static final char[] SHA_256_CHARS = new char[64];

    public static String sha256BytesToHex(byte[] bytes) {
        return bytesToHex(bytes, SHA_256_CHARS);
    }

    // Taken from:
    // http://stackoverflow.com/questions/9655181/convert-from-byte-array-to-hex-string-in-java/9655275#9655275
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
     * Returns the in memory size of the given {@link Bitmap}.
     */
    @TargetApi(19)
    public static int getSize(Bitmap bitmap) {
        if (Build.VERSION.SDK_INT >= 19) {
            return bitmap.getAllocationByteCount();
        } else {
            return bitmap.getHeight() * bitmap.getRowBytes();
        }
    }

    public static void assertMainThread() {
        if (!isOnMainThread()) {
            throw new IllegalArgumentException("You must call this method on the main thread");
        }
    }

    public static void assertBackgroundThread() {
        if (!isOnBackgroundThread()) {
            throw new IllegalArgumentException("YOu must call this method on a background thread");
        }
    }

    public static boolean isOnMainThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }

    public static boolean isOnBackgroundThread() {
        return !isOnMainThread();
    }

}
