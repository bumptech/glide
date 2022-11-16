package com.bumptech.glide.load.engine;

import static com.bumptech.glide.tests.Util.anyResource;
import static com.bumptech.glide.tests.Util.isADataSource;
import static com.bumptech.glide.tests.Util.mockResource;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bumptech.glide.GlideContext;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.cache.DiskCache;
import com.bumptech.glide.load.engine.cache.LruResourceCache;
import com.bumptech.glide.load.engine.cache.MemoryCache;
import com.bumptech.glide.load.engine.executor.GlideExecutor;
import com.bumptech.glide.load.engine.executor.MockGlideExecutor;
import com.bumptech.glide.request.ResourceCallback;
import com.bumptech.glide.tests.BackgroundUtil;
import com.bumptech.glide.util.Executors;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 18)
@SuppressWarnings("unchecked")
public class EngineTest {
  private EngineTestHarness harness;

  @Before
  public void setUp() {
    harness = new EngineTestHarness();
  }

  @Test
  public void testNewRunnerIsCreatedAndPostedWithNoExistingLoad() {
    harness.doLoad();

    verify(harness.job).start((DecodeJob) any());
  }

  @Test
  public void testCallbackIsAddedToNewEngineJobWithNoExistingLoad() {
    harness.doLoad();

    verify(harness.job).addCallback(eq(harness.cb), any(Executor.class));
  }

  @Test
  public void testLoadStatusIsReturnedForNewLoad() {
    assertNotNull(harness.doLoad());
  }

  @Test
  public void testEngineJobReceivesRemoveCallbackFromLoadStatus() {
    Engine.LoadStatus loadStatus = harness.doLoad();
    loadStatus.cancel();

    verify(harness.job).removeCallback(eq(harness.cb));
  }

  @Test
  public void testNewRunnerIsAddedToRunnersMap() {
    harness.doLoad();

    assertThat(harness.jobs.getAll()).containsKey(harness.cacheKey);
  }

  @Test
  public void testNewRunnerIsNotCreatedAndPostedWithExistingLoad() {
    harness.doLoad();
    harness.doLoad();

    verify(harness.job, times(1)).start((DecodeJob) any());
  }

  @Test
  public void testCallbackIsAddedToExistingRunnerWithExistingLoad() {
    harness.doLoad();

    ResourceCallback newCallback = mock(ResourceCallback.class);
    harness.cb = newCallback;
    harness.doLoad();

    verify(harness.job).addCallback(eq(newCallback), any(Executor.class));
  }

  @Test
  public void testLoadStatusIsReturnedForExistingJob() {
    harness.doLoad();
    Engine.LoadStatus loadStatus = harness.doLoad();

    assertNotNull(loadStatus);
  }

  @Test
  public void testResourceIsReturnedFromActiveResourcesIfPresent() {
    harness.activeResources.activate(harness.cacheKey, harness.resource);

    harness.doLoad();

    verify(harness.cb)
        .onResourceReady(eq(harness.resource), eq(DataSource.MEMORY_CACHE), eq(false));
  }

  @Test
  public void testResourceIsAcquiredIfReturnedFromActiveResources() {
    harness.activeResources.activate(harness.cacheKey, harness.resource);

    harness.doLoad();

    verify(harness.resource).acquire();
  }

  @Test
  public void testNewLoadIsNotStartedIfResourceIsActive() {
    harness.activeResources.activate(harness.cacheKey, harness.resource);

    harness.doLoad();

    verify(harness.job, never()).start(anyDecodeJobOrNull());
  }

  @Test
  public void testNullLoadStatusIsReturnedIfResourceIsActive() {
    harness.activeResources.activate(harness.cacheKey, harness.resource);

    assertNull(harness.doLoad());
  }

  @Test
  public void load_withResourceInActiveResources_doesNotCheckMemoryCache() {
    harness.activeResources.activate(harness.cacheKey, harness.resource);

    harness.doLoad();

    verify(harness.cb)
        .onResourceReady(eq(harness.resource), eq(DataSource.MEMORY_CACHE), eq(false));
    verify(harness.cache, never()).remove(any(Key.class));
  }

