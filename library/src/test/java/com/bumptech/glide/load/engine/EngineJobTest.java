package com.bumptech.glide.load.engine;

import static com.bumptech.glide.tests.Util.anyResource;
import static com.bumptech.glide.tests.Util.isADataSource;
import static com.bumptech.glide.tests.Util.mockResource;
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
import android.support.v4.util.Pools;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.engine.executor.GlideExecutor;
import com.bumptech.glide.load.engine.executor.MockGlideExecutor;
import com.bumptech.glide.request.ResourceCallback;
import java.util.ArrayList;
import java.util.List;
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

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 18)
public class EngineJobTest {
  private EngineJobHarness harness;

  @Before
  public void setUp() {
    harness = new EngineJobHarness();
  }

  @Test
  public void testOnResourceReadyPassedToCallbacks() throws Exception {
    EngineJob<Object> job = harness.getJob();
    job.start(harness.decodeJob);
    job.onResourceReady(harness.resource, harness.dataSource);

    ShadowLooper.runUiThreadTasks();
    verify(harness.cb).onResourceReady(eq(harness.engineResource), eq(harness.dataSource));
  }

  @Test
  public void testListenerNotifiedJobCompleteOnOnResourceReady() {
    EngineJob<Object> job = harness.getJob();
    job.start(harness.decodeJob);
    job.onResourceReady(harness.resource, harness.dataSource);

    ShadowLooper.runUiThreadTasks();

    verify(harness.listener).onEngineJobComplete(eq(harness.key), eq(harness.engineResource));
  }

  @Test
  public void testNotifiesAllCallbacksOnReady() {
    MultiCbHarness harness = new MultiCbHarness();
    harness.job.start(harness.decodeJob);
    harness.job.onResourceReady(harness.resource, harness.dataSource);
    for (ResourceCallback cb : harness.cbs) {
      verify(cb).onResourceReady(eq(harness.engineResource), eq(harness.dataSource));
    }
  }

  @Test
  public void testNotifiesAllCallbacksOnException() {
    MultiCbHarness harness = new MultiCbHarness();
    harness.job.start(harness.decodeJob);
    GlideException exception = new GlideException("test");
    harness.job.onLoadFailed(exception);
    for (ResourceCallback cb : harness.cbs) {
      verify(cb).onLoadFailed(eq(exception));
    }
  }

  @Test
  public void testAcquiresResourceOncePerCallback() {
    MultiCbHarness harness = new MultiCbHarness();
    harness.job.start(harness.decodeJob);
    harness.job.onResourceReady(harness.resource, harness.dataSource);

    // Acquired once and then released while notifying.
    InOrder order = inOrder(harness.engineResource);
    order.verify(harness.engineResource, times(harness.numCbs + 1)).acquire();
    order.verify(harness.engineResource, times(1)).release();
  }

  @Test
  public void testListenerNotifiedJobCompleteOnException() {
    harness = new EngineJobHarness();
    EngineJob<Object> job = harness.getJob();
    job.start(harness.decodeJob);
    job.onLoadFailed(new GlideException("test"));
    ShadowLooper.runUiThreadTasks();
    verify(harness.listener).onEngineJobComplete(eq(harness.key), isNull(EngineResource.class));
  }

  @Test
  public void testResourceIsCacheableWhenIsCacheableOnReady() {
    harness.isCacheable = true;
    EngineJob<Object> job = harness.getJob();
    job.start(harness.decodeJob);
    job.onResourceReady(harness.resource, harness.dataSource);

    ShadowLooper.runUiThreadTasks();
    verify(harness.factory).build(anyResource(), eq(harness.isCacheable));
  }

  @Test
  public void testResourceIsCacheableWhenNotIsCacheableOnReady() {
    harness.isCacheable = false;
    EngineJob<Object> job = harness.getJob();
    job.start(harness.decodeJob);
    job.onResourceReady(harness.resource, harness.dataSource);

    ShadowLooper.runUiThreadTasks();
    verify(harness.factory).build(anyResource(), eq(harness.isCacheable));
  }

  @Test
  public void testListenerNotifiedOfCancelOnCancel() {
    EngineJob<Object> job = harness.getJob();
    job.start(harness.decodeJob);
    job.cancel();

    verify(harness.listener).onEngineJobCancelled(eq(job), eq(harness.key));
  }

