package com.bumptech.glide;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, emulateSdk = 18)
public class ListPreloaderTest {

    @Test
    public void testGetItemsIsCalledIncreasing() {
        final AtomicBoolean called = new AtomicBoolean(false);
        final AtomicInteger calledCount = new AtomicInteger();
        ListPreloaderAdapter preloaderAdapter = new ListPreloaderAdapter() {
            @Override
            public Object getPreloadItem(int position) {
                called.set(true);
                final int count = calledCount.getAndIncrement();
                assertEquals(11 + count, position);
                return super.getPreloadItem(position);
            }
        };
        ListPreloader<Object> preloader = new ListPreloader<Object>(preloaderAdapter, preloaderAdapter, 10);
        preloader.onScroll(null, 1, 10, 30);
        assertEquals(10, calledCount.get());
    }

    @Test
    public void testGetItemsIsCalledInOrderIncreasing() {
        final int toPreload = 10;
        final List<Object> objects = new ArrayList<Object>();
        for (int i = 0; i < toPreload; i++) {
            objects.add(new Object());
        }

        ListPreloaderAdapter preloaderAdapter = new ListPreloaderAdapter() {

            @Override
            public int[] getPreloadSize(Object item, int pos) {
                return new int[]{10, 10};
            }

            @Override
            public Object getPreloadItem(int position) {
                return objects.get(position - 10);
            }

            @Override
            public BitmapRequestBuilder getPreloadRequestBuilder(Object item, int pos) {
                assertEquals(objects.get(pos - 10), item);
                return mock(BitmapRequestBuilder.class);
            }
        };
        ListPreloader<Object> preloader = new ListPreloader<Object>(preloaderAdapter, preloaderAdapter,
                                                                    toPreload);
        preloader.onScroll(null, 1, 10, 20);
    }

    @Test
    public void testGetItemsIsCalledDecreasing() {
        final AtomicBoolean called = new AtomicBoolean(false);
        final AtomicInteger calledCount = new AtomicInteger();
        ListPreloaderAdapter preloaderAdapter = new ListPreloaderAdapter() {
            @Override
            public Object getPreloadItem(int position) {
                // Ignore the preload caused from us starting at the end
                if (position >= 40) {
                    return Collections.emptyList();
                }
                final int count = calledCount.getAndIncrement();
                called.set(true);
                assertEquals(28 - count, position);
                return super.getPreloadItem(position);
            }
        };
        ListPreloader<Object> preloader = new ListPreloader<Object>(preloaderAdapter, preloaderAdapter, 10);
        preloader.onScroll(null, 30, 10, 40);
        preloader.onScroll(null, 29, 10, 40);
        assertTrue(called.get());
    }

    @Test
    public void testGetItemsIsCalledInOrderDecreasing() {
        final int toPreload = 10;
        final List<Object> objects = new ArrayList<Object>();
        for (int i = 0; i < toPreload; i++) {
            objects.add(new Object());
        }

        ListPreloaderAdapter preloaderAdapter = new ListPreloaderAdapter() {
            int expectedPosition = toPreload - 1;

            @Override
            public int[] getPreloadSize(Object item, int pos) {
                return new int[]{10, 10};
            }

            @Override
            public Object getPreloadItem(int position) {
                if (position == 40) {
                    return null;
                }
                return objects.get(position);
            }

            @Override
            public BitmapRequestBuilder getPreloadRequestBuilder(Object item, int pos) {
                assertEquals(objects.get(expectedPosition), item);
                expectedPosition--;
                return mock(BitmapRequestBuilder.class);
            }
        };
        ListPreloader<Object> preloader = new ListPreloader<Object>(preloaderAdapter, preloaderAdapter,
                                                                    toPreload);
        preloader.onScroll(null, 30, 10, 10);
        preloader.onScroll(null, 29, 10, 10);
    }

