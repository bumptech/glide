package com.bumptech.glide.load.engine.cache;

import com.bumptech.glide.load.Key;
import com.bumptech.glide.tests.Util;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.util.Arrays;

import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(RobolectricTestRunner.class)
public class DiskLruCacheWrapperTest {
    private DiskLruCacheWrapper cache;
    private byte[] data;
    private StringKey key;

    @Before
    public void setUp() {
        File dir = Robolectric.application.getCacheDir();
        cache = new DiskLruCacheWrapper(dir, 10 * 1024 * 1024);
        key = new StringKey("test" + Math.random());
        data = new byte[] { 1, 2, 3, 4, 5, 6 };
    }

    @Test
    public void testCanInsertAndGet() throws IOException {
        cache.put(key, new DiskCache.Writer() {
            @Override
            public boolean write(File file) {
                try {
                    Util.writeFile(file, data);
                } catch (IOException e) {
                    fail(e.toString());
                }
                return true;
            }
        });

        byte[] received = Util.readFile(cache.get(key), data.length);

        assertTrue(Arrays.equals(data, received));
    }

    @Test
    public void testDoesNotCommitIfWriterReturnsFalse() {
        cache.put(key, new DiskCache.Writer() {
            @Override
            public boolean write(File file) {
                return false;
            }
        });

        assertNull(cache.get(key));
    }

    @Test
    public void testDoesNotCommitIfWriterWritesButReturnsFalse() {
        cache.put(key, new DiskCache.Writer() {
            @Override
            public boolean write(File file) {
                try {
                    Util.writeFile(file, data);
                } catch (IOException e) {
                    fail(e.toString());
                }
                return false;
            }
        });

        assertNull(cache.get(key));
    }

    @Test
    public void testEditIsAbortedIfWriterThrows() throws IOException {
        try {
            cache.put(key, new DiskCache.Writer() {
                @Override
                public boolean write(File file) {
                    throw new RuntimeException("test");
                }
            });
        } catch (RuntimeException e) {
            // Expected.
        }

        cache.put(key, new DiskCache.Writer() {
            @Override
            public boolean write(File file) {
                try {
                    Util.writeFile(file, data);
                } catch (IOException e) {
                    fail(e.toString());
                }
                return true;
            }
        });

        byte[] received = Util.readFile(cache.get(key), data.length);

        assertTrue(Arrays.equals(data, received));
    }

    private static class StringKey implements Key {
        private final String key;

        public StringKey(String key) {
            this.key = key;
        }

        @Override
        public void updateDiskCacheKey(MessageDigest messageDigest) throws UnsupportedEncodingException {
            messageDigest.update(key.getBytes());
        }
    }
}
