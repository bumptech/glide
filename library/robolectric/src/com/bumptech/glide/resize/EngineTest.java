package com.bumptech.glide.resize;

import com.bumptech.glide.loader.bitmap.resource.ResourceFetcher;
import com.bumptech.glide.resize.cache.ResourceCache;
import com.bumptech.glide.resize.load.Transformation;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class EngineTest {
    private static final String ID = "testId";
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

        assertTrue(harness.runners.containsKey(ID));
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
        when(harness.cache.get(eq(ID))).thenReturn(harness.resource);

        harness.doLoad();

        verify(harness.cb).onResourceReady(eq(harness.resource));
    }

    @Test
    public void testNewLoadIsNotStartedIfResourceIsCached() {
        when(harness.cache.get(eq(ID))).thenReturn(mock(Resource.class));

        harness.doLoad();

        verify(harness.runner, never()).queue();
    }

    @Test
    public void testNullLoadStatusIsReturnedForCachedResource() {
        when(harness.cache.get(eq(ID))).thenReturn(mock(Resource.class));

        Engine.LoadStatus loadStatus = harness.doLoad();
        assertNull(loadStatus);
    }

    @Test
    public void testRunnerIsRemovedFromRunnersOnEngineNotifiedJobComplete() {
        harness.doLoad();

        harness.engine.onEngineJobComplete(ID);

        assertFalse(harness.runners.containsKey(ID));
    }

    @Test
    public void testRunnerIsNotCancelledOnEngineNotifiedJobComplete() {
        harness.doLoad();

        harness.engine.onEngineJobComplete(ID);

        verify(harness.runner, never()).cancel();
    }

    @Test
    public void testRunnerIsRemovedFromRunnersOnEngineNotifiedJobCancel() {
        harness.doLoad();

        harness.engine.onEngineJobCancelled(ID);

        assertFalse(harness.runners.containsKey(ID));
    }

    @Test
    public void testRunnerIsCancelledOnEngineNotifiedJobCanceled() {
        harness.doLoad();

        harness.engine.onEngineJobCancelled(ID);

        verify(harness.runner).cancel();
    }

    @Test
    public void testResourceReferenceCounterIsNotifiedWhenResourceRecycled() {
        harness.engine.recycle(harness.resource);

        verify(harness.resourceReferenceCounter).releaseResource(eq(harness.resource));
    }

    @Test
    public void testEngineAddedAsListenerToResourceCache() {
        verify(harness.cache).setResourceRemovedListener(eq(harness.engine));
    }

    @Test
    public void testResourceReferenceCounterIsNotifiedWhenResourceRemovedFromCache() {
        harness.engine.onResourceRemoved(harness.resource);

        verify(harness.resourceReferenceCounter).releaseResource(eq(harness.resource));
    }

    @SuppressWarnings("unchecked")
    private static class EngineTestHarness {
        ResourceDecoder<InputStream, Object> cacheDecoder = mock(ResourceDecoder.class);
        ResourceFetcher<Object> fetcher = mock(ResourceFetcher.class);
        ResourceDecoder<Object, Object> decoder = mock(ResourceDecoder.class);
        ResourceEncoder<Object> encoder = mock(ResourceEncoder.class);
        Metadata metadata = mock(Metadata.class);
        ResourceCallback<Object> cb = mock(ResourceCallback.class);
        Resource<Object> resource = mock(Resource.class);
        Map<String, ResourceRunner> runners = new HashMap<String, ResourceRunner>();
        ResourceReferenceCounter resourceReferenceCounter = mock(ResourceReferenceCounter.class);
        Transformation transformation = mock(Transformation.class);
        int width = 100;
        int height = 100;

        ResourceCache cache = mock(ResourceCache.class);
        ResourceRunner<Object> runner = mock(ResourceRunner.class);
        EngineJob<Object> job;
        Engine engine;

        public EngineTestHarness() {
            ResourceRunnerFactory factory = mock(ResourceRunnerFactory.class);

            job = mock(EngineJob.class);
            when(runner.getJob()).thenReturn(job);

            engine = new Engine(factory, cache, runners, resourceReferenceCounter);

            when(factory.build(eq(ID), eq(width), eq(height), eq(cacheDecoder), eq(fetcher), eq(decoder),
                    eq(transformation), eq(encoder), eq(metadata), eq(engine), eq(cb))).thenReturn(runner);
        }

        public Engine.LoadStatus doLoad() {
            return engine.load(ID, width, height, cacheDecoder, fetcher, decoder, transformation, encoder, metadata,
                    cb);
        }
    }
}
