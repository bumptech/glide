package com.bumptech.glide.load.engine;

import static com.bumptech.glide.RobolectricConstants.ROBOLECTRIC_SDK;
import static org.junit.Assert.assertThrows;

import androidx.annotation.NonNull;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.Option;
import com.bumptech.glide.load.Option.CacheKeyUpdater;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.signature.ObjectKey;
import com.bumptech.glide.util.EqualityKeyProxy;
import com.google.common.testing.EqualsTester;
import com.google.testing.junit.testparameterinjector.TestParameter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestParameterInjector;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestParameterInjector.class)
@Config(sdk = ROBOLECTRIC_SDK)
public class EngineKeyTest {
  @Mock private Transformation<Object> transformation;
  @TestParameter private boolean allowWeakReferences;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void updateDiskCacheKey_throwsException() throws NoSuchAlgorithmException {
    // If this test fails, update equalsAndHashCode to use KeyTester including regression tests.
    final EngineKey key =
        new EngineKey(
            EqualityKeyProxy.of("id", allowWeakReferences),
            new ObjectKey("signature"),
            100,
            100,
            Object.class,
            Object.class,
            Collections.<Class<?>, EqualityKeyProxy<Transformation<?>>>emptyMap(),
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
  public void equalsAndHashCode() {
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
                EqualityKeyProxy.of("id", allowWeakReferences),
                new ObjectKey("signature"),
                100,
                100,
                Object.class,
                Object.class,
                Collections.<Class<?>, EqualityKeyProxy<Transformation<?>>>emptyMap(),
                new Options()),
            new EngineKey(
                EqualityKeyProxy.of("id", allowWeakReferences),
                new ObjectKey("signature"),
                100,
                100,
                Object.class,
                Object.class,
                Collections.<Class<?>, EqualityKeyProxy<Transformation<?>>>emptyMap(),
                new Options()))
        .addEqualityGroup(
            new EngineKey(
                EqualityKeyProxy.of("otherId", allowWeakReferences),
                new ObjectKey("signature"),
                100,
                100,
                Object.class,
                Object.class,
                Collections.<Class<?>, EqualityKeyProxy<Transformation<?>>>emptyMap(),
                new Options()))
        .addEqualityGroup(
            new EngineKey(
                EqualityKeyProxy.of("id", allowWeakReferences),
                new ObjectKey("otherSignature"),
                100,
                100,
                Object.class,
                Object.class,
                Collections.<Class<?>, EqualityKeyProxy<Transformation<?>>>emptyMap(),
                new Options()))
        .addEqualityGroup(
            new EngineKey(
                EqualityKeyProxy.of("id", allowWeakReferences),
                new ObjectKey("signature"),
                200,
                100,
                Object.class,
                Object.class,
                Collections.<Class<?>, EqualityKeyProxy<Transformation<?>>>emptyMap(),
                new Options()))
        .addEqualityGroup(
            new EngineKey(
                EqualityKeyProxy.of("id", allowWeakReferences),
                new ObjectKey("signature"),
                100,
                200,
                Object.class,
                Object.class,
                Collections.<Class<?>, EqualityKeyProxy<Transformation<?>>>emptyMap(),
                new Options()))
        .addEqualityGroup(
            new EngineKey(
                EqualityKeyProxy.of("id", allowWeakReferences),
                new ObjectKey("signature"),
                100,
                100,
                Object.class,
                Object.class,
                Collections.<Class<?>, EqualityKeyProxy<Transformation<?>>>singletonMap(
                    Object.class, EqualityKeyProxy.of(transformation, allowWeakReferences)),
                new Options()))
        .addEqualityGroup(
            new EngineKey(
                EqualityKeyProxy.of("id", allowWeakReferences),
                new ObjectKey("signature"),
                100,
                100,
                Integer.class,
                Object.class,
                Collections.<Class<?>, EqualityKeyProxy<Transformation<?>>>emptyMap(),
                new Options()))
        .addEqualityGroup(
            new EngineKey(
                EqualityKeyProxy.of("id", allowWeakReferences),
                new ObjectKey("signature"),
                100,
                100,
                Object.class,
                Integer.class,
                Collections.<Class<?>, EqualityKeyProxy<Transformation<?>>>emptyMap(),
                new Options()))
        .addEqualityGroup(
            new EngineKey(
                EqualityKeyProxy.of("id", allowWeakReferences),
                new ObjectKey("signature"),
                100,
                100,
                Object.class,
                Object.class,
                Collections.<Class<?>, EqualityKeyProxy<Transformation<?>>>emptyMap(),
                memoryOptions))
        .addEqualityGroup(
            new EngineKey(
                EqualityKeyProxy.of("id", allowWeakReferences),
                new ObjectKey("signature"),
                100,
                100,
                Object.class,
                Object.class,
                Collections.<Class<?>, EqualityKeyProxy<Transformation<?>>>emptyMap(),
                diskOptions))
        .testEquals();
  }
}
