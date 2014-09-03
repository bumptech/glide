package com.bumptech.glide.load.engine.cache;

import org.junit.Before;
import org.junit.Test;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;

public class KeyGeneratorTest {
    private SafeKeyGenerator keyGenerator;

    @Before
    public void setUp() throws Exception {
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
        return keyGenerator.getSafeKey(new StringKey(getRandomId()));
    }

    private static String getRandomId() {
        return UUID.randomUUID().toString();
    }
}
