package com.bumptech.glide.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, emulateSdk = 18)
public class ByteArrayPoolTest {

    @Before
    public void setUp() throws Exception {
        ByteArrayPool.get().clear();
    }

    @Test
    public void testEmptyPoolReturnsBytes() {
        assertNotNull(ByteArrayPool.get().getBytes());
    }

    @Test
    public void testNonEmptyPoolReturnsAvailableBytes() {
        ByteArrayPool pool = ByteArrayPool.get();
        byte[] available = pool.getBytes();
        pool.releaseBytes(available);

        assertEquals(available, pool.getBytes());
    }

    @Test
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

    @Test
    public void testByteArrayPoolIgnoresIncorrectSizes() {
        ByteArrayPool pool = ByteArrayPool.get();
        byte[] toPut = new byte[0];

        assertFalse(pool.releaseBytes(toPut));
    }
}
