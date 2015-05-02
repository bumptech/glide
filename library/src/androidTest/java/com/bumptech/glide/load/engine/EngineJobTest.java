package com.bumptech.glide.load.engine;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.os.Handler;

import com.bumptech.glide.load.Key;
import com.bumptech.glide.request.ResourceCallback;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, emulateSdk = 18)
public class EngineJobTest {
  private EngineJobHarness harness;

  @Before
  public void setUp() {
    harness = new EngineJobHarness();
  }

  @Test
  public void testOnResourceReadyPassedToCallbacks() throws Exception {
    harness.getJob().onResourceReady(harness.resource);

    ShadowLooper.runUiThreadTasks();
    verify(harness.cb).onResourceReady(eq(harness.engineResource));
  }

  @Test
  public void testListenerNotifiedJobCompleteOnOnResourceReady() {
    harness.getJob().onResourceReady(harness.resource);

    ShadowLooper.runUiThreadTasks();

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
    harness.job.onLoadFailed();
    for (ResourceCallback cb : harness.cbs) {
      verify(cb).onLoadFailed();
    }
  }

  @Test
  public void testAcquiresResourceOncePerCallback() {
    MultiCbHarness harness = new MultiCbHarness();
    harness.job.onResourceReady(harness.resource);

    // Acquired once and then released while notifying.
    InOrder order = inOrder(harness.engineResource);
    order.verify(harness.engineResource, times(harness.numCbs + 1)).acquire();
    order.verify(harness.engineResource, times(1)).release();
  }

  @Test
  public void testListenerNotifiedJobCompleteOnException() {
    harness = new EngineJobHarness();
    harness.getJob().onLoadFailed();
    ShadowLooper.runUiThreadTasks();
    verify(harness.listener).onEngineJobComplete(eq(harness.key), (EngineResource) isNull());
  }

  @Test
  public void testResourceIsCacheableWhenIsCacheableOnReady() {
    harness.isCacheable = true;
    harness.getJob().onResourceReady(harness.resource);

    ShadowLooper.runUiThreadTasks();
    verify(harness.factory).build(any(Resource.class), eq(harness.isCacheable));
  }

  @Test
  public void testResourceIsCacheableWhenNotIsCacheableOnReady() {
    harness.isCacheable = false;
    harness.getJob().onResourceReady(harness.resource);

    ShadowLooper.runUiThreadTasks();
    verify(harness.factory).build(any(Resource.class), eq(harness.isCacheable));
  }

  @Test
  public void testListenerNotifiedOfCancelOnCancel() {
    EngineJob job = harness.getJob();
    job.start(harness.decodeJob);
    job.cancel();

    verify(harness.listener).onEngineJobCancelled(eq(job), eq(harness.key));
  }

  @Test
  public void testOnResourceReadyNotDeliveredAfterCancel() {
    EngineJob job = harness.getJob();
    job.start(harness.decodeJob);
    job.cancel();

    job.onResourceReady(harness.resource);

    ShadowLooper.runUiThreadTasks();
    verify(harness.cb, never()).onResourceReady(eq(harness.resource));
  }

  @Test
  public void testOnExceptionNotDeliveredAfterCancel() {
    harness = new EngineJobHarness();
    EngineJob job = harness.getJob();
    job.start(harness.decodeJob);
    job.cancel();

    job.onLoadFailed();

    ShadowLooper.runUiThreadTasks();
    verify(harness.cb, never()).onLoadFailed();
  }

  @Test
  public void testRemovingAllCallbacksCancelsRunner() {
    EngineJob job = harness.getJob();
    job.start(harness.decodeJob);
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
    verify(harness.engineResource, times(2)).acquire();
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
    job.start(harness.decodeJob);
    job.cancel();
    job.cancel();

    verify(harness.listener, times(1)).onEngineJobCancelled(eq(job), eq(harness.key));
  }

  @Test
  public void testDoesNotNotifyCancelledIfReceivedException() {
    EngineJob job = harness.getJob();
    job.start(harness.decodeJob);
    job.onLoadFailed();
    job.cancel();

    verify(harness.listener).onEngineJobComplete(eq(harness.key), (EngineResource) isNull());
    verify(harness.listener, never()).onEngineJobCancelled(any(EngineJob.class), any(Key.class));
  }

