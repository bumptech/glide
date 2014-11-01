package com.bumptech.glide.load.engine.prefill;

import android.graphics.Bitmap;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.engine.cache.MemoryCache;
import com.bumptech.glide.util.Util;
import org.hamcrest.core.CombinableMatcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.either;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, emulateSdk = 18)
public class BitmapPreFillerAllocationTest {
    private static final int DEFAULT_BITMAP_WIDTH = 100;
    private static final int DEFAULT_BITMAP_HEIGHT = 50;
    private static final Bitmap.Config DEFAULT_BITMAP_CONFIG = PreFillType.DEFAULT_CONFIG;
    private static final Bitmap DEFAULT_BITMAP =
            Bitmap.createBitmap(DEFAULT_BITMAP_WIDTH, DEFAULT_BITMAP_HEIGHT, DEFAULT_BITMAP_CONFIG);
    private static final int DEFAULT_BITMAP_SIZE = Util.getBitmapByteSize(DEFAULT_BITMAP);

    private static final int DEFAULT_BITMAPS_IN_POOL = 10;
    private static final int DEFAULT_BITMAPS_IN_CACHE = 10;
    private static final int POOL_SIZE = DEFAULT_BITMAPS_IN_POOL * DEFAULT_BITMAP_SIZE;
    private static final int CACHE_SIZE = DEFAULT_BITMAPS_IN_CACHE * DEFAULT_BITMAP_SIZE;

    private BitmapPool pool;
    private BitmapPreFiller bitmapPreFiller;
    private MemoryCache cache;

    @Before
    public void setUp() {
        pool = mock(BitmapPool.class);
        when(pool.getMaxSize()).thenReturn(POOL_SIZE);
        cache = mock(MemoryCache.class);
        when(cache.getMaxSize()).thenReturn(CACHE_SIZE);

        bitmapPreFiller = new BitmapPreFiller(cache, pool, DecodeFormat.DEFAULT);
    }

    @Test
    public void testAllocationOrderContainsEnoughSizesToFillPoolAndMemoryCache() {
        PreFillQueue allocationOrder = bitmapPreFiller.generateAllocationOrder(
                new PreFillType[] {
                        new PreFillType.Builder(DEFAULT_BITMAP_WIDTH, DEFAULT_BITMAP_HEIGHT)
                                .setConfig(DEFAULT_BITMAP_CONFIG)
                                .build()
                }
        );

        assertEquals(DEFAULT_BITMAPS_IN_POOL + DEFAULT_BITMAPS_IN_CACHE, allocationOrder.getSize());
    }

    @Test
    public void testAllocationOrderThatDoesNotFitExactlyIntoGivenSizeRoundsDown() {
        PreFillType[] sizes = new PreFillType[] {
                new PreFillType.Builder(DEFAULT_BITMAP_WIDTH, DEFAULT_BITMAP_HEIGHT)
                        .setConfig(DEFAULT_BITMAP_CONFIG)
                        .build(),
                new PreFillType.Builder(DEFAULT_BITMAP_WIDTH / 2, DEFAULT_BITMAP_HEIGHT)
                        .setConfig(DEFAULT_BITMAP_CONFIG)
                        .build(),
                new PreFillType.Builder(DEFAULT_BITMAP_WIDTH, DEFAULT_BITMAP_HEIGHT / 2)
                        .setConfig(DEFAULT_BITMAP_CONFIG)
                        .build(),
        };
        PreFillQueue allocationOrder = bitmapPreFiller.generateAllocationOrder(sizes);

        int byteSize = 0;
        while (!allocationOrder.isEmpty()) {
            PreFillType current = allocationOrder.remove();
            byteSize += Util.getBitmapByteSize(current.getWidth(), current.getHeight(), current.getConfig());
        }

        int expectedSize = 0;
        int maxSize = POOL_SIZE + CACHE_SIZE;
        for (PreFillType current : sizes) {
            int currentSize = Util.getBitmapByteSize(current.getWidth(), current.getHeight(), current.getConfig());
            expectedSize += currentSize * (maxSize / (3 * currentSize));
        }

        assertEquals(expectedSize, byteSize);
    }

    @Test
    public void testAllocationOrderDoesNotOverFillWithMultipleSizes() {
        PreFillQueue allocationOrder = bitmapPreFiller.generateAllocationOrder(
                new PreFillType[] {
                        new PreFillType.Builder(DEFAULT_BITMAP_WIDTH, DEFAULT_BITMAP_HEIGHT)
                                .setConfig(DEFAULT_BITMAP_CONFIG)
                                .build(),
                        new PreFillType.Builder(DEFAULT_BITMAP_WIDTH / 2, DEFAULT_BITMAP_HEIGHT)
                                .setConfig(DEFAULT_BITMAP_CONFIG)
                                .build(),
                        new PreFillType.Builder(DEFAULT_BITMAP_WIDTH, DEFAULT_BITMAP_HEIGHT / 2)
                                .setConfig(DEFAULT_BITMAP_CONFIG)
                                .build()
                }
        );

        int byteSize = 0;
        while (!allocationOrder.isEmpty()) {
            PreFillType current = allocationOrder.remove();
            byteSize += Util.getBitmapByteSize(current.getWidth(), current.getHeight(), current.getConfig());
        }

        assertThat(byteSize, lessThanOrEqualTo(POOL_SIZE + CACHE_SIZE));
    }

