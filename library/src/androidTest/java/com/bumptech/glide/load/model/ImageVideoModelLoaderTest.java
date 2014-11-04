package com.bumptech.glide.load.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.net.Uri;
import android.os.ParcelFileDescriptor;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.data.DataFetcher;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, emulateSdk = 18)
public class ImageVideoModelLoaderTest {
    private ImageVideoLoaderHarness harness;

    @Before
    public void setUp() {
        harness =  new ImageVideoLoaderHarness();
    }

    @Test(expected = NullPointerException.class)
    public void testThrowsIfStreamLoaderAndFileDescriptorLoaderAreNull() {
        harness.streamModelLoader = null;
        harness.fileDescriptorModelLoader = null;

        harness.getLoader();
    }

    @Test
    public void testIdIsStreamOrFetcherId() {
        Object model = new Object();
        String expected = "stream";
        when(harness.streamFetcher.getId()).thenReturn(expected);
        when(harness.fileDescriptorFetcher.getId()).thenReturn(expected);

        String id = harness.getLoader().getResourceFetcher(model, 1, 2).getId();

        assertEquals(expected, id);
    }

    @Test
    public void testReturnsFileDescriptorIdIfStreamFetcherNull() {
        Object model = new Object();
        String expected = "fakeId";
        when(harness.fileDescriptorFetcher.getId()).thenReturn(expected);
        harness.streamModelLoader = null;

        String id = harness.getLoader().getResourceFetcher(model, 1, 2).getId();

        assertEquals(expected, id);
    }

    @Test
    public void testReturnsStreamFetcherIdIfFileDescriptorFetcherNull() {
        Object model = new Object();
        String expected = "fakeId";
        when(harness.streamFetcher.getId()).thenReturn(expected);
        harness.fileDescriptorModelLoader = null;

        String id = harness.getLoader().getResourceFetcher(model, 1, 2).getId();

        assertEquals(expected, id);
    }

    @Test
    public void testReturnsImageVideoWrapperWithFetchers() throws Exception {
        InputStream stream = new ByteArrayInputStream(new byte[0]);
        ParcelFileDescriptor fileDescriptor = mock(ParcelFileDescriptor.class);

        when(harness.streamFetcher.loadData(any(Priority.class))).thenReturn(stream);
        when(harness.streamModelLoader
                .getResourceFetcher(any(Uri.class), anyInt(), anyInt()))
                .thenReturn(harness.streamFetcher);
        when(harness.fileDescriptorFetcher.loadData(any(Priority.class))).thenReturn(fileDescriptor);
        when(harness.fileDescriptorModelLoader
                .getResourceFetcher(any(Uri.class), anyInt(), anyInt()))
                .thenReturn(harness.fileDescriptorFetcher);

        ImageVideoWrapper wrapper = harness.getLoader()
                .getResourceFetcher(new Object(), 100, 100)
                .loadData(Priority.LOW);

        assertEquals(stream, wrapper.getStream());
        assertEquals(fileDescriptor, wrapper.getFileDescriptor());
    }

    @Test
    public void testHandlesNullStreamModelLoaderInGetResourceFetcher() {
        harness.streamModelLoader = null;

        assertNotNull(harness.getLoader().getResourceFetcher(new Object(), 100, 100));
    }

    @Test
    public void testHandlesNullFileDescriptorModelLoaderInGetResourceFetcher() {
        harness.fileDescriptorFetcher = null;

        assertNotNull(harness.getLoader().getResourceFetcher(new Object(), 100, 100));
    }

    @Test
    public void testFetcherHandlesNullStreamFetcherInCleanup() {
        harness.streamFetcher = null;

        harness.getFetcher().cleanup();
    }

    @Test
    public void testFetcherHandlesNullStreamFetcherInCancel() {
        harness.streamFetcher = null;

        harness.getFetcher().cancel();
    }

    @Test
    public void testFetcherHandlesNullStreamFetcherInLoadResource() throws Exception {
        harness.streamFetcher = null;

        assertNotNull(harness.getFetcher().loadData(Priority.NORMAL));
    }

    @Test
    public void testFetcherHandlesNullFileDescriptorFetcherInCleanup() {
        harness.fileDescriptorFetcher = null;

        harness.getFetcher().cleanup();
    }

    @Test
    public void testFetcherHandlesNullFileDescriptorInCancel() {
        harness.fileDescriptorFetcher = null;

        harness.getFetcher().cancel();
    }

    @Test
    public void testFetcherHandlesNullFileDesciprtorInLoadResource() throws Exception {
        harness.fileDescriptorFetcher = null;

        assertNotNull(harness.getFetcher().loadData(Priority.NORMAL));
    }

    @Test
    public void testFetcherHandlesExceptionInStreamFetcher() throws Exception {
        when(harness.streamFetcher.loadData(any(Priority.class)))
                .thenThrow(new IOException("test"));

        assertNotNull(harness.getFetcher().loadData(Priority.NORMAL));
    }