    @Test
    public void testGetItemsIsNeverCalledWithEndGreaterThanTotalItems() {
        final AtomicBoolean called = new AtomicBoolean(false);
        final AtomicInteger calledCount = new AtomicInteger();
        ListPreloaderAdapter preloaderAdapter = new ListPreloaderAdapter() {
            @Override
            public Object getPreloadItem(int position) {
                called.set(true);
                final int count = calledCount.getAndIncrement();
                assertEquals(26 + count, position);
                return super.getPreloadItem(position);
            }
        };
        ListPreloader<Object> preloader = new ListPreloader<Object>(preloaderAdapter, preloaderAdapter, 10);
        preloader.onScroll(null, 16, 10, 30);
        assertTrue(called.get());
    }

    @Test
    public void testGetItemsIsNeverCalledWithStartLessThanZero() {
        final AtomicBoolean called = new AtomicBoolean(false);
        final AtomicInteger calledCount = new AtomicInteger();
        ListPreloaderAdapter preloaderAdapter = new ListPreloaderAdapter() {
            @Override
            public Object getPreloadItem(int position) {
                if (position >= 17) {
                    return Collections.emptyList();
                }
                called.set(true);
                final int count = calledCount.getAndIncrement();
                assertEquals(5 - count, position);
                return super.getPreloadItem(position);
            }
        };

        ListPreloader<Object> preloader = new ListPreloader<Object>(preloaderAdapter, preloaderAdapter, 10);
        preloader.onScroll(null, 7, 10, 30);
        preloader.onScroll(null, 6, 10, 30);
        assertTrue(called.get());
    }

    @Test
    public void testDontPreloadItemsRepeatedlyWhileIncreasing() {
        final AtomicInteger called = new AtomicInteger();
        ListPreloaderAdapter preloaderAdapter = new ListPreloaderAdapter() {
            @Override
            public Object getPreloadItem(int position) {
                final int current = called.getAndIncrement();
                assertEquals(11 + current, position);
                return super.getPreloadItem(position);
            }
        };

        ListPreloader<Object> preloader = new ListPreloader<Object>(preloaderAdapter, preloaderAdapter, 10);
        preloader.onScroll(null, 1, 10, 30);
        preloader.onScroll(null, 4, 10, 30);

        assertEquals(13, called.get());
    }

    @Test
    public void testDontPreloadItemsRepeatedlyWhileDecreasing() {
        final AtomicInteger called = new AtomicInteger();
        ListPreloaderAdapter preloaderAdapter = new ListPreloaderAdapter() {
            @Override
            public Object getPreloadItem(int position) {
                if (position >= 20) {
                    return Collections.emptyList();
                }
                final int current = called.getAndIncrement();
                assertEquals(19 - current, position);
                return super.getPreloadItem(position);
            }
        };

        ListPreloader<Object> preloader = new ListPreloader<Object>(preloaderAdapter, preloaderAdapter, 10);
        preloader.onScroll(null, 21, 10, 30);
        preloader.onScroll(null, 20, 10, 30);
        preloader.onScroll(null, 17, 10, 30);
        assertEquals(13, called.get());
    }

    @Test
    public void testItemsArePreloadedWithGlide() {
        final List<Object> objects = new ArrayList<Object>();
        objects.add(new Object());
        objects.add(new Object());
        final HashSet<Object> loadedObjects = new HashSet<Object>();
        ListPreloaderAdapter preloaderAdapter = new ListPreloaderAdapter() {
            @Override
            public Object getPreloadItem(int position) {
                return objects.get(position - 11);
            }

            @Override
            public GenericRequestBuilder getPreloadRequestBuilder(Object item, int pos) {
                loadedObjects.add(item);
                return super.getPreloadRequestBuilder(item, pos);
            }
        };
        ListPreloader<Object> preloader = new ListPreloader<Object>(preloaderAdapter, preloaderAdapter, 10);
        preloader.onScroll(null, 1, 10, 13);
        assertThat(loadedObjects).containsAllIn(objects);
    }

    private static class ListPreloaderAdapter implements ListPreloader.PreloadModelProvider<Object>,
            ListPreloader.PreloadSizeProvider<Object> {

        public ListPreloaderAdapter() {
        }

        @Override
        public Object getPreloadItem(int position) {
            return new Object();
        }

        @Override
        public GenericRequestBuilder getPreloadRequestBuilder(Object item, int position) {
            return mock(BitmapRequestBuilder.class);
        }

        @Override
        public int[] getPreloadSize(Object item, int position) {
            return new int[]{100, 100};
        }
    }
}
