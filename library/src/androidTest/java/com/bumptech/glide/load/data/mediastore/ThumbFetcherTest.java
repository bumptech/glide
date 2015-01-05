package com.bumptech.glide.load.data.mediastore;

import android.net.Uri;
import android.provider.MediaStore;
import com.bumptech.glide.Priority;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, emulateSdk = 18)
public class ThumbFetcherTest {

    @Mock ThumbnailStreamOpener opener;
    private ThumbFetcher fetcher;
    private Uri uri;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        uri = Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "123");
        fetcher = new ThumbFetcher(Robolectric.application, uri, opener);
    }

    @Test
    public void testReturnsInputStreamFromThumbnailOpener() throws Exception {
        InputStream expected = new ByteArrayInputStream(new byte[0]);

        when(opener.open(eq(Robolectric.application), eq(uri))).thenReturn(expected);

        InputStream result = fetcher.loadData(Priority.LOW);
        assertEquals(expected, result);
    }

    @Test
    public void testClosesInputStreamFromThumbnailOpenerOnCleanup() throws Exception {
        InputStream expected = mock(InputStream.class);

        when(opener.open(eq(Robolectric.application), eq(uri))).thenReturn(expected);

        fetcher.loadData(Priority.HIGH);

        fetcher.cleanup();
        verify(expected).close();
    }

    @Test
    public void testDoesNotThrowIfCleanupWithNullInputStream() {
        fetcher.cleanup();
    }

    @Test
    public void testContainsAllRelevantPartsInId() {
        String id = fetcher.getId();
        assertThat(id).contains(uri.toString());
    }
}