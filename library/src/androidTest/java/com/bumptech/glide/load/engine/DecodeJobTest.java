package com.bumptech.glide.load.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.Encoder;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.engine.cache.DiskCache;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;
import com.bumptech.glide.provider.DataLoadProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, emulateSdk = 18)
public class DecodeJobTest {

    private Harness harness;

    @Before
    public void setUp() throws FileNotFoundException {
        harness = new Harness();
    }

    private void mockCacheToReturnResultResource() throws IOException {
        File cacheFile = new File("fake");
        when(harness.diskCache.get(eq(harness.key))).thenReturn(cacheFile);
        when(harness.cacheDecoder.decode(eq(cacheFile), eq(harness.width), eq(harness.height)))
                .thenReturn(harness.resource);
    }

    /** decodeResultFromCache **/

    @Test
    public void testDiskCacheIsCheckedForResultWhenCacheStrategyIncludesResult() throws Exception {
        for (DiskCacheStrategy strategy : list(DiskCacheStrategy.ALL, DiskCacheStrategy.RESULT)) {
            harness = new Harness(strategy);

            mockCacheToReturnResultResource();

            assertEquals("diskCacheStrategy: " + strategy, harness.resource, harness.getJob().decodeResultFromCache());
        }
    }

    @Test
    public void testDiskCacheIsNotCheckedForResultWhenCacheStrategyDoesNotIncludeResult() throws Exception {
        for (DiskCacheStrategy strategy : list(DiskCacheStrategy.NONE, DiskCacheStrategy.SOURCE)) {
            harness = new Harness(strategy);

            harness.getJob().decodeResultFromCache();

            verify(harness.diskCache, never()).get(eq(harness.key));
        }
    }

    @Test
    public void testDecodeResultFromCacheReturnsNullIfDiskCacheStrategyDoesNotIncludeResult() throws Exception {
        for (DiskCacheStrategy strategy : list(DiskCacheStrategy.NONE, DiskCacheStrategy.SOURCE)) {
            harness = new Harness(strategy);
            mockCacheToReturnResultResource();

            assertNull(harness.getJob().decodeResultFromCache());
        }
    }

    @Test
    public void testResultDecodedFromCacheIsTranscodedIfDecodeSucceeds() throws Exception {
        harness.diskCacheStrategy = DiskCacheStrategy.RESULT;
        mockCacheToReturnResultResource();
        Resource<Object> transcoded = mock(Resource.class);
        when(harness.transcoder.transcode(eq(harness.resource))).thenReturn(transcoded);

        assertEquals(transcoded, harness.getJob().decodeResultFromCache());
    }

    @Test
    public void testResultDecodedFromCacheIsNotRecycledIfTranscoded() throws Exception {
        harness.diskCacheStrategy = DiskCacheStrategy.RESULT;
        mockCacheToReturnResultResource();
        Resource<Object> transcoded = mock(Resource.class);
        when(harness.transcoder.transcode(eq(harness.resource))).thenReturn(transcoded);

        harness.getJob().decodeResultFromCache();

        verify(harness.resource, never()).recycle();
    }

    @Test
    public void testDecodeResultFromCacheReturnsNullIfDiskCacheReturnsNull() throws Exception {
        harness.diskCacheStrategy = DiskCacheStrategy.RESULT;
        when(harness.diskCache.get(eq(harness.key))).thenReturn(null);

        assertNull(harness.getJob().decodeResultFromCache());
    }

    @Test(expected = RuntimeException.class)
    public void testDecodeResultFromCacheThrowsIfCacheDecoderThrows() throws Exception {
        harness.diskCacheStrategy = DiskCacheStrategy.RESULT;
        when(harness.diskCache.get(eq(harness.key))).thenReturn(new File("Fake"));
        when(harness.cacheDecoder.decode(any(File.class), anyInt(), anyInt()))
                .thenThrow(new RuntimeException("test"));

        assertNull(harness.getJob().decodeResultFromCache());

        verify(harness.cacheDecoder).decode(any(File.class), anyInt(), anyInt());
    }

