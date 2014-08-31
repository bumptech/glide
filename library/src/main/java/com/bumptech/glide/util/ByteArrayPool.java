package com.bumptech.glide.util;

import android.util.Log;

import java.util.Queue;

/**
 * A pool for reusing byte arrays that produces and contains byte arrays of a fixed size.
 */
public final class ByteArrayPool {
    private static final String TAG = "ByteArrayPool";
    // 64 KB.
    private static final int TEMP_BYTES_SIZE = 64 * 1024;
    // 512 KB.
    private static final int MAX_SIZE = 2 * 1048 * 1024;
    private static final int MAX_BYTE_ARRAY_COUNT = MAX_SIZE / TEMP_BYTES_SIZE;

    private final Queue<byte[]> tempQueue = Util.createQueue(0);
    private static final ByteArrayPool BYTE_ARRAY_POOL = new ByteArrayPool();

    /**
     * Returns a constant singleton byte array pool.
     */
    public static ByteArrayPool get() {
        return BYTE_ARRAY_POOL;
    }

    private ByteArrayPool() {  }

    /**
     * Removes all byte arrays from the pool.
     */
    public void clear() {
        synchronized (tempQueue) {
            tempQueue.clear();
        }
    }

    /**
     * Returns a byte array by retrieving one from the pool if the pool is non empty or otherwise by creating a new
     * byte array.
     */
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

    /**
     * Adds the given byte array to the pool if it is the correct size and the pool is not full and returns true if
     * the byte array was added and false otherwise.
     *
     * @param bytes The bytes to try to add to the pool.
     */
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
