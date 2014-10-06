package com.bumptech.glide.load.engine.cache;

import com.bumptech.glide.load.Key;
import org.junit.Before;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;

public class KeyGeneratorTest {
    private SafeKeyGenerator keyGenerator;
    private int nextId;

    @Before
    public void setUp() throws Exception {
        nextId = 0;
        keyGenerator = new SafeKeyGenerator();
    }

    @Test
    public void testKeysAreValidForDiskCache() {
        final Pattern diskCacheRegex = Pattern.compile("[a-z0-9_-]{64}");
        for (int i = 0; i < 1000; i++) {
            String key = getRandomKeyFromGenerator();
            Matcher matcher = diskCacheRegex.matcher(key);
            assertTrue(key, matcher.matches());
        }
    }

    private String getRandomKeyFromGenerator() {
        return keyGenerator.getSafeKey(new MockKey(getNextId()));
    }

    private String getNextId() {
        return String.valueOf(nextId++);
    }

    private static class MockKey implements Key {
        private String id;

        public MockKey(String id) {
            this.id = id;
        }

        @Override
        public void updateDiskCacheKey(MessageDigest messageDigest) throws UnsupportedEncodingException {
            messageDigest.update(id.getBytes(STRING_CHARSET_NAME));
        }
    }
}
