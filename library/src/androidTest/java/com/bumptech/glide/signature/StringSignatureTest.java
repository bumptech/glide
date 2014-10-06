package com.bumptech.glide.signature;

import com.bumptech.glide.tests.KeyAssertions;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

public class StringSignatureTest {

    @Test
    public void testStringSignatureIsNotEqualIfStringDiffers() throws UnsupportedEncodingException,
            NoSuchAlgorithmException {
        StringSignature first = new StringSignature("first");
        StringSignature second = new StringSignature("second");

        KeyAssertions.assertDifferent(first, second);
    }

    @Test
    public void testStringSignatureIsEqualIfStringIsTheSame() throws UnsupportedEncodingException,
            NoSuchAlgorithmException {
        StringSignature first = new StringSignature("signature");
        StringSignature second = new StringSignature("signature");

        KeyAssertions.assertSame(first, second);
    }
}