package com.bumptech.glide.load.engine.cache;

import com.bumptech.glide.load.Key;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.util.Arrays;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class DiskLruCacheWrapperTest {
    private DiskLruCacheWrapper cache;
    private byte[] data;
    private StringKey key;

    @Before
    public void setUp() {
        File dir = Robolectric.application.getCacheDir();
        cache = new DiskLruCacheWrapper(dir, 10 * 1024 * 1024);
        key = new StringKey("test");
        data = new byte[] { 1, 2, 3, 4, 5, 6 };
    }

    @Test
    //@org.junit.Ignore
    // on windows it will fail because new FileOutputStream keeps to lock
    public void testCanInsertAndGet() throws FileNotFoundException {
        cache.put(key, new DiskCache.Writer() {
            @Override
            public boolean write(File file) {
                try {
                    new FileOutputStream(file).write(data);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return true;
            }
        });

        byte[] received = isToBytes(new FileInputStream(cache.get(key)), data.length);

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
                    new FileOutputStream(file).write(data);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return false;
            }
        });

        assertNull(cache.get(key));
    }

    @Test
    //@org.junit.Ignore
    // on windows it will fail because new FileOutputStream keeps to lock
    public void testEditIsAbortedIfWriterThrows() throws FileNotFoundException {
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
                    new FileOutputStream(file).write(data);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return true;
            }
        });

        byte[] received = isToBytes(new FileInputStream(cache.get(key)), data.length);

        assertTrue(Arrays.equals(data, received));
    }

    private static byte[] isToBytes(InputStream is, int length) {
        byte[] result = new byte[length];
        try {
            assertEquals(length, is.read(result));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
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
