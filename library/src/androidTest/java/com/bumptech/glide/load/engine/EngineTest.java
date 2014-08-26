package com.bumptech.glide.load.engine;

import android.os.Looper;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.Encoder;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.engine.cache.DiskCache;
import com.bumptech.glide.load.engine.cache.MemoryCache;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;
import com.bumptech.glide.request.ResourceCallback;
import com.bumptech.glide.tests.BackgroundUtil;
import com.bumptech.glide.tests.GlideShadowLooper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = { GlideShadowLooper.class })
public class EngineTest {
    private static final String ID = "asdf";
    private EngineTestHarness harness;

    @Before
    public void setUp() {
        harness = new EngineTestHarness();
    }

    @Test
    public void testNewRunnerIsCreatedAndPostedWithNoExistingLoad() {
        harness.doLoad();

        verify(harness.runner).queue();
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

        assertTrue(harness.runners.containsKey(harness.cacheKey));
    }

    @Test
    public void testNewRunnerIsNotCreatedAndPostedWithExistingLoad() {
        harness.doLoad();
        harness.doLoad();

        verify(harness.runner, times(1)).queue();
    }

    @Test
    @SuppressWarnings("unchecked")
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
        harness.activeResources.put(harness.cacheKey, new WeakReference<EngineResource>(harness.resource));

        harness.doLoad();

