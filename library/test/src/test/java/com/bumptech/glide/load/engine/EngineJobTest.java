package com.bumptech.glide.load.engine;

import static com.bumptech.glide.RobolectricConstants.ROBOLECTRIC_SDK;
import static com.bumptech.glide.tests.Util.anyResource;
import static com.bumptech.glide.tests.Util.isADataSource;
import static com.bumptech.glide.tests.Util.mockResource;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.os.Handler;
import android.os.Looper;
import androidx.core.util.Pools;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.EngineResource.ResourceListener;
import com.bumptech.glide.load.engine.executor.GlideExecutor;
import com.bumptech.glide.load.engine.executor.MockGlideExecutor;
import com.bumptech.glide.request.ResourceCallback;
import com.bumptech.glide.util.Executors;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InOrder;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = ROBOLECTRIC_SDK)
public class EngineJobTest {
  private EngineJobHarness harness;

  @Before
  public void setUp() {
    harness = new EngineJobHarness();
  }

  @Test
  public void onResourceReadyPassedToCallbacks() {
    EngineJob<Object> job = harness.getJob();
    job.start(harness.decodeJob);
    job.onResourceReady(
        harness.resource, harness.dataSource, harness.isLoadedFromAlternateCacheKey);

    ShadowLooper.runUiThreadTasks();
    verify(harness.cb)
        .onResourceReady(
            harness.engineResource, harness.dataSource, harness.isLoadedFromAlternateCacheKey);
  }

  @Test
  public void listenerNotifiedJobCompleteOnOnResourceReady() {
    EngineJob<Object> job = harness.getJob();
    job.start(harness.decodeJob);
    job.onResourceReady(
        harness.resource, harness.dataSource, harness.isLoadedFromAlternateCacheKey);

    ShadowLooper.runUiThreadTasks();

    verify(harness.engineJobListener)
        .onEngineJobComplete(eq(job), eq(harness.key), eq(harness.engineResource));
  }

  @Test
  public void notifiesAllCallbacksOnReady() {
    MultiCbHarness harness = new MultiCbHarness();
    harness.job.start(harness.decodeJob);
    harness.job.onResourceReady(
        harness.resource, harness.dataSource, harness.isLoadedFromAlternateCacheKey);
    for (ResourceCallback cb : harness.cbs) {
      verify(cb)
          .onResourceReady(
              harness.engineResource, harness.dataSource, harness.isLoadedFromAlternateCacheKey);
    }
  }

  @Test
  public void notifiesAllCallbacksOnException() {
    MultiCbHarness harness = new MultiCbHarness();
    harness.job.start(harness.decodeJob);
    GlideException exception = new GlideException("test");
    harness.job.onLoadFailed(exception);
    for (ResourceCallback cb : harness.cbs) {
      verify(cb).onLoadFailed(eq(exception));
    }
  }

  @Test
  public void acquiresResourceOncePerCallback() {
    MultiCbHarness harness = new MultiCbHarness();
    harness.job.start(harness.decodeJob);
    harness.job.onResourceReady(
        harness.resource, harness.dataSource, harness.isLoadedFromAlternateCacheKey);

    // Acquired once and then released while notifying.
    InOrder order = inOrder(harness.engineResource);
    order.verify(harness.engineResource, times(harness.numCbs + 1)).acquire();
    order.verify(harness.engineResource, times(1)).release();
  }

  @Test
  public void listenerNotifiedJobCompleteOnException() {
    harness = new EngineJobHarness();
    EngineJob<Object> job = harness.getJob();
    job.start(harness.decodeJob);
    job.onLoadFailed(new GlideException("test"));
    ShadowLooper.runUiThreadTasks();
    verify(harness.engineJobListener)
        .onEngineJobComplete(
            eq(job), eq(harness.key), ArgumentMatchers.<EngineResource<?>>isNull());
  }

  @Test
  public void resourceIsCacheableWhenIsCacheableOnReady() {
    harness.isCacheable = true;
    EngineJob<Object> job = harness.getJob();
    job.start(harness.decodeJob);
    job.onResourceReady(
        harness.resource, harness.dataSource, harness.isLoadedFromAlternateCacheKey);

    ShadowLooper.runUiThreadTasks();
    verify(harness.factory)
        .build(
            anyResource(), eq(harness.isCacheable), eq(harness.key), eq(harness.resourceListener));
  }