  @Test
  public void testOnResourceReadyNotDeliveredAfterCancel() {
    EngineJob<Object> job = harness.getJob();
    job.start(harness.decodeJob);
    job.cancel();

    job.onResourceReady(harness.resource, harness.dataSource);

    ShadowLooper.runUiThreadTasks();
    verify(harness.cb, never()).onResourceReady(anyResource(), isADataSource());
  }

  @Test
  public void testOnExceptionNotDeliveredAfterCancel() {
    harness = new EngineJobHarness();
    EngineJob<Object> job = harness.getJob();
    job.start(harness.decodeJob);
    job.cancel();

    job.onLoadFailed(new GlideException("test"));

    ShadowLooper.runUiThreadTasks();
    verify(harness.cb, never()).onLoadFailed(any(GlideException.class));
  }

  @Test
  public void testRemovingAllCallbacksCancelsRunner() {
    EngineJob<Object> job = harness.getJob();
    job.start(harness.decodeJob);
    job.removeCallback(harness.cb);

    assertTrue(job.isCancelled());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void removingSomeCallbacksDoesNotCancelRunner() {
    EngineJob<Object> job = harness.getJob();
    job.addCallback(mock(ResourceCallback.class));
    job.removeCallback(harness.cb);

    assertFalse(job.isCancelled());
  }

  @Test
  public void testResourceIsAcquiredOncePerConsumerAndOnceForCache() {
    EngineJob<Object> job = harness.getJob();
    job.start(harness.decodeJob);
    job.onResourceReady(harness.resource, harness.dataSource);

    // Once while notifying and once for single callback.
    verify(harness.engineResource, times(2)).acquire();
  }

  @Test
  public void testDoesNotNotifyCancelledIfCompletes() {
    EngineJob<Object> job = harness.getJob();
    job.start(harness.decodeJob);
    job.onResourceReady(harness.resource, harness.dataSource);

    verify(harness.listener, never()).onEngineJobCancelled(eq(job), eq(harness.key));
  }

  @Test
  public void testDoesNotNotifyCancelledIfAlreadyCancelled() {
    EngineJob<Object> job = harness.getJob();
    job.start(harness.decodeJob);
    job.cancel();
    job.cancel();

    verify(harness.listener, times(1)).onEngineJobCancelled(eq(job), eq(harness.key));
  }

  @Test
  public void testDoesNotNotifyCancelledIfReceivedException() {
    EngineJob<Object> job = harness.getJob();
    job.start(harness.decodeJob);
    job.onLoadFailed(new GlideException("test"));

    verify(harness.listener).onEngineJobComplete(eq(harness.key), isNull(EngineResource.class));
    verify(harness.listener, never()).onEngineJobCancelled(any(EngineJob.class), any(Key.class));
  }

  @Test
  public void testReleasesResourceIfCancelledOnReady() {
    ShadowLooper shadowLooper = Shadows.shadowOf(harness.mainHandler.getLooper());
    shadowLooper.pause();

    EngineJob<Object> job = harness.getJob();
    job.start(harness.decodeJob);
    job.onResourceReady(harness.resource, harness.dataSource);
    job.cancel();
    shadowLooper.runOneTask();

    verify(harness.resource).recycle();
  }

  @Test
  public void testDoesNotAcquireOnceForMemoryCacheIfNotCacheable() {
    harness.isCacheable = false;
    EngineJob<Object> job = harness.getJob();
    job.start(harness.decodeJob);
    job.onResourceReady(harness.resource, harness.dataSource);

    verify(harness.engineResource, times(2)).acquire();
  }

  @Test
  public void testNotifiesNewCallbackOfResourceIfCallbackIsAddedDuringOnResourceReady() {
    final EngineJob<Object> job = harness.getJob();
    final ResourceCallback existingCallback = mock(ResourceCallback.class);
    final ResourceCallback newCallback = mock(ResourceCallback.class);

    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
        job.addCallback(newCallback);
        return null;
      }
    }).when(existingCallback).onResourceReady(anyResource(), isADataSource());

    job.addCallback(existingCallback);
    job.start(harness.decodeJob);
    job.onResourceReady(harness.resource, harness.dataSource);

    verify(newCallback).onResourceReady(eq(harness.engineResource), eq(harness.dataSource));
  }

  @Test
  public void testNotifiesNewCallbackOfExceptionIfCallbackIsAddedDuringOnException() {
    harness = new EngineJobHarness();
    final EngineJob<Object> job = harness.getJob();
    final ResourceCallback existingCallback = mock(ResourceCallback.class);
    final ResourceCallback newCallback = mock(ResourceCallback.class);

    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
        job.addCallback(newCallback);
        return null;
      }
    }).when(existingCallback).onLoadFailed(any(GlideException.class));

    GlideException exception = new GlideException("test");
    job.addCallback(existingCallback);
    job.start(harness.decodeJob);
    job.onLoadFailed(exception);

    verify(newCallback).onLoadFailed(eq(exception));
  }

  @Test
  public void testRemovingCallbackDuringOnResourceReadyIsIgnoredIfCallbackHasAlreadyBeenCalled() {
    final EngineJob<Object> job = harness.getJob();
    final ResourceCallback cb = mock(ResourceCallback.class);

    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
        job.removeCallback(cb);
        return null;
      }
    }).when(cb).onResourceReady(anyResource(), isADataSource());

    job.addCallback(cb);
    job.start(harness.decodeJob);
    job.onResourceReady(harness.resource, harness.dataSource);

    verify(cb, times(1)).onResourceReady(anyResource(), isADataSource());
  }

  @Test
  public void testRemovingCallbackDuringOnExceptionIsIgnoredIfCallbackHasAlreadyBeenCalled() {
    harness = new EngineJobHarness();
    final EngineJob<Object> job = harness.getJob();
    final ResourceCallback cb = mock(ResourceCallback.class);

    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
        job.removeCallback(cb);
        return null;
      }
    }).when(cb).onLoadFailed(any(GlideException.class));

    GlideException exception = new GlideException("test");
    job.addCallback(cb);
    job.start(harness.decodeJob);
    job.onLoadFailed(exception);

    verify(cb, times(1)).onLoadFailed(eq(exception));
  }

  @Test
  public void
  testRemovingCallbackDuringOnResourceReadyPreventsCallbackFromBeingCalledIfNotYetCalled() {
    final EngineJob<Object> job = harness.getJob();
    final ResourceCallback notYetCalled = mock(ResourceCallback.class);

    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
        job.removeCallback(notYetCalled);
        return null;
      }
    }).when(harness.cb).onResourceReady(anyResource(), isADataSource());

    job.addCallback(notYetCalled);
    job.start(harness.decodeJob);
    job.onResourceReady(harness.resource, harness.dataSource);

    verify(notYetCalled, never()).onResourceReady(anyResource(), isADataSource());
  }

  @Test
  public void
  testRemovingCallbackDuringOnResourceReadyPreventsResourceFromBeingAcquiredForCallback() {
    final EngineJob<Object> job = harness.getJob();
    final ResourceCallback notYetCalled = mock(ResourceCallback.class);

    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
        job.removeCallback(notYetCalled);
        return null;
      }
    }).when(harness.cb).onResourceReady(anyResource(), isADataSource());

    job.addCallback(notYetCalled);
    job.start(harness.decodeJob);

    job.onResourceReady(harness.resource, harness.dataSource);

    // Once for notifying, once for called.
    verify(harness.engineResource, times(2)).acquire();
  }

  @Test
  public void testRemovingCallbackDuringOnExceptionPreventsCallbackFromBeingCalledIfNotYetCalled() {
    harness = new EngineJobHarness();
    final EngineJob<Object> job = harness.getJob();
    final ResourceCallback called = mock(ResourceCallback.class);
    final ResourceCallback notYetCalled = mock(ResourceCallback.class);

    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
        job.removeCallback(notYetCalled);
        return null;
      }
    }).when(called).onLoadFailed(any(GlideException.class));

    job.addCallback(called);
    job.addCallback(notYetCalled);
    job.start(harness.decodeJob);
    job.onLoadFailed(new GlideException("test"));

    verify(notYetCalled, never()).onResourceReady(anyResource(), isADataSource());
  }

  @Test
  public void testCancelsDecodeJobOnCancel() {
    EngineJob<Object> job = harness.getJob();
    job.start(harness.decodeJob);
    job.cancel();

    verify(harness.decodeJob).cancel();
  }

  @Test
  public void testSubmitsDecodeJobToSourceServiceOnSubmitForSource() {
    EngineJob<Object> job = harness.getJob();
    harness.diskCacheService.shutdownNow();
    job.reschedule(harness.decodeJob);

    verify(harness.decodeJob).run();
  }

  @Test
  public void testSubmitsDecodeJobToDiskCacheServiceWhenDecodingFromCacheOnStart() {
    EngineJob<Object> job = harness.getJob();
    when(harness.decodeJob.willDecodeFromCache()).thenReturn(true);
    harness.diskCacheService.shutdownNow();
    job.start(harness.decodeJob);

    verify(harness.decodeJob).run();
  }

  @Test
  public void testSubmitsDecodeJobToSourceServiceWhenDecodingFromSourceOnlyOnStart() {
    EngineJob<Object> job = harness.getJob();
    when(harness.decodeJob.willDecodeFromCache()).thenReturn(false);
    harness.diskCacheService.shutdownNow();
    job.start(harness.decodeJob);

    verify(harness.decodeJob).run();
  }

  @Test
  public void testSubmitsDecodeJobToUnlimitedSourceServiceWhenDecodingFromSourceOnlyOnStart() {
    harness.useUnlimitedSourceGeneratorPool = true;
    EngineJob<Object> job = harness.getJob();

    when(harness.decodeJob.willDecodeFromCache()).thenReturn(false);
    harness.diskCacheService.shutdownNow();
    job.start(harness.decodeJob);

    verify(harness.decodeJob).run();
  }

  @SuppressWarnings("unchecked")
  private static class MultiCbHarness {
    Key key = mock(Key.class);
    Resource<Object> resource = mockResource();
    EngineResource<Object> engineResource = mock(EngineResource.class);
    EngineJobListener listener = mock(EngineJobListener.class);
    boolean isCacheable = true;
    boolean useUnlimitedSourceGeneratorPool = false;
    int numCbs = 10;
    List<ResourceCallback> cbs = new ArrayList<>();
    EngineJob.EngineResourceFactory factory = mock(EngineJob.EngineResourceFactory.class);
    EngineJob<Object> job;
    GlideExecutor diskCacheService = MockGlideExecutor.newMainThreadExecutor();
    GlideExecutor sourceService = MockGlideExecutor.newMainThreadExecutor();
    GlideExecutor sourceUnlimitedService = MockGlideExecutor.newMainThreadUnlimitedExecutor();
    Pools.Pool<EngineJob<?>> pool = new Pools.SimplePool<>(1);
    DecodeJob<Object> decodeJob = mock(DecodeJob.class);
    DataSource dataSource = DataSource.LOCAL;

    public MultiCbHarness() {
      when(factory.build(eq(resource), eq(isCacheable))).thenReturn(engineResource);
      job = new EngineJob<>(diskCacheService, sourceService, sourceUnlimitedService, listener, pool,
          factory).init(key, isCacheable, useUnlimitedSourceGeneratorPool);
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
    Resource<Object> resource = mockResource();
    EngineResource<Object> engineResource = mock(EngineResource.class);
    EngineJobListener listener = mock(EngineJobListener.class);
    GlideExecutor diskCacheService = MockGlideExecutor.newMainThreadExecutor();
    GlideExecutor sourceService = MockGlideExecutor.newMainThreadExecutor();
    GlideExecutor sourceUnlimitedService = MockGlideExecutor.newMainThreadUnlimitedExecutor();
    boolean isCacheable = true;
    boolean useUnlimitedSourceGeneratorPool = false;
    DecodeJob<Object> decodeJob = mock(DecodeJob.class);
    Pools.Pool<EngineJob<?>> pool = new Pools.SimplePool<>(1);
    DataSource dataSource = DataSource.DATA_DISK_CACHE;

    public EngineJob<Object> getJob() {
      when(factory.build(eq(resource), eq(isCacheable))).thenReturn(engineResource);
      EngineJob<Object> result = new EngineJob<>(
          diskCacheService, sourceService, sourceUnlimitedService, listener, pool, factory)
          .init(key, isCacheable, useUnlimitedSourceGeneratorPool);
      result.addCallback(cb);
      return result;
    }
  }
}
