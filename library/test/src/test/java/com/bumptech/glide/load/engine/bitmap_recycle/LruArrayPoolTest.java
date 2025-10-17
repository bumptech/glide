package com.bumptech.glide.load.engine.bitmap_recycle;

import static android.content.ComponentCallbacks2.TRIM_MEMORY_BACKGROUND;
import static android.content.ComponentCallbacks2.TRIM_MEMORY_COMPLETE;
import static android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL;
import static android.content.ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN;
import static com.bumptech.glide.RobolectricConstants.ROBOLECTRIC_SDK;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = ROBOLECTRIC_SDK)
public class LruArrayPoolTest {
  private static final int MAX_SIZE = 10;
  private static final int MAX_PUT_SIZE = MAX_SIZE / 2;
  private static final Class<byte[]> ARRAY_CLASS = byte[].class;
  private static final ArrayAdapterInterface<byte[]> ADAPTER = new ByteArrayAdapter();
  private LruArrayPool pool;

  @Before
  public void setUp() {
    pool = new LruArrayPool(MAX_SIZE);
  }

  @Test
  public void testNewPoolIsEmpty() {
    assertEquals(pool.getCurrentSize(), 0);
  }

  @Test
  public void testICanAddAndGetValidArray() {
    int size = 758;
    int value = 564;
    fillPool(pool, size - 1, value);
    pool.put(createArray(ARRAY_CLASS, size, value));
    Object array = pool.get(size, ARRAY_CLASS);
    assertNotNull(array);
    assertTrue(array.getClass() == ARRAY_CLASS);
    assertTrue(ADAPTER.getArrayLength((byte[]) array) >= size);
    assertTrue(((byte[]) array)[0] == (byte) 0);
  }

  @Test
  public void testItIsSizeLimited() {
    fillPool(pool, MAX_SIZE / ADAPTER.getElementSizeInBytes() + 1, 1);
    assertTrue(pool.getCurrentSize() <= MAX_SIZE);
  }

  @Test
  public void testArrayLargerThanPoolIsNotAdded() {
    pool = new LruArrayPool(MAX_SIZE);
    pool.put(createArray(ARRAY_CLASS, MAX_SIZE / ADAPTER.getElementSizeInBytes() + 1, 0));
    assertEquals(0, pool.getCurrentSize());
  }

  @Test
  public void testClearMemoryRemovesAllArrays() {
    fillPool(pool, MAX_SIZE / ADAPTER.getElementSizeInBytes() + 1, 0);
    pool.clearMemory();
    assertEquals(0, pool.getCurrentSize());
  }

  @Test
  public void testTrimMemoryUiHiddenOrLessRemovesHalfOfArrays() {
    testTrimMemory(MAX_SIZE, TRIM_MEMORY_UI_HIDDEN, MAX_SIZE / 2);
  }

  @Test
  public void testTrimMemoryRunningCriticalRemovesHalfOfBitmaps() {
    testTrimMemory(MAX_SIZE, TRIM_MEMORY_RUNNING_CRITICAL, MAX_SIZE / 2);
  }

  @Test
  public void testTrimMemoryUiHiddenOrLessRemovesNoArraysIfPoolLessThanHalfFull() {
    testTrimMemory(MAX_SIZE / 2, TRIM_MEMORY_UI_HIDDEN, MAX_SIZE / 2);
  }

  @Test
  public void testTrimMemoryBackgroundOrGreaterRemovesAllArrays() {
    for (int trimLevel : new int[] {TRIM_MEMORY_BACKGROUND, TRIM_MEMORY_COMPLETE}) {
      testTrimMemory(MAX_SIZE, trimLevel, 0);
    }
  }

  @Test
  public void get_withEmptyPool_returnsExactArray() {
    assertThat(pool.get(MAX_PUT_SIZE, byte[].class)).hasLength(MAX_PUT_SIZE);
  }

  @Test
  public void get_withPoolContainingLargerArray_returnsLargerArray() {
    byte[] expected = new byte[MAX_PUT_SIZE];
    pool.put(expected);
    assertThat(pool.get(MAX_PUT_SIZE - 1, byte[].class)).isSameInstanceAs(expected);
  }

