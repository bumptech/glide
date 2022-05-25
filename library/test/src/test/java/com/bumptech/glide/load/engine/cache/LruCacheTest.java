package com.bumptech.glide.load.engine.cache;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.util.LruCache;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class LruCacheTest {
  private static final int SIZE = 10;
  private LruCache<String, Object> cache;
  private CacheListener listener;
  private String currentKey;

  @Before
  public void setUp() {
    currentKey = "";
    listener = mock(CacheListener.class);
    cache = new TestLruCache(SIZE, listener);
    when(listener.getSize(any())).thenReturn(1);
  }

  @Test
  public void testCanAddAndRetrieveItem() {
    String key = getKey();
    Object object = new Object();

    cache.put(key, object);

    assertEquals(object, cache.get(key));
  }

  @Test
  public void testCanPutNullItemWithoutChangingSize() {
    String key = getKey();
    cache.put(key, null);

    for (int i = 0; i < SIZE; i++) {
      cache.put(getKey(), new Object());
    }

    verify(listener, never()).onItemRemoved(any());
  }

  @Test
  public void testReplacingNonNullItemWithNullItemDecreasesSize() {
    String key = getKey();
    Object initialValue = new Object();
    cache.put(key, initialValue);
    cache.put(key, null);

    for (int i = 0; i < SIZE; i++) {
      cache.put(getKey(), new Object());
    }

    verify(listener).onItemRemoved(initialValue);
  }

  @Test
  public void testReplacingNullItemWIthNullItemIncreasesSize() {
    String key = getKey();
    cache.put(key, null);
    cache.put(key, new Object());

    for (int i = 0; i < SIZE; i++) {
      cache.put(getKey(), new Object());
    }

    verify(listener).onItemRemoved(any());
  }

  @Test
  public void testReplacingNonNullItemWithNonNullItemUpdatesSize() {
    String key = getKey();
    Object initialValue = new Object();
    cache.put(key, initialValue);
    cache.put(key, new Object());

    for (int i = 0; i < SIZE - 1; i++) {
      cache.put(getKey(), new Object());
    }

    verify(listener).onItemRemoved(initialValue);
    verify(listener, never()).onItemRemoved(not(eq(initialValue)));
  }

  @Test
  public void testCacheContainsAddedBitmap() {
    final String key = getKey();
    cache.put(key, new Object());
    assertTrue(cache.contains(key));
  }

  @Test
  public void testEmptyCacheDoesNotContainKey() {
    assertFalse(cache.contains(getKey()));
  }

  @Test
  public void testItIsSizeLimited() {
    for (int i = 0; i < SIZE; i++) {
      cache.put(getKey(), new Object());
    }
    verify(listener, never()).onItemRemoved(any());
    cache.put(getKey(), new Object());
    verify(listener).onItemRemoved(any());
  }

  @Test
  public void testLeastRecentlyAddKeyEvictedFirstIfGetsAreEqual() {
    Object first = new Object();
    cache.put(getKey(), first);
    for (int i = 0; i < SIZE; i++) {
      cache.put(getKey(), new Object());
    }

    verify(listener).onItemRemoved(eq(first));
    verify(listener, times(1)).onItemRemoved(any(Object.class));
  }

  @Test
  public void testLeastRecentlyUsedKeyEvictedFirst() {
    String mostRecentlyUsedKey = getKey();
    Object mostRecentlyUsedObject = new Object();
    String leastRecentlyUsedKey = getKey();
    Object leastRecentlyUsedObject = new Object();

    cache.put(mostRecentlyUsedKey, mostRecentlyUsedObject);
    cache.put(leastRecentlyUsedKey, leastRecentlyUsedObject);

    cache.get(mostRecentlyUsedKey);
    for (int i = 0; i < SIZE - 1; i++) {
      cache.put(getKey(), new Object());
    }

    verify(listener).onItemRemoved(eq(leastRecentlyUsedObject));
    verify(listener, times(1)).onItemRemoved(any(Object.class));
  }

  @Test
  public void testItemLargerThanCacheIsImmediatelyEvicted() {
    Object tooLarge = new Object();
    when(listener.getSize(eq(tooLarge))).thenReturn(SIZE + 1);
    cache.put(getKey(), tooLarge);

    verify(listener).onItemRemoved(eq(tooLarge));
  }

  @Test
  public void testItemLargerThanCacheDoesNotCauseAdditionalEvictions() {
    cache.put(getKey(), new Object());

    Object tooLarge = new Object();
    when(listener.getSize(eq(tooLarge))).thenReturn(SIZE + 1);

    cache.put(getKey(), tooLarge);

    verify(listener, times(1)).onItemRemoved(any());
  }

  @Test
  public void testClearMemoryRemovesAllItems() {
    String first = getKey();
    String second = getKey();
    cache.put(first, new Object());
    cache.put(second, new Object());

    cache.clearMemory();

    assertFalse(cache.contains(first));
    assertFalse(cache.contains(second));
  }

  @Test
  public void testCanPutSameItemMultipleTimes() {
    String key = getKey();
    Object value = new Object();
    for (int i = 0; i < SIZE * 2; i++) {
      cache.put(key, value);
    }

    verify(listener, never()).onItemRemoved(any());
  }

  @Test
  public void put_withSameKeyAndValueTwice_doesNotEvictItems() {
    String key = getKey();
    Object value = new Object();
    cache.put(key, value);
    cache.put(key, value);

    verify(listener, never()).onItemRemoved(any());
  }

  @Test
  public void put_withExistingNullValue_doesNotNotifyListener() {
    String key = getKey();
    cache.put(key, /* item= */ null);
    cache.put(key, new Object());

    verify(listener, never()).onItemRemoved(any());
  }

  @Test
  public void put_withNullValue_withSizeGreaterThanMaximum_notifiesListener() {
    String key = getKey();
    when(listener.getSize(null)).thenReturn((int) (cache.getMaxSize() * 2));
    cache.put(key, null);

    verify(listener).onItemRemoved(any());
  }

  @Test
  public void testCanIncreaseSizeDynamically() {
    int sizeMultiplier = 2;
    cache.setSizeMultiplier(sizeMultiplier);
    for (int i = 0; i < SIZE * sizeMultiplier; i++) {
      cache.put(getKey(), new Object());
    }

    verify(listener, never()).onItemRemoved(any());
  }

  @Test
  public void testCanDecreaseSizeDynamically() {
    for (int i = 0; i < SIZE; i++) {
      cache.put(getKey(), new Object());
    }
    verify(listener, never()).onItemRemoved(any());

    float smallerMultiplier = 0.4f;

    cache.setSizeMultiplier(smallerMultiplier);

    verify(listener, times((int) (SIZE * (1 - smallerMultiplier)))).onItemRemoved(any());
  }

  @Test
  public void testCanResetSizeDynamically() {
    int sizeMultiplier = 2;
    cache.setSizeMultiplier(sizeMultiplier);
    for (int i = 0; i < SIZE * sizeMultiplier; i++) {
      cache.put(getKey(), new Object());
    }

    cache.setSizeMultiplier(1);

    verify(listener, times((sizeMultiplier * SIZE) - SIZE)).onItemRemoved(any());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testThrowsIfMultiplierLessThanZero() {
    cache.setSizeMultiplier(-1);
  }

  @Test
  public void testCanHandleZeroAsMultiplier() {
    for (int i = 0; i < SIZE; i++) {
      cache.put(getKey(), new Object());
    }
    cache.setSizeMultiplier(0);

    verify(listener, times(SIZE)).onItemRemoved(any());
  }

  @Test
  public void testCanRemoveKeys() {
    String key = getKey();
    Object value = new Object();
    cache.put(key, value);
    cache.remove(key);

    assertNull(cache.get(key));
    assertFalse(cache.contains(key));
  }

  @Test
  public void testDecreasesSizeWhenRemovesKey() {
    String key = getKey();
    Object value = new Object();
    cache.put(key, value);
    for (int i = 0; i < SIZE - 1; i++) {
      cache.put(getKey(), value);
    }
    cache.remove(key);
    cache.put(key, value);

    verify(listener, never()).onItemRemoved(any());
  }

  @Test
  public void testDoesNotCallListenerWhenRemovesKey() {
    String key = getKey();
    cache.put(key, new Object());
    cache.remove(key);

    verify(listener, never()).onItemRemoved(any());
  }

  @Test
  public void testGetMaxSizeReturnsCurrentMaxSizeOfCache() {
    assertEquals(SIZE, cache.getMaxSize());
  }

  @Test
  public void setSizeMultiplier_withItemWhoseSizeDecreasesAfterAdd_doesNotCrash() {
    Object itemWhoseSizeWillChange = new Object();
    when(listener.getSize(itemWhoseSizeWillChange)).thenReturn(SIZE - 1).thenReturn(SIZE / 2);
    cache.put(getKey(), itemWhoseSizeWillChange);
    cache.setSizeMultiplier(0);
  }

  @Test
  public void getCurrentSize_afterRemovingItemWhoseSizeChanged_returnsZero() {
    Object itemWhoseSizeWillChange = new Object();
    when(listener.getSize(itemWhoseSizeWillChange)).thenReturn(SIZE - 1).thenReturn(SIZE / 2);
    String key = getKey();
    cache.put(key, itemWhoseSizeWillChange);
    cache.remove(key);

    assertThat(cache.getCurrentSize()).isEqualTo(0);
  }

  @Test
  public void clearMemory_afterRemovingItemWhoseSizeChanged_doesNotCrash() {
    Object itemWhoseSizeWillChange = new Object();
    when(listener.getSize(itemWhoseSizeWillChange)).thenReturn(SIZE - 1).thenReturn((SIZE / 2) - 1);
    String key = getKey();
    cache.put(key, itemWhoseSizeWillChange);
    cache.remove(key);

    cache.clearMemory();
  }

  @Test
  public void getCurrentSize_afterUpdatingItemWhoseSizeChanged_returnsTheNewSize() {
    Object itemWhoseSizeWillChange = new Object();
    when(listener.getSize(itemWhoseSizeWillChange)).thenReturn(SIZE - 1).thenReturn((SIZE / 2) - 1);
    String key = getKey();
    cache.put(key, itemWhoseSizeWillChange);
    cache.put(key, new Object());

    assertThat(cache.getCurrentSize()).isEqualTo(1);
  }

  @Test
  public void clearMemory_afterUpdatingItemWhoseSizeChanged_doesNotCrash() {
    Object itemWhoseSizeWillChange = new Object();
    when(listener.getSize(itemWhoseSizeWillChange)).thenReturn(SIZE - 1).thenReturn((SIZE / 2) - 1);
    String key = getKey();
    cache.put(key, itemWhoseSizeWillChange);
    cache.put(key, new Object());

    cache.clearMemory();
  }

  @Test
  public void testGetMaxSizeChangesIfMaxSizeChanges() {
    int multiplier = 2;
    cache.setSizeMultiplier(multiplier);

    assertEquals(SIZE * multiplier, cache.getMaxSize());
  }

  @Test
  public void getCurrentSizeReturnsZeroForEmptyCache() {
    assertEquals(0, cache.getCurrentSize());
  }

  @Test
  public void testGetCurrentSizeIncreasesAsSizeIncreases() {
    cache.put(getKey(), new Object());
    assertEquals(1, cache.getCurrentSize());
    cache.put(getKey(), new Object());
    assertEquals(2, cache.getCurrentSize());
  }

  @Test
  public void testGetCurrentSizeDoesNotChangeWhenSizeMultiplierChangesIfNoItemsAreEvicted() {
    cache.put(getKey(), new Object());
    assertEquals(1, cache.getCurrentSize());
    cache.setSizeMultiplier(2);
    assertEquals(1, cache.getCurrentSize());
  }

  @Test
  public void testGetCurrentSizeChangesIfItemsAreEvictedWhenSizeMultiplierChanges() {
    for (int i = 0; i < SIZE; i++) {
      cache.put(getKey(), new Object());
    }
    assertEquals(SIZE, cache.getCurrentSize());
    cache.setSizeMultiplier(0.5f);
    assertEquals(SIZE / 2, cache.getCurrentSize());
  }

  private String getKey() {
    currentKey += "1";
    return currentKey;
  }

  private interface CacheListener {
    void onItemRemoved(Object item);

    int getSize(Object item);
  }

  private static class TestLruCache extends LruCache<String, Object> {
    private final CacheListener listener;

    TestLruCache(int size, CacheListener listener) {
      super(size);
      this.listener = listener;
    }

    @Override
    protected void onItemEvicted(@NonNull String key, @Nullable Object item) {
      listener.onItemRemoved(item);
    }

    @Override
    protected int getSize(@Nullable Object item) {
      return listener.getSize(item);
    }
  }
}
