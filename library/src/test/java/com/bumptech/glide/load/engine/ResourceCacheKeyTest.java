package com.bumptech.glide.load.engine;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.signature.ObjectKey;
import com.bumptech.glide.tests.KeyAssertions;
import com.bumptech.glide.tests.Util;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ResourceCacheKeyTest {

  private Factory factory;

  @Before
  public void setUp() {
    factory = new Factory();
  }

  @Test
  public void testIdenticalWithSameArguments()
      throws UnsupportedEncodingException, NoSuchAlgorithmException {
    KeyAssertions.assertSame(factory.build(), factory.build());
  }

  @Test
  public void testDifferIfSourceKeyDiffers()
      throws UnsupportedEncodingException, NoSuchAlgorithmException {
    mutateAndAssertDifferent(new FactoryMutation() {
      @Override
      public void mutate(Factory factory) {
        factory.sourceKey = new ObjectKey("secondKey");
      }
    });
  }

  @Test
  public void testDiffersIfSignatureDiffers() {
    mutateAndAssertDifferent(new FactoryMutation() {
      @Override
      public void mutate(Factory factory) {
        factory.signature = new ObjectKey("secondSignature");
      }
    });
  }

  @Test
  public void testDiffersIfWidthDiffers() {
    mutateAndAssertDifferent(new FactoryMutation() {
      @Override
      public void mutate(Factory factory) {
        factory.width = factory.width * 2;
      }
    });
  }

  @Test
  public void testDiffersIfHeightDiffers() {
    mutateAndAssertDifferent(new FactoryMutation() {
      @Override
      public void mutate(Factory factory) {
        factory.height = factory.height * 2;
      }
    });
  }

  @Test
  public void tesDiffersIfTransformationDiffers() {
    mutateAndAssertDifferent(new FactoryMutation() {
      @Override
      public void mutate(Factory factory) {
        factory.transformation = mock(Transformation.class);
        doAnswer(new Util.WriteDigest("otherTransformation")).when(factory.transformation)
            .updateDiskCacheKey(any(MessageDigest.class));
      }
    });
  }

  @Test
  public void testDiffersIfResourceDiffers() {
    mutateAndAssertDifferent(new FactoryMutation() {
      @Override
      public void mutate(Factory factory) {
        factory.resourceClass = Integer.class;
      }
    });
  }

  interface FactoryMutation {
    void mutate(Factory factory);
  }

  private void mutateAndAssertDifferent(FactoryMutation mutation) {
    ResourceCacheKey original = factory.build();
    mutation.mutate(factory);
    ResourceCacheKey mutated = factory.build();

    try {
      KeyAssertions.assertDifferent(original, mutated);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  static class Factory {
    Key sourceKey = new ObjectKey("sourceKey");
    Key signature = new ObjectKey("signature");
    int width = 100;
    int height = 100;
    Transformation<?> transformation = mock(Transformation.class);
    Class<?> resourceClass = Object.class;
    Options options = new Options();

    Factory() {
      doAnswer(new Util.WriteDigest("transformation")).when(transformation)
          .updateDiskCacheKey(any(MessageDigest.class));
    }

    ResourceCacheKey build() {
      return new ResourceCacheKey(sourceKey, signature, width, height, transformation,
          resourceClass, options);
    }
  }
}
