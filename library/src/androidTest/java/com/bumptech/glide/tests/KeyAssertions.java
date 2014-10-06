package com.bumptech.glide.tests;

import com.bumptech.glide.load.Key;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class KeyAssertions {

    public static void assertSame(Key first, Key second) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        assertSameOrdered(first, second);
        assertSameOrdered(second, first);
    }

    private static void assertSameOrdered(Key first, Key second) throws UnsupportedEncodingException,
            NoSuchAlgorithmException {
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());

        assertThat(getDigest(first), equalTo(getDigest(second)));
    }

    public static void assertDifferent(Key first, Key second)
            throws NoSuchAlgorithmException, UnsupportedEncodingException {
        assertDifferent(first, second, true);
        assertDifferent(second, first, true);
    }

    public static void assertDifferent(Key first, Key second, boolean diskCacheDiffers)
            throws NoSuchAlgorithmException, UnsupportedEncodingException {
        assertNotEquals(first, second);
        assertNotEquals(first.hashCode(), second.hashCode());

        if (diskCacheDiffers) {
            MessageDigest firstDigest = MessageDigest.getInstance("SHA-1");
            first.updateDiskCacheKey(firstDigest);
            MessageDigest secondDigest = MessageDigest.getInstance("SHA-1");
            second.updateDiskCacheKey(secondDigest);

            assertThat(getDigest(first), not(equalTo(getDigest(second))));
        }
    }

    private static byte[] getDigest(Key key) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        key.updateDiskCacheKey(md);
        return md.digest();
    }
}