  @Test
  public void resourceIsCacheableWhenNotIsCacheableOnReady() {
    harness.isCacheable = false;
    EngineJob<Object> job = harness.getJob();
    job.start(harness.decodeJob);
    job.onResourceReady(
        harness.resource, harness.dataSource, harness.isLoadedFromAlternateCacheKey);

    ShadowLooper.runUiThreadTasks();
    verify(harness.factory)
        .build(
            anyResource(), eq(harness.isCacheable), eq(harness.key), eq(harness.resourceListener));
  }

  @Test
  public void listenerNotifiedOfCancelOnCancel() {
    EngineJob<Object> job = harness.getJob();
    job.start(harness.decodeJob);
    job.cancel();

    verify(harness.engineJobListener).onEngineJobCancelled(eq(job), eq(harness.key));
  }

  @Test
  public void onResourceReadyNotDeliveredAfterCancel() {
    EngineJob<Object> job = harness.getJob();
    job.start(harness.decodeJob);
    job.cancel();

    job.onResourceReady(
        harness.resource, harness.dataSource, harness.isLoadedFromAlternateCacheKey);

    ShadowLooper.runUiThreadTasks();
    verify(harness.cb, never()).onResourceReady(anyResource(), isADataSource(), anyBoolean());
  }

  @Test
  public void onExceptionNotDeliveredAfterCancel() {
    harness = new EngineJobHarness();
    EngineJob<Object> job = harness.getJob();
    job.start(harness.decodeJob);
    job.cancel();

    job.onLoadFailed(new GlideException("test"));

    ShadowLooper.runUiThreadTasks();
    verify(harness.cb, never()).onLoadFailed(any(GlideException.class));
  }

  @Test
  public void removingAllCallbacksCancelsRunner() {
    EngineJob<Object> job = harness.getJob();
    job.start(harness.decodeJob);
    job.removeCallback(harness.cb);

    assertTrue(job.isCancelled());
  }

  @Test
  public void removingSomeCallbacksDoesNotCancelRunner() {
    EngineJob<Object> job = harness.getJob();
    job.addCallback(mockResourceCallback(), Executors.directExecutor());
    job.removeCallback(harness.cb);

    assertFalse(job.isCancelled());
  }

  @Test
  public void resourceIsAcquiredOncePerConsumerAndOnceForCache() {
    EngineJob<Object> job = harness.getJob();
    job.start(harness.decodeJob);
    job.onResourceReady(
        harness.resource, harness.dataSource, harness.isLoadedFromAlternateCacheKey);

    // Once while notifying and once for single callback.
    verify(harness.engineResource, times(2)).acquire();
  }

  @Test
  public void doesNotNotifyCancelledIfCompletes() {
    EngineJob<Object> job = harness.getJob();
    job.start(harness.decodeJob);
    job.onResourceReady(
        harness.resource, harness.dataSource, harness.isLoadedFromAlternateCacheKey);

    verify(harness.engineJobListener, never()).onEngineJobCancelled(eq(job), eq(harness.key));
  }

  @Test
  public void doesNotNotifyCancelledIfAlreadyCancelled() {
    EngineJob<Object> job = harness.getJob();
    job.start(harness.decodeJob);
    job.cancel();
    job.cancel();

    verify(harness.engineJobListener, times(1)).onEngineJobCancelled(eq(job), eq(harness.key));
  }

  @Test
  public void doesNotNotifyCancelledIfReceivedException() {
    EngineJob<Object> job = harness.getJob();
    job.start(harness.decodeJob);
    job.onLoadFailed(new GlideException("test"));

    verify(harness.engineJobListener)
        .onEngineJobComplete(
            eq(job), eq(harness.key), ArgumentMatchers.<EngineResource<?>>isNull());
    verify(harness.engineJobListener, never())
        .onEngineJobCancelled(any(EngineJob.class), any(EngineKey.class));
  }

  @Test
  public void releasesResourceIfCancelledOnReady() {
    Looper looper = harness.mainHandler.getLooper();
    Shadows.shadowOf(looper).pause();

    final EngineJob<Object> job = harness.getJob();
    job.start(harness.decodeJob);
    job.cancel();
    job.onResourceReady(
        harness.resource, harness.dataSource, harness.isLoadedFromAlternateCacheKey);

    verify(harness.resource).recycle();
  }

