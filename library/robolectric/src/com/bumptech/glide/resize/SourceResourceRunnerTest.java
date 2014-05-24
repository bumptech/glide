package com.bumptech.glide.resize;

import com.bumptech.glide.loader.bitmap.resource.ResourceFetcher;
import com.bumptech.glide.resize.cache.DiskCache;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SourceResourceRunnerTest {
    private static final String ID = "asdf2";

    @Test
    public void testResourceFetcherIsCalled() throws Exception {
        ResourceFetcher resourceFetcher = mock(ResourceFetcher.class);

        SourceResourceRunner runner = new SourceResourceRunner(ID, resourceFetcher, mock(ResourceDecoder.class),
                mock(ResourceEncoder.class), mock(DiskCache.class), mock(Metadata.class), mock(ResourceCallback.class));
        runner.run();

        verify(resourceFetcher).loadResource(any(Metadata.class));
    }

    @Test
    public void testDecoderIsCalledIfFetched() throws Exception {
        ResourceFetcher resourceFetcher = mock(ResourceFetcher.class);
        Object fetched = new Object();
        when(resourceFetcher.loadResource(any(Metadata.class)))
                .thenReturn(fetched);
        ResourceDecoder decoder = mock(ResourceDecoder.class);

        SourceResourceRunner runner = new SourceResourceRunner(ID, resourceFetcher, decoder,
                mock(ResourceEncoder.class), mock(DiskCache.class), mock(Metadata.class), mock(ResourceCallback.class));
        runner.run();

        verify(decoder).decode(eq(fetched));
    }

    @Test
    public void testCallbackIsCalledIfFetchedAndDecoded() throws Exception {
        ResourceFetcher resourceFetcher = mock(ResourceFetcher.class);
        InputStream is = new ByteArrayInputStream(new byte[0]);
        when(resourceFetcher.loadResource(any(Metadata.class))).thenReturn(is);
        ResourceDecoder decoder = mock(ResourceDecoder.class);
        Resource result = mock(Resource.class);
        when(decoder.decode(eq(is))).thenReturn(result);
        ResourceCallback cb = mock(ResourceCallback.class);

        SourceResourceRunner runner = new SourceResourceRunner(ID, resourceFetcher, decoder,
                mock(ResourceEncoder.class), mock(DiskCache.class), mock(Metadata.class), cb);
        runner.run();

        verify(cb).onResourceReady(eq(result));
    }

    @Test
    public void testResourceIsWrittenToCacheIfFetchedAndDecoded() throws Exception {
        ResourceFetcher resourceFetcher = mock(ResourceFetcher.class);
        InputStream is = new ByteArrayInputStream(new byte[0]);
        when(resourceFetcher.loadResource(any(Metadata.class))).thenReturn(is);
        ResourceDecoder decoder = mock(ResourceDecoder.class);
        Resource result = mock(Resource.class);
        when(decoder.decode(eq(is))).thenReturn(result);

        DiskCache diskCache = mock(DiskCache.class);
        final OutputStream expected = new ByteArrayOutputStream();
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                DiskCache.Writer writer = (DiskCache.Writer) invocation.getArguments()[1];
                writer.write(expected);
                return null;
            }
        }).when(diskCache).put(eq(ID), any(DiskCache.Writer.class));
        ResourceEncoder encoder = mock(ResourceEncoder.class);

        SourceResourceRunner runner = new SourceResourceRunner(ID, resourceFetcher, decoder, encoder, diskCache,
                mock(Metadata.class), mock(ResourceCallback.class));
        runner.run();

        verify(encoder).encode(eq(result), eq(expected));
    }

    @Test
    public void testCallbackIsCalledIfFetchFails() throws Exception {
        ResourceFetcher fetcher = mock(ResourceFetcher.class);
        Exception expected = new Exception("Test");
        when(fetcher.loadResource(any(Metadata.class))).thenThrow(expected);
        ResourceCallback cb = mock(ResourceCallback.class);

        SourceResourceRunner runner = new SourceResourceRunner(ID, fetcher, mock(ResourceDecoder.class),
                mock(ResourceEncoder.class), mock(DiskCache.class), mock(Metadata.class), cb);
        runner.run();

        verify(cb).onException(eq(expected));
    }

    @Test
    public void testCallbackIsCalledIfDecodeFails() throws Exception {
        ResourceFetcher fetcher = mock(ResourceFetcher.class);
        when(fetcher.loadResource(any(Metadata.class))).thenReturn(new Object());
        ResourceDecoder decoder = mock(ResourceDecoder.class);
        when(decoder.decode(anyObject())).thenReturn(null);
        ResourceCallback cb = mock(ResourceCallback.class);

        SourceResourceRunner runner = new SourceResourceRunner(ID, fetcher, decoder, mock(ResourceEncoder.class),
                mock(DiskCache.class), mock(Metadata.class), cb);
        runner.run();

        verify(cb).onException((Exception) isNull());
    }

}
