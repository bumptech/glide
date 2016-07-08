package com.bumptech.glide.load.engine;

import static com.bumptech.glide.tests.Util.anyResource;
import static com.bumptech.glide.tests.Util.isADataSource;
import static com.bumptech.glide.tests.Util.mockResource;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
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
import com.bumptech.glide.load.engine.cache.MemoryCache;
import com.bumptech.glide.load.engine.executor.GlideExecutor;
import com.bumptech.glide.load.engine.executor.MockGlideExecutor;
import com.bumptech.glide.request.ResourceCallback;
import com.bumptech.glide.tests.BackgroundUtil;
import com.bumptech.glide.tests.GlideShadowLooper;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 18, shadows = { GlideShadowLooper.class })
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

    verify(harness.job).start(any(DecodeJob.class));
  }

  @Test
  public void testCallbackIsAddedToNewEngineJobWithNoExistingLoad() {
    harness.doLoad();

    verify(harness.job).addCallback(eq(harness.cb));
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

    assertThat(harness.jobs).containsKey(harness.cacheKey);
  }

  @Test
  public void testNewRunnerIsNotCreatedAndPostedWithExistingLoad() {
    harness.doLoad();
    harness.doLoad();

    verify(harness.job, times(1)).start(any(DecodeJob.class));
  }

  @Test
  public void testCallbackIsAddedToExistingRunnerWithExistingLoad() {
    harness.doLoad();

    ResourceCallback newCallback = mock(ResourceCallback.class);
    harness.cb = newCallback;
    harness.doLoad();

    verify(harness.job).addCallback(eq(newCallback));
  }

  @Test
  public void testLoadStatusIsReturnedForExistingJob() {
    harness.doLoad();
    Engine.LoadStatus loadStatus = harness.doLoad();

    assertNotNull(loadStatus);
  }

  @Test
  public void testResourceIsReturnedFromActiveResourcesIfPresent() {
    harness.activeResources
        .put(harness.cacheKey, new WeakReference<EngineResource<?>>(harness.resource));

    harness.doLoad();

    verify(harness.cb).onResourceReady(eq(harness.resource), eq(DataSource.MEMORY_CACHE));
  }

  @Test
  public void testResourceIsNotReturnedFromActiveResourcesIfRefIsCleared() {
    harness.activeResources.put(harness.cacheKey, new WeakReference<EngineResource<?>>(null));

    harness.doLoad();

    verify(harness.cb, never()).onResourceReady(isNull(Resource.class), isADataSource());
  }

  @Test
  public void testKeyIsRemovedFromActiveResourcesIfRefIsCleared() {
    harness.activeResources.put(harness.cacheKey, new WeakReference<EngineResource<?>>(null));

    harness.doLoad();

    assertThat(harness.activeResources).doesNotContainKey(harness.cacheKey);
  }

  @Test
  public void testResourceIsAcquiredIfReturnedFromActiveResources() {
    harness.activeResources
        .put(harness.cacheKey, new WeakReference<EngineResource<?>>(harness.resource));

    harness.doLoad();

    verify(harness.resource).acquire();
  }

  @Test
  public void testNewLoadIsNotStartedIfResourceIsActive() {
    harness.activeResources
        .put(harness.cacheKey, new WeakReference<EngineResource<?>>(harness.resource));

    harness.doLoad();

    verify(harness.job, never()).start(any(DecodeJob.class));
  }

  @Test
  public void testNullLoadStatusIsReturnedIfResourceIsActive() {
    harness.activeResources
        .put(harness.cacheKey, new WeakReference<EngineResource<?>>(harness.resource));

    assertNull(harness.doLoad());
  }

  @Test
  public void testActiveResourcesIsNotCheckedIfReturnedFromCache() {
    when(harness.cache.remove(eq(harness.cacheKey))).thenReturn(harness.resource);
    EngineResource<?> other = mock(EngineResource.class);
    harness.activeResources.put(harness.cacheKey, new WeakReference<EngineResource<?>>(other));

    harness.doLoad();

    verify(harness.cb).onResourceReady(eq(harness.resource), eq(DataSource.MEMORY_CACHE));
    verify(harness.cb, never()).onResourceReady(eq(other), isADataSource());
  }

  @Test
  public void testActiveResourcesIsNotCheckedIfNotMemoryCacheable() {
    harness.activeResources
        .put(harness.cacheKey, new WeakReference<EngineResource<?>>(harness.resource));

    harness.isMemoryCacheable = false;
    harness.doLoad();

    verify(harness.resource, never()).acquire();
    verify(harness.job).start(any(DecodeJob.class));
  }

  @Test
  public void testCacheIsCheckedIfMemoryCacheable() {
    when(harness.cache.remove(eq(harness.cacheKey))).thenReturn(harness.resource);

    harness.doLoad();

    verify(harness.cb).onResourceReady(eq(harness.resource), eq(DataSource.MEMORY_CACHE));
  }

  @Test
  public void testCacheIsNotCheckedIfNotMemoryCacheable() {
    when(harness.cache.remove(eq(harness.cacheKey))).thenReturn(harness.resource);

    harness.isMemoryCacheable = false;
    harness.doLoad();

    verify(harness.job).start(any(DecodeJob.class));
  }

  @Test
  public void testResourceIsReturnedFromCacheIfPresent() {
    when(harness.cache.remove(eq(harness.cacheKey))).thenReturn(harness.resource);

    harness.doLoad();

    verify(harness.cb).onResourceReady(eq(harness.resource), eq(DataSource.MEMORY_CACHE));
  }

  @Test
  public void testHandlesNonEngineResourcesFromCacheIfPresent() {
    final Object expected = new Object();
    @SuppressWarnings("rawtypes") Resource fromCache = mockResource();
    when(fromCache.get()).thenReturn(expected);
    when(harness.cache.remove(eq(harness.cacheKey))).thenReturn(fromCache);

    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
        Resource<?> resource = (Resource<?>) invocationOnMock.getArguments()[0];
        assertEquals(expected, resource.get());
        return null;
      }
    }).when(harness.cb).onResourceReady(anyResource(), isADataSource());

    harness.doLoad();

    verify(harness.cb).onResourceReady(anyResource(), isADataSource());
  }

  @Test
  public void testResourceIsAddedToActiveResourceIfReturnedFromCache() {
    when(harness.cache.remove(eq(harness.cacheKey))).thenReturn(harness.resource);

    harness.doLoad();

    assertEquals(harness.resource, harness.activeResources.get(harness.cacheKey).get());
  }

  @Test
  public void testResourceIsAcquiredIfReturnedFromCache() {
    when(harness.cache.remove(eq(harness.cacheKey))).thenReturn(harness.resource);

    harness.doLoad();

    verify(harness.resource).acquire();
  }

  @Test
  public void testNewLoadIsNotStartedIfResourceIsCached() {
    when(harness.cache.remove(eq(harness.cacheKey))).thenReturn(mock(EngineResource.class));

    harness.doLoad();

    verify(harness.job, never()).start(any(DecodeJob.class));
  }

  @Test
  public void testNullLoadStatusIsReturnedForCachedResource() {
    when(harness.cache.remove(eq(harness.cacheKey))).thenReturn(mock(EngineResource.class));

    Engine.LoadStatus loadStatus = harness.doLoad();
    assertNull(loadStatus);
  }

  @Test
  public void testRunnerIsRemovedFromRunnersOnEngineNotifiedJobComplete() {
    harness.doLoad();

    harness.engine.onEngineJobComplete(harness.cacheKey, harness.resource);

    assertThat(harness.jobs).doesNotContainKey(harness.cacheKey);
  }

  @Test
  public void testEngineIsSetAsResourceListenerOnJobComplete() {
    harness.doLoad();

    harness.engine.onEngineJobComplete(harness.cacheKey, harness.resource);

    verify(harness.resource).setResourceListener(eq(harness.cacheKey), eq(harness.engine));
  }

  @Test
  public void testEngineIsNotSetAsResourceListenerIfResourceIsNullOnJobComplete() {
    harness.doLoad();

    harness.engine.onEngineJobComplete(harness.cacheKey, null);
  }

  @Test
  public void testResourceIsAddedToActiveResourcesOnEngineComplete() {
    when(harness.resource.isCacheable()).thenReturn(true);
    harness.engine.onEngineJobComplete(harness.cacheKey, harness.resource);

    WeakReference<EngineResource<?>> resourceRef = harness.activeResources.get(harness.cacheKey);
    assertThat(harness.resource).isEqualTo(resourceRef.get());
  }

  @Test
  public void testDoesNotPutNullResourceInActiveResourcesOnEngineComplete() {
    harness.engine.onEngineJobComplete(harness.cacheKey, null);
    assertThat(harness.activeResources).doesNotContainKey(harness.cacheKey);
  }

  @Test
  public void testDoesNotPutResourceThatIsNotCacheableInActiveResourcesOnEngineComplete() {
    when(harness.resource.isCacheable()).thenReturn(false);
    harness.engine.onEngineJobComplete(harness.cacheKey, harness.resource);
    assertThat(harness.activeResources).doesNotContainKey(harness.cacheKey);
  }

  @Test
  public void testRunnerIsRemovedFromRunnersOnEngineNotifiedJobCancel() {
    harness.doLoad();

    harness.engine.onEngineJobCancelled(harness.job, harness.cacheKey);

    assertThat(harness.jobs).doesNotContainKey(harness.cacheKey);
  }

  @Test
  public void testJobIsNotRemovedFromJobsIfOldJobIsCancelled() {
    harness.doLoad();

    harness.engine.onEngineJobCancelled(mock(EngineJob.class), harness.cacheKey);

    assertEquals(harness.job, harness.jobs.get(harness.cacheKey));
  }

  @Test
  public void testResourceIsAddedToCacheOnReleased() {
    final Object expected = new Object();
    when(harness.resource.isCacheable()).thenReturn(true);
    when(harness.resource.get()).thenReturn(expected);
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
        Resource<?> resource = (Resource<?>) invocationOnMock.getArguments()[1];
        assertEquals(expected, resource.get());
        return null;
      }
    }).when(harness.cache).put(eq(harness.cacheKey), anyResource());

    harness.engine.onResourceReleased(harness.cacheKey, harness.resource);

    verify(harness.cache).put(eq(harness.cacheKey), anyResource());
  }

  @Test
  public void testResourceIsNotAddedToCacheOnReleasedIfNotCacheable() {
    when(harness.resource.isCacheable()).thenReturn(false);
    harness.engine.onResourceReleased(harness.cacheKey, harness.resource);

    verify(harness.cache, never()).put(eq(harness.cacheKey), eq(harness.resource));
  }

  @Test
  public void testResourceIsRecycledIfNotCacheableWhenReleased() {
    when(harness.resource.isCacheable()).thenReturn(false);
    harness.engine.onResourceReleased(harness.cacheKey, harness.resource);
    verify(harness.resourceRecycler).recycle(eq(harness.resource));
  }

  @Test
  public void testResourceIsRemovedFromActiveResourcesWhenReleased() {
    harness.activeResources
        .put(harness.cacheKey, new WeakReference<EngineResource<?>>(harness.resource));

    harness.engine.onResourceReleased(harness.cacheKey, harness.resource);

    assertThat(harness.activeResources).doesNotContainKey(harness.cacheKey);
  }

  @Test
  public void testEngineAddedAsListenerToMemoryCache() {
    verify(harness.cache).setResourceRemovedListener(eq(harness.engine));
  }

  @Test
  public void testResourceIsRecycledWhenRemovedFromCache() {
    harness.engine.onResourceRemoved(harness.resource);
    verify(harness.resourceRecycler).recycle(eq(harness.resource));
  }

  @Test
  public void testJobIsPutInJobWithCacheKeyWithRelevantIds() {
    harness.doLoad();

    assertThat(harness.jobs).containsEntry(harness.cacheKey, harness.job);
  }

  @Test
  public void testKeyFactoryIsGivenNecessaryArguments() {
    harness.doLoad();

    verify(harness.keyFactory)
        .buildKey(eq(harness.model), eq(harness.signature), eq(harness.width), eq(harness.height),
            eq(harness.transformations), eq(Object.class), eq(Object.class), eq(harness.options));
  }

  @Test
  public void testFactoryIsGivenNecessaryArguments() {
    harness.doLoad();

    verify(harness.engineJobFactory).build(
        eq(harness.cacheKey),
        eq(true) /*isMemoryCacheable*/,
        eq(false) /*useUnlimitedSourceGeneratorPool*/);
  }

  @Test
  public void testFactoryIsGivenNecessaryArgumentsWithUnlimitedPool() {
    harness.useUnlimitedSourceGeneratorPool = true;
    harness.doLoad();

    verify(harness.engineJobFactory).build(
        eq(harness.cacheKey),
        eq(true) /*isMemoryCacheable*/,
        eq(true) /*useUnlimitedSourceGeneratorPool*/);
  }

  @Test
  public void testReleaseReleasesEngineResource() {
    EngineResource<Object> engineResource = mock(EngineResource.class);
    harness.engine.release(engineResource);
    verify(engineResource).release();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testThrowsIfAskedToReleaseNonEngineResource() {
    harness.engine.release(mockResource());
  }

  @Test(expected = RuntimeException.class)
  public void testThrowsIfLoadCalledOnBackgroundThread() throws InterruptedException {
    BackgroundUtil.testInBackground(new BackgroundUtil.BackgroundTester() {
      @Override
      public void runTest() throws Exception {
        harness.doLoad();
      }
    });
  }

  private static class EngineTestHarness {
    EngineKey cacheKey = mock(EngineKey.class);
    EngineKeyFactory keyFactory = mock(EngineKeyFactory.class);
    ResourceCallback cb = mock(ResourceCallback.class);
    @SuppressWarnings("rawtypes")
    EngineResource resource = mock(EngineResource.class);
    Map<Key, EngineJob<?>> jobs = new HashMap<>();
    Map<Key, WeakReference<EngineResource<?>>> activeResources = new HashMap<>();

    int width = 100;
    int height = 100;

    Object model = new Object();
    MemoryCache cache = mock(MemoryCache.class);
    EngineJob<?> job;
    Engine engine;
    Engine.EngineJobFactory engineJobFactory = mock(Engine.EngineJobFactory.class);
    Engine.DecodeJobFactory decodeJobFactory = mock(Engine.DecodeJobFactory.class);
    ResourceRecycler resourceRecycler = mock(ResourceRecycler.class);
    Key signature = mock(Key.class);
    Map<Class<?>, Transformation<?>> transformations = new HashMap<>();
    Options options = new Options();
    GlideContext glideContext = mock(GlideContext.class);
    boolean isMemoryCacheable = true;
    boolean useUnlimitedSourceGeneratorPool = false;

    public EngineTestHarness() {
      when(keyFactory.buildKey(eq(model), eq(signature), anyInt(), anyInt(), eq(transformations),
          eq(Object.class), eq(Object.class), eq(options))).thenReturn(cacheKey);

      job = mock(EngineJob.class);

      engine = new Engine(cache, mock(DiskCache.Factory.class),
          GlideExecutor.newDiskCacheExecutor(),
          MockGlideExecutor.newMainThreadExecutor(),
          MockGlideExecutor.newMainThreadUnlimitedExecutor(),
          jobs, keyFactory, activeResources,
          engineJobFactory, decodeJobFactory, resourceRecycler);
    }

    public Engine.LoadStatus doLoad() {
      when(engineJobFactory.build(eq(cacheKey), anyBoolean(), anyBoolean()))
          .thenReturn((EngineJob<Object>) job);
      return engine.load(glideContext,
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
          options,
          isMemoryCacheable,
          useUnlimitedSourceGeneratorPool,
          cb);
    }
  }
}
