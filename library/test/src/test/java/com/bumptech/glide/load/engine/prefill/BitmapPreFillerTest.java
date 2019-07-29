package com.bumptech.glide.load.engine.prefill;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.graphics.Bitmap;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.engine.cache.MemoryCache;
import com.bumptech.glide.tests.Util.CreateBitmap;
import com.bumptech.glide.util.Util;
import com.google.common.collect.Range;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 18)
public class BitmapPreFillerTest {
  private static final int DEFAULT_BITMAP_WIDTH = 100;
  private static final int DEFAULT_BITMAP_HEIGHT = 50;

  private static final int BITMAPS_IN_POOL = 10;
  private static final int BITMAPS_IN_CACHE = 10;

  private final Bitmap.Config defaultBitmapConfig = PreFillType.DEFAULT_CONFIG;
  private final Bitmap defaultBitmap =
      Bitmap.createBitmap(DEFAULT_BITMAP_WIDTH, DEFAULT_BITMAP_HEIGHT, defaultBitmapConfig);
  private final long defaultBitmapSize = Util.getBitmapByteSize(defaultBitmap);
  private final long poolSize = BITMAPS_IN_CACHE * defaultBitmapSize;
  private final long cacheSize = BITMAPS_IN_POOL * defaultBitmapSize;

  @Mock private BitmapPool pool;
  @Mock private MemoryCache cache;
  private BitmapPreFiller bitmapPreFiller;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    when(pool.getMaxSize()).thenReturn(poolSize);
    when(pool.getDirty(anyInt(), anyInt(), any(Bitmap.Config.class)))
        .thenAnswer(new CreateBitmap());
    when(cache.getMaxSize()).thenReturn(cacheSize);

