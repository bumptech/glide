package com.bumptech.glide.load.engine;

import com.bumptech.glide.Priority;
import com.bumptech.glide.Resource;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.cache.DiskCache;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class ResourceRunnerTest {
    private static final String ID = "asdf";
    private ResourceRunnerHarness harness;

    @Before
    public void setUp() {
        harness = new ResourceRunnerHarness();
    }

    @Test
    public void testSourceRunnerIsSubmittedIfCacheDecoderThrows() throws IOException {
        when(harness.diskCache.get(eq(harness.key))).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(harness.decoder.decode(anyObject(), anyInt(), anyInt())).thenThrow(new IOException("Test"));

        harness.runner.run();

        verify(harness.resizeService).submit(eq(harness.sourceRunner));
    }

    @Test
    public void testDiskCacheEntryIsDeletedIfCacheDecoderThrows() throws IOException {
        when(harness.diskCache.get(eq(harness.key))).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(harness.decoder.decode(anyObject(), anyInt(), anyInt())).thenThrow(new IOException("Test"));

        harness.runner.run();

        verify(harness.diskCache).delete(eq(harness.key));
    }

    @Test
    public void testDiskCacheEntryIsDeletedIfDiskCacheContainsIdAndCacheDecoderReturnsNull() throws IOException {
        when(harness.diskCache.get(eq(harness.key))).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(harness.decoder.decode(anyObject(), anyInt(), anyInt())).thenReturn(null);

        harness.runner.run();

        verify(harness.diskCache).delete(eq(harness.key));
    }

    @Test
    public void testDiskCacheIsAlwaysChecked() {
        harness.runner.run();

        verify(harness.diskCache).get(eq(harness.key));
    }

    @Test
    public void testCacheDecoderIsCalledIfInCache() throws IOException {
        InputStream result = new ByteArrayInputStream(new byte[0]);
        when(harness.diskCache.get(eq(harness.key))).thenReturn(result);

        harness.runner.run();

        verify(harness.decoder).decode(eq(result), eq(harness.width), eq(harness.height));
    }

    @Test
    public void testTransformationIsCalledIfCacheDecodeSucceeds() throws IOException {
        InputStream is = new ByteArrayInputStream(new byte[0]);
        when(harness.diskCache.get(eq(harness.key))).thenReturn(is);
        when(harness.decoder.decode(eq(is), eq(harness.width), eq(harness.height))).thenReturn(harness.decoded);

        harness.runner.run();

        verify(harness.tranformation).transform(eq(harness.decoded), eq(harness.width), eq(harness.height));
    }

    @Test
    public void testTranscoderIsCalledIfCacheDecodeSucceeds() throws IOException {
        InputStream is = new ByteArrayInputStream(new byte[0]);
        when(harness.diskCache.get(eq(harness.key))).thenReturn(is);
        when(harness.decoder.decode(eq(is), eq(harness.width), eq(harness.height))).thenReturn(harness.decoded);
        when(harness.tranformation.transform(eq(harness.decoded), eq(harness.width), eq(harness.height)))
                .thenReturn(harness.transformed);

        harness.runner.run();

        verify(harness.transcoder).transcode(eq(harness.transformed));
    }

    @Test
    public void testCallbackIsCalledIfCacheDecodeSucceeds() throws IOException {
        InputStream is = new ByteArrayInputStream(new byte[0]);
        when(harness.diskCache.get(eq(harness.key))).thenReturn(is);
        when(harness.decoder.decode(eq(is), eq(harness.width), eq(harness.height))).thenReturn(harness.decoded);
        when(harness.tranformation.transform(eq(harness.decoded), eq(harness.width), eq(harness.height)))
                .thenReturn(harness.transformed);
        when(harness.transcoder
                .transcode(eq(harness.transformed))).thenReturn(harness.transcoded);

        harness.runner.run();

        verify(harness.engineJob).onResourceReady(eq(harness.transcoded));
    }

    @Test
    public void testDecodedResourceIsNotRecycledIfTranscoded() throws IOException {
        InputStream is = new ByteArrayInputStream(new byte[0]);
        when(harness.diskCache.get(eq(harness.key))).thenReturn(is);
        when(harness.decoder.decode(eq(is), eq(harness.width), eq(harness.height))).thenReturn(harness.decoded);
        when(harness.tranformation.transform(eq(harness.decoded), eq(harness.width), eq(harness.height)))
                .thenReturn(harness.transformed);
        when(harness.transcoder
                .transcode(eq(harness.transformed))).thenReturn(harness.transcoded);

        harness.runner.run();

        verify(harness.transformed, never()).recycle();
        verify(harness.transformed, never()).release();
    }

    @Test
    public void testCallbackIsNotCalledIfDiskCacheReturnsNull() {
        when(harness.diskCache.get(eq(harness.key))).thenReturn(null);

        harness.runner.run();

        verify(harness.diskCache, atLeastOnce()).get(eq(harness.key));
        verify(harness.engineJob, never()).onException(any(Exception.class));
    }

    @Test
    public void testCallbackIsNotCalledIfCacheDecodeFails() throws IOException {
        when(harness.diskCache.get(eq(harness.key))).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(harness.decoder.decode(anyObject(), anyInt(), anyInt())).thenReturn(null);
        harness.runner.run();

        verify(harness.engineJob, never()).onException(any(Exception.class));
    }

    @Test
    public void testSourceRunnerIsQueuedIfNotInCache() {
        when(harness.diskCache.get(eq(harness.key))).thenReturn(null);

        harness.runner.run();

        verify(harness.resizeService).submit(eq(harness.sourceRunner));
    }

    @Test
    public void testSourceRunnerIsQueuedIfCacheDecodeFails() throws IOException {
        when(harness.diskCache.get(eq(harness.key))).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(harness.decoder.decode(anyObject(), anyInt(), anyInt())).thenReturn(null);

        harness.runner.run();

        verify(harness.resizeService).submit(eq(harness.sourceRunner));
    }

    @Test
    public void testSourceRunnerIsQueuedIfCacheDecodeThrows() throws IOException {
        when(harness.diskCache.get(eq(harness.key))).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(harness.decoder.decode(anyObject(), anyInt(), anyInt())).thenThrow(new IOException("test"));

        harness.runner.run();

        verify(harness.resizeService).submit(eq(harness.sourceRunner));
    }

    @Test
    public void testPostedToDiskCacheSerciceWhenQueued() {
        harness.runner.queue();

        verify(harness.diskCacheService).submit(eq(harness.runner));
    }

    @Test
    public void testCancelsFutureFromDiskCacheServiceWhenCancelledIfNotYetQueuedToResizeService() {
        Future future = mock(Future.class);
        when(harness.diskCacheService.submit(eq(harness.runner))).thenReturn(future);
        harness.runner.queue();
        harness.runner.cancel();

        verify(future).cancel(eq(false));
    }

    @Test
    public void testResourceIsNotLoadedFromDiskCacheIfCancelled() {
        harness.runner.queue();
        harness.runner.cancel();
        harness.runner.run();

        verify(harness.diskCache, never()).get(any(Key.class));
    }

    @Test
    public void testSourceRunnerIsCancelledIfCancelledAfterSubmitted() {
        harness.runner.queue();
        harness.runner.run();
        harness.runner.cancel();

        verify(harness.sourceRunner).cancel();
    }

    @Test
    public void testSourceRunnerFutureIsCancelledIfCancelledAfterSubmitted() {
        harness.runner.queue();
        harness.runner.run();
        harness.runner.cancel();

        verify(harness.sourceFuture).cancel(anyBoolean());
    }

    @Test
    public void testDecodedResourceIsRecycledIfTransformedResourceIsDifferent() throws IOException {
        InputStream is = new ByteArrayInputStream(new byte[0]);
        when(harness.diskCache.get(eq(harness.key))).thenReturn(is);
        when(harness.decoder.decode(eq(is), eq(harness.width), eq(harness.height))).thenReturn(harness.decoded);
        when(harness.tranformation.transform(eq(harness.decoded), eq(harness.width), eq(harness.height)))
                .thenReturn(harness.transformed);

        harness.runner.run();

        verify(harness.decoded).recycle();
    }

    @Test
    public void testDecodedResourceIsNotRecycledIfTransformedResourceIsDecodedResource() throws IOException {
        InputStream is = new ByteArrayInputStream(new byte[0]);
        when(harness.diskCache.get(eq(harness.key))).thenReturn(is);
        when(harness.decoder.decode(eq(is), eq(harness.width), eq(harness.height))).thenReturn(harness.decoded);
        when(harness.tranformation.transform(eq(harness.decoded), eq(harness.width), eq(harness.height)))
                .thenReturn(harness.decoded);

        harness.runner.run();

        verify(harness.decoded, never()).recycle();
    }

    @Test
    public void testReturnsGivenPriority() {
        assertEquals(harness.priority.ordinal(), harness.runner.getPriority());
    }

    @SuppressWarnings("unchecked")
    private static class ResourceRunnerHarness {
        Key key = mock(Key.class);
        DiskCache diskCache = mock(DiskCache.class);
        ResourceDecoder<Object, Object> decoder = mock(ResourceDecoder.class);
        SourceResourceRunner<Object, Object, Object> sourceRunner = mock(SourceResourceRunner.class);
        ResourceTranscoder<Object, Object> transcoder = mock(ResourceTranscoder.class);
        ExecutorService resizeService = mock(ExecutorService.class);
        ExecutorService diskCacheService = mock(ExecutorService.class);
        EngineJob engineJob = mock(EngineJob.class);
        Transformation<Object> tranformation = mock(Transformation.class);
        int width = 100;
        int height = 100;
        Priority priority = Priority.HIGH;
        ResourceRunner<Object, Object> runner = new ResourceRunner(key, width, height, diskCache, decoder,
                tranformation, transcoder, sourceRunner, diskCacheService, resizeService,engineJob, priority);
        Future future = mock(Future.class);
        Future sourceFuture = mock(Future.class);
        Resource<Object> decoded = mock(Resource.class);
        Resource<Object> transformed = mock(Resource.class);
        Resource<Object> transcoded = mock(Resource.class);

        public ResourceRunnerHarness() {
            when(key.toString()).thenReturn(ID);
            when(resizeService.submit(eq(runner))).thenReturn(future);
            when(resizeService.submit(eq(sourceRunner))).thenReturn(sourceFuture);
        }
    }
}
