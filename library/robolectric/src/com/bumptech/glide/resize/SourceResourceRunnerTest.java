package com.bumptech.glide.resize;

import com.bumptech.glide.loader.bitmap.resource.ResourceFetcher;
import com.bumptech.glide.resize.cache.DiskCache;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SourceResourceRunnerTest {
    private static final String ID = "asdf2";
    private SourceResourceHarness harness;

    @Before
    public void setUp() {
        harness = new SourceResourceHarness();
    }

    @Test
    public void testResourceFetcherIsCalled() throws Exception {
        harness.runner.run();

        verify(harness.fetcher).loadResource(eq(harness.metadata));
    }

    @Test
    public void testDecoderIsCalledIfFetched() throws Exception {
        Object fetched = new Object();
        when(harness.fetcher.loadResource(eq(harness.metadata))).thenReturn(fetched);

        harness.runner.run();

        verify(harness.decoder).decode(eq(fetched), eq(harness.width), eq(harness.height));
    }

    @Test
    public void testCallbackIsCalledIfFetchedAndDecoded() throws Exception {
        InputStream is = new ByteArrayInputStream(new byte[0]);
        when(harness.fetcher.loadResource(eq(harness.metadata))).thenReturn(is);
        when(harness.decoder.decode(eq(is), eq(harness.width), eq(harness.height))).thenReturn(harness.result);

        harness.runner.run();

        verify(harness.cb).onResourceReady(eq(harness.result));
    }

    @Test
    public void testResourceIsWrittenToCacheIfFetchedAndDecoded() throws Exception {
        InputStream is = new ByteArrayInputStream(new byte[0]);
        when(harness.fetcher.loadResource(eq(harness.metadata))).thenReturn(is);
        when(harness.decoder.decode(eq(is), eq(harness.width), eq(harness.height))).thenReturn(harness.result);

        final OutputStream expected = new ByteArrayOutputStream();
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                DiskCache.Writer writer = (DiskCache.Writer) invocation.getArguments()[1];
                writer.write(expected);
                return null;
            }
        }).when(harness.diskCache).put(eq(ID), any(DiskCache.Writer.class));

        harness.runner.run();

        verify(harness.encoder).encode(eq(harness.result), eq(expected));
    }

    @Test
    public void testCallbackIsCalledIfFetchFails() throws Exception {
        Exception expected = new Exception("Test");
        when(harness.fetcher.loadResource(eq(harness.metadata))).thenThrow(expected);

        harness.runner.run();

        verify(harness.cb).onException(eq(expected));
    }

    @Test
    public void testCallbackIsCalledIfDecodeFails() throws Exception {
        when(harness.fetcher.loadResource(eq(harness.metadata))).thenReturn(new Object());
        when(harness.decoder.decode(anyObject(), anyInt(), anyInt())).thenReturn(null);

        harness.runner.run();

        verify(harness.cb).onException((Exception) isNull());
    }

    @Test
    public void testResourceFetcherCancelIsCalledWhenCancelled() {
        harness.runner.cancel();

        verify(harness.fetcher).cancel();
    }

    @Test
    public void testFetcherNotCalledIfCancelled() throws Exception {
        harness.runner.cancel();
        harness.runner.run();

        verify(harness.fetcher, never()).loadResource(any(Metadata.class));
    }

    @SuppressWarnings("unchecked")
    private static class SourceResourceHarness {
        ResourceFetcher<Object> fetcher = mock(ResourceFetcher.class);
        ResourceDecoder<Object, Object> decoder = mock(ResourceDecoder.class);
        ResourceEncoder<Object> encoder = mock(ResourceEncoder.class);
        DiskCache diskCache = mock(DiskCache.class);
        Metadata metadata = mock(Metadata.class);
        ResourceCallback<Object> cb = mock(ResourceCallback.class);
        Resource<Object> result = mock(Resource.class);
        int width = 150;
        int height = 200;
        SourceResourceRunner<Object, Object> runner = new SourceResourceRunner<Object, Object>(ID, width, height,
                fetcher, decoder, encoder, diskCache, metadata, cb);
    }
}