  @Test
  public void doesNotAcquireOnceForMemoryCacheIfNotCacheable() {
    harness.isCacheable = false;
    EngineJob<Object> job = harness.getJob();
    job.start(harness.decodeJob);
    job.onResourceReady(
        harness.resource, harness.dataSource, harness.isLoadedFromAlternateCacheKey);

    verify(harness.engineResource, times(2)).acquire();
  }

  @Test
  public void notifiesNewCallbackOfResourceIfCallbackIsAddedDuringOnResourceReady() {
    final EngineJob<Object> job = harness.getJob();
    final ResourceCallback existingCallback = mockResourceCallback();
    final ResourceCallback newCallback = mockResourceCallback();

    doAnswer(
            new Answer<Void>() {
              @Override
              public Void answer(InvocationOnMock invocationOnMock) {
                job.addCallback(newCallback, Executors.directExecutor());
                return null;
              }
            })
        .when(existingCallback)
        .onResourceReady(anyResource(), isADataSource(), anyBoolean());

    job.addCallback(existingCallback, Executors.directExecutor());
    job.start(harness.decodeJob);
    job.onResourceReady(
        harness.resource, harness.dataSource, harness.isLoadedFromAlternateCacheKey);

    verify(newCallback)
        .onResourceReady(
            harness.engineResource, harness.dataSource, harness.isLoadedFromAlternateCacheKey);
  }

  @Test
  public void notifiesNewCallbackOfExceptionIfCallbackIsAddedDuringOnException() {
    harness = new EngineJobHarness();
    final EngineJob<Object> job = harness.getJob();
    final ResourceCallback existingCallback = mockResourceCallback();
    final ResourceCallback newCallback = mockResourceCallback();

    doAnswer(
            new Answer<Void>() {
              @Override
              public Void answer(InvocationOnMock invocationOnMock) {
                job.addCallback(newCallback, Executors.directExecutor());
                return null;
              }
            })
        .when(existingCallback)
        .onLoadFailed(any(GlideException.class));

    GlideException exception = new GlideException("test");
    job.addCallback(existingCallback, Executors.directExecutor());
    job.start(harness.decodeJob);
    job.onLoadFailed(exception);

    verify(newCallback).onLoadFailed(eq(exception));
  }

  @Test
  public void removingCallbackDuringOnResourceReadyIsIgnoredIfCallbackHasAlreadyBeenCalled() {
    final EngineJob<Object> job = harness.getJob();
    final ResourceCallback cb = mockResourceCallback();

    doAnswer(
            new Answer<Void>() {
              @Override
              public Void answer(InvocationOnMock invocationOnMock) {
                job.removeCallback(cb);
                return null;
              }
            })
        .when(cb)
        .onResourceReady(anyResource(), isADataSource(), anyBoolean());

    job.addCallback(cb, Executors.directExecutor());
    job.start(harness.decodeJob);
    job.onResourceReady(
        harness.resource, harness.dataSource, harness.isLoadedFromAlternateCacheKey);

    verify(cb, times(1)).onResourceReady(anyResource(), isADataSource(), anyBoolean());
  }

  @Test
  public void removingCallbackDuringOnExceptionIsIgnoredIfCallbackHasAlreadyBeenCalled() {
    harness = new EngineJobHarness();
    final EngineJob<Object> job = harness.getJob();
    final ResourceCallback cb = mockResourceCallback();

    doAnswer(
            new Answer<Void>() {
              @Override
              public Void answer(InvocationOnMock invocationOnMock) {
                job.removeCallback(cb);
                return null;
              }
            })
        .when(cb)
        .onLoadFailed(any(GlideException.class));

    GlideException exception = new GlideException("test");
    job.addCallback(cb, Executors.directExecutor());
    job.start(harness.decodeJob);
    job.onLoadFailed(exception);

    verify(cb, times(1)).onLoadFailed(eq(exception));
  }

