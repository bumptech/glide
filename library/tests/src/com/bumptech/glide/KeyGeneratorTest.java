package com.bumptech.glide;

import android.graphics.Bitmap;
import android.test.AndroidTestCase;
import com.bumptech.glide.resize.SafeKeyGenerator;
import com.bumptech.glide.resize.bitmap_recycle.BitmapPool;
import com.bumptech.glide.resize.load.Downsampler;
import com.bumptech.glide.resize.load.Transformation;

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
        return keyGenerator.getSafeKey(getRandomId(), new RandomTransformation(), new RandomDownsampler(),
                getRandomDimen(), getRandomDimen());
    }

    private static int getRandomDimen() {
        return (int) Math.round(Math.random() * 1000);
    }

    private static String getRandomId() {
        return UUID.randomUUID().toString();
    }

    private static class RandomDownsampler extends Downsampler {

        @Override
        protected int getSampleSize(int inWidth, int inHeight, int outWidth, int outHeight) {
            return 0;
        }

        @Override
        public String getId() {
            return getRandomId();
        }
    }

    private static class RandomTransformation extends Transformation {

        @Override
        public Bitmap transform(Bitmap bitmap, BitmapPool pool, int outWidth, int outHeight) {
            return null;
        }

        @Override
        public String getId() {
            return getRandomId();
        }
    }
}