    @Test
    public void testEntryIsDeletedFromCacheIfCacheDecoderThrowsButResultIsInCache() throws IOException {
        harness.diskCacheStrategy = DiskCacheStrategy.RESULT;
        when(harness.diskCache.get(eq(harness.key))).thenReturn(new File("fake"));
        when(harness.cacheDecoder.decode(any(File.class), anyInt(), anyInt())).thenThrow(new RuntimeException("test"));

        try {
            harness.getJob().decodeResultFromCache();
            fail("Failed to get expected exception");
        } catch (Exception exception) {
            // Expected.
        }

        verify(harness.diskCache).delete(eq(harness.key));
    }

    @Test
    public void testDecodeResultFromCacheReturnsNullIfCacheDecoderReturnsNull() throws Exception {
        harness.diskCacheStrategy = DiskCacheStrategy.RESULT;
        when(harness.diskCache.get(eq(harness.key))).thenReturn(new File("fake"));
        when(harness.cacheDecoder.decode(any(File.class), anyInt(), anyInt())).thenReturn(null);

        assertNull(harness.getJob().decodeResultFromCache());
    }

    @Test
    public void testEntryIsDeletedFromCacheIfCacheDecoderReturnsNullButResultIsInCache() throws Exception {
        harness.diskCacheStrategy = DiskCacheStrategy.RESULT;
        when(harness.diskCache.get(eq(harness.key))).thenReturn(new File("fake"));
        when(harness.cacheDecoder.decode(any(File.class), anyInt(), anyInt())).thenReturn(null);

        harness.getJob().decodeResultFromCache();
        verify(harness.diskCache).delete(eq(harness.key));
    }

    @Test
    public void testDecodeResultFromCacheReturnsNullIfTranscoderReturnsNull() throws Exception {
        harness.diskCacheStrategy = DiskCacheStrategy.RESULT;
        mockCacheToReturnResultResource();
        when(harness.transcoder.transcode(any(Resource.class))).thenReturn(null);

        assertNull(harness.getJob().decodeResultFromCache());
    }

    @Test(expected = RuntimeException.class)
    public void testDecodeResultFromCacheThrowsIfTranscoderThrows() throws Exception {
        harness.diskCacheStrategy = DiskCacheStrategy.RESULT;
        mockCacheToReturnResultResource();
        when(harness.transcoder.transcode(any(Resource.class))).thenThrow(new RuntimeException("test"));

        assertNull(harness.getJob().decodeResultFromCache());

        verify(harness.transcoder).transcode(any(Resource.class));
    }

    /** decodeSourceFromCache **/

    private void mockCacheToReturnSourceResource() throws IOException {
        File file = new File("Test");
        when(harness.diskCache.get(eq(harness.originalKey))).thenReturn(file);
        when(harness.cacheDecoder.decode(eq(file), eq(harness.width), eq(harness.height)))
                .thenReturn(harness.resource);
    }

    @Test
    public void testDecodeSourceFromCacheReturnsNullIfCacheStrategyDoesNotCacheSource() throws Exception {
        for (DiskCacheStrategy strategy : list(DiskCacheStrategy.NONE, DiskCacheStrategy.RESULT)) {
            harness = new Harness(strategy);

            mockCacheToReturnSourceResource();

            assertNull(harness.getJob().decodeSourceFromCache());
        }
    }

    @Test
    public void testDiskCacheIsCheckedForSourceWhenCacheStrategyIncludesResult() throws Exception {
        for (DiskCacheStrategy strategy : list(DiskCacheStrategy.ALL, DiskCacheStrategy.SOURCE)) {
            harness = new Harness(strategy);

            mockCacheToReturnSourceResource();

            assertEquals(harness.resource, harness.getJob().decodeSourceFromCache());
        }
    }

    @Test
    public void testResourceFetcherIsNotCalledIfSourceIsCached() throws Exception {
        for (DiskCacheStrategy strategy : list(DiskCacheStrategy.ALL, DiskCacheStrategy.SOURCE)) {
            harness = new Harness(strategy);

            mockCacheToReturnSourceResource();
            harness.getJob().decodeSourceFromCache();

            verify(harness.dataFetcher, never()).loadData(any(Priority.class));
        }
    }