        verify(harness.cb).onResourceReady(eq(harness.resource));
    }

    @Test
    public void testResourceIsNotReturnedFromActiveResourcesIfRefIsCleared() {
        harness.activeResources.put(harness.cacheKey, new WeakReference<EngineResource>(null));

        harness.doLoad();

        verify(harness.cb, never()).onResourceReady((Resource) isNull());
    }

    @Test
    public void testKeyIsRemovedFromActiveResourcesIfRefIsCleared() {
        harness.activeResources.put(harness.cacheKey, new WeakReference<EngineResource>(null));

        harness.doLoad();

        assertFalse(harness.activeResources.containsKey(harness.cacheKey));
    }

    @Test
    public void testResourceIsAcquiredIfReturnedFromActiveResources() {
        harness.activeResources.put(harness.cacheKey, new WeakReference<EngineResource>(harness.resource));

        harness.doLoad();

        verify(harness.resource).acquire(eq(1));
    }

    @Test
    public void testNewLoadIsNotStartedIfResourceIsActive() {
        harness.activeResources.put(harness.cacheKey, new WeakReference<EngineResource>(harness.resource));

        harness.doLoad();

        verify(harness.runner, never()).queue();
    }

    @Test
    public void testNullLoadStatusIsReturnedIfResourceIsActive() {
        harness.activeResources.put(harness.cacheKey, new WeakReference<EngineResource>(harness.resource));

        assertNull(harness.doLoad());
    }

    @Test
    public void testActiveResourcesIsNotCheckedIfReturnedFromCache() {
        when(harness.cache.remove(eq(harness.cacheKey))).thenReturn(harness.resource);
        EngineResource other = mock(EngineResource.class);
        harness.activeResources.put(harness.cacheKey, new WeakReference<EngineResource>(other));

        harness.doLoad();

        verify(harness.cb).onResourceReady(eq(harness.resource));
        verify(harness.cb, never()).onResourceReady(eq(other));
    }

    @Test
    public void testCacheIsChecked() {
        when(harness.cache.remove(eq(harness.cacheKey))).thenReturn(harness.resource);

        harness.doLoad();

        verify(harness.cb).onResourceReady(eq(harness.resource));
    }

    @Test
    public void testResourceIsReturnedFromCacheIfPresent() {
        when(harness.cache.remove(eq(harness.cacheKey))).thenReturn(harness.resource);

        harness.doLoad();

        verify(harness.cb).onResourceReady(eq(harness.resource));
    }

    @Test
    public void testResourceIsAddedToActiveResourceIfReturnedFromCache() {
        when(harness.cache.remove(eq(harness.cacheKey))).thenReturn(harness.resource);

        harness.doLoad();

        assertEquals(harness.resource, harness.activeResources.get(harness.cacheKey)
                .get());
    }

    @Test
    public void testResourceIsAcquiredIfReturnedFromCache() {
        when(harness.cache.remove(eq(harness.cacheKey))).thenReturn(harness.resource);

        harness.doLoad();

        verify(harness.resource).acquire(1);
    }

    @Test
    public void testNewLoadIsNotStartedIfResourceIsCached() {
        when(harness.cache.remove(eq(harness.cacheKey))).thenReturn(mock(EngineResource.class));

        harness.doLoad();

        verify(harness.runner, never()).queue();
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

        assertFalse(harness.runners.containsKey(harness.cacheKey));
    }

    @Test
    public void testRunnerIsNotCancelledOnEngineNotifiedJobComplete() {
        harness.doLoad();

        harness.engine.onEngineJobComplete(harness.cacheKey, harness.resource);

        verify(harness.runner, never()).cancel();
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
        harness.engine.onEngineJobComplete(harness.cacheKey, harness.resource);

        WeakReference<EngineResource> resourceRef = harness.activeResources.get(harness.cacheKey);
        assertEquals(harness.resource, resourceRef.get());
    }

    @Test
    public void testDoesNotPutNullResourceInActiveResourcesOnEngineComplete() {
        harness.engine.onEngineJobComplete(harness.cacheKey, null);
        assertFalse(harness.activeResources.containsKey(harness.cacheKey));
    }

    @Test
    public void testRunnerIsRemovedFromRunnersOnEngineNotifiedJobCancel() {
        harness.doLoad();

        harness.engine.onEngineJobCancelled(harness.job, harness.cacheKey);

        assertFalse(harness.runners.containsKey(harness.cacheKey));
    }

    @Test
    public void testRunnerIsCancelledOnEngineNotifiedJobCanceled() {
        harness.doLoad();

        harness.engine.onEngineJobCancelled(harness.job, harness.cacheKey);

        verify(harness.runner).cancel();
    }

    @Test
    public void testRunnerIsNotRemovedFromRunnersIfOldJobIsCancelled() {
        harness.doLoad();

        harness.engine.onEngineJobCancelled(mock(EngineJob.class), harness.cacheKey);

        assertEquals(harness.runner, harness.runners.get(harness.cacheKey));
    }

    @Test
    public void testRunnerIsNotCancelledIfOldJobIsCancelled() {
        harness.doLoad();

        harness.engine.onEngineJobCancelled(mock(EngineJob.class), harness.cacheKey);

        verify(harness.runner, never()).cancel();
    }

    @Test
    public void testResourceIsAddedToCacheOnReleased() {
        harness.engine.onResourceReleased(harness.cacheKey, harness.resource);

        verify(harness.cache).put(eq(harness.cacheKey), eq(harness.resource));
    }

    @Test
    public void testResourceIsNotAddedToCacheOnReleasedIfNotCacheable() {
        when(harness.resource.isCacheable()).thenReturn(false);
        harness.engine.onResourceReleased(harness.cacheKey, harness.resource);

        verify(harness.cache, never()).put(eq(harness.cacheKey), eq(harness.resource));
    }

    @Test
    public void testResourceIsRecycledIfNotCacheableWhenReleased() {
        ShadowLooper shadowLooper = Robolectric.shadowOf(Looper.getMainLooper());
        when(harness.resource.isCacheable()).thenReturn(false);
        shadowLooper.pause();
        harness.engine.onResourceReleased(harness.cacheKey, harness.resource);
        verify(harness.resource, never()).recycle();
        shadowLooper.runOneTask();
        verify(harness.resource).recycle();
    }

    @Test
    public void testResourceIsRemovedFromActiveResourcesWhenReleased() {
        harness.activeResources.put(harness.cacheKey, new WeakReference<EngineResource>(harness.resource));

        harness.engine.onResourceReleased(harness.cacheKey, harness.resource);

        assertFalse(harness.activeResources.containsKey(harness.cacheKey));
    }

    @Test
    public void testEngineAddedAsListenerToMemoryCache() {
        verify(harness.cache).setResourceRemovedListener(eq(harness.engine));
    }

    @Test
    public void testResourceIsRecycledWhenRemovedFromCache() {
        ShadowLooper shadowLooper = Robolectric.shadowOf(Looper.getMainLooper());
        shadowLooper.pause();
        harness.engine.onResourceRemoved(harness.resource);
        // We expect the release to be posted
        verify(harness.resource, never()).recycle();
        shadowLooper.runOneTask();
        verify(harness.resource).recycle();
    }

    @Test
    public void testRunnerIsPutInRunnersWithCacheKeyWithRelevantIds() {
        harness.doLoad();

        assertNotNull(harness.runners.get(harness.cacheKey));
    }

    @Test
    public void testKeyFactoryIsGivenNecessaryArguments() {
        harness.doLoad();

        verify(harness.keyFactory).buildKey(eq(ID), eq(harness.width), eq(harness.height), eq(harness.cacheDecoder),
                eq(harness.decoder), eq(harness.transformation), eq(harness.encoder), eq(harness.transcoder),
                eq(harness.sourceEncoder));
    }

    @Test
    public void testFactoryIsGivenCacheKeyToBuildRunner() {
        harness.doLoad();

        verify(harness.factory).build(eq(harness.cacheKey), anyInt(), anyInt(), any(ResourceDecoder.class),
                any(DataFetcher.class), any(Encoder.class), any(ResourceDecoder.class), any(Transformation.class),
                any(ResourceEncoder.class), any(ResourceTranscoder.class), any(Priority.class), anyBoolean(),
                any(DiskCacheStrategy.class), any(EngineJobListener.class));
    }

    @Test
    public void testFactoryIsGivenNecessaryArguments() {
        harness.doLoad();

        verify(harness.factory).build(eq(harness.cacheKey), eq(harness.width), eq(harness.height),
                eq(harness.cacheDecoder), eq(harness.fetcher), eq(harness.sourceEncoder),
                eq(harness.decoder), eq(harness.transformation), eq(harness.encoder), eq(harness.transcoder),
                eq(harness.priority), eq(harness.isMemoryCacheable), eq(harness.diskCacheStrategy), eq(harness.engine));
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

    @SuppressWarnings("unchecked")
    private static class EngineTestHarness {
        EngineKey cacheKey = mock(EngineKey.class);
        EngineKeyFactory keyFactory = mock(EngineKeyFactory.class);
        ResourceDecoder<File, Object> cacheDecoder = mock(ResourceDecoder.class);
        DataFetcher<Object> fetcher = mock(DataFetcher.class);
        ResourceDecoder<Object, Object> decoder = mock(ResourceDecoder.class);
        ResourceEncoder<Object> encoder = mock(ResourceEncoder.class);
        ResourceTranscoder<Object, Object> transcoder = mock(ResourceTranscoder.class);
        Priority priority = Priority.NORMAL;
        ResourceCallback cb = mock(ResourceCallback.class);
        EngineResource resource = mock(EngineResource.class);
        Map<Key, ResourceRunner> runners = new HashMap<Key, ResourceRunner>();
        Transformation transformation = mock(Transformation.class);
        ResourceRunnerFactory factory = mock(ResourceRunnerFactory.class);
        Map<Key, WeakReference<EngineResource>> activeResources = new HashMap<Key, WeakReference<EngineResource>>();
        Encoder<Object> sourceEncoder = mock(Encoder.class);
        DiskCacheStrategy diskCacheStrategy = DiskCacheStrategy.RESULT;

        int width = 100;
        int height = 100;

        MemoryCache cache = mock(MemoryCache.class);
        ResourceRunner<Object, Object> runner = mock(ResourceRunner.class);
        EngineJob job;
        Engine engine;
        boolean isMemoryCacheable;


        public EngineTestHarness() {
            when(resource.isCacheable()).thenReturn(true);
            when(keyFactory.buildKey(anyString(), anyInt(), anyInt(), any(ResourceDecoder.class),
                    any(ResourceDecoder.class), any(Transformation.class), any(ResourceEncoder.class),
                    any(ResourceTranscoder.class), any(Encoder.class))).thenReturn(cacheKey);
            when(fetcher.getId()).thenReturn(ID);

            job = mock(EngineJob.class);
            when(runner.getJob()).thenReturn(job);

            engine = new Engine(factory, cache, mock(DiskCache.class), mock(ExecutorService.class),
                    mock(ExecutorService.class), runners, keyFactory, activeResources);

            when(factory.build(eq(cacheKey), eq(width), eq(height), eq(cacheDecoder), eq(fetcher),
                    eq(sourceEncoder), eq(decoder), eq(transformation), eq(encoder), eq(transcoder), eq(priority),
                    eq(isMemoryCacheable), eq(diskCacheStrategy), eq(engine))).thenReturn(runner);
        }

        public Engine.LoadStatus doLoad() {
            return engine.load(width, height, cacheDecoder, fetcher, sourceEncoder, decoder, transformation, encoder,
                    transcoder, priority, isMemoryCacheable, diskCacheStrategy, cb);
        }
    }
}
