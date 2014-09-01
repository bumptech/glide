package com.bumptech.glide.load.engine;

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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
        when(harness.factory.build(eq(harness.encoder), eq(harness.transformed))).thenReturn(harness.writer);

        runner.run();

        verify(harness.diskCache).put(eq(harness.key), eq(harness.writer));
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
        harness.mockSuccessfulFetchAndDecode();
        when(harness.transcoder.transcode(harness.decoded)).thenReturn(harness.transcoded);

        harness.getRunner().run();

        verify(harness.cb).onResourceReady(eq(harness.transcoded));
    }

    @Test
    public void testResultResourceIsWrittenToCacheIfFetchedAndDecoded() throws Exception {
        harness.mockSuccessfulFetchAndDecode();
        when(harness.factory.build(eq(harness.encoder), eq(harness.transformed))).thenReturn(harness.writer);

        harness.getRunner().run();

        verify(harness.diskCache).put(eq(harness.key), eq(harness.writer));
    }

    @Test
    public void testResultResourceIsWrittenToCacheIfCacheStrategyIsResult() throws Exception {
        harness.diskCacheStrategy = DiskCacheStrategy.RESULT;
        harness.mockSuccessfulFetchAndDecode();
        when(harness.factory.build(eq(harness.encoder), eq(harness.transformed))).thenReturn(harness.writer);

        harness.getRunner().run();

        verify(harness.diskCache).put(eq(harness.key), eq(harness.writer));
    }

    @Test
    public void testResultResourceIsWrittenToCacheIfCacheStrategyIsAll() throws Exception {
        harness.diskCacheStrategy = DiskCacheStrategy.RESULT;
        harness.mockSuccessfulFetchAndDecode();
        when(harness.factory.build(eq(harness.sourceEncoder), eq(harness.decoded))).thenReturn(harness.sourceWriter);
        when(harness.sourceWriter.write(any(File.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                when(harness.cacheLoader.load(eq(harness.originalKey), eq(harness.cacheDecoder), eq(harness.width),
                        eq(harness.height))).thenReturn(harness.decoded);
                return null;
            }
        });
        when(harness.factory.build(eq(harness.encoder), eq(harness.transformed))).thenReturn(harness.writer);

        harness.getRunner().run();

        verify(harness.diskCache).put(eq(harness.key), eq(harness.writer));
    }

    @Test
    public void testResultResourceIsNotWrittenToCacheIfCacheStrategyIsNoneOrSource() throws Exception {
        for (DiskCacheStrategy strategy : new DiskCacheStrategy[] { DiskCacheStrategy.NONE, DiskCacheStrategy.SOURCE}) {
            harness = new SourceResourceHarness();
            harness.diskCacheStrategy = strategy;
            harness.mockSuccessfulFetchAndDecode();
            when(harness.factory.build(eq(harness.encoder), eq(harness.transformed))).thenReturn(harness.writer);

            harness.getRunner().run();

            verify(harness.diskCache, never()).put(eq(harness.key), any(DiskCache.Writer.class));
        }
    }

    @Test
    public void testResourceIsTransformedBeforeBeingWrittenToCache() throws Exception {
        SourceResourceRunner<Object, Object, Object> runner = harness.getRunner();
        harness.mockSuccessfulFetchAndDecode();
        when(harness.transformation.transform(eq(harness.decoded), eq(harness.width), eq(harness.height)))
                .thenReturn(harness.transformed);
        when(harness.factory.build(eq(harness.encoder), eq(harness.transformed))).thenReturn(harness.writer);

        runner.run();

        verify(harness.diskCache).put(eq(harness.key), eq(harness.writer));
    }

    @Test
    public void testDecodedResourceIsRecycledIfTransformedResourceIsDifferent() throws Exception {
        harness.mockSuccessfulFetchAndDecode();
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
        harness.mockSuccessfulFetchAndDecode();
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
    public void testDoesNotNormallyEncodeRetrievedData() throws Exception {
        Object data = new Object();
        when(harness.fetcher.loadData(any(Priority.class))).thenReturn(data);

        harness.getRunner().run();

        verify(harness.sourceEncoder, never()).encode(eq(data), any(OutputStream.class));
    }

    @Test
    public void testEncodesRetrievedDataIfAskedToCacheSourceOrAll() throws Exception {
        for (DiskCacheStrategy strategy : new DiskCacheStrategy[] { DiskCacheStrategy.ALL, DiskCacheStrategy.SOURCE}) {
            harness = new SourceResourceHarness();
            harness.diskCacheStrategy = strategy;
            Object data = new Object();
            when(harness.fetcher.loadData(any(Priority.class))).thenReturn(data);
            when(harness.factory.build(eq(harness.sourceEncoder), eq(data))).thenReturn(harness.sourceWriter);

            harness.getRunner().run();

            verify(harness.diskCache).put(eq(harness.originalKey), eq(harness.sourceWriter));
        }
    }

    @Test
    public void testDoesNotEncodeRetrievedDataIfAskedToCacheResultOrNone() throws Exception {
        for (DiskCacheStrategy strategy : new DiskCacheStrategy[] { DiskCacheStrategy.NONE, DiskCacheStrategy.RESULT}) {
            harness = new SourceResourceHarness();
            harness.diskCacheStrategy = strategy;
            Object data = new Object();
            when(harness.fetcher.loadData(any(Priority.class))).thenReturn(data);
            when(harness.factory.build(eq(harness.sourceEncoder), eq(data))).thenReturn(harness.sourceWriter);

            harness.getRunner().run();

            verify(harness.diskCache, never()).put(eq(harness.originalKey), eq(harness.sourceWriter));
        }
    }

    @Test
    public void testReturnsDecodedDataFromCacheIfAskedToCacheSource() throws Exception {
        harness.diskCacheStrategy = DiskCacheStrategy.SOURCE;
        Object data = new Object();
        when(harness.fetcher.loadData(any(Priority.class))).thenReturn(data);
        doAnswer(new Answer() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                when(harness.cacheLoader.load(eq(harness.originalKey), eq(harness.cacheDecoder), eq(harness.width),
                        eq(harness.height))).thenReturn(harness.decoded);
                return null;
            }
        }).when(harness.diskCache).put(eq(harness.originalKey), any(DiskCache.Writer.class));

        harness.getRunner().run();

        verify(harness.cb).onResourceReady(eq(harness.transcoded));
    }

    @Test
    public void testNotifiesJobOfFailureIfCacheLoaderThrows() {
        final Exception exception = new RuntimeException("test");
        when(harness.cacheLoader.load(any(Key.class), any(ResourceDecoder.class), anyInt(), anyInt())).thenAnswer(
                new Answer<Object>() {
                    @Override
                    public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                        throw exception;
                    }
                });
        harness.getRunner().run();
        verify(harness.cb).onException(eq(exception));
    }

    @Test
    public void testNotifiesJobOfFailureIfTransformationThrows() throws Exception {
        harness.mockSuccessfulFetchAndDecode();

        final Exception exception = new RuntimeException("test");
        when(harness.transformation.transform(any(Resource.class), anyInt(), anyInt())).thenAnswer(
                new Answer<Object>() {
                    @Override
                    public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                        throw exception;
                    }
                });
        harness.getRunner().run();
        verify(harness.cb).onException(eq(exception));
    }

    @Test
    public void testNotifiesJobOfFailureIfDiskCacheThrows() throws Exception {
        harness.mockSuccessfulFetchAndDecode();

        final Exception exception = new IOException("test");
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                throw exception;
            }
        }).when(harness.diskCache).put(any(Key.class), any(DiskCache.Writer.class));
        harness.getRunner().run();
        verify(harness.cb).onException(eq(exception));
    }

    @Test
    public void testNotifiesJobOfFailureIfTranscoderThrows() throws Exception {
        harness.mockSuccessfulFetchAndDecode();

        final Exception exception = new RuntimeException("test");
        when(harness.transcoder.transcode(any(Resource.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                throw exception;
            }
        });

        harness.getRunner().run();
        verify(harness.cb).onException(eq(exception));
    }

    @Test
    public void testNotifiesJobOfFailureIfFetcherThrows() throws Exception {
        final IOException exception = new IOException("test");
        when(harness.fetcher.loadData(any(Priority.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                throw exception;
            }
        });
        harness.getRunner().run();
        verify(harness.cb).onException(eq(exception));
    }

    @Test
    public void testNotifiesJobOfFailureIfDecoderThrows() throws Exception {
        when(harness.fetcher.loadData(any(Priority.class))).thenReturn(new ByteArrayInputStream(new byte[0]));

        final Exception exception = new RuntimeException("test");
        when(harness.decoder.decode(anyObject(), anyInt(), anyInt())).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                throw exception;
            }
        });
        harness.getRunner().run();
        verify(harness.cb).onException(exception);
    }

    @Test
    public void testNotifiesJobOfFailureOnlyOnce() throws Exception {
        when(harness.fetcher.loadData(any(Priority.class))).thenThrow(new IOException("test"));
        harness.getRunner().run();

        verify(harness.cb, times(1)).onException(any(Exception.class));
    }

    @SuppressWarnings("unchecked")
    private static class SourceResourceHarness {
        CacheLoader cacheLoader = mock(CacheLoader.class);
        ResourceDecoder<File, Object> cacheDecoder = mock(ResourceDecoder.class);
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
        SourceResourceRunner.WriterFactory factory = mock(SourceResourceRunner.WriterFactory.class);
        SourceResourceRunner.SourceWriter<Resource<Object>> writer = mock(SourceResourceRunner.SourceWriter.class);
        SourceResourceRunner.SourceWriter<Object> sourceWriter = mock(SourceResourceRunner.SourceWriter
                .class);
        DiskCacheStrategy diskCacheStrategy = DiskCacheStrategy.RESULT;
        Encoder<Object> sourceEncoder = mock(Encoder.class);
        int width = 150;
        int height = 200;
        EngineKey key = mock(EngineKey.class);
        Key originalKey = mock(Key.class);

        public SourceResourceRunner<Object, Object, Object> getRunner() {
            return new SourceResourceRunner<Object, Object, Object>(key, width, height, cacheLoader, cacheDecoder,
                    fetcher, sourceEncoder, decoder, transformation, encoder, transcoder, diskCache,
                    priority, diskCacheStrategy, cb, factory);
        }

        public SourceResourceHarness() {
            when(key.getOriginalKey()).thenReturn(originalKey);
            when(transformation.transform(eq(decoded), eq(width), eq(height))).thenReturn(transformed);
            when(transcoder.transcode(eq(transformed))).thenReturn(transcoded);
        }

        public void mockSuccessfulFetchAndDecode() throws Exception {
            InputStream is = new ByteArrayInputStream(new byte[0]);
            when(fetcher.loadData(eq(priority))).thenReturn(is);
            when(decoder.decode(eq(is), eq(width), eq(height))).thenReturn(decoded);
        }
    }
}