  @Test
  public void testActiveResourcesIsNotCheckedIfNotMemoryCacheable() {
    harness.activeResources.activate(harness.cacheKey, harness.resource);

    harness.isMemoryCacheable = false;
    harness.doLoad();

    verify(harness.resource, never()).acquire();
    verify(harness.job).start((DecodeJob) any());
  }

  @Test
  public void testCacheIsCheckedIfMemoryCacheable() {
    when(harness.cache.remove(eq(harness.cacheKey))).thenReturn(harness.resource);

    harness.doLoad();

    verify(harness.cb)
        .onResourceReady(eq(harness.resource), eq(DataSource.MEMORY_CACHE), eq(false));
  }

  @Test
  public void testCacheIsNotCheckedIfNotMemoryCacheable() {
    when(harness.cache.remove(eq(harness.cacheKey))).thenReturn(harness.resource);

    harness.isMemoryCacheable = false;
    harness.doLoad();

    verify(harness.job).start((DecodeJob) any());
  }

  @Test
  public void testResourceIsReturnedFromCacheIfPresent() {
    when(harness.cache.remove(eq(harness.cacheKey))).thenReturn(harness.resource);

    harness.doLoad();

    verify(harness.cb)
        .onResourceReady(eq(harness.resource), eq(DataSource.MEMORY_CACHE), eq(false));
  }

  @Test
  public void testHandlesNonEngineResourcesFromCacheIfPresent() {
    final Object expected = new Object();
    @SuppressWarnings("rawtypes")
    Resource fromCache = mockResource();
    when(fromCache.get()).thenReturn(expected);
    when(harness.cache.remove(eq(harness.cacheKey))).thenReturn(fromCache);

    doAnswer(
            new Answer<Void>() {
              @Override
              public Void answer(InvocationOnMock invocationOnMock) {
                Resource<?> resource = (Resource<?>) invocationOnMock.getArguments()[0];
                assertEquals(expected, resource.get());
                return null;
              }
            })
        .when(harness.cb)
        .onResourceReady(anyResource(), isADataSource(), anyBoolean());

    harness.doLoad();

    verify(harness.cb).onResourceReady(anyResource(), isADataSource(), anyBoolean());
  }

  @Test
  public void testResourceIsAddedToActiveResourceIfReturnedFromCache() {
    when(harness.cache.remove(eq(harness.cacheKey))).thenReturn(harness.resource);

    harness.doLoad();
    EngineResource<?> activeResource = harness.activeResources.get(harness.cacheKey);
    assertThat(activeResource).isEqualTo(harness.resource);
  }

  @Test
  public void testResourceIsAcquiredIfReturnedFromCache() {
    when(harness.cache.remove(eq(harness.cacheKey))).thenReturn(harness.resource);

    harness.doLoad();

    verify(harness.resource).acquire();
  }

  @Test
  public void testNewLoadIsNotStartedIfResourceIsCached() {
    when(harness.cache.remove(eq(harness.cacheKey))).thenReturn(harness.resource);

    harness.doLoad();

    verify(harness.job, never()).start(anyDecodeJobOrNull());
  }

  @Test
  public void testNullLoadStatusIsReturnedForCachedResource() {
    when(harness.cache.remove(eq(harness.cacheKey))).thenReturn(harness.resource);

    Engine.LoadStatus loadStatus = harness.doLoad();
    assertNull(loadStatus);
  }

  @Test
  public void testRunnerIsRemovedFromRunnersOnEngineNotifiedJobComplete() {
    harness.doLoad();

    harness.callOnEngineJobComplete();

    assertThat(harness.jobs.getAll()).doesNotContainKey(harness.cacheKey);
  }

  @Test
  public void testEngineIsNotSetAsResourceListenerIfResourceIsNullOnJobComplete() {
    harness.doLoad();

    harness.getEngine().onEngineJobComplete(harness.job, harness.cacheKey, /* resource= */ null);
  }

