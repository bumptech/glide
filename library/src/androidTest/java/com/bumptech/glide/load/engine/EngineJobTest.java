package com.bumptech.glide.load.engine;

import android.os.Handler;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.request.ResourceCallback;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLooper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class EngineJobTest {
    private EngineJobHarness harness;

    @Before
    public void setUp() {
        harness = new EngineJobHarness();
    }

    @Test
    public void testOnResourceReadyPassedToCallbacks() throws Exception {
        harness.getJob().onResourceReady(harness.resource);

        Robolectric.runUiThreadTasks();
        verify(harness.cb).onResourceReady(eq(harness.engineResource));
    }

    @Test
    public void testListenerNotifiedJobCompleteOnOnResourceReady() {
        harness.getJob().onResourceReady(harness.resource);

        Robolectric.runUiThreadTasks();

        verify(harness.listener).onEngineJobComplete(eq(harness.key), eq(harness.engineResource));
    }

     @Test
    public void testNotifiesAllCallbacksOnReady() {
         MultiCbHarness harness = new MultiCbHarness();
        harness.job.onResourceReady(harness.resource);
        for (ResourceCallback cb : harness.cbs) {
            verify(cb).onResourceReady(eq(harness.engineResource));
        }
    }

    @Test
    public void testNotifiesAllCallbacksOnException() {
        MultiCbHarness harness = new MultiCbHarness();
        Exception exception = new IOException("test");
        harness.job.onException(exception);
        for (ResourceCallback cb : harness.cbs) {
            verify(cb).onException(eq(exception));
        }
    }

    @Test
    public void testAcquiresResourceOncePerCallback() {
        MultiCbHarness harness = new MultiCbHarness();
        harness.job.onResourceReady(harness.resource);
        verify(harness.engineResource).acquire(eq(harness.numCbs));
    }

    @Test
    public void testListenerNotifiedJobCompleteOnException() {
        harness.getJob().onException(new Exception("test"));

        Robolectric.runUiThreadTasks();
        verify(harness.listener).onEngineJobComplete(eq(harness.key), (EngineResource) isNull());
    }

    @Test
    public void testResourceSetCacheableCalledWhenIsCacheableOnReady() {
        harness.isCacheable = true;
        harness.getJob().onResourceReady(harness.resource);

        Robolectric.runUiThreadTasks();
        verify(harness.engineResource).setCacheable(eq(harness.isCacheable));
    }

    @Test
    public void testResourceSetCacheableCalledWhenNotIsCacheableOnReady() {
        harness.isCacheable = false;
        harness.getJob().onResourceReady(harness.resource);

        Robolectric.runUiThreadTasks();
        verify(harness.engineResource).setCacheable(eq(harness.isCacheable));
    }

    @Test
    public void testListenerNotifiedOfCancelOnCancel() {
        EngineJob job = harness.getJob();
        job.cancel();

        verify(harness.listener).onEngineJobCancelled(eq(job), eq(harness.key));
    }

    @Test
    public void testOnResourceReadyNotDeliveredAfterCancel() {
        EngineJob job = harness.getJob();
        job.cancel();

        job.onResourceReady(harness.resource);

        Robolectric.runUiThreadTasks();
        verify(harness.cb, never()).onResourceReady(eq(harness.resource));
    }

    @Test
    public void testOnExceptionNotDeliveredAfterCancel() {
        EngineJob job = harness.getJob();
        job.cancel();

        job.onException(new Exception("test"));

        Robolectric.runUiThreadTasks();
        verify(harness.cb, never()).onException(any(Exception.class));
    }

    @Test
    public void testRemovingAllCallbacksCancelsRunner() {
        EngineJob job = harness.getJob();
        job.removeCallback(harness.cb);

        assertTrue(job.isCancelled());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void removingSomeCallbacksDoesNotCancelRunner() {
        EngineJob job = harness.getJob();
        job.addCallback(mock(ResourceCallback.class));
        job.removeCallback(harness.cb);

        assertFalse(job.isCancelled());
    }

    @Test
    public void testResourceIsAcquiredOncePerConsumerAndOnceForCache() {
        harness.getJob().onResourceReady(harness.resource);

        // Once while notifying and once for single callback.
        verify(harness.engineResource, times(2)).acquire(eq(1));
    }

    @Test
    public void testDoesNotNotifyCancelledIfCompletesBeforeCancel() {
        EngineJob job = harness.getJob();
        job.onResourceReady(harness.resource);
        job.cancel();

        verify(harness.listener, never()).onEngineJobCancelled(eq(job), eq(harness.key));
    }

    @Test
    public void testDoesNotNotifyCancelledIfAlreadyCancelled() {
        EngineJob job = harness.getJob();
        job.cancel();
        job.cancel();

        verify(harness.listener, times(1)).onEngineJobCancelled(eq(job), eq(harness.key));
    }

    @Test
    public void testReleasesResourceIfCancelledOnReady() {
        ShadowLooper shadowLooper = Robolectric.shadowOf(harness.mainHandler.getLooper());
        shadowLooper.pause();

        EngineJob job = harness.getJob();
        job.onResourceReady(harness.resource);
        job.cancel();
        shadowLooper.runOneTask();

        verify(harness.resource).recycle();
    }

    @Test
    public void testDoesNotAcquireOnceForMemoryCacheIfNotCacheable() {
        harness.isCacheable = false;
        harness.getJob().onResourceReady(harness.resource);

        verify(harness.engineResource, times(2)).acquire(eq(1));
    }

    private static class MultiCbHarness {
        Key key = mock(Key.class);
        Handler mainHandler = new Handler();
        Resource<Object> resource = mock(Resource.class);
        EngineResource<Object> engineResource = mock(EngineResource.class);
        EngineJobListener listener = mock(EngineJobListener.class);
        boolean isCacheable = true;
        int numCbs = 10;
        List<ResourceCallback> cbs = new ArrayList<ResourceCallback>();
        EngineJob.EngineResourceFactory factory = mock(EngineJob.EngineResourceFactory.class);
        EngineJob job;

        public MultiCbHarness() {
            when(factory.build(eq(resource))).thenReturn(engineResource);
            job = new EngineJob(key, mainHandler, isCacheable, listener, factory);
            for (int i = 0; i < numCbs; i++) {
                cbs.add(mock(ResourceCallback.class));
            }
            for (ResourceCallback cb : cbs) {
                job.addCallback(cb);
            }
        }

    }

    @SuppressWarnings("unchecked")
    private static class EngineJobHarness {
        Key key = mock(Key.class);
        Handler mainHandler = new Handler();
        ResourceCallback cb = mock(ResourceCallback.class);
        Resource<Object> resource = mock(Resource.class);
        EngineResource<Object> engineResource = mock(EngineResource.class);
        EngineJobListener listener = mock(EngineJobListener.class);
        boolean isCacheable = true;

        public EngineJob getJob() {
            EngineJob.EngineResourceFactory factory = mock(EngineJob.EngineResourceFactory.class);
            when(factory.build(eq(resource))).thenReturn(engineResource);
            EngineJob result = new EngineJob(key, mainHandler, isCacheable, listener, factory);
            result.addCallback(cb);
            return result;
        }
    }
}
