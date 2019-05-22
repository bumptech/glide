package com.bumptech.glide.load.model;

import static org.junit.Assert.assertEquals;

import com.google.common.testing.EqualsTester;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ModelCacheTest {

  private ModelCache<Object, Object> cache;

  @Before
  public void setUp() {
    cache = new ModelCache<>(10);
  }

  @Test
  public void testModelKeyEquivalence() {
    new EqualsTester()
        .addEqualityGroup(
            ModelCache.ModelKey.get(14f, 100, 200), ModelCache.ModelKey.get(14f, 100, 200))
        .addEqualityGroup(ModelCache.ModelKey.get(13f, 100, 200))
        .addEqualityGroup(ModelCache.ModelKey.get(14f, 200, 200))
        .addEqualityGroup(ModelCache.ModelKey.get(14f, 100, 300))
        .testEquals();
  }

  @Test
  public void testCanSetAndGetModel() {
    Object model = new Object();
    int width = 10;
    int height = 20;
    Object result = new Object();
    cache.put(model, width, height, result);
    assertEquals(result, cache.get(model, width, height));
  }

  @Test
  public void testCanSetAndGetMultipleResultsWithDifferentDimensionsForSameObject() {
    Object model = new Object();
    int firstWidth = 10, firstHeight = 20;
    Object firstResult = new Object();
    int secondWidth = 30, secondHeight = 40;
    Object secondResult = new Object();

    cache.put(model, firstWidth, firstHeight, firstResult);
    cache.put(model, secondWidth, secondHeight, secondResult);

    assertEquals(firstResult, cache.get(model, firstWidth, firstHeight));
    assertEquals(secondResult, cache.get(model, secondWidth, secondHeight));
  }
}
