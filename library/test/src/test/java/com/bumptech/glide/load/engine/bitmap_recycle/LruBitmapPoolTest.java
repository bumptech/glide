package com.bumptech.glide.load.engine.bitmap_recycle;

import static android.content.ComponentCallbacks2.TRIM_MEMORY_BACKGROUND;
import static android.content.ComponentCallbacks2.TRIM_MEMORY_COMPLETE;
import static android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL;
import static android.content.ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.graphics.Bitmap;
import android.os.Build;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class LruBitmapPoolTest {
  private static final int MAX_SIZE = 10;
  private static final Set<Bitmap.Config> ALLOWED_CONFIGS =
      Collections.singleton(Bitmap.Config.ARGB_8888);
  private MockStrategy strategy;
  private LruBitmapPool pool;

  @Before
  public void setUp() throws Exception {
    strategy = new MockStrategy();
    pool = new LruBitmapPool(MAX_SIZE, strategy, ALLOWED_CONFIGS);
  }

  @Test
  public void testICanAddAndGetABitmap() {
    fillPool(pool, 1);
    pool.put(createMutableBitmap());
    assertNotNull(pool.get(100, 100, Bitmap.Config.ARGB_8888));
  }

  @Test
  public void testImmutableBitmapsAreNotAdded() {
    Bitmap bitmap = createMutableBitmap();
    Shadows.shadowOf(bitmap).setMutable(false);
    pool.put(bitmap);
    assertThat(strategy.bitmaps).isEmpty();
  }

  @Test
  public void testItIsSizeLimited() {
    fillPool(pool, MAX_SIZE + 2);
    assertEquals(2, strategy.numRemoves);
  }

  @Test
  public void testBitmapLargerThanPoolIsNotAdded() {
    strategy =
        new MockStrategy() {
          @Override
          public int getSize(Bitmap bitmap) {
            return 4;
          }
        };
    pool = new LruBitmapPool(3, strategy, ALLOWED_CONFIGS);
    pool.put(createMutableBitmap());
    assertEquals(0, strategy.numRemoves);
    assertEquals(0, strategy.numPuts);
  }

  @Test
  public void testClearMemoryRemovesAllBitmaps() {
    fillPool(pool, MAX_SIZE);
    pool.clearMemory();

    assertEquals(MAX_SIZE, strategy.numRemoves);
  }

  @Test
  public void testEvictedBitmapsAreRecycled() {
    fillPool(pool, MAX_SIZE);
    List<Bitmap> bitmaps = new ArrayList<>(MAX_SIZE);
    bitmaps.addAll(strategy.bitmaps);

    pool.clearMemory();

    for (Bitmap b : bitmaps) {
      assertTrue(b.isRecycled());
    }
  }

  @Config(sdk = Build.VERSION_CODES.KITKAT)
  @Test
  public void testTrimMemoryUiHiddenOrLessRemovesHalfOfBitmaps_preM() {
    testTrimMemory(MAX_SIZE, TRIM_MEMORY_UI_HIDDEN, MAX_SIZE / 2);
  }

  @Config(sdk = Build.VERSION_CODES.M)
  @Test
  public void testTrimMemoryUiHiddenOrLessRemovesHalfOfBitmaps_postM() {
    testTrimMemory(MAX_SIZE, TRIM_MEMORY_UI_HIDDEN, 0);
  }

  @Test
  public void testTrimMemoryRunningCriticalRemovesHalfOfBitmaps() {
    testTrimMemory(MAX_SIZE, TRIM_MEMORY_RUNNING_CRITICAL, MAX_SIZE / 2);
  }

  @Test
  public void testTrimMemoryRunningCriticalOrLessRemovesNoBitmapsIfPoolLessThanHalfFull() {
    testTrimMemory(MAX_SIZE / 2, TRIM_MEMORY_RUNNING_CRITICAL, MAX_SIZE / 2);
  }

  @Test
  public void testTrimMemoryBackgroundOrGreaterRemovesAllBitmaps() {
    for (int trimLevel : new int[] {TRIM_MEMORY_BACKGROUND, TRIM_MEMORY_COMPLETE}) {
      testTrimMemory(MAX_SIZE, trimLevel, 0);
    }
  }

  @Test
  public void testPassesArgb888ToStrategyAsConfigForRequestsWithNullConfigsOnGet() {
    LruPoolStrategy strategy = mock(LruPoolStrategy.class);
    LruBitmapPool pool = new LruBitmapPool(100, strategy, ALLOWED_CONFIGS);

    Bitmap expected = createMutableBitmap();
    when(strategy.get(anyInt(), anyInt(), eq(Bitmap.Config.ARGB_8888))).thenReturn(expected);
    Bitmap result = pool.get(100, 100, null);

    assertEquals(expected, result);
  }

  @Test
  public void testPassesArgb8888ToStrategyAsConfigForRequestsWithNullConfigsOnGetDirty() {
    LruPoolStrategy strategy = mock(LruPoolStrategy.class);
    LruBitmapPool pool = new LruBitmapPool(100, strategy, ALLOWED_CONFIGS);

    Bitmap expected = createMutableBitmap();
    when(strategy.get(anyInt(), anyInt(), eq(Bitmap.Config.ARGB_8888))).thenReturn(expected);
    Bitmap result = pool.getDirty(100, 100, null);

    assertEquals(expected, result);
  }

  @Test
  public void get_withNullConfig_andEmptyPool_returnsNewArgb8888Bitmap() {
    Bitmap result = pool.get(100, 100, /*config=*/ null);
    assertThat(result.getConfig()).isEqualTo(Bitmap.Config.ARGB_8888);
  }

  @Test
  public void getDirty_withNullConfig_andEmptyPool_returnsNewArgb8888Bitmap() {
    Bitmap result = pool.getDirty(100, 100, /*config=*/ null);
    assertThat(result.getConfig()).isEqualTo(Bitmap.Config.ARGB_8888);
  }

  private void testTrimMemory(int fillSize, int trimLevel, int expectedSize) {
    MockStrategy strategy = new MockStrategy();
    LruBitmapPool pool = new LruBitmapPool(MAX_SIZE, strategy, ALLOWED_CONFIGS);
    fillPool(pool, fillSize);
    pool.trimMemory(trimLevel);
    assertEquals("Failed level=" + trimLevel, expectedSize, strategy.bitmaps.size());
  }

  @Test
  public void testCanIncreaseSizeDynamically() {
    int sizeMultiplier = 2;
    pool.setSizeMultiplier(2);
    fillPool(pool, MAX_SIZE * sizeMultiplier);

    assertEquals(0, strategy.numRemoves);
  }

  @Test
  public void testCanDecreaseSizeDynamically() {
    fillPool(pool, MAX_SIZE);
    assertEquals(0, strategy.numRemoves);

    float sizeMultiplier = 0.5f;
    pool.setSizeMultiplier(sizeMultiplier);

    assertEquals(Math.round(MAX_SIZE * sizeMultiplier), strategy.numRemoves);
  }

  @Test
  public void testCanResetSizeDynamically() {
    int sizeMultiplier = 2;
    pool.setSizeMultiplier(sizeMultiplier);
    fillPool(pool, MAX_SIZE * sizeMultiplier);

    pool.setSizeMultiplier(1);

    assertEquals(MAX_SIZE * sizeMultiplier - MAX_SIZE, strategy.numRemoves);
  }

  @Test
  public void testCanGetCurrentMaxSize() {
    assertEquals(MAX_SIZE, pool.getMaxSize());
  }

  @Test
  public void testMaxSizeChangesAfterSizeMultiplier() {
    pool.setSizeMultiplier(2);
    assertEquals(2 * MAX_SIZE, pool.getMaxSize());
  }

  @Test
  public void testBitmapsWithDisallowedConfigsAreIgnored() {
    pool = new LruBitmapPool(100, strategy, Collections.singleton(Bitmap.Config.ARGB_4444));

    Bitmap bitmap = createMutableBitmap(Bitmap.Config.RGB_565);
    pool.put(bitmap);

    assertEquals(0, strategy.numPuts);
  }

  @Test
  @Config(sdk = 19)
  public void testBitmapsWithAllowedNullConfigsAreAllowed() {
    pool = new LruBitmapPool(100, strategy, Collections.<Bitmap.Config>singleton(null));

    Bitmap bitmap = createMutableBitmap();
    bitmap.setConfig(null);

    pool.put(bitmap);

    assertEquals(1, strategy.numPuts);
  }

  private void fillPool(LruBitmapPool pool, int fillCount) {
    for (int i = 0; i < fillCount; i++) {
      pool.put(createMutableBitmap());
    }
  }

  private Bitmap createMutableBitmap() {
    return createMutableBitmap(Bitmap.Config.ARGB_8888);
  }

  private Bitmap createMutableBitmap(Bitmap.Config config) {
    Bitmap bitmap = Bitmap.createBitmap(100, 100, config);
    Shadows.shadowOf(bitmap).setMutable(true);
    return bitmap;
  }

  private static class MockStrategy implements LruPoolStrategy {
    private final ArrayDeque<Bitmap> bitmaps = new ArrayDeque<>();
    private int numRemoves;
    private int numPuts;

    @Override
    public void put(Bitmap bitmap) {
      numPuts++;
      bitmaps.add(bitmap);
    }

    @Override
    public Bitmap get(int width, int height, Bitmap.Config config) {
      return bitmaps.isEmpty() ? null : bitmaps.removeLast();
    }

    @Override
    public Bitmap removeLast() {
      numRemoves++;
      return bitmaps.removeLast();
    }

    @Override
    public String logBitmap(Bitmap bitmap) {
      return null;
    }

    @Override
    public String logBitmap(int width, int height, Bitmap.Config config) {
      return null;
    }

    @Override
    public int getSize(Bitmap bitmap) {
      return 1;
    }
  }
}
