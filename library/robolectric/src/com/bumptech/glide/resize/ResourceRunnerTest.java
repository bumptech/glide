package com.bumptech.glide.resize;

import com.bumptech.glide.resize.cache.DiskCache;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ResourceRunnerTest {

    @Test
    public void testDiskCacheIsAlwaysChecked() {
        String id = "asdf";
        DiskCache diskCache = mock(DiskCache.class);
        ResourceDecoder decoder = mock(ResourceDecoder.class);

        ResourceRunner runner = new ResourceRunner(id, diskCache, decoder, mock(SourceResourceRunner.class),
                mock(ExecutorService.class), mock(ResourceCallback.class));
        runner.run();

        verify(diskCache).get(eq(id));
    }

    @Test
    public void testCacheDecoderIsCalledIfInCache() {
        String id = "asdf";
        DiskCache diskCache = mock(DiskCache.class);
        InputStream result = new ByteArrayInputStream(new byte[0]);
        when(diskCache.get(eq(id))).thenReturn(result);
        ResourceDecoder decoder = mock(ResourceDecoder.class);

        ResourceRunner runner = new ResourceRunner(id, diskCache, decoder, mock(SourceResourceRunner.class),
                mock(ExecutorService.class), mock(ResourceCallback.class));
        runner.run();

        verify(decoder).decode(eq(result));
    }

    @Test
    public void callbackIsCalledIfCacheDecodeSucceeds() {
        String id = "asdf";
        DiskCache diskCache = mock(DiskCache.class);
        InputStream is = new ByteArrayInputStream(new byte[0]);
        when(diskCache.get(eq(id))).thenReturn(is);
        Resource result = mock(Resource.class);
        ResourceDecoder decoder = mock(ResourceDecoder.class);
        when(decoder.decode(eq(is))).thenReturn(result);
        ResourceCallback cb = mock(ResourceCallback.class);

        ResourceRunner runner = new ResourceRunner(id, diskCache, decoder, mock(SourceResourceRunner.class),
                mock(ExecutorService.class), cb);
        runner.run();

        verify(cb).onResourceReady(eq(result));
    }

    @Test
    public void testCallbackIsNotCalledIfDiskCacheReturnsNull() {
        String id = "asdf";
        DiskCache diskCache = mock(DiskCache.class);
        when(diskCache.get(eq(id))).thenReturn(null);
        ResourceCallback cb = mock(ResourceCallback.class);

        ResourceRunner runner = new ResourceRunner(id, diskCache, mock(ResourceDecoder.class),
                mock(SourceResourceRunner.class), mock(ExecutorService.class), cb);
        runner.run();


        verify(diskCache, atLeastOnce()).get(eq(id));
        verify(cb, never()).onException(any(Exception.class));
    }

    @Test
    public void testCallbackIsNotCalledIfCacheDecodeFails() {
        String id = "asdf";
        DiskCache diskCache = mock(DiskCache.class);
        when(diskCache.get(eq(id))).thenReturn(new ByteArrayInputStream(new byte[0]));
        ResourceDecoder decoder = mock(ResourceDecoder.class);
        when(decoder.decode(anyObject())).thenReturn(null);
        ResourceCallback cb = mock(ResourceCallback.class);

        ResourceRunner runner = new ResourceRunner(id, diskCache, decoder, mock(SourceResourceRunner.class),
               mock(ExecutorService.class), cb);
        runner.run();

        verify(cb, never()).onException(any(Exception.class));
    }

    @Test
    public void testSourceRunnerIsQueuedIfNotInCache() {
        String id = "asdf";
        DiskCache diskCache = mock(DiskCache.class);
        when(diskCache.get(eq(id))).thenReturn(null);

        SourceResourceRunner sourceRunner = mock(SourceResourceRunner.class);
        ExecutorService executorService = mock(ExecutorService.class);

        ResourceRunner runner = new ResourceRunner(id, diskCache, mock(ResourceDecoder.class), sourceRunner,
                executorService, mock(ResourceCallback.class));
        runner.run();

        verify(executorService).submit(eq(sourceRunner));
    }

    @Test
    public void testSourceRunnerIsQueuedIfCacheDecodeFails() {
        String id = "asdf";
        DiskCache diskCache = mock(DiskCache.class);
        when(diskCache.get(eq(id))).thenReturn(new ByteArrayInputStream(new byte[0]));
        ResourceDecoder decoder = mock(ResourceDecoder.class);
        when(decoder.decode(anyObject())).thenReturn(null);
        SourceResourceRunner sourceRunner = mock(SourceResourceRunner.class);
        ExecutorService executorService = mock(ExecutorService.class);

        ResourceRunner runner = new ResourceRunner(id, diskCache, decoder, sourceRunner, executorService,
                mock(ResourceCallback.class));
        runner.run();

        verify(executorService).submit(eq(sourceRunner));
    }
}