  @Test
  public void testResourceIsAddedToActiveResourcesOnEngineComplete() {
    when(harness.resource.isMemoryCacheable()).thenReturn(true);
    harness.callOnEngineJobComplete();

    EngineResource<?> resource = harness.activeResources.get(harness.cacheKey);
    assertThat(harness.resource).isEqualTo(resource);
  }

  @Test
  public void testDoesNotPutNullResourceInActiveResourcesOnEngineComplete() {
    harness.getEngine().onEngineJobComplete(harness.job, harness.cacheKey, /* resource= */ null);
    assertThat(harness.activeResources.get(harness.cacheKey)).isNull();
  }

  @Test
  public void testDoesNotPutResourceThatIsNotCacheableInActiveResourcesOnEngineComplete() {
    when(harness.resource.isMemoryCacheable()).thenReturn(false);
    harness.callOnEngineJobComplete();
    assertThat(harness.activeResources.get(harness.cacheKey)).isNull();
  }

  @Test
  public void testRunnerIsRemovedFromRunnersOnEngineNotifiedJobCancel() {
    harness.doLoad();

    harness.getEngine().onEngineJobCancelled(harness.job, harness.cacheKey);

    assertThat(harness.jobs.getAll()).doesNotContainKey(harness.cacheKey);
  }

  @Test
  public void testJobIsNotRemovedFromJobsIfOldJobIsCancelled() {
    harness.doLoad();

    harness.getEngine().onEngineJobCancelled(mock(EngineJob.class), harness.cacheKey);

    assertEquals(harness.job, harness.jobs.get(harness.cacheKey, harness.onlyRetrieveFromCache));
  }

  @Test
  public void testResourceIsAddedToCacheOnReleased() {
    final Object expected = new Object();
    when(harness.resource.isMemoryCacheable()).thenReturn(true);
    when(harness.resource.get()).thenReturn(expected);
    doAnswer(
            new Answer<Void>() {
              @Override
              public Void answer(InvocationOnMock invocationOnMock) {
                Resource<?> resource = (Resource<?>) invocationOnMock.getArguments()[1];
                assertEquals(expected, resource.get());
                return null;
              }
            })
        .when(harness.cache)
        .put(eq(harness.cacheKey), anyResource());

    harness.getEngine().onResourceReleased(harness.cacheKey, harness.resource);

    verify(harness.cache).put(eq(harness.cacheKey), anyResource());
  }

  @Test
  public void testResourceIsNotAddedToCacheOnReleasedIfNotCacheable() {
    when(harness.resource.isMemoryCacheable()).thenReturn(false);
    harness.getEngine().onResourceReleased(harness.cacheKey, harness.resource);

    verify(harness.cache, never()).put(eq(harness.cacheKey), eq(harness.resource));
  }

  @Test
  public void testResourceIsRecycledIfNotCacheableWhenReleased() {
    when(harness.resource.isMemoryCacheable()).thenReturn(false);
    harness.getEngine().onResourceReleased(harness.cacheKey, harness.resource);
    verify(harness.resourceRecycler).recycle(eq(harness.resource), eq(false));
  }

  @Test
  public void testResourceIsRemovedFromActiveResourcesWhenReleased() {
    harness.activeResources.activate(harness.cacheKey, harness.resource);

    harness.getEngine().onResourceReleased(harness.cacheKey, harness.resource);

    assertThat(harness.activeResources.get(harness.cacheKey)).isNull();
  }

  @Test
  public void testEngineAddedAsListenerToMemoryCache() {
    harness.getEngine();
    verify(harness.cache).setResourceRemovedListener(eq(harness.getEngine()));
  }

  @Test
  public void testResourceIsRecycledWhenRemovedFromCache() {
    harness.getEngine().onResourceRemoved(harness.resource);
    verify(harness.resourceRecycler).recycle(eq(harness.resource), eq(true));
  }

  @Test
  public void testJobIsPutInJobWithCacheKeyWithRelevantIds() {
    harness.doLoad();

    assertThat(harness.jobs.getAll()).containsEntry(harness.cacheKey, harness.job);
  }

