package com.bumptech.glide.signature;

import com.bumptech.glide.load.Key;
import com.bumptech.glide.tests.KeyAssertions;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

import static org.junit.Assert.assertNotNull;

@RunWith(RobolectricTestRunner.class)
public class ApplicationVersionSignatureTest {

    @After
    public void tearDown() {
        ApplicationVersionSignature.reset();
    }

    @Test
    public void testCanGetKeyForSignature() {
        Key key = ApplicationVersionSignature.obtain(Robolectric.application);
        assertNotNull(key);
    }

    @Test
    public void testKeyForSignatureIsTheSameAcrossCallsInTheSamePackage() throws NoSuchAlgorithmException,
            UnsupportedEncodingException {
        Key first = ApplicationVersionSignature.obtain(Robolectric.application);
        Key second = ApplicationVersionSignature.obtain(Robolectric.application);
        KeyAssertions.assertSame(first, second);
    }
}