package com.bumptech.glide.load.engine;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;

import com.bumptech.glide.load.Key;
import com.bumptech.glide.tests.KeyAssertions;
import com.bumptech.glide.tests.Util.WriteDigest;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(JUnit4.class)
public class DataCacheKeyTest {

  @Mock Key firstKey;
  @Mock Key firstSignature;
  @Mock Key secondKey;
  @Mock Key secondSignature;

  @Before
  public void setUp() throws UnsupportedEncodingException {
    MockitoAnnotations.initMocks(this);
    doAnswer(new WriteDigest("firstKey")).when(firstKey)
        .updateDiskCacheKey(any(MessageDigest.class));
    doAnswer(new WriteDigest("firstSignature")).when(firstSignature)
        .updateDiskCacheKey(any(MessageDigest.class));
    doAnswer(new WriteDigest("secondKey")).when(secondKey)
        .updateDiskCacheKey(any(MessageDigest.class));
    doAnswer(new WriteDigest("secondSignature")).when(secondSignature)
        .updateDiskCacheKey(any(MessageDigest.class));
  }

  @Test
  public void testDiffersIfIdDiffers()
      throws UnsupportedEncodingException, NoSuchAlgorithmException {
    DataCacheKey first = new DataCacheKey(firstKey, firstSignature);
    DataCacheKey second = new DataCacheKey(secondKey, firstSignature);

    KeyAssertions.assertDifferent(first, second);
  }

  @Test
  public void testDiffersIfSignatureDiffers()
      throws UnsupportedEncodingException, NoSuchAlgorithmException {
    DataCacheKey first = new DataCacheKey(firstKey, firstSignature);
    DataCacheKey second = new DataCacheKey(firstKey, secondSignature);

    KeyAssertions.assertDifferent(first, second);
  }

  @Test
  public void testSameIfIdAndSignatureAreTheSame()
      throws UnsupportedEncodingException, NoSuchAlgorithmException {
    DataCacheKey first = new DataCacheKey(firstKey, firstSignature);
    DataCacheKey second = new DataCacheKey(firstKey, firstSignature);

    KeyAssertions.assertSame(first, second);
  }
}
