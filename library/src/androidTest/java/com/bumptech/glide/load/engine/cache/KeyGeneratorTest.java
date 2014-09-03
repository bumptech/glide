package com.bumptech.glide.load.engine.cache;

import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.matchers.Matches;

import java.util.UUID;

import static org.junit.Assert.assertThat;

public class KeyGeneratorTest {
    private SafeKeyGenerator keyGenerator;

    @Before
    public void setUp() throws Exception {
        keyGenerator = new SafeKeyGenerator();
    }

    @Test
    public void testKeysAreValidForDiskCache() {
        Matches matchesSafeKeyPattern = new Matches("[a-z0-9_-]{64}");
        for (int i = 0; i < 1000; i++) {
            String key = getRandomKeyFromGenerator();
            assertThat(key, matchesSafeKeyPattern);
        }
    }

    private String getRandomKeyFromGenerator() {
        return keyGenerator.getSafeKey(new StringKey(getRandomId()));
    }

    private static String getRandomId() {
        return UUID.randomUUID().toString();
    }
}
