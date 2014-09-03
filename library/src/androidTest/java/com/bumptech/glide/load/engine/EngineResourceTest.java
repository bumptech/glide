package com.bumptech.glide.load.engine;

import com.bumptech.glide.load.Key;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class EngineResourceTest {
    private EngineResource<Object> engineResource;
    private EngineResource.ResourceListener listener;
    private Key cacheKey = mock(Key.class);
    private Resource<Object> resource;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() {
        resource = mock(Resource.class);
        engineResource = new EngineResource<Object>(resource);
        listener = mock(EngineResource.ResourceListener.class);
        engineResource.setResourceListener(cacheKey, listener);
    }

    @Test
    public void testCanAcquireAndRelease() {
        engineResource.acquire(1);
        engineResource.release();

        verify(listener).onResourceReleased(cacheKey, engineResource);
    }

    @Test
    public void testCanAcquireMultipleTimesAndRelease() {
        engineResource.acquire(2);
        engineResource.release();
        engineResource.release();

        verify(listener).onResourceReleased(eq(cacheKey), eq(engineResource));
    }

    @Test
    public void testDelegatesGetToWrappedResource() {
        Object expected = new Object();
        when(resource.get()).thenReturn(expected);
        assertEquals(expected, engineResource.get());
    }

    @Test
    public void testDelegatesGetSizeToWrappedResource() {
        int expectedSize = 1234;
        when(resource.getSize()).thenReturn(expectedSize);
        assertEquals(expectedSize, engineResource.getSize());
    }

    @Test
    public void testRecyclesWrappedResourceWhenRecycled() {
        engineResource.acquire(1);
        engineResource.release();
        engineResource.recycle();
        verify(resource).recycle();
    }

    @Test(expected = IllegalStateException.class)
    public void testThrowsIfRecycledTwice() {
        engineResource.recycle();
        engineResource.recycle();
    }

    @Test(expected = IllegalStateException.class)
    public void testThrowsIfReleasedBeforeAcquired() {
        engineResource.release();
    }

    @Test(expected = IllegalStateException.class)
    public void testThrowsIfRecycledWhileAcquired() {
        engineResource.acquire(1);
        engineResource.recycle();
    }

    @Test(expected = IllegalStateException.class)
    public void testThrowsIfAcquiredAfterRecycled() {
        engineResource.recycle();
        engineResource.acquire(1);
    }

    @Test
    public void testThrowsIfAcquiredOnBackgroundThread() throws InterruptedException {
        Thread otherThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    engineResource.acquire(1);
                } catch (IllegalThreadStateException e) {
                    return;
                }
                fail("Failed to receive expected IllegalThreadStateException");
            }
        });
        otherThread.start();
        otherThread.join();
    }

    @Test
    public void testThrowsIfReleasedOnBackgroundThread() throws InterruptedException {
        engineResource.acquire(1);
        Thread otherThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                     engineResource.release();
                } catch (IllegalThreadStateException e) {
                    return;
                }
                fail("Failed to receive expected IllegalThreadStateException");
            }
        });
        otherThread.start();
        otherThread.join();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowsIfAcquiredWithTimesEqualToZero() {
        engineResource.acquire(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowsIfAcquiredWithTimesLessThanToZero() {
        engineResource.acquire(-1);
    }

    @Test(expected = IllegalStateException.class)
    public void testThrowsIfReleasedMoreThanAcquired() {
        engineResource.acquire(1);
        engineResource.release();
        engineResource.release();
    }

    @Test
    public void testCanSetAndGetIsCacheable() {
        engineResource.setCacheable(true);
        assertTrue(engineResource.isCacheable());
        engineResource.setCacheable(false);
        assertFalse(engineResource.isCacheable());
    }
}
