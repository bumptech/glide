package com.bumptech.glide.load.engine;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class ResourceRunnerTest {
    private static final String ID = "testEngineKey";
    private ResourceRunnerHarness harness;

    @Before
    public void setUp() {
        harness = new ResourceRunnerHarness();
    }


    @Test
    public void testDiskCacheIsCheckedWhenNeeded() {
        for (DiskCacheStrategy strategy : new DiskCacheStrategy[] { DiskCacheStrategy.ALL, DiskCacheStrategy.SOURCE}) {
            harness = new ResourceRunnerHarness();
            harness.diskCacheStrategy = strategy;

            harness.getRunner().run();

            verify(harness.cacheLoader)
                    .load(eq(harness.key), eq(harness.decoder), eq(harness.width), eq(harness.height));
        }
    }

    @Test
    public void testDiskCacheIsNotCheckedWhenNotNeeded() {
        for (DiskCacheStrategy strategy : new DiskCacheStrategy[] { DiskCacheStrategy.NONE, DiskCacheStrategy.RESULT}) {
            harness = new ResourceRunnerHarness();
            harness.diskCacheStrategy = strategy;

            harness.getRunner().run();

            verify(harness.cacheLoader, never())
                    .load(eq(harness.key), eq(harness.decoder), eq(harness.width), eq(harness.height));
        }
    }

    @Test
    public void testTransformationIsCalledIfCacheDecodeSucceeds() throws IOException {
        harness.getRunner().run();

        verify(harness.tranformation).transform(eq(harness.decoded), eq(harness.width), eq(harness.height));
    }

    @Test
    public void testTransformationIsNotCalledIfNoSourceCache() throws IOException {
        harness.diskCacheStrategy = DiskCacheStrategy.NONE;

        harness.getRunner().run();

        verify(harness.tranformation, never()).transform(eq(harness.decoded), eq(harness.width), eq(harness.height));
    }

    @Test
    public void testTranscoderIsCalledIfCacheDecodeSucceeds() throws IOException {
        when(harness.tranformation.transform(eq(harness.decoded), eq(harness.width), eq(harness.height)))
                .thenReturn(harness.transformed);

        harness.getRunner().run();

        verify(harness.transcoder).transcode(eq(harness.transformed));
    }

    @Test
    public void testTranscoderIsNotCalledIfNoSourceCache() throws IOException {
        harness.diskCacheStrategy = DiskCacheStrategy.NONE;

        harness.getRunner().run();

        verify(harness.transcoder, never()).transcode(eq(harness.transformed));
    }

    @Test
    public void testCallbackIsCalledIfCacheDecodeSucceeds() throws IOException {
        when(harness.tranformation.transform(eq(harness.decoded), eq(harness.width), eq(harness.height)))
                .thenReturn(harness.transformed);
        when(harness.transcoder.transcode(eq(harness.transformed))).thenReturn(harness.transcoded);

        harness.getRunner().run();

        verify(harness.engineJob).onResourceReady(eq(harness.transcoded));
    }

    @Test
    public void testDecodedResourceIsNotRecycledIfTranscoded() throws IOException {
        when(harness.tranformation.transform(eq(harness.decoded), eq(harness.width), eq(harness.height)))
                .thenReturn(harness.transformed);
        when(harness.transcoder.transcode(eq(harness.transformed))).thenReturn(harness.transcoded);

        harness.getRunner().run();

        verify(harness.transformed, never()).recycle();
        verify(harness.transformed, never()).release();
    }

    @Test
    public void testCallbackIsNotCalledIfDiskCacheReturnsNull() {
        when(harness.cacheLoader.load(eq(harness.key), eq(harness.decoder), eq(harness.width), eq(harness.height)))
                .thenReturn(null);

        harness.getRunner().run();

        verify(harness.cacheLoader).load(eq(harness.key), eq(harness.decoder), eq(harness.width),
                eq(harness.height));
        verify(harness.engineJob, never()).onException(any(Exception.class));
    }

    @Test
    public void testSourceRunnerIsQueuedIfNotInCache() {
        when(harness.cacheLoader.load(eq(harness.key), eq(harness.decoder), eq(harness.width), eq(harness.height)))
                .thenReturn(null);
        ResourceRunner runner = harness.getRunner();
        when(harness.resizeService.submit(eq(runner))).thenReturn(harness.future);

        runner.run();

        verify(harness.resizeService).submit(eq(harness.sourceRunner));
    }

    @Test
    public void testSourceRunnerIsQueuedIfNoSourceCache() {
        harness.diskCacheStrategy = DiskCacheStrategy.NONE;
        ResourceRunner runner = harness.getRunner();
        when(harness.resizeService.submit(eq(runner))).thenReturn(harness.future);

        runner.run();

        verify(harness.resizeService).submit(eq(harness.sourceRunner));
    }

    @Test
    public void testPostedToDiskCacheSerciceWhenQueued() {
        ResourceRunner runner = harness.getRunner();
        when(harness.diskCacheService.submit(eq(runner))).thenReturn(harness.future);

        runner.queue();

        verify(harness.diskCacheService).submit(eq(runner));
    }

    @Test
    public void testCancelsFutureFromDiskCacheServiceWhenCancelledIfNotYetQueuedToResizeService() {
        ResourceRunner runner = harness.getRunner();
        when(harness.diskCacheService.submit(eq(runner))).thenReturn(harness.future);

        runner.queue();
        runner.cancel();

        verify(harness.future).cancel(eq(false));
    }

    @Test
    public void testResourceIsNotLoadedFromDiskCacheIfCancelled() {
        ResourceRunner runner = harness.getRunner();

        runner.queue();
        runner.cancel();
        runner.run();

        verify(harness.cacheLoader, never()).load(any(Key.class), any(ResourceDecoder.class), anyInt(), anyInt());
    }

    @Test
    public void testSourceRunnerIsCancelledIfCancelledAfterSubmitted() {
        ResourceRunner runner = harness.getRunner();

        runner.queue();
        runner.run();
        runner.cancel();

        verify(harness.sourceRunner).cancel();
    }

    @Test
    public void testSourceRunnerFutureIsCancelledIfCancelledAfterSubmitted() {
        when(harness.cacheLoader.load(eq(harness.key), eq(harness.decoder), eq(harness.width), eq(harness.height)))
                .thenReturn(null);
        ResourceRunner runner = harness.getRunner();

        runner.queue();
        runner.run();
        runner.cancel();

        verify(harness.sourceFuture).cancel(anyBoolean());
    }

    @Test
    public void testDecodedResourceIsRecycledIfTransformedResourceIsDifferent() throws IOException {
        when(harness.tranformation.transform(eq(harness.decoded), eq(harness.width), eq(harness.height)))
                .thenReturn(harness.transformed);

        harness.getRunner().run();

        verify(harness.decoded).recycle();
    }

    @Test
    public void testDecodedResourceIsNotRecycledIfTransformedResourceIsDecodedResource() throws IOException {
        when(harness.tranformation.transform(eq(harness.decoded), eq(harness.width), eq(harness.height)))
                .thenReturn(harness.decoded);

        harness.getRunner().run();

        verify(harness.decoded, never()).recycle();
    }

    @Test
    public void testReturnsGivenPriority() {
        for (Priority priority : Priority.values()) {
            harness = new ResourceRunnerHarness();
            harness.priority = priority;

            int actualPriority = harness.getRunner().getPriority();

            assertEquals(harness.priority.ordinal(), actualPriority);
        }
    }

    @Test
    public void testNotifiesJobOfFailureIfCacheLoaderThrows() {
        Exception exception = new RuntimeException("test");
        when(harness.cacheLoader.load(any(Key.class), any(ResourceDecoder.class), anyInt(), anyInt()))
                .thenThrow(exception);

        harness.getRunner().run();

        verify(harness.engineJob).onException(eq(exception));
    }

    @Test
    public void testNotifiesJobOfFailureIfTransformationThrows() {
        Exception exception = new RuntimeException("test");
        when(harness.tranformation.transform(any(Resource.class), anyInt(), anyInt()))
                .thenThrow(exception);
        harness.getRunner().run();

        verify(harness.engineJob).onException(eq(exception));
    }

    @Test
    public void testNotifiesJobOfFailureIfTranscoderThrows() {
        Exception exception = new RuntimeException("test");
        when(harness.transcoder.transcode(any(Resource.class)))
                .thenThrow(exception);
        harness.getRunner().run();

        verify(harness.engineJob).onException(eq(exception));
    }

    @Test
    public void testNotifiesJobOfFailureIfExecutorThrows() {
        Exception exception = new RejectedExecutionException("test");
        when(harness.cacheLoader.load(any(Key.class), any(ResourceDecoder.class), anyInt(), anyInt())).thenReturn(null);
        when(harness.resizeService.submit(any(Runnable.class)))
                .thenThrow(exception);

        harness.getRunner().run();

        verify(harness.engineJob).onException(eq(exception));
    }

    @SuppressWarnings("unchecked")
    private static class ResourceRunnerHarness {
        EngineKey key = mock(EngineKey.class);
        Key originalKey = mock(Key.class);
        ResourceDecoder<File, Object> decoder = mock(ResourceDecoder.class);
        SourceResourceRunner<Object, Object, Object> sourceRunner = mock(SourceResourceRunner.class);
        ResourceTranscoder<Object, Object> transcoder = mock(ResourceTranscoder.class);
        ExecutorService resizeService = mock(ExecutorService.class);
        ExecutorService diskCacheService = mock(ExecutorService.class);
        DiskCacheStrategy diskCacheStrategy = DiskCacheStrategy.SOURCE;
        EngineJob engineJob = mock(EngineJob.class);
        Transformation<Object> tranformation = mock(Transformation.class);
        CacheLoader cacheLoader = mock(CacheLoader.class);
        int width = 100;
        int height = 100;
        Priority priority = Priority.HIGH;

        Future future = mock(Future.class);
        Future sourceFuture = mock(Future.class);
        Resource<Object> decoded = mock(Resource.class);
        Resource<Object> transformed = mock(Resource.class);
        Resource<Object> transcoded = mock(Resource.class);

        public ResourceRunnerHarness() {
            when(key.toString()).thenReturn(ID);
            when(key.getOriginalKey()).thenReturn(originalKey);
            when(resizeService.submit(eq(sourceRunner))).thenReturn(sourceFuture);
            when(cacheLoader.load(eq(key), eq(decoder), eq(width), eq(height))).thenReturn(decoded);
        }

        ResourceRunner getRunner() {
            return new ResourceRunner(key, width, height, cacheLoader, decoder, tranformation, transcoder,
                    sourceRunner, diskCacheService, diskCacheStrategy, resizeService, engineJob, priority);
        }
    }
}
