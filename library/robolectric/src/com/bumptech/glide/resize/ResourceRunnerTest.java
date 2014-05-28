package com.bumptech.glide.resize;

import android.os.Handler;
import com.bumptech.glide.resize.cache.DiskCache;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

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

        verify(harness.service).submit(eq(harness.sourceRunner));
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
    public void testCallbackIsCalledIfCacheDecodeSucceeds() throws IOException {
        InputStream is = new ByteArrayInputStream(new byte[0]);
        when(harness.diskCache.get(eq(harness.key))).thenReturn(is);
        when(harness.decoder.decode(eq(is), eq(harness.width), eq(harness.height))).thenReturn(harness.result);

        harness.runner.run();

        verify(harness.engineJob).onResourceReady(eq(harness.result));
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

        verify(harness.service).submit(eq(harness.sourceRunner));
    }

    @Test
    public void testSourceRunnerIsQueuedIfCacheDecodeFails() throws IOException {
        when(harness.diskCache.get(eq(harness.key))).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(harness.decoder.decode(anyObject(), anyInt(), anyInt())).thenReturn(null);

        harness.runner.run();

        verify(harness.service).submit(eq(harness.sourceRunner));
    }

    @Test
    public void testPostedToBackgroundHandlerWhenQueued() {
        harness.runner.queue();

        verify(harness.bgHandler).post(eq(harness.runner));
    }

    @Test
    public void testRemovedFromBackgroundHandlerWhenCancelled() {
        harness.runner.queue();
        harness.runner.cancel();

        verify(harness.bgHandler).removeCallbacks(eq(harness.runner));
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

    @SuppressWarnings("unchecked")
    private static class ResourceRunnerHarness {
        Key key = mock(Key.class);
        DiskCache diskCache = mock(DiskCache.class);
        ResourceDecoder<Object, Object> decoder = mock(ResourceDecoder.class);
        SourceResourceRunner<Object, Object> sourceRunner = mock(SourceResourceRunner.class);
        ExecutorService service = mock(ExecutorService.class);
        EngineJob<Object> engineJob = mock(EngineJob.class);
        Handler bgHandler = mock(Handler.class);
        int width = 100;
        int height = 100;
        ResourceRunner<Object> runner = new ResourceRunner(key, width, height, diskCache, decoder,
                sourceRunner, service, bgHandler, engineJob);
        Future future = mock(Future.class);
        Future sourceFuture = mock(Future.class);
        Resource<Object> result = mock(Resource.class);

        public ResourceRunnerHarness() {
            when(key.toString()).thenReturn(ID);
            when(service.submit(eq(runner))).thenReturn(future);
            when(service.submit(eq(sourceRunner))).thenReturn(sourceFuture);
        }
    }
}