  @Test
  public void testKeyFactoryIsGivenNecessaryArguments() {
    harness.doLoad();

    verify(harness.keyFactory)
        .buildKey(
            eq(harness.model),
            eq(harness.signature),
            eq(harness.width),
            eq(harness.height),
            eq(harness.transformations),
            eq(Object.class),
            eq(Object.class),
            eq(harness.options));
  }

  @Test
  public void testFactoryIsGivenNecessaryArguments() {
    harness.doLoad();

    verify(harness.engineJobFactory)
        .build(
            eq(harness.cacheKey),
            eq(true) /*isMemoryCacheable*/,
            eq(false) /*useUnlimitedSourceGeneratorPool*/,
            /* useAnimationPool= */ eq(false),
            /* onlyRetrieveFromCache= */ eq(false));
  }

  @Test
  public void testFactoryIsGivenNecessaryArgumentsWithUnlimitedPool() {
    harness.useUnlimitedSourceGeneratorPool = true;
    harness.doLoad();

    verify(harness.engineJobFactory)
        .build(
            eq(harness.cacheKey),
            eq(true) /*isMemoryCacheable*/,
            eq(true) /*useUnlimitedSourceGeneratorPool*/,
            /* useAnimationPool= */ eq(false),
            /* onlyRetrieveFromCache= */ eq(false));
  }

