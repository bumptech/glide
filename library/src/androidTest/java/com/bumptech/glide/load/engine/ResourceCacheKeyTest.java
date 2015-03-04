package com.bumptech.glide.load.engine;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.signature.StringSignature;
import com.bumptech.glide.tests.KeyAssertions;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

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
        factory.sourceKey = new StringSignature("secondKey");
      }
    });
  }

  @Test
  public void testDiffersIfSignatureDiffers() {
    mutateAndAssertDifferent(new FactoryMutation() {
      @Override
      public void mutate(Factory factory) {
        factory.signature = new StringSignature("secondSignature");
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
        when(factory.transformation.getId()).thenReturn("otherTransformation");
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
    } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  static class Factory {
    Key sourceKey = new StringSignature("sourceKey");
    Key signature = new StringSignature("signature");
    int width = 100;
    int height = 100;
    Transformation<?> transformation = mock(Transformation.class);
    Class<?> resourceClass = Object.class;

    Factory() {
      when(transformation.getId()).thenReturn("transformation");
    }

    ResourceCacheKey build() {
      return new ResourceCacheKey(sourceKey, signature, width, height, transformation,
          resourceClass);
    }
  }
}