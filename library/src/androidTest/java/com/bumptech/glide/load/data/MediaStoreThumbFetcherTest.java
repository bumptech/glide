package com.bumptech.glide.load.data;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.net.Uri;
import android.provider.MediaStore;

import com.bumptech.glide.Priority;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, emulateSdk = 18)
public class MediaStoreThumbFetcherTest {
    private Harness harness;

    @Before
    public void setUp() {
        harness = new Harness();
    }

    @Test
    public void testReturnsInputStreamFromThumbnailOpener() throws Exception {
        InputStream expected = new ByteArrayInputStream(new byte[0]);

        when(harness.thumbnailFetcher.open(eq(Robolectric.application), eq(harness.uri))).thenReturn(expected);

        InputStream result = harness.get().loadData(Priority.LOW);
        assertEquals(expected, result);
    }

    @Test
    public void testReturnsInputStreamFromDefaultFetcherIfStreamFromThumbnailOpenerIsNull() throws Exception {
        InputStream expected = new ByteArrayInputStream(new byte[0]);

        when(harness.thumbnailFetcher.open(eq(Robolectric.application), eq(harness.uri))).thenReturn(null);
        when(harness.defaultFetcher.loadData(any(Priority.class))).thenReturn(expected);

        assertEquals(expected, harness.get().loadData(Priority.HIGH));
    }

    @Test
    public void testReturnsInputStreamFromDefaultFetcherIfFactoryReturnsNull() throws Exception {
        InputStream expected = new ByteArrayInputStream(new byte[0]);

        when(harness.factory.build(any(Uri.class), anyInt(), anyInt())).thenReturn(null);
        when(harness.defaultFetcher.loadData(any(Priority.class))).thenReturn(expected);

        assertEquals(expected, harness.get().loadData(Priority.IMMEDIATE));
    }

    @Test
    public void testClosesInputStreamFromThumbnailOpenerOnCleanup() throws Exception {
        InputStream expected = mock(InputStream.class);

        when(harness.thumbnailFetcher.open(eq(Robolectric.application), eq(harness.uri))).thenReturn(expected);

        MediaStoreThumbFetcher fetcher = harness.get();
        fetcher.loadData(Priority.HIGH);

        fetcher.cleanup();
        verify(expected).close();
    }

    @Test
    public void testCallsCleanupOnDefaultFetcherOnCleanup() {
        harness.get().cleanup();
        verify(harness.defaultFetcher).cleanup();
    }

    @Test
    public void testDoesNotThrowIfCleanupWithNullInputStream() {
        harness.get().cleanup();
    }

    @Test
    public void testContainsAllRelevantPartsInId() {
        String id = harness.get().getId();
        assertThat(id).contains(harness.uri.toString());
    }

    @SuppressWarnings("unchecked")
    private static class Harness {
        Uri uri = Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "123");
        DataFetcher<InputStream> defaultFetcher = mock(DataFetcher.class);
        int width = 123;
        int height = 222;

        MediaStoreThumbFetcher.ThumbnailStreamOpenerFactory factory = mock(
                MediaStoreThumbFetcher.ThumbnailStreamOpenerFactory.class);
        MediaStoreThumbFetcher.ThumbnailStreamOpener
                thumbnailFetcher = mock(MediaStoreThumbFetcher.ThumbnailStreamOpener.class);

        public Harness() {
            when(factory.build(eq(uri), eq(width), eq(height))).thenReturn(thumbnailFetcher);
        }

        public MediaStoreThumbFetcher get() {
            return new MediaStoreThumbFetcher(Robolectric.application, uri, defaultFetcher, width, height, factory);
        }
    }
}