  @Test
  public void removingCallbackDuringOnResourceReadyPreventsCallbackFromBeingCalledIfNotYetCalled() {
    final EngineJob<Object> job = harness.getJob();
    final ResourceCallback notYetCalled = mockResourceCallback();

    doAnswer(
            new Answer<Void>() {
              @Override
              public Void answer(InvocationOnMock invocationOnMock) {
                job.removeCallback(notYetCalled);
                return null;
              }
            })
        .when(harness.cb)
        .onResourceReady(anyResource(), isADataSource(), anyBoolean());

    job.addCallback(notYetCalled, Executors.directExecutor());
    job.start(harness.decodeJob);
    job.onResourceReady(
        harness.resource, harness.dataSource, harness.isLoadedFromAlternateCacheKey);

    verify(notYetCalled, never()).onResourceReady(anyResource(), isADataSource(), anyBoolean());
  }

  @Test
  public void removingCallbackDuringOnResourceReadyPreventsResourceFromBeingAcquiredForCallback() {
    final EngineJob<Object> job = harness.getJob();
    final ResourceCallback notYetCalled = mockResourceCallback();

    doAnswer(
            new Answer<Void>() {
              @Override
              public Void answer(InvocationOnMock invocationOnMock) {
                job.removeCallback(notYetCalled);
                return null;
              }
            })
        .when(harness.cb)
        .onResourceReady(anyResource(), isADataSource(), anyBoolean());

    job.addCallback(notYetCalled, Executors.directExecutor());
    job.start(harness.decodeJob);

    job.onResourceReady(
        harness.resource, harness.dataSource, harness.isLoadedFromAlternateCacheKey);

    // Once for notifying, once for called.
    verify(harness.engineResource, times(2)).acquire();
  }

  @Test
  public void removingCallbackDuringOnExceptionPreventsCallbackFromBeingCalledIfNotYetCalled() {
    harness = new EngineJobHarness();
    final EngineJob<Object> job = harness.getJob();
    final ResourceCallback called = mockResourceCallback();
    final ResourceCallback notYetCalled = mockResourceCallback();

    doAnswer(
            new Answer<Void>() {
              @Override
              public Void answer(InvocationOnMock invocationOnMock) {
                job.removeCallback(notYetCalled);
                return null;
              }
            })
        .when(called)
        .onLoadFailed(any(GlideException.class));

    job.addCallback(called, Executors.directExecutor());
    job.addCallback(notYetCalled, Executors.directExecutor());
    job.start(harness.decodeJob);
    job.onLoadFailed(new GlideException("test"));

    verify(notYetCalled, never()).onResourceReady(anyResource(), isADataSource(), anyBoolean());
  }

  @Test
  public void cancelsDecodeJobOnCancel() {
    EngineJob<Object> job = harness.getJob();
    job.start(harness.decodeJob);
    job.cancel();

    verify(harness.decodeJob).cancel();
  }

  @Test
  public void submitsDecodeJobToSourceServiceOnSubmitForSource() {
    EngineJob<Object> job = harness.getJob();
    harness.diskCacheService.shutdownNow();
    job.reschedule(harness.decodeJob);

    verify(harness.decodeJob).run();
  }

  @Test
  public void submitsDecodeJobToDiskCacheServiceWhenDecodingFromCacheOnStart() {
    EngineJob<Object> job = harness.getJob();
    when(harness.decodeJob.willDecodeFromCache()).thenReturn(true);
    harness.sourceService.shutdownNow();
    job.start(harness.decodeJob);

    verify(harness.decodeJob).run();
  }

  @Test
  public void submitsDecodeJobToSourceServiceWhenDecodingFromSourceOnlyOnStart() {
    EngineJob<Object> job = harness.getJob();
    when(harness.decodeJob.willDecodeFromCache()).thenReturn(false);
    harness.diskCacheService.shutdownNow();
    job.start(harness.decodeJob);

    verify(harness.decodeJob).run();
  }

  @Test
  public void submitsDecodeJobToUnlimitedSourceServiceWhenDecodingFromSourceOnlyOnStart() {
    harness.useUnlimitedSourceGeneratorPool = true;
    EngineJob<Object> job = harness.getJob();

    when(harness.decodeJob.willDecodeFromCache()).thenReturn(false);
    harness.diskCacheService.shutdownNow();
    job.start(harness.decodeJob);

    verify(harness.decodeJob).run();
  }

  private static ResourceCallback mockResourceCallback() {
    ResourceCallback result = mock(ResourceCallback.class);
    when(result.getLock()).thenReturn(result);
    return result;
  }

