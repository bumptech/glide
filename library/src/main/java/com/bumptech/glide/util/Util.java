package com.bumptech.glide.util;

import android.graphics.Bitmap;
import android.os.Build;

public class Util {
    private static final char[] hexArray = "0123456789abcdef".toCharArray();
    private static final char[] sha256Chars = new char[64]; //32 bytes from sha-256 -> 64 hex chars

    public static String sha256BytesToHex(byte[] bytes) {
        return bytesToHex(bytes, sha256Chars);
    }

    // Taken from:
    // http://stackoverflow.com/questions/9655181/convert-from-byte-array-to-hex-string-in-java/9655275#9655275
    private static String bytesToHex(byte[] bytes, char[] hexChars) {
        int v;
        for ( int j = 0; j < bytes.length; j++ ) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    /**
     * Returns the in memory size of the given {@link Bitmap}.
     */
    public static int getSize(Bitmap bitmap) {
        if (Build.VERSION.SDK_INT >= 19) {
            return bitmap.getAllocationByteCount();
        } else {
            return bitmap.getHeight() * bitmap.getRowBytes();
        }
    }

}
