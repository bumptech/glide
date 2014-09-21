package com.bumptech.glide.load.engine.prefill;

import android.graphics.Bitmap;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.engine.cache.MemoryCache;
import com.bumptech.glide.util.Util;
import org.hamcrest.core.CombinableMatcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

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
public class BitmapPreFillerAllocationTest {
    private static final int DEFAULT_BITMAP_WIDTH = 100;
    private static final int DEFAULT_BITMAP_HEIGHT = 50;
    private static final Bitmap.Config DEFAULT_BITMAP_CONFIG = PreFillBitmapAttribute.DEFAULT_CONFIG;
    private static final Bitmap DEFAULT_BITMAP =
            Bitmap.createBitmap(DEFAULT_BITMAP_WIDTH, DEFAULT_BITMAP_HEIGHT, DEFAULT_BITMAP_CONFIG);
    private static final int DEFAULT_BITMAP_SIZE = Util.getSize(DEFAULT_BITMAP);

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

        bitmapPreFiller = new BitmapPreFiller(cache, pool);
    }

    @Test
    public void testAllocationOrderContainsEnoughSizesToFillPoolAndMemoryCache() {
        PreFillQueue allocationOrder = bitmapPreFiller.generateAllocationOrder(
                new PreFillBitmapAttribute[]{
                        new PreFillBitmapAttribute(DEFAULT_BITMAP_WIDTH, DEFAULT_BITMAP_HEIGHT)
                }
        );

        assertEquals(DEFAULT_BITMAPS_IN_POOL + DEFAULT_BITMAPS_IN_CACHE, allocationOrder.getSize());
    }

    @Test
    public void testAllocationOrderThatDoesNotFitExactlyIntoGivenSizeRoundsDown() {
        PreFillBitmapAttribute[] sizes = new PreFillBitmapAttribute[] {
                new PreFillBitmapAttribute(DEFAULT_BITMAP_WIDTH, DEFAULT_BITMAP_HEIGHT),
                new PreFillBitmapAttribute(DEFAULT_BITMAP_WIDTH / 2, DEFAULT_BITMAP_HEIGHT),
                new PreFillBitmapAttribute(DEFAULT_BITMAP_WIDTH, DEFAULT_BITMAP_HEIGHT / 2)
        };
        PreFillQueue allocationOrder = bitmapPreFiller.generateAllocationOrder(sizes);

        int byteSize = 0;
        while (!allocationOrder.isEmpty()) {
            PreFillBitmapAttribute current = allocationOrder.remove();
            byteSize += Util.getBitmapPixelSize(current.getWidth(), current.getHeight(), current.getConfig());
        }

        int expectedSize = 0;
        int maxSize = POOL_SIZE + CACHE_SIZE;
        for (PreFillBitmapAttribute current : sizes) {
            int currentSize = Util.getBitmapPixelSize(current.getWidth(), current.getHeight(), current.getConfig());
            expectedSize += currentSize * (maxSize / (3 * currentSize));
        }

        assertEquals(expectedSize, byteSize);
    }

    @Test
    public void testAllocationOrderDoesNotOverFillWithMultipleSizes() {
        PreFillQueue allocationOrder = bitmapPreFiller.generateAllocationOrder(
                new PreFillBitmapAttribute[] {
                        new PreFillBitmapAttribute(DEFAULT_BITMAP_WIDTH, DEFAULT_BITMAP_HEIGHT),
                        new PreFillBitmapAttribute(DEFAULT_BITMAP_WIDTH / 2, DEFAULT_BITMAP_HEIGHT),
                        new PreFillBitmapAttribute(DEFAULT_BITMAP_WIDTH, DEFAULT_BITMAP_HEIGHT / 2)
                }
        );

        int byteSize = 0;
        while (!allocationOrder.isEmpty()) {
            PreFillBitmapAttribute current = allocationOrder.remove();
            byteSize += Util.getBitmapPixelSize(current.getWidth(), current.getHeight(), current.getConfig());
        }

        assertThat(byteSize, lessThanOrEqualTo(POOL_SIZE + CACHE_SIZE));
    }

    @Test
    public void testAllocationOrderDoesNotOverFillWithMultipleSizesAndWeights() {
        PreFillQueue allocationOrder = bitmapPreFiller.generateAllocationOrder(
                new PreFillBitmapAttribute[]{
                        new PreFillBitmapAttribute(DEFAULT_BITMAP_WIDTH, DEFAULT_BITMAP_HEIGHT,
                                DEFAULT_BITMAP_CONFIG, 4),
                        new PreFillBitmapAttribute(DEFAULT_BITMAP_WIDTH / 2, DEFAULT_BITMAP_HEIGHT),
                        new PreFillBitmapAttribute(DEFAULT_BITMAP_WIDTH, DEFAULT_BITMAP_HEIGHT / 3,
                                DEFAULT_BITMAP_CONFIG, 3)
                }
        );

        int byteSize = 0;
        while (!allocationOrder.isEmpty()) {
            PreFillBitmapAttribute current = allocationOrder.remove();
            byteSize += Util.getBitmapPixelSize(current.getWidth(), current.getHeight(), current.getConfig());
        }

        assertThat(byteSize, lessThanOrEqualTo(POOL_SIZE + CACHE_SIZE));
    }

    @Test
    public void testAllocationOrderContainsSingleSizeIfSingleSizeIsProvided() {
        PreFillQueue allocationOrder = bitmapPreFiller.generateAllocationOrder(
                new PreFillBitmapAttribute[] {
                    new PreFillBitmapAttribute(DEFAULT_BITMAP_WIDTH, DEFAULT_BITMAP_HEIGHT)
                }
        );

        while (!allocationOrder.isEmpty()) {
            PreFillBitmapAttribute size = allocationOrder.remove();
            assertEquals(DEFAULT_BITMAP_WIDTH, size.getWidth());
            assertEquals(DEFAULT_BITMAP_HEIGHT, size.getHeight());
            assertEquals(DEFAULT_BITMAP_CONFIG, size.getConfig());
        }
    }

    @Test
    public void testAllocationOrderSplitsEvenlyBetweenEqualSizesWithEqualWeights() {
        PreFillBitmapAttribute smallWidth = new PreFillBitmapAttribute(DEFAULT_BITMAP_WIDTH / 2, DEFAULT_BITMAP_HEIGHT);
        PreFillBitmapAttribute smallHeight = new PreFillBitmapAttribute(DEFAULT_BITMAP_WIDTH,
                DEFAULT_BITMAP_HEIGHT / 2);
        PreFillQueue allocationOrder = bitmapPreFiller.generateAllocationOrder(
                new PreFillBitmapAttribute[] { smallWidth, smallHeight, }
        );

        int numSmallWidth = 0, numSmallHeight = 0;
        while (!allocationOrder.isEmpty()) {
            PreFillBitmapAttribute current = allocationOrder.remove();
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
        PreFillBitmapAttribute smallWidth = new PreFillBitmapAttribute(DEFAULT_BITMAP_WIDTH / 2, DEFAULT_BITMAP_HEIGHT);
        PreFillBitmapAttribute normal = new PreFillBitmapAttribute(DEFAULT_BITMAP_WIDTH, DEFAULT_BITMAP_HEIGHT);
        PreFillQueue allocationOrder = bitmapPreFiller.generateAllocationOrder(
                new PreFillBitmapAttribute[] { smallWidth, normal }
        );

        int numSmallWidth = 0, numNormal = 0;
        while (!allocationOrder.isEmpty()) {
            PreFillBitmapAttribute current = allocationOrder.remove();
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
        PreFillBitmapAttribute doubleWeight = new PreFillBitmapAttribute(DEFAULT_BITMAP_WIDTH / 2,
                DEFAULT_BITMAP_HEIGHT, DEFAULT_BITMAP_CONFIG, 2);
        PreFillBitmapAttribute normal = new PreFillBitmapAttribute(DEFAULT_BITMAP_WIDTH, DEFAULT_BITMAP_HEIGHT / 2,
                DEFAULT_BITMAP_CONFIG, 1);
        PreFillQueue allocationOrder = bitmapPreFiller.generateAllocationOrder(
                new PreFillBitmapAttribute[] { doubleWeight, normal }
        );

        int numDoubleWeight = 0, numNormal = 0;
        while (!allocationOrder.isEmpty()) {
            PreFillBitmapAttribute current = allocationOrder.remove();
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
        PreFillBitmapAttribute smallWidth = new PreFillBitmapAttribute(DEFAULT_BITMAP_WIDTH / 2, DEFAULT_BITMAP_HEIGHT);
        PreFillBitmapAttribute smallHeight = new PreFillBitmapAttribute(DEFAULT_BITMAP_WIDTH,
                DEFAULT_BITMAP_HEIGHT / 2);

        PreFillQueue allocationOrder = bitmapPreFiller.generateAllocationOrder(
                new PreFillBitmapAttribute[] { smallWidth, smallHeight, }
        );

        List<PreFillBitmapAttribute> attributes = new ArrayList<PreFillBitmapAttribute>();
        while (!allocationOrder.isEmpty()) {
            attributes.add(allocationOrder.remove());
        }

        CombinableMatcher.CombinableEitherMatcher<Iterable<? extends PreFillBitmapAttribute>> either =
                either(contains(smallWidth, smallHeight, smallWidth, smallHeight));
        assertThat(attributes, either.or(contains(smallHeight, smallWidth, smallHeight, smallWidth)));
    }
}