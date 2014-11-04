package com.bumptech.glide.load.engine;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import com.bumptech.glide.load.Key;
import com.bumptech.glide.tests.KeyAssertions;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@RunWith(JUnit4.class)
public class OriginalKeyTest {

    @Test
    public void testDiffersIfIdDiffers() throws UnsupportedEncodingException, NoSuchAlgorithmException {
        Key signature = mock(Key.class);
        OriginalKey first = new OriginalKey("first", signature);
        OriginalKey second = new OriginalKey("second", signature);

        KeyAssertions.assertDifferent(first, second);
    }

    @Test
    public void testDiffersIfSignatureDiffers() throws UnsupportedEncodingException, NoSuchAlgorithmException {
        Key firstSignature = mock(Key.class);
        doAnswer(new WriteDigest("firstSignature")).when(firstSignature).updateDiskCacheKey(any(MessageDigest.class));
        Key secondSignature = mock(Key.class);
        doAnswer(new WriteDigest("secondSignature")).when(secondSignature).updateDiskCacheKey(any(MessageDigest.class));

        OriginalKey first = new OriginalKey("key", firstSignature);
        OriginalKey second = new OriginalKey("key", secondSignature);

        KeyAssertions.assertDifferent(first, second);
    }

    @Test
    public void testSameIfIdAndSignatureAreTheSame() throws UnsupportedEncodingException, NoSuchAlgorithmException {
        Key signature = mock(Key.class);
        OriginalKey first = new OriginalKey("key", signature);
        OriginalKey second = new OriginalKey("key", signature);

        KeyAssertions.assertSame(first, second);
    }

    private static class WriteDigest implements Answer<Void> {
        private String toWrite;

        public WriteDigest(String toWrite) {
            this.toWrite = toWrite;
        }

        @Override
        public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
            MessageDigest md = (MessageDigest) invocationOnMock.getArguments()[0];
            md.update(toWrite.getBytes());
            return null;
        }
    }
}