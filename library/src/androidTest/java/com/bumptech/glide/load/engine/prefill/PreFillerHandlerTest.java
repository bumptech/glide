package com.bumptech.glide.load.engine.prefill;

import android.graphics.Bitmap;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.engine.cache.MemoryCache;
import com.bumptech.glide.load.resource.bitmap.BitmapResource;
import com.bumptech.glide.util.Util;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class PreFillerHandlerTest {
    private BitmapPreFillIdleHandler.Clock clock;
    private BitmapPool pool;
    private MemoryCache cache;
    private List<Bitmap> addedBitmaps = new ArrayList<Bitmap>();

    @Before
    public void setUp() {
        clock = mock(BitmapPreFillIdleHandler.Clock.class);

        pool = mock(BitmapPool.class);
        when(pool.put(any(Bitmap.class))).thenAnswer(new AddBitmapPoolAnswer(addedBitmaps));
        cache = mock(MemoryCache.class);
        when(cache.put(any(Key.class), any(Resource.class))).thenAnswer(new AddBitmapCacheAnswer(addedBitmaps));
    }

    private BitmapPreFillIdleHandler getHandler(Map<PreFillBitmapAttribute, Integer> allocationOrder) {
        int total = 0;
        for (Integer count : allocationOrder.values()) {
            total += count;
        }
        return new BitmapPreFillIdleHandler(pool, cache, new PreFillQueue(allocationOrder), clock);
    }

    @Test
    public void testAllocatesABitmapPerSizeInAllocationOrder() {
        PreFillBitmapAttribute size = new PreFillBitmapAttribute(100, 100);
        final int toAdd = 3;
        Map<PreFillBitmapAttribute, Integer> allocationOrder = new HashMap<PreFillBitmapAttribute, Integer>();
        allocationOrder.put(size, toAdd);
        BitmapPreFillIdleHandler handler = getHandler(allocationOrder);
        handler.queueIdle();

        Bitmap expected = Bitmap.createBitmap(size.getWidth(), size.getHeight(), size.getConfig());
        assertThat(addedBitmaps, contains(expected, expected, expected));
    }

    @Test
    public void testAllocatesBitmapsInOrderGivenByAllocationOrder() {
        PreFillBitmapAttribute smallWidth = new PreFillBitmapAttribute(50, 100);
        PreFillBitmapAttribute smallHeight = new PreFillBitmapAttribute(100, 50);

        PreFillBitmapAttribute[] expectedOrder = new PreFillBitmapAttribute[] {
                smallWidth,
                smallHeight,
                smallWidth,
                smallHeight,
        };

        HashMap<PreFillBitmapAttribute, Integer> allocationOrder = new HashMap<PreFillBitmapAttribute, Integer>();
        allocationOrder.put(smallWidth, 2);
        allocationOrder.put(smallHeight, 2);
        BitmapPreFillIdleHandler handler = getHandler(allocationOrder);
        handler.queueIdle();


        Bitmap[] expectedBitmaps = new Bitmap[expectedOrder.length];
        for (int i = 0; i < expectedBitmaps.length; i++) {
            PreFillBitmapAttribute current = expectedOrder[i];
            expectedBitmaps[i] = Bitmap.createBitmap(current.getWidth(), current.getHeight(), current.getConfig());
        }

        Bitmap current = addedBitmaps.get(0);
        for (int i = 1; i < addedBitmaps.size(); i++) {
            assertNotEquals(current, addedBitmaps.get(i));
            current = addedBitmaps.get(i);
        }

        assertThat(addedBitmaps, hasSize(4));
    }

    @Test
    public void testStopsAllocatingBitmapsUntilNextIdleCallIfAllocationsTakeLongerThanLimit() {
        PreFillBitmapAttribute size = new PreFillBitmapAttribute(1, 1);
        Map<PreFillBitmapAttribute, Integer> allocationOrder = new HashMap<PreFillBitmapAttribute, Integer>();
        allocationOrder.put(size, 3);
        when(clock.now()).thenReturn(0L).thenReturn(0L).thenReturn(BitmapPreFillIdleHandler.MAX_DURATION_MILLIS);
        BitmapPreFillIdleHandler handler = getHandler(allocationOrder);
        handler.queueIdle();

        assertThat(addedBitmaps, hasSize(1));

        handler.queueIdle();

        assertThat(addedBitmaps, hasSize(3));
    }

    @Test
    public void testPreFillHandlerReturnsFalseFromQueueIdleIfHasNoBitmapsToAllocate() {
        BitmapPreFillIdleHandler handler = getHandler(new HashMap<PreFillBitmapAttribute, Integer>());
        assertFalse(handler.queueIdle());
    }

    @Test
    public void testPreFillHandlerReturnsTrueFromQueueIdleIfHasBitmapsToAllocate() {
        PreFillBitmapAttribute size = new PreFillBitmapAttribute(1, 1);
        Map<PreFillBitmapAttribute, Integer> allocationOrder = new HashMap<PreFillBitmapAttribute, Integer>();
        allocationOrder.put(size, 2);
        BitmapPreFillIdleHandler handler = getHandler(allocationOrder);
        when(clock.now()).thenReturn(0L).thenReturn(0L).thenReturn(BitmapPreFillIdleHandler.MAX_DURATION_MILLIS);
        assertTrue(handler.queueIdle());
    }

    @Test
    public void testPreFillHandlerReturnsFalseFromQueueIdleIfHasBitmapsButIsCancelled() {
        PreFillBitmapAttribute size = new PreFillBitmapAttribute(1, 1);
        Map<PreFillBitmapAttribute, Integer> allocationOrder = new HashMap<PreFillBitmapAttribute, Integer>();
        allocationOrder.put(size, 2);

        BitmapPreFillIdleHandler handler = getHandler(allocationOrder);
        when(clock.now()).thenReturn(0L).thenReturn(0L).thenReturn(BitmapPreFillIdleHandler.MAX_DURATION_MILLIS);
        handler.cancel();
        handler.queueIdle();
        assertFalse(handler.queueIdle());
    }

    @Test
    public void testAddsBitmapsToMemoryCacheIfMemoryCacheHasEnoughSpaceRemaining() {
        Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        when(cache.getMaxSize()).thenReturn(Util.getBitmapByteSize(bitmap));

        PreFillBitmapAttribute size = new PreFillBitmapAttribute(bitmap.getWidth(), bitmap.getHeight(),
                bitmap.getConfig(), 1);
        Map<PreFillBitmapAttribute, Integer> allocationOrder = new HashMap<PreFillBitmapAttribute, Integer>();
        allocationOrder.put(size, 1);

        BitmapPreFillIdleHandler handler = getHandler(allocationOrder);
        handler.queueIdle();

        verify(cache).put(any(Key.class), any(Resource.class));
        verify(pool, never()).put(any(Bitmap.class));
        assertThat(addedBitmaps, contains(bitmap));
    }

    @Test
    public void testAddsBitmapsToBitmapPoolIfMemoryCacheIsFull() {
        Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        when(cache.getMaxSize()).thenReturn(0);

        PreFillBitmapAttribute size = new PreFillBitmapAttribute(bitmap.getWidth(), bitmap.getHeight(),
                bitmap.getConfig(), 1);
        Map<PreFillBitmapAttribute, Integer> allocationOrder = new HashMap<PreFillBitmapAttribute, Integer>();
        allocationOrder.put(size, 1);

        BitmapPreFillIdleHandler handler = getHandler(allocationOrder);
        handler.queueIdle();

        verify(cache, never()).put(any(Key.class), any(Resource.class));
        verify(pool).put(eq(bitmap));
        assertThat(addedBitmaps, contains(bitmap));
    }

    @Test
    public void testAddsBitmapsToPoolIfMemoryCacheIsNotFullButCannotFitBitmap() {
        Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        when(cache.getMaxSize()).thenReturn(Util.getBitmapByteSize(bitmap) / 2);

        PreFillBitmapAttribute size = new PreFillBitmapAttribute(bitmap.getWidth(), bitmap.getHeight(),
                bitmap.getConfig(), 1);
        Map<PreFillBitmapAttribute, Integer> allocationOrder = new HashMap<PreFillBitmapAttribute, Integer>();
        allocationOrder.put(size, 1);

        BitmapPreFillIdleHandler handler = getHandler(allocationOrder);
        handler.queueIdle();

        verify(cache, never()).put(any(Key.class), any(Resource.class));
        verify(pool).put(eq(bitmap));
        assertThat(addedBitmaps, contains(bitmap));
    }

    private static class AddBitmapPoolAnswer implements Answer<Boolean> {
        private List<Bitmap> bitmaps;

        public AddBitmapPoolAnswer(List<Bitmap> bitmaps) {
            this.bitmaps = bitmaps;
        }

        @Override
        public Boolean answer(InvocationOnMock invocationOnMock) throws Throwable {
            Bitmap bitmap = (Bitmap) invocationOnMock.getArguments()[0];
            bitmaps.add(bitmap);
            return null;
        }
    }

    private static class AddBitmapCacheAnswer implements Answer<Resource<?>> {
        private List<Bitmap> bitmaps;

        public AddBitmapCacheAnswer(List<Bitmap> bitmaps) {
            this.bitmaps = bitmaps;
        }

        @Override
        public Resource<?> answer(InvocationOnMock invocationOnMock) throws Throwable {
            BitmapResource resource = (BitmapResource) invocationOnMock.getArguments()[1];
            bitmaps.add(resource.get());
            return null;
        }
    }
}