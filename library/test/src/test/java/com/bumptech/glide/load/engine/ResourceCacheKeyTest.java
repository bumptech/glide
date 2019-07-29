package com.bumptech.glide.load.engine;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import androidx.annotation.NonNull;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.Option;
import com.bumptech.glide.load.Option.CacheKeyUpdater;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.bitmap_recycle.LruArrayPool;
import com.bumptech.glide.signature.ObjectKey;
import com.bumptech.glide.tests.KeyTester;
import com.bumptech.glide.tests.Util;
import java.security.MessageDigest;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class ResourceCacheKeyTest {
  @Rule public final KeyTester keyTester = new KeyTester();

  @Mock private Transformation<Object> transformation1;
  @Mock private Transformation<Object> transformation2;
  private LruArrayPool arrayPool;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    arrayPool = new LruArrayPool();
    doAnswer(new Util.WriteDigest("transformation1"))
        .when(transformation1)
        .updateDiskCacheKey(any(MessageDigest.class));
    doAnswer(new Util.WriteDigest("transformation1"))
        .when(transformation2)
        .updateDiskCacheKey(any(MessageDigest.class));
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

    for (int i = 0; i < 20; i++) {
      byte[] array = new byte[9];
      Arrays.fill(array, (byte) 2);
      arrayPool.put(array);
    }

    keyTester
        .addEquivalenceGroup(
            new ResourceCacheKey(
                arrayPool,
                new ObjectKey("source"),
                new ObjectKey("signature"),
                100,
                100,
                transformation1,
                Object.class,
                new Options()),
            new ResourceCacheKey(
                arrayPool,
                new ObjectKey("source"),
                new ObjectKey("signature"),
                100,
                100,
                transformation1,
                Object.class,
                new Options()))
        .addEquivalenceGroup(
            new ResourceCacheKey(
                arrayPool,
                new ObjectKey("otherSource"),
                new ObjectKey("signature"),
                100,
                100,
                transformation1,
                Object.class,
                new Options()))
        .addEquivalenceGroup(
            new ResourceCacheKey(
                arrayPool,
                new ObjectKey("source"),
                new ObjectKey("otherSignature"),
                100,
                100,
                transformation1,
                Object.class,
                new Options()))
        .addEquivalenceGroup(
            new ResourceCacheKey(
                arrayPool,
                new ObjectKey("source"),
                new ObjectKey("signature"),
                200,
                100,
                transformation1,
                Object.class,
                new Options()))
        .addEquivalenceGroup(
            new ResourceCacheKey(
                arrayPool,
                new ObjectKey("source"),
                new ObjectKey("signature"),
                100,
                200,
                transformation1,
                Object.class,
                new Options()))
        .addEquivalenceGroup(
            new ResourceCacheKey(
                arrayPool,
                new ObjectKey("source"),
                new ObjectKey("signature"),
                100,
                100,
                transformation2,
                Object.class,
                new Options()))
        .addEquivalenceGroup(
            new ResourceCacheKey(
                arrayPool,
                new ObjectKey("source"),
                new ObjectKey("signature"),
                100,
                100,
                transformation1,
                Integer.class,
                new Options()))
        .addEquivalenceGroup(
            new ResourceCacheKey(
                arrayPool,
                new ObjectKey("source"),
                new ObjectKey("signature"),
                100,
                100,
                transformation1,
                Object.class,
                memoryOptions))
        .addEquivalenceGroup(
            new ResourceCacheKey(
                arrayPool,
                new ObjectKey("source"),
                new ObjectKey("signature"),
                100,
                100,
                transformation1,
                Object.class,
                diskOptions))
        .addRegressionTest(
            new ResourceCacheKey(
                arrayPool,
                new ObjectKey("source"),
                new ObjectKey("signature"),
                100,
                100,
                transformation1,
                Object.class,
                new Options()),
            "04d632bfe8e588544909fc44edb7328fa28bea6831b96927ade22b44818654e2")
        .addRegressionTest(
            new ResourceCacheKey(
                arrayPool,
                new ObjectKey("source"),
                new ObjectKey("signature"),
                100,
                100,
                transformation1,
                Object.class,
                diskOptions),
            "781ff8cd30aaaf248134580004ea6d63a1b87ae20ea0f769caf379d7d84986d0")
        .test();
  }
}