  @Test
  public void testReleasesResourceIfCancelledOnReady() {
    ShadowLooper shadowLooper = Shadows.shadowOf(harness.mainHandler.getLooper());
    shadowLooper.pause();

    EngineJob job = harness.getJob();
    job.start(harness.decodeJob);
    job.onResourceReady(harness.resource);
    job.cancel();
    shadowLooper.runOneTask();

    verify(harness.resource).recycle();
  }

  @Test
  public void testDoesNotAcquireOnceForMemoryCacheIfNotCacheable() {
    harness.isCacheable = false;
    harness.getJob().onResourceReady(harness.resource);

    verify(harness.engineResource, times(2)).acquire();
  }

  @Test
  public void testNotifiesNewCallbackOfResourceIfCallbackIsAddedDuringOnResourceReady() {
    final EngineJob job = harness.getJob();
    final ResourceCallback existingCallback = mock(ResourceCallback.class);
    final ResourceCallback newCallback = mock(ResourceCallback.class);

    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
        job.addCallback(newCallback);
        return null;
      }
    }).when(existingCallback).onResourceReady(any(Resource.class));

    job.addCallback(existingCallback);
    job.onResourceReady(harness.resource);

    verify(newCallback).onResourceReady(eq(harness.engineResource));
  }

  @Test
  public void testNotifiesNewCallbackOfExceptionIfCallbackIsAddedDuringOnException() {
    harness = new EngineJobHarness();
    final EngineJob job = harness.getJob();
    final ResourceCallback existingCallback = mock(ResourceCallback.class);
    final ResourceCallback newCallback = mock(ResourceCallback.class);

    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
        job.addCallback(newCallback);
        return null;
      }
    }).when(existingCallback).onLoadFailed();

    job.addCallback(existingCallback);
    job.onLoadFailed();

    verify(newCallback).onLoadFailed();
  }

  @Test
  public void testRemovingCallbackDuringOnResourceReadyIsIgnoredIfCallbackHasAlreadyBeenCalled() {
    final EngineJob job = harness.getJob();
    final ResourceCallback cb = mock(ResourceCallback.class);

    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
        job.removeCallback(cb);
        return null;
      }
    }).when(cb).onResourceReady(any(Resource.class));

    job.addCallback(cb);
    job.onResourceReady(harness.resource);

    verify(cb, times(1)).onResourceReady(any(Resource.class));
  }

  @Test
  public void testRemovingCallbackDuringOnExceptionIsIgnoredIfCallbackHasAlreadyBeenCalled() {
    harness = new EngineJobHarness();
    final EngineJob job = harness.getJob();
    final ResourceCallback cb = mock(ResourceCallback.class);

    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
        job.removeCallback(cb);
        return null;
      }
    }).when(cb).onLoadFailed();

    job.addCallback(cb);
    job.onLoadFailed();

    verify(cb, times(1)).onLoadFailed();
  }

  @Test
  public void
  testRemovingCallbackDuringOnResourceReadyPreventsCallbackFromBeingCalledIfNotYetCalled() {
    final EngineJob job = harness.getJob();
    final ResourceCallback notYetCalled = mock(ResourceCallback.class);

    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
        job.removeCallback(notYetCalled);
        return null;
      }
    }).when(harness.cb).onResourceReady(any(Resource.class));

    job.addCallback(notYetCalled);

    job.onResourceReady(harness.resource);

    verify(notYetCalled, never()).onResourceReady(any(Resource.class));
  }

  @Test
  public void
  testRemovingCallbackDuringOnResourceReadyPreventsResourceFromBeingAcquiredForCallback() {
    final EngineJob job = harness.getJob();
    final ResourceCallback notYetCalled = mock(ResourceCallback.class);

    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
        job.removeCallback(notYetCalled);
        return null;
      }
    }).when(harness.cb).onResourceReady(any(Resource.class));

    job.addCallback(notYetCalled);

    job.onResourceReady(harness.resource);

    // Once for notifying, once for called.
    verify(harness.engineResource, times(2)).acquire();
  }

  @Test
  public void testRemovingCallbackDuringOnExceptionPreventsCallbackFromBeingCalledIfNotYetCalled() {
    harness = new EngineJobHarness();
    final EngineJob job = harness.getJob();
    final ResourceCallback called = mock(ResourceCallback.class);
    final ResourceCallback notYetCalled = mock(ResourceCallback.class);

    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
        job.removeCallback(notYetCalled);
        return null;
      }
    }).when(called).onLoadFailed();

    job.addCallback(called);
    job.addCallback(notYetCalled);
    job.onLoadFailed();

    verify(notYetCalled, never()).onResourceReady(any(Resource.class));
  }

  @Test
  public void testRemovingCallbackAfterLoadFailsWithNullExceptionDoesNotCancelJob() {
    EngineJob job = harness.getJob();

    job.onLoadFailed();
    job.removeCallback(harness.cb);
    verify(harness.listener, never()).onEngineJobCancelled(any(EngineJob.class), any(Key.class));
  }

  @Test
  public void testCancelsFutureFromDiskCacheServiceIfCancelledAfterStartButBeforeSourceSubmit() {
    Future future = mock(Future.class);
    when(harness.diskCacheService.submit(eq(harness.decodeJob))).thenReturn(future);

    EngineJob job = harness.getJob();
    job.start(harness.decodeJob);
    job.cancel();

    verify(future).cancel(eq(true));
  }

  @Test
  public void testCancelsFutureFromSourceServiceIfCancelledAfterSourceSubmit() {
    Future future = mock(Future.class);
    when(harness.sourceService.submit(eq(harness.decodeJob))).thenReturn(future);

    EngineJob job = harness.getJob();
    job.start(harness.decodeJob);
    job.reschedule(harness.decodeJob);
    job.cancel();

    verify(future).cancel(eq(true));
  }

  @Test
  public void testCancelsEngineRunnableOnCancel() {
    EngineJob job = harness.getJob();
    job.start(harness.decodeJob);
    job.cancel();

    verify(harness.decodeJob).cancel();
  }

  @Test
  public void testSubmitsRunnableToSourceServiceOnSubmitForSource() {
    EngineJob job = harness.getJob();
    job.reschedule(harness.decodeJob);

    verify(harness.sourceService).submit(eq(harness.decodeJob));
  }

  @Test
  public void testSubimtsRunnableToDiskCacheServiceOnStart() {
    EngineJob job = harness.getJob();
    job.start(harness.decodeJob);

    verify(harness.diskCacheService).submit(eq(harness.decodeJob));
  }

  @SafeVarargs
  private static <T> List<T> list(T... items) {
    return Arrays.asList(items);
  }

  private static class MultiCbHarness {
    Key key = mock(Key.class);
    Resource<Object> resource = mock(Resource.class);
    EngineResource<Object> engineResource = mock(EngineResource.class);
    EngineJobListener listener = mock(EngineJobListener.class);
    boolean isCacheable = true;
    int numCbs = 10;
    List<ResourceCallback> cbs = new ArrayList<>();
    EngineJob.EngineResourceFactory factory = mock(EngineJob.EngineResourceFactory.class);
    EngineJob job;
    ExecutorService diskCacheService = mock(ExecutorService.class);
    ExecutorService sourceService = mock(ExecutorService.class);

    public MultiCbHarness() {
      when(factory.build(eq(resource), eq(isCacheable))).thenReturn(engineResource);
      job = new EngineJob(key, diskCacheService, sourceService, isCacheable, listener, factory);
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
    EngineJob.EngineResourceFactory factory = mock(EngineJob.EngineResourceFactory.class);
    Key key = mock(Key.class);
    Handler mainHandler = new Handler();
    ResourceCallback cb = mock(ResourceCallback.class);
    Resource<Object> resource = mock(Resource.class);
    EngineResource<Object> engineResource = mock(EngineResource.class);
    EngineJobListener listener = mock(EngineJobListener.class);
    ExecutorService diskCacheService = mock(ExecutorService.class);
    ExecutorService sourceService = mock(ExecutorService.class);
    boolean isCacheable = true;
    DecodeJob<Object> decodeJob = mock(DecodeJob.class);

    public EngineJob getJob() {
      when(factory.build(eq(resource), eq(isCacheable))).thenReturn(engineResource);
      EngineJob result =
          new EngineJob(key, diskCacheService, sourceService, isCacheable, listener, factory);
      result.addCallback(cb);
      return result;
    }
  }
}
