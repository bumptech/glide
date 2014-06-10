package com.bumptech.glide.load.engine;

import android.os.Handler;
import com.bumptech.glide.Resource;
import com.bumptech.glide.request.ResourceCallback;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.engine.cache.MemoryCache;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLooper;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
public class EngineJobTest {
    private EngineJobHarness harness;

    @Before
    public void setUp() {
        harness = new EngineJobHarness();
    }

    @Test
    public void testOnResourceReadyPassedToCallbacks() throws Exception {
        harness.job.onResourceReady(harness.resource);

        Robolectric.runUiThreadTasks();
        verify(harness.cb).onResourceReady(eq(harness.resource));
    }

    @Test
    public void testListenerNotifiedJobCompleteOnOnResourceReady() {
        harness.job.onResourceReady(harness.resource);

        Robolectric.runUiThreadTasks();

        verify(harness.listener).onEngineJobComplete(eq(harness.key));
    }

    @Test
    public void testResourceAddedToCacheOnResourceReady() {
        harness.job.onResourceReady(harness.resource);

        Robolectric.runUiThreadTasks();
        verify(harness.memoryCache).put(eq(harness.key), eq(harness.resource));
    }

    @Test
    public void testOnExceptionPassedToCallbacks() throws Exception {
        Exception exception = new Exception("Test");

        harness.job.onException(exception);

        Robolectric.runUiThreadTasks();
        verify(harness.cb).onException(eq(exception));
    }

    @Test
    public void testListenerNotifiedJobCompleteOnException() {
        harness.job.onException(new Exception("test"));

        Robolectric.runUiThreadTasks();
        verify(harness.listener).onEngineJobComplete(eq(harness.key));
    }

    @Test
    public void testListenerNotifiedOfCancelOnCancel() {
        harness.job.cancel();

        verify(harness.listener).onEngineJobCancelled(eq(harness.key));
    }

    @Test
    public void testOnResourceReadyNotDeliveredAfterCancel() {
        harness.job.cancel();

        harness.job.onResourceReady(harness.resource);

        Robolectric.runUiThreadTasks();
        verify(harness.cb, never()).onResourceReady(eq(harness.resource));
    }

    @Test
    public void testOnExceptionNotDeliveredAfterCancel() {
        harness.job.cancel();

        harness.job.onException(new Exception("test"));

        Robolectric.runUiThreadTasks();
        verify(harness.cb, never()).onException(any(Exception.class));
    }

    @Test
    public void testRemovingAllCallbacksCancelsRunner() {
        harness.job.removeCallback(harness.cb);

        assertTrue(harness.job.isCancelled());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void removingSomeCallbacksDoesNotCancelRunner() {
        harness.job.addCallback(mock(ResourceCallback.class));
        harness.job.removeCallback(harness.cb);

        assertFalse(harness.job.isCancelled());
    }

    @Test
    public void testResourceIsAcquiredOncePerConsumerAndOnceForCache() {
        harness.job.onResourceReady(harness.resource);

        // Once while notifying, once for memory cache, and once for resource.
        verify(harness.resource).acquire(eq(3));
    }

    @Test
    public void testResourceIsNotRecycledIfMemoryCacheEvictsSynchronously() {
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                harness.resource.release();
                return null;
            }
        }).when(harness.memoryCache).put(eq(harness.key), eq(harness.resource));

        harness.job.onResourceReady(harness.resource);
        // Once while notifying, once for memory cache, and once for resource.
        verify(harness.resource).acquire(eq(3));
        // Once for notifying, and once for resource.
        verify(harness.resource, times(2)).release();
    }

    @Test
    public void testDoesNotNotifyCancelledIfCompletesBeforeCancel() {
        harness.job.onResourceReady(harness.resource);
        harness.job.cancel();

        verify(harness.listener, never()).onEngineJobCancelled(eq(harness.key));
    }

    @Test
    public void testDoesNotNotifyCancelledIfAlreadyCancelled() {
        harness.job.cancel();
        harness.job.cancel();

        verify(harness.listener, times(1)).onEngineJobCancelled(eq(harness.key));
    }

    @Test
    public void testReleasesResourceIfCancelledOnReady() {
        ShadowLooper shadowLooper = Robolectric.shadowOf(harness.mainHandler.getLooper());
        shadowLooper.pause();

        harness.job.onResourceReady(harness.resource);
        harness.job.cancel();
        shadowLooper.runOneTask();

        verify(harness.resource).recycle();
    }

    @SuppressWarnings("unchecked")
    private static class EngineJobHarness {
        Key key = mock(Key.class);
        MemoryCache memoryCache = mock(MemoryCache.class);
        Handler mainHandler = new Handler();
        ResourceCallback cb = mock(ResourceCallback.class);
        Resource<Object> resource = mock(Resource.class);
        EngineJobListener listener = mock(EngineJobListener.class);

        EngineJob job = new EngineJob(key, memoryCache, mainHandler, listener);

        public EngineJobHarness() {
            job.addCallback(cb);
        }
    }
}
