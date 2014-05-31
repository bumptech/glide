package com.bumptech.glide.load.engine;

import com.bumptech.glide.Priority;
import com.bumptech.glide.Resource;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.cache.MemoryCache;
import com.bumptech.glide.load.resource.ResourceFetcher;
import com.bumptech.glide.request.ResourceCallback;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    public void testResourceIsReturnedFromCacheIfPresent() {
        when(harness.cache.get(eq(harness.cacheKey))).thenReturn(harness.resource);

        harness.doLoad();

        verify(harness.cb).onResourceReady(eq(harness.resource));
    }

    @Test
    public void testResourceIsAcquiredIfReturnedFromCache() {
        when(harness.cache.get(eq(harness.cacheKey))).thenReturn(harness.resource);

        harness.doLoad();

        verify(harness.resource).acquire(1);
    }

    @Test
    public void testNewLoadIsNotStartedIfResourceIsCached() {
        when(harness.cache.get(eq(harness.cacheKey))).thenReturn(mock(Resource.class));

        harness.doLoad();

        verify(harness.runner, never()).queue();
    }

    @Test
    public void testNullLoadStatusIsReturnedForCachedResource() {
        when(harness.cache.get(eq(harness.cacheKey))).thenReturn(mock(Resource.class));

        Engine.LoadStatus loadStatus = harness.doLoad();
        assertNull(loadStatus);
    }

    @Test
    public void testRunnerIsRemovedFromRunnersOnEngineNotifiedJobComplete() {
        harness.doLoad();

        harness.engine.onEngineJobComplete(harness.cacheKey);

        assertFalse(harness.runners.containsKey(harness.cacheKey));
    }

    @Test
    public void testRunnerIsNotCancelledOnEngineNotifiedJobComplete() {
        harness.doLoad();

        harness.engine.onEngineJobComplete(harness.cacheKey);

        verify(harness.runner, never()).cancel();
    }

    @Test
    public void testRunnerIsRemovedFromRunnersOnEngineNotifiedJobCancel() {
        harness.doLoad();

        harness.engine.onEngineJobCancelled(harness.cacheKey);

        assertFalse(harness.runners.containsKey(harness.cacheKey));
    }

    @Test
    public void testRunnerIsCancelledOnEngineNotifiedJobCanceled() {
        harness.doLoad();

        harness.engine.onEngineJobCancelled(harness.cacheKey);

        verify(harness.runner).cancel();
    }

    @Test
    public void testEngineAddedAsListenerToMemoryCache() {
        verify(harness.cache).setResourceRemovedListener(eq(harness.engine));
    }

    @Test
    public void testResourceIsReleasedWhenRemovedFromCache() {
        harness.engine.onResourceRemoved(harness.resource);

        verify(harness.resource).release();
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
                eq(harness.decoder), eq(harness.transformation), eq(harness.encoder));
    }

    @Test
    public void testFactoryIsGivenCacheKeyToBuildRunner() {
        harness.doLoad();

        verify(harness.factory).build(eq(harness.cacheKey), anyInt(), anyInt(), any(ResourceDecoder.class),
                any(ResourceFetcher.class), any(ResourceDecoder.class), any(Transformation.class),
                any(ResourceEncoder.class), any(Priority.class), any(EngineJobListener.class));
    }


    @SuppressWarnings("unchecked")
    private static class EngineTestHarness {
        Key cacheKey = mock(Key.class);
        KeyFactory keyFactory = mock(KeyFactory.class);
        ResourceDecoder<InputStream, Object> cacheDecoder = mock(ResourceDecoder.class);
        ResourceFetcher<Object> fetcher = mock(ResourceFetcher.class);
        ResourceDecoder<Object, Object> decoder = mock(ResourceDecoder.class);
        ResourceEncoder<Object> encoder = mock(ResourceEncoder.class);
        Priority priority = Priority.NORMAL;
        ResourceCallback <Object> cb = mock(ResourceCallback.class);
        Resource<Object> resource = mock(Resource.class);
        Map<Key, ResourceRunner> runners = new HashMap<Key, ResourceRunner>();
        Transformation transformation = mock(Transformation.class);
        ResourceRunnerFactory factory = mock(ResourceRunnerFactory.class);
        int width = 100;
        int height = 100;

        MemoryCache cache = mock(MemoryCache.class);
        ResourceRunner<Object> runner = mock(ResourceRunner.class);
        EngineJob<Object> job;
        Engine engine;

        public EngineTestHarness() {
            when(keyFactory.buildKey(anyString(), anyInt(), anyInt(), any(ResourceDecoder.class),
                    any(ResourceDecoder.class), any(Transformation.class), any(ResourceEncoder.class)))
                    .thenReturn(cacheKey);

            job = mock(EngineJob.class);
            when(runner.getJob()).thenReturn(job);

            engine = new Engine(factory, cache, runners, keyFactory);

            when(factory.build(eq(cacheKey), eq(width), eq(height), eq(cacheDecoder), eq(fetcher), eq(decoder),
                    eq(transformation), eq(encoder), eq(priority), eq(engine))).thenReturn(runner);
        }

        public Engine.LoadStatus doLoad() {
            return engine.load(ID, width, height, cacheDecoder, fetcher, decoder, transformation, encoder, priority,
                    cb);
        }
    }
}
