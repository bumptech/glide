package com.bumptech.glide.load.engine;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.Option;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.tests.KeyAssertions;
import com.bumptech.glide.tests.Util;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Tests if {@link EngineKey} {@link Object#hashCode() hashCode} and {@link Object#equals(Object)
 * equals} and SHA-1 disk cache key are different on any difference in ID or existence of a certain
 * workflow part. Also checking whether the equals method is symmetric.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 18)
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
    Class<?> resourceClass = Object.class;
    Class<?> transcodeClass = Integer.class;
    Key signature = mock(Key.class);
    @SuppressWarnings("unchecked")
    Transformation<Object> transformation = mock(Transformation.class);
    Options options = new Options();

    public Harness() {
      doAnswer(new Util.WriteDigest("transformation")).when(transformation)
          .updateDiskCacheKey(any(MessageDigest.class));
    }

    public EngineKey build() {
      return new EngineKey(id, signature, width, height,
          Collections.<Class<?>, Transformation<?>>singletonMap(Object.class, transformation),
          resourceClass, transcodeClass, options);
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
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
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

  @Test
  public void testDiffersIfTransformationsDiffer() throws NoSuchAlgorithmException {
    EngineKey first = harness.build();

    @SuppressWarnings("unchecked") Transformation<Object> other = mock(Transformation.class);
    doAnswer(new Util.WriteDigest("other")).when(other)
        .updateDiskCacheKey(any(MessageDigest.class));
    harness.transformation = other;
    EngineKey second = harness.build();
    KeyAssertions.assertDifferent(first, second, false /*checkDiskCacheKey*/);
  }

  @Test
  public void testDiffersIfOptionsDiffer() throws NoSuchAlgorithmException {
    EngineKey first = harness.build();
    harness.options = new Options();
    harness.options.set(Option.memory("fakeKey"), "someValue");
    EngineKey second = harness.build();
    KeyAssertions.assertDifferent(first, second, false /*checkDiskCacheKey*/);
  }
}
