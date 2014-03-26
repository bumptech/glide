package com.bumptech.glide;

import android.graphics.Bitmap;
import android.test.AndroidTestCase;
import com.bumptech.glide.resize.BitmapLoad;
import com.bumptech.glide.resize.SafeKeyGenerator;
import com.bumptech.glide.resize.bitmap_recycle.BitmapPool;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KeyGeneratorTest extends AndroidTestCase {
    private SafeKeyGenerator keyGenerator;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        keyGenerator = new SafeKeyGenerator();
    }

    public void testKeysAreValidForDiskCache() {
        String key;
        final Pattern diskCacheRegex = Pattern.compile("[a-z0-9_-]{64}");
        for (int i = 0; i < 1000; i++) {
            key = getRandomKeyFromGenerator();
            final Matcher matcher = diskCacheRegex.matcher(key);
            assertTrue(matcher.matches());
        }
    }

    private String getRandomKeyFromGenerator() {
        return keyGenerator.getSafeKey(new RandomBitmapLoad());
    }

    private static int getRandomDimen() {
        return (int) Math.round(Math.random() * 1000);
    }

    private static String getRandomId() {
        return UUID.randomUUID().toString();
    }

    private static class RandomBitmapLoad implements BitmapLoad {
        @Override
        public String getId() {
            return getRandomId();
        }

        @Override
        public void cancel() {
        }

        @Override
        public Bitmap load(BitmapPool bitmapPool) throws Exception {
            return null;
        }
    }
}
