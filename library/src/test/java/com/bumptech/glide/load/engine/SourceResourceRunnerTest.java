package com.bumptech.glide.load.engine;

import com.bumptech.glide.CacheLoader;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.Encoder;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.engine.cache.DiskCache;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;
import com.bumptech.glide.request.ResourceCallback;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
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

@RunWith(RobolectricTestRunner.class)
public class SourceResourceRunnerTest {
    private SourceResourceHarness harness;

    @Before
    public void setUp() {
        harness = new SourceResourceHarness();
    }

    @Test
    public void testCacheLoaderIsCalledWithOriginalKey() {
        harness.getRunner().run();

        verify(harness.cacheLoader).load(eq(harness.originalKey), eq(harness.cacheDecoder), eq(harness.width),
                eq(harness.height));
    }

    @Test
    public void testIfCacheLoaderReturnsOriginalResourceFetcherIsNotCalled() throws Exception {
        when(harness.cacheLoader.load(eq(harness.originalKey), eq(harness.cacheDecoder), eq(harness.width),
                eq(harness.height))).thenReturn(harness.decoded);

        harness.getRunner().run();

        verify(harness.fetcher, never()).loadData(any(Priority.class));
    }

    @Test
    public void testIfCacheLoaderReturnsOriginalResourceThenOriginalResourceIsTransformedAndReturned() {
        when(harness.cacheLoader.load(eq(harness.originalKey), eq(harness.cacheDecoder), eq(harness.width),
                eq(harness.height))).thenReturn(harness.decoded);

        harness.getRunner().run();

        verify(harness.cb).onResourceReady(eq(harness.transcoded));
    }

