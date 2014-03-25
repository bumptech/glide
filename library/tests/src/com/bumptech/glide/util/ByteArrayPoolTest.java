package com.bumptech.glide.util;

import android.test.AndroidTestCase;

public class ByteArrayPoolTest extends AndroidTestCase {
    @Override
    protected void setUp() throws Exception {
        ByteArrayPool.get().clear();
    }

    public void testEmptyPoolReturnsBytes() {
        assertNotNull(ByteArrayPool.get().getBytes());
    }

    public void testNonEmptyPoolReturnsAvailableBytes() {
        ByteArrayPool pool = ByteArrayPool.get();
        byte[] available = pool.getBytes();
        pool.releaseBytes(available);

        assertEquals(available, pool.getBytes());
    }

    public void testPoolIsSizeBounded() {
        ByteArrayPool pool = ByteArrayPool.get();

        byte[] seed = pool.getBytes();
        boolean rejected = false;
        // Some way too high number
        for (int i = 0; i < 2000 && !rejected; i++) {
            byte[] toPut = new byte[seed.length];
            rejected = !pool.releaseBytes(toPut);
        }

        assertTrue(rejected);
    }

    public void testByteArrayPoolIgnoresIncorrectSizes() {
        ByteArrayPool pool = ByteArrayPool.get();
        byte[] toPut = new byte[0];

        assertFalse(pool.releaseBytes(toPut));
    }
}