    @Test
    public void testOriginalResourceIsTransformedTranscodedAndReturnedIfSourceIsCached() throws Exception {
        for (DiskCacheStrategy strategy : list(DiskCacheStrategy.ALL, DiskCacheStrategy.SOURCE)) {
            harness = new Harness(strategy);

            mockCacheToReturnSourceResource();
            Resource<Object> transformed = mock(Resource.class);
            when(harness.transformation.transform(eq(harness.resource), eq(harness.width), eq(harness.height)))
                    .thenReturn(transformed);
            Resource<Object> transcoded = mock(Resource.class);
            when(harness.transcoder.transcode(eq(transformed))).thenReturn(transcoded);

            assertEquals(transcoded, harness.getJob().decodeSourceFromCache());
        }
    }

    @Test
    public void testOriginalResourceIsRecycledIfDifferentThanTransformedResource() throws Exception {
        for (DiskCacheStrategy strategy : list(DiskCacheStrategy.ALL, DiskCacheStrategy.SOURCE)) {
            harness = new Harness(strategy);

            mockCacheToReturnSourceResource();
            Resource<Object> transformed = mock(Resource.class);
            when(harness.transformation.transform(eq(harness.resource), eq(harness.width), eq(harness.height)))
                    .thenReturn(transformed);

            harness.getJob().decodeSourceFromCache();
            verify(harness.resource).recycle();
        }
    }

    @Test
    public void testOriginalResourceIsNotRecycledIfSameAsTransformedResource() throws Exception {
        for (DiskCacheStrategy strategy : list(DiskCacheStrategy.ALL, DiskCacheStrategy.SOURCE)) {
            harness = new Harness(strategy);

            mockCacheToReturnSourceResource();
            when(harness.transformation.transform(eq(harness.resource), eq(harness.width), eq(harness.height)))
                    .thenReturn(harness.resource);

            harness.getJob().decodeSourceFromCache();

            verify(harness.resource, never()).recycle();
        }
    }

    @Test
    public void testTransformedResourceIsWrittenToCacheIfSourceIsCachedAndStrategyCachesResult() throws Exception {
        harness.diskCacheStrategy = DiskCacheStrategy.ALL;

        mockCacheToReturnSourceResource();
        Resource<Object> transformed = mock(Resource.class);
        when(harness.transformation.transform(eq(harness.resource), anyInt(), anyInt())).thenReturn(transformed);
        doAnswer(new CallWriter()).when(harness.diskCache).put(eq(harness.key), any(DiskCache.Writer.class));

        harness.getJob().decodeSourceFromCache();

        verify(harness.resultEncoder).encode(eq(transformed), any(OutputStream.class));
    }

    @Test
    public void testTransformedResourceIsNotWrittenToCacheIfSourceIsCachedAndStrategyDoesNotCacheResult()
            throws Exception {
        harness.diskCacheStrategy = DiskCacheStrategy.SOURCE;

        mockCacheToReturnSourceResource();
        harness.getJob().decodeSourceFromCache();

        verify(harness.diskCache, never()).put(eq(harness.key), any(DiskCache.Writer.class));
    }

    @Test
    public void testSourceIsDeletedFromCacheIfCacheDecodeFails() throws Exception {
        harness.diskCacheStrategy = DiskCacheStrategy.ALL;
        when(harness.diskCache.get(eq(harness.originalKey))).thenReturn(new File("fake"));
        when(harness.cacheDecoder.decode(any(File.class), anyInt(), anyInt())).thenReturn(null);

        assertNull(harness.getJob().decodeSourceFromCache());

        verify(harness.diskCache).delete(eq(harness.originalKey));
    }

    @Test
    public void testSourceIsDeletedFromCacheIfCacheDecoderThrows() throws IOException {
        harness.diskCacheStrategy = DiskCacheStrategy.ALL;
        when(harness.diskCache.get(eq(harness.originalKey))).thenReturn(new File("test"));
        when(harness.cacheDecoder.decode(any(File.class), anyInt(), anyInt())).thenThrow(new RuntimeException("test"));

        try {
            harness.getJob().decodeSourceFromCache();
            fail("Failed to get expected exception");
        } catch (Exception e) {
            // Expected.
        }
        verify(harness.diskCache).delete(eq(harness.originalKey));
    }

    @Test
    public void testReturnsNullIfSourceCacheDecodeOfCachedSourceReturnsNull() throws Exception {
        harness.diskCacheStrategy = DiskCacheStrategy.ALL;
        when(harness.diskCache.get(eq(harness.originalKey))).thenReturn(new File("test"));
        when(harness.cacheDecoder.decode(any(File.class), anyInt(), anyInt())).thenReturn(null);

        assertNull(harness.getJob().decodeSourceFromCache());
    }