    @Test
    public void testEncoderIsCalledWithTransformedIfOriginalResourceInCache() {
        final SourceResourceRunner<Object, Object, Object> runner = harness.getRunner();
        when(harness.cacheLoader.load(eq(harness.originalKey), eq(harness.cacheDecoder), eq(harness.width),
                eq(harness.height))).thenReturn(harness.decoded);
        final OutputStream expected = new ByteArrayOutputStream();
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                runner.write(expected);
                return null;
            }
        }).when(harness.diskCache).put(eq(harness.key), eq(runner));

        runner.run();

        verify(harness.encoder).encode(eq(harness.transformed), eq(expected));
    }

    @Test
    public void testResourceFetcherIsCalledIfOriginalNotInCache() throws Exception {
        harness.getRunner().run();

        verify(harness.fetcher).loadData(eq(harness.priority));
    }

    @Test
    public void testDecoderIsCalledIfFetched() throws Exception {
        Object fetched = new Object();
        when(harness.fetcher.loadData(eq(harness.priority))).thenReturn(fetched);

        harness.getRunner().run();

        verify(harness.decoder).decode(eq(fetched), eq(harness.width), eq(harness.height));
    }

    @Test
    public void testCallbackIsCalledWithTranscodedResourceIfFetchedAndDecoded() throws Exception {
        InputStream is = new ByteArrayInputStream(new byte[0]);
        when(harness.fetcher.loadData(eq(harness.priority))).thenReturn(is);
        when(harness.decoder.decode(eq(is), eq(harness.width), eq(harness.height))).thenReturn(harness.decoded);
        when(harness.transcoder.transcode(harness.decoded)).thenReturn(harness.transcoded);

        harness.getRunner().run();

        verify(harness.cb).onResourceReady(eq(harness.transcoded));
    }

    @Test
    public void testResourceIsWrittenToCacheIfFetchedAndDecoded() throws Exception {
        SourceResourceRunner<Object, Object, Object> runner = harness.getRunner();
        InputStream is = new ByteArrayInputStream(new byte[0]);
        when(harness.fetcher.loadData(eq(harness.priority))).thenReturn(is);
        when(harness.decoder.decode(eq(is), eq(harness.width), eq(harness.height))).thenReturn(harness.decoded);

        final OutputStream expected = new ByteArrayOutputStream();

        runner.run();
        runner.write(expected);

        verify(harness.encoder).encode(eq(harness.transformed), eq(expected));
    }

    @Test
    public void testResourceIsTransformedBeforeBeingWrittenToCache() throws Exception {
        SourceResourceRunner<Object, Object, Object> runner = harness.getRunner();
        InputStream is = new ByteArrayInputStream(new byte[0]);
        when(harness.fetcher.loadData(eq(harness.priority))).thenReturn(is);
        when(harness.decoder.decode(eq(is), eq(harness.width), eq(harness.height))).thenReturn(harness.decoded);
        Resource transformed = mock(Resource.class);
        when(harness.transformation.transform(eq(harness.decoded), eq(harness.width), eq(harness.height)))
                .thenReturn(transformed);

        OutputStream expected = new ByteArrayOutputStream();
        runner.run();
        runner.write(expected);

        verify(harness.encoder).encode(eq(transformed), eq(expected));
    }

    @Test
    public void testDecodedResourceIsRecycledIfTransformedResourceIsDifferent() throws Exception {
        InputStream is = new ByteArrayInputStream(new byte[0]);
        when(harness.fetcher.loadData(eq(harness.priority))).thenReturn(is);
        when(harness.decoder.decode(eq(is), eq(harness.width), eq(harness.height))).thenReturn(harness.decoded);
        Resource transformed = mock(Resource.class);
        when(harness.transformation.transform(eq(harness.decoded), eq(harness.width), eq(harness.height)))
                .thenReturn(transformed);

        harness.getRunner().run();

        verify(harness.decoded).recycle();
    }

    @Test
    public void testFetcherIsCleanedUp() {
        harness.getRunner().run();

        verify(harness.fetcher).cleanup();
    }

    @Test
    public void testFetcherIsCleanedUpIfDecodeThrows() throws Exception {
        when(harness.fetcher.loadData(any(Priority.class))).thenReturn(new Object());
        when(harness.decoder.decode(anyObject(), anyInt(), anyInt())).thenThrow(new IOException("test"));

        harness.getRunner().run();

        verify(harness.fetcher).cleanup();
    }

    @Test
    public void testFetcherIsCleanedUpIfFetcherThrows() throws Exception {
        when(harness.fetcher.loadData(any(Priority.class))).thenThrow(new IOException("test"));

        harness.getRunner().run();

        verify(harness.fetcher).cleanup();
    }

    @Test
    public void testDecodedResourceIsNotRecycledIfResourceIsNotTransformed() throws Exception {
        InputStream is = new ByteArrayInputStream(new byte[0]);
        when(harness.fetcher.loadData(eq(harness.priority))).thenReturn(is);
        when(harness.decoder.decode(eq(is), eq(harness.width), eq(harness.height))).thenReturn(harness.decoded);
        when(harness.transformation.transform(eq(harness.decoded), eq(harness.width), eq(harness.height)))
                .thenReturn(harness.decoded);

        harness.getRunner().run();

        verify(harness.decoded, never()).recycle();
    }

    @Test
    public void testCallbackIsCalledIfFetchFails() throws Exception {
        Exception expected = new Exception("Test");
        when(harness.fetcher.loadData(eq(harness.priority))).thenThrow(expected);

        harness.getRunner().run();

        verify(harness.cb).onException(eq(expected));
    }

    @Test
    public void testCallbackIsCalledIfDecodeFails() throws Exception {
        when(harness.fetcher.loadData(eq(harness.priority))).thenReturn(new Object());
        when(harness.decoder.decode(anyObject(), anyInt(), anyInt())).thenReturn(null);

        harness.getRunner().run();

        verify(harness.cb).onException((Exception) isNull());
    }

    @Test
    public void testResourceFetcherCancelIsCalledWhenCancelled() {
        harness.getRunner().cancel();

        verify(harness.fetcher).cancel();
    }

    @Test
    public void testFetcherNotCalledIfCancelled() throws Exception {
        SourceResourceRunner<Object, Object, Object> runner = harness.getRunner();
        runner.cancel();
        runner.run();

        verify(harness.fetcher, never()).loadData(any(Priority.class));
    }

    @Test
    public void testPriorityMatchesPriority() {
        harness.priority = Priority.LOW;
        assertEquals(harness.priority.ordinal(), harness.getRunner()
                .getPriority());
    }

    @Test
    public void testReturnsEncodersWriteResultFromWrite() {
        when(harness.encoder.encode(any(Resource.class), any(OutputStream.class))).thenReturn(true);
        assertTrue(harness.getRunner().write(new ByteArrayOutputStream()));

        when(harness.encoder.encode(any(Resource.class), any(OutputStream.class))).thenReturn(false);
        assertFalse(harness.getRunner()
                .write(new ByteArrayOutputStream()));
    }

    @Test
    public void testDoesNotNormallyEncodeRetrievedData() throws Exception {
        Object data = new Object();
        when(harness.fetcher.loadData(any(Priority.class))).thenReturn(data);

        harness.getRunner().run();

        verify(harness.sourceEncoder, never()).encode(eq(data), any(OutputStream.class));
    }

    @Test
    public void testEncodesRetrievedDataIfAskedToCacheSource() throws Exception {
        harness.cacheSource = true;
        Object data = new Object();
        when(harness.fetcher.loadData(any(Priority.class))).thenReturn(data);
        doAnswer(new CallWriter()).when(harness.diskCache).put(eq(harness.originalKey), any(DiskCache.Writer.class));

        harness.getRunner().run();

        verify(harness.sourceEncoder).encode(eq(data), any(OutputStream.class));
    }

    @Test
    public void testReturnsDecodedDataFromCacheIfAskedToCacheSource() throws Exception {
        harness.cacheSource = true;
        Object data = new Object();
        when(harness.fetcher.loadData(any(Priority.class))).thenReturn(data);
        doAnswer(new CallWriter() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                super.answer(invocation);
                when(harness.cacheLoader.load(eq(harness.originalKey), eq(harness.cacheDecoder), eq(harness.width),
                        eq(harness.height))).thenReturn(harness.decoded);
                return null;
            }
        }).when(harness.diskCache).put(eq(harness.originalKey), any(DiskCache.Writer.class));

        harness.getRunner().run();

        verify(harness.cb).onResourceReady(eq(harness.transcoded));
    }

    private static class CallWriter implements Answer<Void> {
        @Override
        public Void answer(InvocationOnMock invocation) throws Throwable {
            DiskCache.Writer writer = (DiskCache.Writer) invocation.getArguments()[1];
            writer.write(new ByteArrayOutputStream());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static class SourceResourceHarness {
        CacheLoader cacheLoader = mock(CacheLoader.class);
        ResourceDecoder<InputStream, Object> cacheDecoder = mock(ResourceDecoder.class);
        DataFetcher<Object> fetcher = mock(DataFetcher.class);
        ResourceDecoder<Object, Object> decoder = mock(ResourceDecoder.class);
        ResourceEncoder<Object> encoder = mock(ResourceEncoder.class);
        ResourceTranscoder<Object, Object> transcoder = mock(ResourceTranscoder.class);
        DiskCache diskCache = mock(DiskCache.class);
        Priority priority = Priority.LOW;
        ResourceCallback cb = mock(ResourceCallback.class);
        Resource<Object> decoded = mock(Resource.class);
        Resource<Object> transformed = mock(Resource.class);
        Resource<Object> transcoded = mock(Resource.class);
        Transformation<Object> transformation = mock(Transformation.class);
        boolean cacheSource = false;
        Encoder<Object> sourceEncoder = mock(Encoder.class);
        int width = 150;
        int height = 200;
        EngineKey key = mock(EngineKey.class);
        Key originalKey = mock(Key.class);

        public SourceResourceRunner<Object, Object, Object> getRunner() {
            return new SourceResourceRunner<Object, Object, Object>(key, width, height, cacheLoader, cacheDecoder,
                    fetcher, cacheSource, sourceEncoder, decoder, transformation, encoder, transcoder, diskCache,
                    priority, cb);
        }

        public SourceResourceHarness() {
            when(key.getOriginalKey()).thenReturn(originalKey);
            when(transformation.transform(eq(decoded), eq(width), eq(height))).thenReturn(transformed);
            when(transcoder.transcode(eq(transformed))).thenReturn(transcoded);
        }
    }
}