  @Test
  public void get_withPoolContainingSmallerArray_returnsExactArray() {
    pool.put(new byte[MAX_PUT_SIZE - 1]);
    assertThat(pool.get(MAX_PUT_SIZE, byte[].class)).hasLength(MAX_PUT_SIZE);
  }

  @Test
  public void get_withPoolLessThanHalfFull_returnsFromPools() {
    int size = MAX_SIZE / 2;
    byte[] expected = new byte[size];
    pool.put(expected);
    assertThat(pool.get(1, byte[].class)).isSameInstanceAs(expected);
  }

  @Test
  public void get_withPoolMoreThanHalfFull_sizeMoreThanHalfArrayInPool_returnsArray() {
    Set<byte[]> expected = new HashSet<>();
    for (int i = 0; i < 3; i++) {
      byte[] toPut = new byte[MAX_SIZE / 3];
      expected.add(toPut);
      pool.put(toPut);
    }
    byte[] received = pool.get(2, byte[].class);
    assertThat(expected).contains(received);
  }

  @Test
  public void get_withPoolMoreThanHalfFull_sizeLessThanHalfArrayInPool_returnsNewArray() {
    pool = new LruArrayPool(100);
    for (int i = 0; i < 3; i++) {
      byte[] toPut = new byte[100 / 3];
      pool.put(toPut);
    }
    int requestedSize = 100 / 3 / LruArrayPool.MAX_OVER_SIZE_MULTIPLE;
    byte[] received = pool.get(requestedSize, byte[].class);
    assertThat(received).hasLength(requestedSize);
  }

  @Test
  public void getExact_withEmptyPool_returnsExactArray() {
    byte[] result = pool.getExact(MAX_PUT_SIZE, byte[].class);
    assertThat(result).hasLength(MAX_PUT_SIZE);
  }

  @Test
  public void getExact_withPoolContainingLargerArray_returnsExactArray() {
    pool.put(new byte[MAX_PUT_SIZE]);
    int expectedSize = MAX_PUT_SIZE - 1;
    assertThat(pool.getExact(expectedSize, byte[].class)).hasLength(expectedSize);
  }

  @Test
  public void getExact_withPoolContainingSmallerArray_returnsExactArray() {
    pool.put(new byte[MAX_PUT_SIZE - 1]);
    assertThat(pool.getExact(MAX_PUT_SIZE, byte[].class)).hasLength(MAX_PUT_SIZE);
  }

  @Test
  public void getExact_withPoolContainingExactArray_returnsArray() {
    byte[] expected = new byte[MAX_PUT_SIZE];
    pool.put(expected);
    assertThat(pool.getExact(MAX_PUT_SIZE, byte[].class)).isSameInstanceAs(expected);
  }

  @Test
  public void put_withArrayMoreThanHalfPoolSize_doesNotRetainArray() {
    int targetSize = (MAX_SIZE / 2) + 1;
    byte[] toPut = new byte[targetSize];
    pool.put(toPut);
    assertThat(pool.getCurrentSize()).isEqualTo(0);
    assertThat(pool.get(targetSize, byte[].class)).isNotSameInstanceAs(toPut);
  }

  private void testTrimMemory(int fillSize, int trimLevel, int expectedSize) {
    pool = new LruArrayPool(MAX_SIZE);
    fillPool(pool, fillSize / ADAPTER.getElementSizeInBytes(), 1);
    pool.trimMemory(trimLevel);
    assertEquals("Failed level=" + trimLevel, expectedSize, pool.getCurrentSize());
  }

  private void fillPool(LruArrayPool pool, int arrayCount, int arrayLength) {
    for (int i = 0; i < arrayCount; i++) {
      pool.put(createArray(ARRAY_CLASS, arrayLength, 10));
    }
  }

  @SuppressWarnings("unchecked")
  private static <T> T createArray(Class<T> type, int size, int value) {
    Object array = null;
    if (type.equals(int[].class)) {
      array = new int[size];
      Arrays.fill((int[]) array, value);
    } else if (type.equals(byte[].class)) {
      array = new byte[size];
      Arrays.fill((byte[]) array, (byte) value);
    }
    return (T) array;
  }
}