    @Test(expected = RuntimeException.class)
    public void testThrowsIfCacheDecodeOfCachedSourceThrows() throws Exception {
        harness.diskCacheStrategy = DiskCacheStrategy.ALL;
        when(harness.diskCache.get(eq(harness.originalKey))).thenReturn(new File("test"));
        when(harness.cacheDecoder.decode(any(File.class), anyInt(), anyInt())).thenThrow(new RuntimeException("test"));

        harness.getJob().decodeSourceFromCache();
    }

    @Test
    public void testReturnsNullIfTransformationOfResourceDecodedFromCachedSourceReturnsNull() throws Exception {
        harness.diskCacheStrategy = DiskCacheStrategy.ALL;
        mockCacheToReturnSourceResource();
        when(harness.transformation.transform(any(Resource.class), anyInt(), anyInt())).thenReturn(null);

        assertNull(harness.getJob().decodeSourceFromCache());
    }

    @Test(expected = RuntimeException.class)
    public void testThrowsIfTransformationOfResourceDecodedFromCachedSourceThrows() throws Exception {
        harness.diskCacheStrategy = DiskCacheStrategy.ALL;
        mockCacheToReturnSourceResource();
        when(harness.transformation.transform(any(Resource.class), anyInt(), anyInt()))
                .thenThrow(new RuntimeException("test"));

        harness.getJob().decodeSourceFromCache();
    }

    @Test
    public void testReturnsNullIfTranscoderOfResourceDecodedFromCachedSourceReturnsNull() throws Exception {
        harness.diskCacheStrategy = DiskCacheStrategy.ALL;
        mockCacheToReturnSourceResource();
        when(harness.transcoder.transcode(any(Resource.class))).thenReturn(null);

        assertNull(harness.getJob().decodeSourceFromCache());
    }

    @Test(expected = RuntimeException.class)
    public void testThrowsIfTranscoderOfResourceDecodedFromCachedSourceThrows() throws Exception {
        harness.diskCacheStrategy = DiskCacheStrategy.ALL;
        mockCacheToReturnSourceResource();
        when(harness.transcoder.transcode(any(Resource.class))).thenThrow(new RuntimeException("test"));

        harness.getJob().decodeSourceFromCache();
    }

    /** decodeFromSource **/

    private void mockSourceToReturnResource() throws Exception {
        // For NONE/RESULT
        Object data = new Object();
        when(harness.dataFetcher.loadData(eq(harness.priority))).thenReturn(data);
        when(harness.sourceDecoder.decode(eq(data), eq(harness.width), eq(harness.height)))
                .thenReturn(harness.resource);

        // For ALL/SOURCE
        File cachedSource = new File("source");
        when(harness.diskCache.get(eq(harness.originalKey))).thenReturn(cachedSource);
        when(harness.cacheDecoder.decode(eq(cachedSource), eq(harness.width), eq(harness.height)))
                .thenReturn(harness.resource);
    }

    @Test
    public void testDecodeFromSourceDecodesTransformsAndTranscodesDataFromDataFetcher() throws Exception {
        for (DiskCacheStrategy strategy : list(DiskCacheStrategy.values())) {
            harness = new Harness(strategy);
            mockSourceToReturnResource();

            Resource<Object> transformed = mock(Resource.class);
            when(harness.transformation.transform(eq(harness.resource), eq(harness.width), eq(harness.height)))
                    .thenReturn(transformed);
            Resource<Object> transcoded = mock(Resource.class);
            when(harness.transcoder.transcode(eq(transformed))).thenReturn(transcoded);

            assertEquals(transcoded, harness.getJob().decodeFromSource());
        }
    }

    @Test
    public void testSourceDataIsWrittenToCacheIfCacheStrategyCachesSource() throws Exception {
        for (DiskCacheStrategy strategy : list(DiskCacheStrategy.SOURCE, DiskCacheStrategy.ALL)) {
            harness = new Harness(strategy);
            Object data = new Object();
            when(harness.dataFetcher.loadData(eq(harness.priority))).thenReturn(data);

            doAnswer(new CallWriter()).when(harness.diskCache)
                    .put(eq(harness.originalKey), any(DiskCache.Writer.class));

            harness.getJob().decodeFromSource();

            verify(harness.sourceEncoder).encode(eq(data), any(OutputStream.class));
        }
    }

