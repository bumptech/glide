package com.bumptech.glide.load.engine.cache;

import static com.bumptech.glide.load.engine.cache.MemoryCache.ResourceRemovedListener;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ComponentCallbacks2;

import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.engine.Resource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.security.MessageDigest;

@RunWith(JUnit4.class)
public class LruResourceCacheTest {
  private static class TrimClearMemoryCacheHarness {
    LruResourceCache resourceCache = new LruResourceCache(100);
    Resource first = mock(Resource.class);
    Resource second = mock(Resource.class);

    ResourceRemovedListener listener = mock(ResourceRemovedListener.class);

    public TrimClearMemoryCacheHarness() {
      when(first.getSize()).thenReturn(50);
      when(second.getSize()).thenReturn(50);
      resourceCache.put(new MockKey(), first);
      resourceCache.put(new MockKey(), second);
      resourceCache.setResourceRemovedListener(listener);
    }
  }

  @Test
  public void testTrimMemoryModerate() {
    TrimClearMemoryCacheHarness harness = new TrimClearMemoryCacheHarness();

    harness.resourceCache.trimMemory(ComponentCallbacks2.TRIM_MEMORY_MODERATE);

    verify(harness.listener).onResourceRemoved(eq(harness.first));
    verify(harness.listener).onResourceRemoved(eq(harness.second));
  }

  @Test
  public void testTrimMemoryComplete() {
    TrimClearMemoryCacheHarness harness = new TrimClearMemoryCacheHarness();

    harness.resourceCache.trimMemory(ComponentCallbacks2.TRIM_MEMORY_COMPLETE);

    verify(harness.listener).onResourceRemoved(harness.first);
    verify(harness.listener).onResourceRemoved(harness.second);
  }

  @Test
  public void testTrimMemoryBackground() {
    TrimClearMemoryCacheHarness harness = new TrimClearMemoryCacheHarness();

    harness.resourceCache.trimMemory(ComponentCallbacks2.TRIM_MEMORY_BACKGROUND);

    verify(harness.listener).onResourceRemoved(harness.first);
    verify(harness.listener, never()).onResourceRemoved(harness.second);
  }

  @Test
  public void testResourceRemovedListenerIsNotifiedWhenResourceIsRemoved() {
    LruResourceCache resourceCache = new LruResourceCache(100);
    Resource resource = mock(Resource.class);
    when(resource.getSize()).thenReturn(200);

    ResourceRemovedListener listener = mock(ResourceRemovedListener.class);

    resourceCache.setResourceRemovedListener(listener);
    resourceCache.put(new MockKey(), resource);

    verify(listener).onResourceRemoved(eq(resource));
  }

  @Test
  public void testSizeIsBasedOnResource() {
    LruResourceCache resourceCache = new LruResourceCache(100);
    Resource first = getResource(50);
    MockKey firstKey = new MockKey();
    resourceCache.put(firstKey, first);
    Resource second = getResource(50);
    MockKey secondKey = new MockKey();
    resourceCache.put(secondKey, second);

    assertTrue(resourceCache.contains(firstKey));
    assertTrue(resourceCache.contains(secondKey));

    Resource third = getResource(50);
    MockKey thirdKey = new MockKey();
    resourceCache.put(thirdKey, third);

    assertFalse(resourceCache.contains(firstKey));
    assertTrue(resourceCache.contains(secondKey));
    assertTrue(resourceCache.contains(thirdKey));
  }

  private Resource getResource(int size) {
    Resource resource = mock(Resource.class);
    when(resource.getSize()).thenReturn(size);
    return resource;
  }

  private static class MockKey implements Key {
    @Override
    public void updateDiskCacheKey(MessageDigest messageDigest) {
      messageDigest.update(toString().getBytes(CHARSET));
    }
  }
}
