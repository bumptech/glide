package com.bumptech.glide.load.engine.cache;

import static com.bumptech.glide.load.engine.cache.MemoryCache.ResourceRemovedListener;
import static com.bumptech.glide.tests.Util.anyResource;
import static com.bumptech.glide.tests.Util.mockResource;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ComponentCallbacks2;
import androidx.annotation.NonNull;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.util.LruCache;
import java.security.MessageDigest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class LruResourceCacheTest {

  @Test
  public void put_withExistingItem_updatesSizeCorrectly() {
    PutWithExistingEntryHarness harness = new PutWithExistingEntryHarness();
    harness.cache.put(harness.key, harness.first);
    harness.cache.put(harness.key, harness.second);

    assertThat(harness.cache.getCurrentSize()).isEqualTo(harness.second.getSize());
  }

  @Test
  public void put_withExistingItem_evictsExistingItem() {
    PutWithExistingEntryHarness harness = new PutWithExistingEntryHarness();
    harness.cache.put(harness.key, harness.first);
    harness.cache.put(harness.key, harness.second);

    verify(harness.listener).onResourceRemoved(harness.first);
  }

  @Test
  public void get_afterPutWithExistingItem_returnsNewItem() {
    PutWithExistingEntryHarness harness = new PutWithExistingEntryHarness();
    harness.cache.put(harness.key, harness.first);
    harness.cache.put(harness.key, harness.second);

    assertThat(harness.cache.get(harness.key)).isEqualTo(harness.second);
  }

  @Test
  public void onItemEvicted_withNullValue_doesNotNotifyListener() {
    PutWithExistingEntryHarness harness = new PutWithExistingEntryHarness();
    harness.cache.onItemEvicted(new MockKey(), null);
    verify(harness.listener, never()).onResourceRemoved(anyResource());
  }

  @Test
  public void clearMemory_afterPutWithExistingItem_evictsOnlyNewItem() {
    PutWithExistingEntryHarness harness = new PutWithExistingEntryHarness();
    harness.cache.put(harness.key, harness.first);
    harness.cache.put(harness.key, harness.second);

    verify(harness.listener).onResourceRemoved(harness.first);
    verify(harness.listener, never()).onResourceRemoved(harness.second);

    harness.cache.clearMemory();

    verify(harness.listener, times(1)).onResourceRemoved(harness.first);
    verify(harness.listener).onResourceRemoved(harness.second);
  }

  @Test
  public void testTrimMemoryBackground() {
    TrimClearMemoryCacheHarness harness = new TrimClearMemoryCacheHarness();

    harness.resourceCache.trimMemory(ComponentCallbacks2.TRIM_MEMORY_BACKGROUND);

    verify(harness.listener).onResourceRemoved(eq(harness.first));
    verify(harness.listener).onResourceRemoved(eq(harness.second));
  }

  @Test
  public void testTrimMemoryModerate() {
    TrimClearMemoryCacheHarness harness = new TrimClearMemoryCacheHarness();

    harness.resourceCache.trimMemory(ComponentCallbacks2.TRIM_MEMORY_MODERATE);

    verify(harness.listener).onResourceRemoved(harness.first);
    verify(harness.listener).onResourceRemoved(harness.second);
  }

  @Test
  public void testTrimMemoryUiHidden() {
    TrimClearMemoryCacheHarness harness = new TrimClearMemoryCacheHarness();

    harness.resourceCache.trimMemory(ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN);

    verify(harness.listener).onResourceRemoved(harness.first);
    verify(harness.listener, never()).onResourceRemoved(harness.second);
  }

  @Test
  public void testTrimMemoryRunningCritical() {
    TrimClearMemoryCacheHarness harness = new TrimClearMemoryCacheHarness();

    harness.resourceCache.trimMemory(ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL);

    verify(harness.listener).onResourceRemoved(harness.first);
    verify(harness.listener, never()).onResourceRemoved(harness.second);
  }

  @Test
  public void testResourceRemovedListenerIsNotifiedWhenResourceIsRemoved() {
    LruResourceCache resourceCache = new LruResourceCache(100);
    Resource<?> resource = mockResource();
    when(resource.getSize()).thenReturn(200);

    ResourceRemovedListener listener = mock(ResourceRemovedListener.class);

    resourceCache.setResourceRemovedListener(listener);
    resourceCache.put(new MockKey(), resource);

    verify(listener).onResourceRemoved(eq(resource));
  }

  @Test
  public void testSizeIsBasedOnResource() {
    LruResourceCache resourceCache = new LruResourceCache(100);
    Resource<?> first = getResource(50);
    MockKey firstKey = new MockKey();
    resourceCache.put(firstKey, first);
    Resource<?> second = getResource(50);
    MockKey secondKey = new MockKey();
    resourceCache.put(secondKey, second);

    assertTrue(resourceCache.contains(firstKey));
    assertTrue(resourceCache.contains(secondKey));

    Resource<?> third = getResource(50);
    MockKey thirdKey = new MockKey();
    resourceCache.put(thirdKey, third);

    assertFalse(resourceCache.contains(firstKey));
    assertTrue(resourceCache.contains(secondKey));
    assertTrue(resourceCache.contains(thirdKey));
  }

  @Test
  public void testPreventEviction() {
    final MemoryCache cache = new LruResourceCache(100);
    final Resource<?> first = getResource(30);
    final Key firstKey = new MockKey();
    cache.put(firstKey, first);
    Resource<?> second = getResource(30);
    Key secondKey = new MockKey();
    cache.put(secondKey, second);
    Resource<?> third = getResource(30);
    Key thirdKey = new MockKey();
    cache.put(thirdKey, third);
    cache.setResourceRemovedListener(
        new ResourceRemovedListener() {
          @Override
          public void onResourceRemoved(@NonNull Resource<?> removed) {
            if (removed == first) {
              cache.put(firstKey, first);
            }
          }
        });

    // trims from 100 to 50, having 30+30+30 items, it should trim to 1 item
    cache.trimMemory(ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN);

    // and that 1 item must be first, because it's forced to return to cache in the listener
    @SuppressWarnings("unchecked")
    LruCache<Key, Resource<?>> lruCache = (LruCache<Key, Resource<?>>) cache;
    assertTrue(lruCache.contains(firstKey));
    assertFalse(lruCache.contains(secondKey));
    assertFalse(lruCache.contains(thirdKey));
  }

  private Resource<?> getResource(int size) {
    Resource<?> resource = mockResource();
    when(resource.getSize()).thenReturn(size);
    return resource;
  }

  private static class MockKey implements Key {
    @Override
    public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {
      messageDigest.update(toString().getBytes(CHARSET));
    }
  }

  private static class PutWithExistingEntryHarness {
    final LruResourceCache cache = new LruResourceCache(100);
    final Resource<?> first = mockResource();
    final Resource<?> second = mockResource();
    final ResourceRemovedListener listener = mock(ResourceRemovedListener.class);
    final Key key = new MockKey();

    PutWithExistingEntryHarness() {
      when(first.getSize()).thenReturn(50);
      when(second.getSize()).thenReturn(50);
      cache.setResourceRemovedListener(listener);
    }
  }

  private static class TrimClearMemoryCacheHarness {
    final LruResourceCache resourceCache = new LruResourceCache(100);
    final Resource<?> first = mockResource();
    final Resource<?> second = mockResource();
    final ResourceRemovedListener listener = mock(ResourceRemovedListener.class);

    TrimClearMemoryCacheHarness() {
      when(first.getSize()).thenReturn(50);
      when(second.getSize()).thenReturn(50);
      resourceCache.put(new MockKey(), first);
      resourceCache.put(new MockKey(), second);
      resourceCache.setResourceRemovedListener(listener);
    }
  }
}
