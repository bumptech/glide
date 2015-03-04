package com.bumptech.glide.load.engine;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.tests.KeyAssertions;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Tests if {@link EngineKey} {@link Object#hashCode() hashCode} and {@link Object#equals(Object)
 * equals} and SHA-1 disk cache key are different on any difference in ID or existence of a certain
 * workflow part. Also checking whether the equals method is symmetric.
 */
@RunWith(JUnit4.class)
public class EngineKeyTest {
  private Harness harness;

  @Before
  public void setUp() {
    harness = new Harness();
  }

  private static class Harness {
    String id = "testId";
    int width = 1;
    int height = 2;
    Class resourceClass = Object.class;
    Class transcodeClass = Integer.class;
    ResourceEncoder encoder = mock(ResourceEncoder.class);
    Key signature = mock(Key.class);

    public EngineKey build() {
      return new EngineKey(id, signature, width, height, resourceClass, transcodeClass);
    }
  }

  @Test
  public void testIsIdenticalWithSameArguments() {
    assertEquals(harness.build(), harness.build());
  }

  @Test
  public void testDiffersIfIdDiffers() throws Exception {
    EngineKey first = harness.build();
    harness.id = harness.id + "2";
    EngineKey second = harness.build();

    KeyAssertions.assertDifferent(first, second, false /*checkDiskCacheKey*/);
  }

  @Test
  public void testDiffersIfHeightDiffers() throws Exception {
    EngineKey first = harness.build();
    harness.height += 1;
    EngineKey second = harness.build();

    KeyAssertions.assertDifferent(first, second, false /*checkDiskCacheKey*/);
  }

  @Test
  public void testDiffersIfWidthDiffers() throws Exception {
    EngineKey first = harness.build();
    harness.width += 1;
    EngineKey second = harness.build();

    KeyAssertions.assertDifferent(first, second, false /*checkDiskCacheKey*/);
  }

  @Test
  public void testDiffersIfSignatureDiffers()
      throws UnsupportedEncodingException, NoSuchAlgorithmException {
    EngineKey first = harness.build();
    Key signature = mock(Key.class);
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
        MessageDigest digest = (MessageDigest) invocationOnMock.getArguments()[0];
        digest.update("signature".getBytes("UTF-8"));
        return null;
      }
    }).when(signature).updateDiskCacheKey(any(MessageDigest.class));
    harness.signature = signature;
    EngineKey second = harness.build();

    KeyAssertions.assertDifferent(first, second, false /*checkDiskCacheKey*/);
  }

  @Test
  public void testDiffersIfResourceClassDiffers()
      throws UnsupportedEncodingException, NoSuchAlgorithmException {
    EngineKey first = harness.build();
    harness.resourceClass = Long.class;
    EngineKey second = harness.build();
    KeyAssertions.assertDifferent(first, second, false /*checkDiskCacheKey*/);
  }

  @Test
  public void testDiffersIfTranscodeClassDiffers()
      throws UnsupportedEncodingException, NoSuchAlgorithmException {
    EngineKey first = harness.build();
    harness.transcodeClass = Long.class;
    EngineKey second = harness.build();
    KeyAssertions.assertDifferent(first, second, false /*checkDiskCacheKey*/);
  }
}
