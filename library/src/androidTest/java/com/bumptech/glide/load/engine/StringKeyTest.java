package com.bumptech.glide.load.engine;

import com.bumptech.glide.load.engine.cache.StringKey;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class StringKeyTest {

    @Test
    public void testIsEqualToAnotherKeyWithSameId() {
        String id = "fakeId";
        StringKey first = new StringKey(id);
        StringKey second = new StringKey(id);
        assertEquals(first, second);
    }

    @Test
    public void testReturnsSameHashCodeAsAnotherKeyWithSameId() {
        String id = "testId";
        StringKey first = new StringKey(id);
        StringKey second = new StringKey(id);

        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    public void testUpdatesDigestWithGivenId() throws NoSuchAlgorithmException, UnsupportedEncodingException {
        String id = "testId2";

        StringKey firstKey = new StringKey(id);
        MessageDigest firstDigest = MessageDigest.getInstance("SHA-1");
        firstKey.updateDiskCacheKey(firstDigest);
        byte[] firstBytes = firstDigest.digest();

        StringKey secondKey = new StringKey(id);
        MessageDigest secondDigest = MessageDigest.getInstance("SHA-1");
        secondKey.updateDiskCacheKey(secondDigest);
        byte[] secondBytes = secondDigest.digest();

        assertArrayEquals(firstBytes, secondBytes);
    }
}