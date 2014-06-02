package com.bumptech.glide.util;

import android.util.Log;

import java.util.LinkedList;
import java.util.Queue;

public class ByteArrayPool {
    private static final String TAG = "ByteArrayPool";
    // 64 KB.
    private static final int TEMP_BYTES_SIZE = 64 * 1024;
    // 512 KB.
    private static final int MAX_SIZE = 512 * 1024;
    private static final int MAX_BYTE_ARRAY_COUNT = MAX_SIZE / TEMP_BYTES_SIZE;

    private final Queue<byte[]> tempQueue = new LinkedList<byte[]>();
    private static final ByteArrayPool BYTE_ARRAY_POOL = new ByteArrayPool();

    public static ByteArrayPool get() {
        return BYTE_ARRAY_POOL;
    }

    private ByteArrayPool() {  }

    public void clear() {
        synchronized (tempQueue) {
            tempQueue.clear();
        }
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

    public boolean releaseBytes(byte[] bytes) {
        if (bytes.length != TEMP_BYTES_SIZE) {
            return false;
        }

        boolean accepted = false;
        synchronized (tempQueue) {
            if (tempQueue.size() < MAX_BYTE_ARRAY_COUNT) {
                accepted = true;
                tempQueue.offer(bytes);
            }
        }
        return accepted;
    }
}
