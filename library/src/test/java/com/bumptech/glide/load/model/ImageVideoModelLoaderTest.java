package com.bumptech.glide.load.model;

import android.net.Uri;
import android.os.ParcelFileDescriptor;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.data.DataFetcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
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
    public void testIdIncludesStreamAndFileIds() {
        Object model = new Object();
        String streamId = "stream";
        String fileId = "file";
        when(harness.streamModelLoader.getId(eq(model))).thenReturn(streamId);
        when(harness.fileDescriptorModelLoader.getId(eq(model))).thenReturn(fileId);

        String id = harness.getLoader().getId(model);

        assertTrue(id, id.contains(streamId));
        assertTrue(id, id.contains(fileId));
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
    public void testHandlesNullStreamModelLoaderInGetId() {
        Object model = new Object();
        harness.streamModelLoader = null;
        when(harness.fileDescriptorModelLoader.getId(eq(model))).thenReturn(model.toString());

        String id = harness.getLoader().getId(model);

        assertEquals(model.toString(), id);
    }

    @Test
    public void testHandlesNullFileDescriptorModelLoaderInGetId() {
        Object model = new Object();
        harness.fileDescriptorModelLoader = null;
        when(harness.streamModelLoader.getId(eq(model))).thenReturn(model.toString());

        String id = harness.getLoader().getId(model);

        assertEquals(model.toString(), id);
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
        when(harness.streamModelLoader.getId(eq(first))).thenReturn(firstStreamId);
        String firstFileDescriptorId = "firstFileDescriptor";
        when(harness.fileDescriptorModelLoader.getId(eq(first))).thenReturn(firstFileDescriptorId);

        String firstId = harness.getLoader().getId(first);
        assertTrue(firstId.contains(firstStreamId));
        assertTrue(firstId.contains(firstFileDescriptorId));

        Object second = new Object();
        String secondStreamId = "secondStream";
        when(harness.streamModelLoader.getId(eq(second))).thenReturn(secondStreamId);
        String secondFileDescriptorId = "secondFileDescriptor";
        when(harness.fileDescriptorModelLoader.getId(eq(second))).thenReturn(secondFileDescriptorId);

        String secondId = harness.getLoader().getId(second);
        assertTrue(secondId.contains(secondStreamId));
        assertTrue(secondId.contains(secondFileDescriptorId));
    }

    @SuppressWarnings("unchecked")
    private static class ImageVideoLoaderHarness {
        ModelLoader<Object, InputStream> streamModelLoader = mock(ModelLoader.class);
        ModelLoader<Object, ParcelFileDescriptor> fileDescriptorModelLoader = mock(ModelLoader.class);
        DataFetcher<InputStream> streamFetcher = mock(DataFetcher.class);
        DataFetcher<ParcelFileDescriptor> fileDescriptorFetcher = mock(DataFetcher.class);

        private ImageVideoModelLoader<Object> getLoader() {
            return new ImageVideoModelLoader<Object>(streamModelLoader, fileDescriptorModelLoader);
        }

        private ImageVideoModelLoader.ImageVideoFetcher getFetcher() {
            return new ImageVideoModelLoader.ImageVideoFetcher(streamFetcher, fileDescriptorFetcher);
        }
    }
}