  @Test
  public void testReleaseReleasesEngineResource() {
    EngineResource<Object> engineResource = mock(EngineResource.class);
    harness.getEngine().release(engineResource);
    verify(engineResource).release();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testThrowsIfAskedToReleaseNonEngineResource() {
    harness.getEngine().release(mockResource());
  }

  @Test
  public void load_whenCalledOnBackgroundThread_doesNotThrow() throws InterruptedException {
    BackgroundUtil.testInBackground(
        new BackgroundUtil.BackgroundTester() {
          @Override
          public void runTest() {
            harness.doLoad();
          }
        });
  }

  @Test
  public void load_afterResourceIsLoadedInActiveResources_returnsFromMemoryCache() {
    when(harness.resource.isMemoryCacheable()).thenReturn(true);
    doAnswer(
            new Answer<Object>() {
              @Override
              public Object answer(InvocationOnMock invocationOnMock) {
                harness.callOnEngineJobComplete();
                return null;
              }
            })
        .when(harness.job)
        .start(anyDecodeJobOrNull());
    harness.doLoad();
    harness.doLoad();
    verify(harness.cb).onResourceReady(any(Resource.class), eq(DataSource.MEMORY_CACHE), eq(false));
  }

  @Test
  public void load_afterResourceIsLoadedAndReleased_returnsFromMemoryCache() {
    harness.cache = new LruResourceCache(100);
    when(harness.resource.isMemoryCacheable()).thenReturn(true);
    doAnswer(
            new Answer<Object>() {
              @Override
              public Object answer(InvocationOnMock invocationOnMock) {
                harness.callOnEngineJobComplete();
                return null;
              }
            })
        .when(harness.job)
        .start(anyDecodeJobOrNull());
    harness.doLoad();
    harness.getEngine().onResourceReleased(harness.cacheKey, harness.resource);
    harness.doLoad();
    verify(harness.cb).onResourceReady(any(Resource.class), eq(DataSource.MEMORY_CACHE), eq(false));
  }

  @Test
  public void load_withOnlyRetrieveFromCache_andPreviousNormalLoad_startsNewLoad() {
    EngineJob<?> first = harness.job;
    harness.doLoad();
    EngineJob<?> second = mock(EngineJob.class);
    harness.job = second;
    harness.onlyRetrieveFromCache = true;
    harness.doLoad();

    verify(first).start(anyDecodeJobOrNull());
    verify(second).start(anyDecodeJobOrNull());
  }

  @Test
  public void load_withNormalLoad_afterPreviousRetrieveFromCache_startsNewLoad() {
    EngineJob<?> first = harness.job;
    harness.onlyRetrieveFromCache = true;
    harness.doLoad();
    EngineJob<?> second = mock(EngineJob.class);
    harness.job = second;
    harness.onlyRetrieveFromCache = false;
    harness.doLoad();

    verify(first).start(anyDecodeJobOrNull());
    verify(second).start(anyDecodeJobOrNull());
  }

  @Test
  public void load_afterFinishedOnlyRetrieveFromCache_withPendingNormal_doesNotStartNewLoad() {
    EngineJob<?> firstNormal = harness.job;
    harness.doLoad();

    harness.job = mock(EngineJob.class);
    harness.onlyRetrieveFromCache = true;
    harness.doLoad();
    harness.callOnEngineJobComplete();

    EngineJob<?> secondNormal = mock(EngineJob.class);
    harness.job = secondNormal;
    harness.onlyRetrieveFromCache = false;
    harness.doLoad();

    verify(firstNormal).start(anyDecodeJobOrNull());
    verify(secondNormal, never()).start(anyDecodeJobOrNull());
  }

  @Test
  public void load_afterCancelledOnlyRetrieveFromCache_withPendingNormal_doesNotStartNewLoad() {
    EngineJob<?> firstNormal = harness.job;
    harness.doLoad();

    harness.job = mock(EngineJob.class);
    harness.onlyRetrieveFromCache = true;
    harness.doLoad();
    harness.getEngine().onEngineJobCancelled(harness.job, harness.cacheKey);

    EngineJob<?> secondNormal = mock(EngineJob.class);
    harness.job = secondNormal;
    harness.onlyRetrieveFromCache = false;
    harness.doLoad();

    verify(firstNormal).start(anyDecodeJobOrNull());
    verify(secondNormal, never()).start(anyDecodeJobOrNull());
  }

  @Test
  public void load_withOnlyRetrieveFromCache_withOtherRetrieveFromCachePending_doesNotStartNew() {
    harness.onlyRetrieveFromCache = true;
    harness.doLoad();

    EngineJob<?> second = mock(EngineJob.class);
    harness.job = second;
    harness.doLoad();

    verify(second, never()).start(anyDecodeJobOrNull());
  }

  @Test
  public void load_withOnlyRetrieveFromCache_afterPreviousFinishedOnlyFromCacheLoad_startsNew() {
    harness.onlyRetrieveFromCache = true;
    harness.doLoad();
    harness.callOnEngineJobComplete();

    EngineJob<?> second = mock(EngineJob.class);
    harness.job = second;
    harness.doLoad();

    verify(second).start(anyDecodeJobOrNull());
  }

  @Test
  public void load_withOnlyRetrieveFromCache_afterPreviousCancelledOnlyFromCacheLoad_startsNew() {
    harness.onlyRetrieveFromCache = true;
    harness.doLoad();
    harness.getEngine().onEngineJobCancelled(harness.job, harness.cacheKey);

    EngineJob<?> second = mock(EngineJob.class);
    harness.job = second;
    harness.doLoad();

    verify(second).start(anyDecodeJobOrNull());
  }

  @Test
  public void onEngineJobComplete_withOldJobForKey_doesNotRemoveJob() {
    harness.doLoad();
    harness
        .getEngine()
        .onEngineJobComplete(mock(EngineJob.class), harness.cacheKey, harness.resource);

    harness.job = mock(EngineJob.class);
    harness.doLoad();

    verify(harness.job, never()).start(anyDecodeJobOrNull());
  }

  @Test
  public void onEngineJobCancelled_withOldJobForKey_doesNotRemoveJob() {
    harness.doLoad();
    harness.getEngine().onEngineJobCancelled(mock(EngineJob.class), harness.cacheKey);

    harness.job = mock(EngineJob.class);
    harness.doLoad();

    verify(harness.job, never()).start(anyDecodeJobOrNull());
  }

  @Test
  public void onEngineJobComplete_withOnlyRetrieveFromCacheAndOldJobForKey_doesNotRemoveJob() {
    harness.onlyRetrieveFromCache = true;
    harness.doLoad();
    harness
        .getEngine()
        .onEngineJobComplete(mock(EngineJob.class), harness.cacheKey, harness.resource);

    harness.job = mock(EngineJob.class);
    harness.doLoad();

    verify(harness.job, never()).start(anyDecodeJobOrNull());
  }

  @Test
  public void onEngineJobCancelled_withOnlyRetrieveFromCacheAndOldJobForKey_doesNotRemoveJob() {
    harness.onlyRetrieveFromCache = true;
    harness.doLoad();
    harness.getEngine().onEngineJobCancelled(mock(EngineJob.class), harness.cacheKey);

    harness.job = mock(EngineJob.class);
    harness.doLoad();

    verify(harness.job, never()).start(anyDecodeJobOrNull());
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static DecodeJob anyDecodeJobOrNull() {
    return any();
  }

  private static class EngineTestHarness {
    final EngineKey cacheKey = mock(EngineKey.class);
    final EngineKeyFactory keyFactory = mock(EngineKeyFactory.class);
    ResourceCallback cb = mock(ResourceCallback.class);

    @SuppressWarnings("rawtypes")
    final EngineResource resource = mock(EngineResource.class);

    final Jobs jobs = new Jobs();
    final ActiveResources activeResources =
        new ActiveResources(/* isActiveResourceRetentionAllowed= */ true);

    final int width = 100;
    final int height = 100;

    final Object model = new Object();
    MemoryCache cache = mock(MemoryCache.class);
    EngineJob<?> job;
    private Engine engine;
    final Engine.EngineJobFactory engineJobFactory = mock(Engine.EngineJobFactory.class);
    final Engine.DecodeJobFactory decodeJobFactory = mock(Engine.DecodeJobFactory.class);
    final ResourceRecycler resourceRecycler = mock(ResourceRecycler.class);
    final Key signature = mock(Key.class);
    final Map<Class<?>, Transformation<?>> transformations = new HashMap<>();
    final Options options = new Options();
    final GlideContext glideContext = mock(GlideContext.class);
    boolean isMemoryCacheable = true;
    boolean useUnlimitedSourceGeneratorPool = false;
    boolean onlyRetrieveFromCache = false;
    final boolean isScaleOnlyOrNoTransform = true;

    EngineTestHarness() {
      when(keyFactory.buildKey(
              eq(model),
              eq(signature),
              anyInt(),
              anyInt(),
              eq(transformations),
              eq(Object.class),
              eq(Object.class),
              eq(options)))
          .thenReturn(cacheKey);
      when(resource.getResource()).thenReturn(mock(Resource.class));

      job = mock(EngineJob.class);
    }

    void callOnEngineJobComplete() {
      getEngine().onEngineJobComplete(job, cacheKey, resource);
    }

    Engine.LoadStatus doLoad() {
      when(engineJobFactory.build(
              eq(cacheKey), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean()))
          .thenReturn((EngineJob<Object>) job);
      when(job.onlyRetrieveFromCache()).thenReturn(onlyRetrieveFromCache);
      return getEngine()
          .load(
              glideContext,
              model,
              signature,
              width,
              height,
              Object.class /*resourceClass*/,
              Object.class /*transcodeClass*/,
              Priority.HIGH,
              DiskCacheStrategy.ALL,
              transformations,
              false /*isTransformationRequired*/,
              isScaleOnlyOrNoTransform,
              options,
              isMemoryCacheable,
              useUnlimitedSourceGeneratorPool,
              /* useAnimationPool= */ false,
              onlyRetrieveFromCache,
              cb,
              Executors.directExecutor());
    }

    Engine getEngine() {
      if (engine == null) {
        engine =
            new Engine(
                cache,
                mock(DiskCache.Factory.class),
                GlideExecutor.newDiskCacheExecutor(),
                MockGlideExecutor.newMainThreadExecutor(),
                MockGlideExecutor.newMainThreadExecutor(),
                MockGlideExecutor.newMainThreadExecutor(),
                jobs,
                keyFactory,
                activeResources,
                engineJobFactory,
                decodeJobFactory,
                resourceRecycler,
                /* isActiveResourceRetentionAllowed= */ true);
      }
      return engine;
    }
  }
}