    @Test
    public void testAllocationOrderDoesNotOverFillWithMultipleSizesAndWeights() {
        PreFillQueue allocationOrder = bitmapPreFiller.generateAllocationOrder(
                new PreFillType[]{
                        new PreFillType.Builder(DEFAULT_BITMAP_WIDTH, DEFAULT_BITMAP_HEIGHT)
                                .setConfig(DEFAULT_BITMAP_CONFIG)
                                .setWeight(4)
                                .build(),
                        new PreFillType.Builder(DEFAULT_BITMAP_WIDTH / 2, DEFAULT_BITMAP_HEIGHT)
                                .setConfig(DEFAULT_BITMAP_CONFIG)
                                .build(),
                        new PreFillType.Builder(DEFAULT_BITMAP_WIDTH, DEFAULT_BITMAP_HEIGHT / 3)
                                .setConfig(DEFAULT_BITMAP_CONFIG)
                                .setWeight(3)
                                .build()
                }
        );

        int byteSize = 0;
        while (!allocationOrder.isEmpty()) {
            PreFillType current = allocationOrder.remove();
            byteSize += Util.getBitmapByteSize(current.getWidth(), current.getHeight(), current.getConfig());
        }

        assertThat(byteSize, lessThanOrEqualTo(POOL_SIZE + CACHE_SIZE));
    }

    @Test
    public void testAllocationOrderContainsSingleSizeIfSingleSizeIsProvided() {
        PreFillQueue allocationOrder = bitmapPreFiller.generateAllocationOrder(
                new PreFillType[] {
                    new PreFillType.Builder(DEFAULT_BITMAP_WIDTH, DEFAULT_BITMAP_HEIGHT)
                            .setConfig(DEFAULT_BITMAP_CONFIG)
                            .build()
                }
        );

        while (!allocationOrder.isEmpty()) {
            PreFillType size = allocationOrder.remove();
            assertEquals(DEFAULT_BITMAP_WIDTH, size.getWidth());
            assertEquals(DEFAULT_BITMAP_HEIGHT, size.getHeight());
            assertEquals(DEFAULT_BITMAP_CONFIG, size.getConfig());
        }
    }

    @Test
    public void testAllocationOrderSplitsEvenlyBetweenEqualSizesWithEqualWeights() {
        PreFillType smallWidth = new PreFillType.Builder(DEFAULT_BITMAP_WIDTH / 2, DEFAULT_BITMAP_HEIGHT)
                .setConfig(DEFAULT_BITMAP_CONFIG)
                .build();
        PreFillType smallHeight =
                new PreFillType.Builder(DEFAULT_BITMAP_WIDTH, DEFAULT_BITMAP_HEIGHT / 2)
                        .setConfig(DEFAULT_BITMAP_CONFIG)
                        .build();
        PreFillQueue allocationOrder = bitmapPreFiller.generateAllocationOrder(
                new PreFillType[] { smallWidth, smallHeight, }
        );

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
                        .setConfig(DEFAULT_BITMAP_CONFIG)
                        .build();
        PreFillType normal =
                new PreFillType.Builder(DEFAULT_BITMAP_WIDTH, DEFAULT_BITMAP_HEIGHT)
                        .setConfig(DEFAULT_BITMAP_CONFIG)
                        .build();
        PreFillQueue allocationOrder = bitmapPreFiller.generateAllocationOrder(
                new PreFillType[] { smallWidth, normal }
        );

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
                        .setConfig(DEFAULT_BITMAP_CONFIG)
                        .setWeight(2)
                        .build();
        PreFillType normal = new PreFillType.Builder(DEFAULT_BITMAP_WIDTH, DEFAULT_BITMAP_HEIGHT / 2)
                .setConfig(DEFAULT_BITMAP_CONFIG)
                .build();
        PreFillQueue allocationOrder = bitmapPreFiller.generateAllocationOrder(
                new PreFillType[] { doubleWeight, normal }
        );

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
        when(pool.getMaxSize()).thenReturn(DEFAULT_BITMAP_SIZE);
        when(cache.getMaxSize()).thenReturn(DEFAULT_BITMAP_SIZE);
        PreFillType smallWidth =
                new PreFillType.Builder(DEFAULT_BITMAP_WIDTH / 2, DEFAULT_BITMAP_HEIGHT)
                        .setConfig(DEFAULT_BITMAP_CONFIG)
                        .build();
        PreFillType smallHeight =
                new PreFillType.Builder(DEFAULT_BITMAP_WIDTH, DEFAULT_BITMAP_HEIGHT / 2)
                        .setConfig(DEFAULT_BITMAP_CONFIG)
                        .build();

        PreFillQueue allocationOrder = bitmapPreFiller.generateAllocationOrder(
                new PreFillType[] { smallWidth, smallHeight, }
        );

        List<PreFillType> attributes = new ArrayList<PreFillType>();
        while (!allocationOrder.isEmpty()) {
            attributes.add(allocationOrder.remove());
        }

        CombinableMatcher.CombinableEitherMatcher<Iterable<? extends PreFillType>> either =
                either(contains(smallWidth, smallHeight, smallWidth, smallHeight));
        assertThat(attributes, either.or(contains(smallHeight, smallWidth, smallHeight, smallWidth)));
    }
}