  @SuppressWarnings("unchecked")
  private static class MultiCbHarness {
    final EngineKey key = mock(EngineKey.class);
    final Resource<Object> resource = mockResource();
    final EngineResource<Object> engineResource = mock(EngineResource.class);
    final EngineJobListener engineJobListener = mock(EngineJobListener.class);
    final ResourceListener resourceListener = mock(ResourceListener.class);
    final boolean isCacheable = true;
    final boolean useUnlimitedSourceGeneratorPool = false;
    final boolean useAnimationPool = false;
    final boolean onlyRetrieveFromCache = false;
    final int numCbs = 10;
    final List<ResourceCallback> cbs = new ArrayList<>();
    final EngineJob.EngineResourceFactory factory = mock(EngineJob.EngineResourceFactory.class);
    final EngineJob<Object> job;
    final GlideExecutor diskCacheService = MockGlideExecutor.newMainThreadExecutor();
    final GlideExecutor sourceService = MockGlideExecutor.newMainThreadExecutor();
    final GlideExecutor sourceUnlimitedService = MockGlideExecutor.newMainThreadExecutor();
    final GlideExecutor animationService = MockGlideExecutor.newMainThreadExecutor();
    final Pools.Pool<EngineJob<?>> pool = new Pools.SimplePool<>(1);
    final DecodeJob<Object> decodeJob = mock(DecodeJob.class);
    final DataSource dataSource = DataSource.LOCAL;
    final boolean isLoadedFromAlternateCacheKey = true;

    MultiCbHarness() {
      when(factory.build(resource, isCacheable, key, resourceListener)).thenReturn(engineResource);
      job =
          new EngineJob<>(
              diskCacheService,
              sourceService,
              sourceUnlimitedService,
              animationService,
              engineJobListener,
              resourceListener,
              pool,
              factory);
      job.init(
          key,
          isCacheable,
          useUnlimitedSourceGeneratorPool,
          useAnimationPool,
          onlyRetrieveFromCache);
      for (int i = 0; i < numCbs; i++) {
        cbs.add(mockResourceCallback());
      }
      for (ResourceCallback cb : cbs) {
        job.addCallback(cb, Executors.directExecutor());
      }
    }
  }

  @SuppressWarnings("unchecked")
  private static class EngineJobHarness {
    final EngineJob.EngineResourceFactory factory = mock(EngineJob.EngineResourceFactory.class);
    final EngineKey key = mock(EngineKey.class);
    final Handler mainHandler = new Handler();
    final ResourceCallback cb = mockResourceCallback();
    final Resource<Object> resource = mockResource();
    final EngineResource<Object> engineResource = mock(EngineResource.class);
    final EngineJobListener engineJobListener = mock(EngineJobListener.class);
    final ResourceListener resourceListener = mock(ResourceListener.class);
    final GlideExecutor diskCacheService = MockGlideExecutor.newMainThreadExecutor();
    final GlideExecutor sourceService = MockGlideExecutor.newMainThreadExecutor();
    final GlideExecutor sourceUnlimitedService = MockGlideExecutor.newMainThreadExecutor();
    final GlideExecutor animationService = MockGlideExecutor.newMainThreadExecutor();
    boolean isCacheable = true;
    boolean useUnlimitedSourceGeneratorPool = false;
    final boolean useAnimationPool = false;
    final boolean onlyRetrieveFromCache = false;
    final DecodeJob<Object> decodeJob = mock(DecodeJob.class);
    final Pools.Pool<EngineJob<?>> pool = new Pools.SynchronizedPool<>(1);
    final DataSource dataSource = DataSource.DATA_DISK_CACHE;
    final boolean isLoadedFromAlternateCacheKey = true;

    EngineJob<Object> getJob() {
      when(factory.build(resource, isCacheable, key, resourceListener)).thenReturn(engineResource);
      EngineJob<Object> result =
          new EngineJob<>(
              diskCacheService,
              sourceService,
              sourceUnlimitedService,
              animationService,
              engineJobListener,
              resourceListener,
              pool,
              factory);
      result.init(
          key,
          isCacheable,
          useUnlimitedSourceGeneratorPool,
          useAnimationPool,
          onlyRetrieveFromCache);
      result.addCallback(cb, Executors.directExecutor());
      return result;
    }
  }
}
