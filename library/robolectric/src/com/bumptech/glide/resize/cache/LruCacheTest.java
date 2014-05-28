package com.bumptech.glide.resize.cache;

import com.bumptech.glide.util.LruCache;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LruCacheTest {
    // 1MB
    private static final int SIZE = 2;
    private LruCache<String, Object> cache;
    private CacheListener listener;
    private String currentKey;

    @Before
    public void setUp() throws Exception {
        currentKey = "";
        listener = mock(CacheListener.class);
        cache = new TestLruCache(SIZE, listener);
        when(listener.getSize(anyObject())).thenReturn(1);
    }

    @Test
    public void testCanAddAndRetrieveItem() {
        String key = getKey();
        Object object = new Object();

        cache.put(key, object);

        assertEquals(object, cache.get(key));
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
        verify(listener, never()).onItemRemoved(anyObject());
        cache.put(getKey(), new Object());
        verify(listener).onItemRemoved(anyObject());
    }

    @Test
    public void testLeastRecentlyAddKeyEvictedFirstIfGetsAreEqual() {
        Object first = new Object();
        cache.put(getKey(), first);
        cache.put(getKey(), new Object());
        cache.put(getKey(), new Object());

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
        cache.put(getKey(), new Object());

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

        verify(listener, times(1)).onItemRemoved(anyObject());
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

    private String getKey() {
        currentKey += "1";
        return currentKey;
    }

    private interface CacheListener {
        public void onItemRemoved(Object item);
        public int getSize(Object item);
    }

    private static class TestLruCache extends LruCache<String, Object> {
        private final CacheListener listener;

        public TestLruCache(int size, CacheListener listener) {
            super(size);
            this.listener = listener;
        }

        @Override
        protected void onItemRemoved(Object item) {
            listener.onItemRemoved(item);
        }

        @Override
        protected int getSize(Object item) {
            return listener.getSize(item);
        }
    }
}