    @Test
    public void testSourceDataIsNotWrittenToCacheIfCacheStrategyDoesNotCacheSource() throws Exception {
        for (DiskCacheStrategy strategy : list(DiskCacheStrategy.NONE, DiskCacheStrategy.RESULT)) {
            harness = new Harness(strategy);
            mockSourceToReturnResource();

            harness.getJob().decodeFromSource();

            verify(harness.diskCache, never()).put(eq(harness.originalKey), any(DiskCache.Writer.class));
        }
    }

    @Test
    public void testTransformedResourceDecodedFromSourceIsWrittenToCacheIfCacheStrategyCachesResult() throws Exception {
        for (DiskCacheStrategy strategy : list(DiskCacheStrategy.ALL, DiskCacheStrategy.RESULT)) {
            harness = new Harness(strategy);

            mockSourceToReturnResource();
            Resource<Object> transformed = mock(Resource.class);
            when(harness.transformation.transform(eq(harness.resource), eq(harness.width), eq(harness.height)))
                    .thenReturn(transformed);
            doAnswer(new CallWriter()).when(harness.diskCache).put(eq(harness.key), any(DiskCache.Writer.class));

            harness.getJob().decodeFromSource();

            verify(harness.resultEncoder).encode(eq(transformed), any(OutputStream.class));
        }
    }

    @Test
    public void testTransformedResultResourceIsNotWrittenToCacheIfCacheStrategyDoesNotCacheResult() throws Exception {
        for (DiskCacheStrategy strategy : list(DiskCacheStrategy.NONE, DiskCacheStrategy.SOURCE)) {
            harness = new Harness(strategy);
            mockSourceToReturnResource();
            Resource<Object> transformed = mock(Resource.class);
            when(harness.transformation.transform(eq(harness.resource), eq(harness.width), eq(harness.height)))
                    .thenReturn(transformed);

            harness.getJob().decodeFromSource();

            verify(harness.diskCache, never()).put(eq(harness.key), any(DiskCache.Writer.class));
        }
    }

    @Test
    public void testResourceDecodedFromSourceDataIsRecycledIfDifferentThanTransformed() throws Exception {
        for (DiskCacheStrategy strategy : DiskCacheStrategy.values()) {
            harness = new Harness(strategy);
            mockSourceToReturnResource();
            Resource<Object> transformed = mock(Resource.class);
            when(harness.transformation.transform(eq(harness.resource), eq(harness.width), eq(harness.height)))
                    .thenReturn(transformed);

            harness.getJob().decodeFromSource();

            verify(harness.resource).recycle();
        }
    }

    @Test
    public void testResourceDecodedFromSourceDataIsNotRecycledIfSameAsTransformed() throws Exception {
         for (DiskCacheStrategy strategy : DiskCacheStrategy.values()) {
            harness = new Harness(strategy);
            mockSourceToReturnResource();
            when(harness.transformation.transform(eq(harness.resource), eq(harness.width), eq(harness.height)))
                    .thenReturn(harness.resource);

            harness.getJob().decodeFromSource();

            verify(harness.resource, never()).recycle();
        }
    }

    @Test
    public void testFetcherIsCleanedUp() throws Exception {
        harness.getJob().decodeFromSource();

        verify(harness.dataFetcher).cleanup();
    }

    @Test
    public void testFetcherIsCleanedUpIfDecodeThrows() throws Exception {
        when(harness.dataFetcher.loadData(any(Priority.class))).thenReturn(new Object());
        when(harness.sourceDecoder.decode(anyObject(), anyInt(), anyInt())).thenThrow(new IOException("test"));

        try {
            harness.getJob().decodeFromSource();
            fail("Failed to get expected exception");
        } catch (IOException e) {
            // Expected.`
        }

        verify(harness.dataFetcher).cleanup();
    }

    @Test
    public void testFetcherIsCleanedUpIfFetcherThrows() throws Exception {
        when(harness.dataFetcher.loadData(any(Priority.class))).thenThrow(new IOException("test"));

        try {
            harness.getJob().decodeFromSource();
            fail("Failed to get expected exception");
        } catch (IOException e) {
            // Expected.
        }

        verify(harness.dataFetcher).cleanup();
    }

