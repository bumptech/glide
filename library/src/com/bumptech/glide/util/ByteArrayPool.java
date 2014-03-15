package com.bumptech.glide.util;

import android.util.Log;

import java.util.LinkedList;
import java.util.Queue;

public class ByteArrayPool {
    private static final String TAG = "ByteArrayPool";
    private static final int TEMP_BYTES_SIZE = 16 * 1024; //16kb
    // 512 KB
    private static final int MAX_SIZE = (512 * 1024) / TEMP_BYTES_SIZE;

    private final Queue<byte[]> tempQueue = new LinkedList<byte[]>();
    private static final ByteArrayPool BYTE_ARRAY_POOL = new ByteArrayPool();

    public static ByteArrayPool get() {
        return BYTE_ARRAY_POOL;
    }

    private ByteArrayPool() {

    }

    public byte[] getBytes() {
        byte[] result;
        synchronized (tempQueue) {
            result = tempQueue.poll();
        }
        if (result == null) {
            result = new byte[TEMP_BYTES_SIZE];
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Created temp bytes");
            }
        }
        return result;
    }

    public void releaseBytes(byte[] bytes) {
        synchronized (tempQueue) {
            tempQueue.offer(bytes);
        }
    }
}
