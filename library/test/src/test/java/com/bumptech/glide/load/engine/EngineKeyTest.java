package com.bumptech.glide.load.engine;

import static org.junit.Assert.assertThrows;

import androidx.annotation.NonNull;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.Option;
import com.bumptech.glide.load.Option.CacheKeyUpdater;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.signature.ObjectKey;
import com.google.common.testing.EqualsTester;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 18)
public class EngineKeyTest {
  @Mock private Transformation<Object> transformation;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void updateDiskCacheKey_throwsException() throws NoSuchAlgorithmException {
    // If this test fails, update testEqualsAndHashcode to use KeyTester including regression tests.
    final EngineKey key =
        new EngineKey(
            "id",
            new ObjectKey("signature"),
            100,
            100,
            Collections.<Class<?>, Transformation<?>>emptyMap(),
            Object.class,
            Object.class,
            new Options());
    assertThrows(
        UnsupportedOperationException.class,
        new ThrowingRunnable() {
          @Override
          public void run() throws NoSuchAlgorithmException {
            key.updateDiskCacheKey(MessageDigest.getInstance("SHA-1"));
          }
        });
  }

  @Test
  public void testEqualsAndHashCode() {
    Options memoryOptions = new Options();
    memoryOptions.set(Option.memory("key", new Object()), new Object());

    Options diskOptions = new Options();
    diskOptions.set(
        Option.disk(
            "key",
            new CacheKeyUpdater<String>() {
              @Override
              public void update(
                  @NonNull byte[] keyBytes,
                  @NonNull String value,
                  @NonNull MessageDigest messageDigest) {
                messageDigest.update(keyBytes);
                messageDigest.update(value.getBytes(Key.CHARSET));
              }
            }),
        "value");

    new EqualsTester()
        .addEqualityGroup(
            new EngineKey(
                "id",
                new ObjectKey("signature"),
                100,
                100,
                Collections.<Class<?>, Transformation<?>>emptyMap(),
                Object.class,
                Object.class,
                new Options()),
            new EngineKey(
                "id",
                new ObjectKey("signature"),
                100,
                100,
                Collections.<Class<?>, Transformation<?>>emptyMap(),
                Object.class,
                Object.class,
                new Options()))
        .addEqualityGroup(
            new EngineKey(
                "otherId",
                new ObjectKey("signature"),
                100,
                100,
                Collections.<Class<?>, Transformation<?>>emptyMap(),
                Object.class,
                Object.class,
                new Options()))
        .addEqualityGroup(
            new EngineKey(
                "id",
                new ObjectKey("otherSignature"),
                100,
                100,
                Collections.<Class<?>, Transformation<?>>emptyMap(),
                Object.class,
                Object.class,
                new Options()))
        .addEqualityGroup(
            new EngineKey(
                "id",
                new ObjectKey("signature"),
                200,
                100,
                Collections.<Class<?>, Transformation<?>>emptyMap(),
                Object.class,
                Object.class,
                new Options()))
        .addEqualityGroup(
            new EngineKey(
                "id",
                new ObjectKey("signature"),
                100,
                200,
                Collections.<Class<?>, Transformation<?>>emptyMap(),
                Object.class,
                Object.class,
                new Options()))
        .addEqualityGroup(
            new EngineKey(
                "id",
                new ObjectKey("signature"),
                100,
                100,
                Collections.<Class<?>, Transformation<?>>singletonMap(Object.class, transformation),
                Object.class,
                Object.class,
                new Options()))
        .addEqualityGroup(
            new EngineKey(
                "id",
                new ObjectKey("signature"),
                100,
                100,
                Collections.<Class<?>, Transformation<?>>emptyMap(),
                Integer.class,
                Object.class,
                new Options()))
        .addEqualityGroup(
            new EngineKey(
                "id",
                new ObjectKey("signature"),
                100,
                100,
                Collections.<Class<?>, Transformation<?>>emptyMap(),
                Object.class,
                Integer.class,
                new Options()))
        .addEqualityGroup(
            new EngineKey(
                "id",
                new ObjectKey("signature"),
                100,
                100,
                Collections.<Class<?>, Transformation<?>>emptyMap(),
                Object.class,
                Object.class,
                memoryOptions))
        .addEqualityGroup(
            new EngineKey(
                "id",
                new ObjectKey("signature"),
                100,
                100,
                Collections.<Class<?>, Transformation<?>>emptyMap(),
                Object.class,
                Object.class,
                diskOptions))
        .testEquals();
  }
}