    @Test
    public void testFetcherHandlesExceptionInFileDescriptorFetcher() throws Exception {
        when(harness.streamFetcher.loadData(any(Priority.class)))
                .thenReturn(new ByteArrayInputStream(new byte[0]));
        when(harness.fileDescriptorFetcher
                .loadData(any(Priority.class)))
                .thenThrow(new IOException("test"));

        assertNotNull(harness.getFetcher().loadData(Priority.LOW));
    }

    @Test
    public void testReturnsNullFetcherIfBothStreamAndFileDescriptorLoadersReturnNullFetchers() throws Exception {
        when(harness.streamModelLoader.getResourceFetcher(anyObject(), anyInt(), anyInt())).thenReturn(null);
        when(harness.fileDescriptorModelLoader.getResourceFetcher(anyObject(), anyInt(), anyInt())).thenReturn(null);

        assertNull(harness.getLoader().getResourceFetcher(new Object(), 100, 100));
    }

    @Test
    public void testReturnsNullFetcherIfStreamModelLoaderIsNullAndFileModelLoaderReturnsNullFetcher() throws Exception {
        harness.streamModelLoader = null;
        when(harness.fileDescriptorModelLoader.getResourceFetcher(anyObject(), anyInt(), anyInt())).thenReturn(null);

        assertNull(harness.getLoader().getResourceFetcher(new Object(), 100, 100));
    }

    @Test
    public void testReturnsNullFetcherIfFileDescriptorModelLoaderIsNullAndStreamModelLoaderReturnsNullFetcher()
            throws Exception {
        harness.fileDescriptorModelLoader = null;
        when(harness.streamModelLoader.getResourceFetcher(anyObject(), anyInt(), anyInt())).thenReturn(null);

        assertNull(harness.getLoader().getResourceFetcher(new Object(), 100, 100));
    }

    @Test(expected = IOException.class)
    public void testFetcherThrowsIfBothFileDescriptorAndStreamFetchersThrows() throws Exception {
        when(harness.fileDescriptorFetcher.loadData(any(Priority.class)))
                .thenThrow(new IOException("test"));
        when(harness.streamFetcher.loadData(any(Priority.class)))
                .thenThrow(new IOException("test"));

        harness.getFetcher().loadData(Priority.LOW);
    }

    @Test(expected = IOException.class)
    public void testFetcherThrowsIfStreamFetcherIsNullAndFileDescriptorThrows() throws Exception {
        harness.streamFetcher = null;
        when(harness.fileDescriptorFetcher.loadData(any(Priority.class)))
                .thenThrow(new IOException("test"));

        harness.getFetcher().loadData(Priority.LOW);
    }

    @Test(expected = IOException.class)
    public void testFetcherThrowsIfFileDescriptorFetcherIsNullAndStreamLoaderThrows() throws Exception {
        harness.fileDescriptorFetcher = null;
        when(harness.streamFetcher.loadData(any(Priority.class)))
                .thenThrow(new IOException("test"));

        harness.getFetcher().loadData(Priority.LOW);
    }

    @Test
    public void testReturnsDifferentIdsForDifferentObjects() {
        Object first = new Object();
        String firstStreamId = "firstStream";
        when(harness.streamFetcher.getId()).thenReturn(firstStreamId);

        String firstId = harness.getLoader().getResourceFetcher(first, 1, 2).getId();
        assertEquals(firstStreamId, firstId);

        Object second = new Object();
        String secondFileDescriptorId = "secondStream";
        when(harness.streamFetcher.getId()).thenReturn(secondFileDescriptorId);

        String secondId = harness.getLoader().getResourceFetcher(second, 1, 2).getId();
        assertEquals(secondFileDescriptorId, secondId);
    }

    @SuppressWarnings("unchecked")
    private static class ImageVideoLoaderHarness {
        ModelLoader<Object, InputStream> streamModelLoader = mock(ModelLoader.class);
        ModelLoader<Object, ParcelFileDescriptor> fileDescriptorModelLoader = mock(ModelLoader.class);
        DataFetcher<InputStream> streamFetcher = mock(DataFetcher.class);
        DataFetcher<ParcelFileDescriptor> fileDescriptorFetcher = mock(DataFetcher.class);

        public ImageVideoLoaderHarness() {
            when(streamModelLoader.getResourceFetcher(anyObject(), anyInt(), anyInt())).thenReturn(streamFetcher);
            when(fileDescriptorModelLoader.getResourceFetcher(anyObject(), anyInt(), anyInt()))
                    .thenReturn(fileDescriptorFetcher);
        }

        private ImageVideoModelLoader<Object> getLoader() {
            return new ImageVideoModelLoader<Object>(streamModelLoader, fileDescriptorModelLoader);
        }

        private ImageVideoModelLoader.ImageVideoFetcher getFetcher() {
            return new ImageVideoModelLoader.ImageVideoFetcher(streamFetcher, fileDescriptorFetcher);
        }
    }
}