    @Test
    public void testReturnsNullFromDecodeSourceIfDecoderReturnsNull() throws Exception {
        for (DiskCacheStrategy strategy : DiskCacheStrategy.values()) {
            harness = new Harness(strategy);
            mockSourceToReturnResource();
            when(harness.sourceDecoder.decode(any(Object.class), anyInt(), anyInt())).thenReturn(null);
            when(harness.cacheDecoder.decode(any(File.class), anyInt(), anyInt())).thenReturn(null);

            assertNull(harness.getJob().decodeFromSource());
        }
    }

    @Test
    public void testReturnsNullFromDecodeSourceIfTransformationReturnsNull() throws Exception {
        for (DiskCacheStrategy strategy : DiskCacheStrategy.values()) {
            harness = new Harness(strategy);
            mockSourceToReturnResource();
            when(harness.transformation.transform(any(Resource.class), anyInt(), anyInt())).thenReturn(null);

            assertNull(harness.getJob().decodeFromSource());
        }
    }

    @Test
    public void testReturnsNullFromDecodeSourceIfTranscoderReturnsNull() throws Exception {
        for (DiskCacheStrategy strategy : DiskCacheStrategy.values()) {
            harness = new Harness(strategy);
            mockSourceToReturnResource();
            when(harness.transcoder.transcode(any(Resource.class))).thenReturn(null);

            assertNull(harness.getJob().decodeFromSource());
        }
    }

    private static <T> List<T> list(T... items) {
        return Arrays.asList(items);
    }

    @SuppressWarnings("unchecked")
    private static class Harness {
        EngineKey key = mock(EngineKey.class);
        Key originalKey = mock(Key.class);
        int width = 100;
        int height = 200;
        DataFetcher<Object> dataFetcher = mock(DataFetcher.class);
        DataLoadProvider<Object, Object> loadProvider = mock(DataLoadProvider.class);
        Transformation<Object> transformation = mock(Transformation.class);
        ResourceTranscoder<Object, Object> transcoder = mock(ResourceTranscoder.class);
        DiskCache diskCache = mock(DiskCache.class);
        DecodeJob.DiskCacheProvider diskCacheProvider = mock(DecodeJob.DiskCacheProvider.class);
        Priority priority = Priority.IMMEDIATE;

        ResourceDecoder<File, Object> cacheDecoder = mock(ResourceDecoder.class);
        Resource<Object> resource = mock(Resource.class);
        ResourceEncoder<Object> resultEncoder = mock(ResourceEncoder.class);
        ResourceDecoder<Object, Object> sourceDecoder = mock(ResourceDecoder.class);
        Encoder<Object> sourceEncoder = mock(Encoder.class);
        DecodeJob.FileOpener fileOpener = mock(DecodeJob.FileOpener.class);

        DiskCacheStrategy diskCacheStrategy;

        public Harness() throws FileNotFoundException {
            this(DiskCacheStrategy.RESULT);
        }

        public Harness(DiskCacheStrategy diskCacheStrategy) throws FileNotFoundException {
            this.diskCacheStrategy = diskCacheStrategy;
            when(fileOpener.open(any(File.class))).thenReturn(mock(OutputStream.class));
            when(key.getOriginalKey()).thenReturn(originalKey);
            when(transcoder.transcode(eq(resource))).thenReturn(resource);
            when(transformation.transform(eq(resource), eq(width), eq(height))).thenReturn(resource);
            when(loadProvider.getCacheDecoder()).thenReturn(cacheDecoder);
            when(loadProvider.getEncoder()).thenReturn(resultEncoder);
            when(loadProvider.getSourceDecoder()).thenReturn(sourceDecoder);
            when(loadProvider.getSourceEncoder()).thenReturn(sourceEncoder);
            when(diskCacheProvider.getDiskCache()).thenReturn(diskCache);
        }

        public DecodeJob<Object, Object, Object> getJob() {
            return new DecodeJob<Object, Object, Object>(key, width, height, dataFetcher, loadProvider, transformation,
                    transcoder, diskCacheProvider, diskCacheStrategy, priority, fileOpener);
        }
    }

    private static class CallWriter implements Answer<Void> {

        @Override
        public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
            DiskCache.Writer writer = (DiskCache.Writer) invocationOnMock.getArguments()[1];
            writer.write(new File("test"));
            return null;
        }
    }
}