    bitmapPreFiller = new BitmapPreFiller(cache, pool, DecodeFormat.DEFAULT);
  }

  @Test
  public void testAllocationOrderContainsEnoughSizesToFillPoolAndMemoryCache() {
    PreFillQueue allocationOrder =
        bitmapPreFiller.generateAllocationOrder(
            new PreFillType.Builder(DEFAULT_BITMAP_WIDTH, DEFAULT_BITMAP_HEIGHT)
                .setConfig(defaultBitmapConfig)
                .build());

    assertEquals(BITMAPS_IN_POOL + BITMAPS_IN_CACHE, allocationOrder.getSize());
  }

  @Test
  public void testAllocationOrderThatDoesNotFitExactlyIntoGivenSizeRoundsDown() {
    PreFillType[] sizes =
        new PreFillType[] {
          new PreFillType.Builder(DEFAULT_BITMAP_WIDTH, DEFAULT_BITMAP_HEIGHT)
              .setConfig(defaultBitmapConfig)
              .build(),
          new PreFillType.Builder(DEFAULT_BITMAP_WIDTH / 2, DEFAULT_BITMAP_HEIGHT)
              .setConfig(defaultBitmapConfig)
              .build(),
          new PreFillType.Builder(DEFAULT_BITMAP_WIDTH, DEFAULT_BITMAP_HEIGHT / 2)
              .setConfig(defaultBitmapConfig)
              .build(),
        };
    PreFillQueue allocationOrder = bitmapPreFiller.generateAllocationOrder(sizes);

    int byteSize = 0;
    while (!allocationOrder.isEmpty()) {
      PreFillType current = allocationOrder.remove();
      byteSize +=
          Util.getBitmapByteSize(current.getWidth(), current.getHeight(), current.getConfig());
    }

    int expectedSize = 0;
    long maxSize = poolSize + cacheSize;
    for (PreFillType current : sizes) {
      int currentSize =
          Util.getBitmapByteSize(current.getWidth(), current.getHeight(), current.getConfig());
      // See https://errorprone.info/bugpattern/NarrowingCompoundAssignment.
      expectedSize = (int) (expectedSize + (currentSize * (maxSize / (3 * currentSize))));
    }

    assertEquals(expectedSize, byteSize);
  }

  @Test
  public void testAllocationOrderDoesNotOverFillWithMultipleSizes() {
    PreFillQueue allocationOrder =
        bitmapPreFiller.generateAllocationOrder(
            new PreFillType.Builder(DEFAULT_BITMAP_WIDTH, DEFAULT_BITMAP_HEIGHT)
                .setConfig(defaultBitmapConfig)
                .build(),
            new PreFillType.Builder(DEFAULT_BITMAP_WIDTH / 2, DEFAULT_BITMAP_HEIGHT)
                .setConfig(defaultBitmapConfig)
                .build(),
            new PreFillType.Builder(DEFAULT_BITMAP_WIDTH, DEFAULT_BITMAP_HEIGHT / 2)
                .setConfig(defaultBitmapConfig)
                .build());

    long byteSize = 0;
    while (!allocationOrder.isEmpty()) {
      PreFillType current = allocationOrder.remove();
      byteSize +=
          Util.getBitmapByteSize(current.getWidth(), current.getHeight(), current.getConfig());
    }

    assertThat(byteSize).isIn(Range.atMost(poolSize + cacheSize));
  }

  @Test
  public void testAllocationOrderDoesNotOverFillWithMultipleSizesAndWeights() {
    PreFillQueue allocationOrder =
        bitmapPreFiller.generateAllocationOrder(
            new PreFillType.Builder(DEFAULT_BITMAP_WIDTH, DEFAULT_BITMAP_HEIGHT)
                .setConfig(defaultBitmapConfig)
                .setWeight(4)
                .build(),
            new PreFillType.Builder(DEFAULT_BITMAP_WIDTH / 2, DEFAULT_BITMAP_HEIGHT)
                .setConfig(defaultBitmapConfig)
                .build(),
            new PreFillType.Builder(DEFAULT_BITMAP_WIDTH, DEFAULT_BITMAP_HEIGHT / 3)
                .setConfig(defaultBitmapConfig)
                .setWeight(3)
                .build());

    long byteSize = 0;
    while (!allocationOrder.isEmpty()) {
      PreFillType current = allocationOrder.remove();
      byteSize +=
          Util.getBitmapByteSize(current.getWidth(), current.getHeight(), current.getConfig());
    }

    assertThat(byteSize).isIn(Range.atMost(poolSize + cacheSize));
  }

  @Test
  public void testAllocationOrderContainsSingleSizeIfSingleSizeIsProvided() {
    PreFillQueue allocationOrder =
        bitmapPreFiller.generateAllocationOrder(
            new PreFillType.Builder(DEFAULT_BITMAP_WIDTH, DEFAULT_BITMAP_HEIGHT)
                .setConfig(defaultBitmapConfig)
                .build());

    while (!allocationOrder.isEmpty()) {
      PreFillType size = allocationOrder.remove();
      assertEquals(DEFAULT_BITMAP_WIDTH, size.getWidth());
      assertEquals(DEFAULT_BITMAP_HEIGHT, size.getHeight());
      assertEquals(defaultBitmapConfig, size.getConfig());
    }
  }

  @Test
  public void testAllocationOrderSplitsEvenlyBetweenEqualSizesWithEqualWeights() {
    PreFillType smallWidth =
        new PreFillType.Builder(DEFAULT_BITMAP_WIDTH / 2, DEFAULT_BITMAP_HEIGHT)
            .setConfig(defaultBitmapConfig)
            .build();
    PreFillType smallHeight =
        new PreFillType.Builder(DEFAULT_BITMAP_WIDTH, DEFAULT_BITMAP_HEIGHT / 2)
            .setConfig(defaultBitmapConfig)
            .build();
    PreFillQueue allocationOrder = bitmapPreFiller.generateAllocationOrder(smallWidth, smallHeight);

    int numSmallWidth = 0, numSmallHeight = 0;
    while (!allocationOrder.isEmpty()) {
      PreFillType current = allocationOrder.remove();
      if (smallWidth.equals(current)) {
        numSmallWidth++;
      } else if (smallHeight.equals(current)) {
        numSmallHeight++;
      } else {
        fail("Unexpected size, size: " + current);
      }
    }

    assertEquals(numSmallWidth, numSmallHeight);
  }

  @Test
  public void testAllocationOrderSplitsByteSizeEvenlyBetweenUnEqualSizesWithEqualWeights() {
    PreFillType smallWidth =
        new PreFillType.Builder(DEFAULT_BITMAP_WIDTH / 2, DEFAULT_BITMAP_HEIGHT)
            .setConfig(defaultBitmapConfig)
            .build();
    PreFillType normal =
        new PreFillType.Builder(DEFAULT_BITMAP_WIDTH, DEFAULT_BITMAP_HEIGHT)
            .setConfig(defaultBitmapConfig)
            .build();
    PreFillQueue allocationOrder = bitmapPreFiller.generateAllocationOrder(smallWidth, normal);

    int numSmallWidth = 0, numNormal = 0;
    while (!allocationOrder.isEmpty()) {
      PreFillType current = allocationOrder.remove();
      if (smallWidth.equals(current)) {
        numSmallWidth++;
      } else if (normal.equals(current)) {
        numNormal++;
      } else {
        fail("Unexpected size, size: " + current);
      }
    }

    assertEquals(2 * numNormal, numSmallWidth);
  }

  @Test
  public void testAllocationOrderSplitsByteSizeUnevenlyBetweenEqualSizesWithUnequalWeights() {
    PreFillType doubleWeight =
        new PreFillType.Builder(DEFAULT_BITMAP_WIDTH / 2, DEFAULT_BITMAP_HEIGHT)
            .setConfig(defaultBitmapConfig)
            .setWeight(2)
            .build();
    PreFillType normal =
        new PreFillType.Builder(DEFAULT_BITMAP_WIDTH, DEFAULT_BITMAP_HEIGHT / 2)
            .setConfig(defaultBitmapConfig)
            .build();
    PreFillQueue allocationOrder = bitmapPreFiller.generateAllocationOrder(doubleWeight, normal);

    int numDoubleWeight = 0, numNormal = 0;
    while (!allocationOrder.isEmpty()) {
      PreFillType current = allocationOrder.remove();
      if (doubleWeight.equals(current)) {
        numDoubleWeight++;
      } else if (normal.equals(current)) {
        numNormal++;
      } else {
        fail("Unexpected size, size: " + current);
      }
    }

    assertEquals(2 * numNormal, numDoubleWeight);
  }

  @Test
  public void testAllocationOrderRoundRobinsDifferentSizes() {
    when(pool.getMaxSize()).thenReturn(defaultBitmapSize);
    when(cache.getMaxSize()).thenReturn(defaultBitmapSize);
    PreFillType smallWidth =
        new PreFillType.Builder(DEFAULT_BITMAP_WIDTH / 2, DEFAULT_BITMAP_HEIGHT)
            .setConfig(defaultBitmapConfig)
            .build();
    PreFillType smallHeight =
        new PreFillType.Builder(DEFAULT_BITMAP_WIDTH, DEFAULT_BITMAP_HEIGHT / 2)
            .setConfig(defaultBitmapConfig)
            .build();

    PreFillQueue allocationOrder = bitmapPreFiller.generateAllocationOrder(smallWidth, smallHeight);

    List<PreFillType> attributes = new ArrayList<>();
    while (!allocationOrder.isEmpty()) {
      attributes.add(allocationOrder.remove());
    }

    // Either width, height, width, height or height, width, height, width.
    try {
      assertThat(attributes)
          .containsExactly(smallWidth, smallHeight, smallWidth, smallHeight)
          .inOrder();
    } catch (AssertionError e) {
      assertThat(attributes)
          .containsExactly(smallHeight, smallWidth, smallHeight, smallWidth)
          .inOrder();
    }
  }

  @Test
  @SuppressWarnings("deprecation")
  public void testSetsConfigOnBuildersToDefaultIfNotSet() {
    PreFillType.Builder builder = mock(PreFillType.Builder.class);
    when(builder.build())
        .thenReturn(new PreFillType.Builder(100).setConfig(Bitmap.Config.RGB_565).build());

    bitmapPreFiller.preFill(builder);

    InOrder order = inOrder(builder);
    order
        .verify(builder)
        .setConfig(
            DecodeFormat.DEFAULT == DecodeFormat.PREFER_ARGB_8888
                ? Bitmap.Config.ARGB_8888
                : Bitmap.Config.RGB_565);
    order.verify(builder).build();
  }

  @Test
  public void testDoesNotSetConfigOnBuildersIfConfigIsAlreadySet() {
    PreFillType.Builder builder = mock(PreFillType.Builder.class);

    when(builder.getConfig()).thenReturn(Bitmap.Config.ARGB_4444);
    when(builder.build())
        .thenReturn(new PreFillType.Builder(100).setConfig(Bitmap.Config.ARGB_4444).build());
    bitmapPreFiller.preFill(builder);

    verify(builder, never()).setConfig(any(Bitmap.Config.class));
  }
}
