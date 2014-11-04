package com.bumptech.glide.load.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.MediaStore;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.tester.android.database.SimpleTestCursor;
import org.robolectric.tester.android.database.TestCursor;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, emulateSdk = 18)
public class ThumbnailStreamOpenerTest {
    private Harness harness;

    @Before
    public void setUp() {
        harness = new Harness();
    }

    @Test
    public void testReturnsNullIfCursorIsNull() throws FileNotFoundException {
        when(harness.query.query(eq(Robolectric.application), eq(harness.uri))).thenReturn(null);
        assertNull(harness.get()
                .open(Robolectric.application, harness.uri));
    }

    @Test
    public void testReturnsNullIfCursorIsEmpty() throws FileNotFoundException {
        when(harness.query.query(eq(Robolectric.application), eq(harness.uri))).thenReturn(
                new MatrixCursor(new String[1]));
        assertNull(harness.get()
                .open(Robolectric.application, harness.uri));
    }

    @Test
    public void testReturnsNullIfCursorHasEmptyPath() throws FileNotFoundException {
        MatrixCursor cursor = new MatrixCursor(new String[1]);
        cursor.addRow(new Object[]{ "" });
        when(harness.query.query(eq(Robolectric.application), eq(harness.uri))).thenReturn(cursor);
        assertNull(harness.get()
                .open(Robolectric.application, harness.uri));
    }

    @Test
    public void testReturnsNullIfFileDoesNotExist() throws FileNotFoundException {
        when(harness.service.get(anyString())).thenReturn(harness.file);
        when(harness.service.exists(eq(harness.file))).thenReturn(false);
        assertNull(harness.get().open(Robolectric.application, harness.uri));
    }

    @Test
    public void testReturnNullIfFileLengthIsZero() throws FileNotFoundException {
        when(harness.service.get(anyString())).thenReturn(harness.file);
        when(harness.service.length(eq(harness.file))).thenReturn(0L);
        assertNull(harness.get().open(Robolectric.application, harness.uri));
    }

    @Test
    public void testClosesCursor() throws FileNotFoundException {
        harness.get().open(Robolectric.application, harness.uri);
        assertTrue(harness.cursor.isClosed());
    }

    @Test
    public void testReturnsOpenedInputStreamWhenFileFound() throws FileNotFoundException {
        InputStream expected = new ByteArrayInputStream(new byte[0]);
        Robolectric.shadowOf(Robolectric.application.getContentResolver()).registerInputStream(harness.uri, expected);
        assertEquals(expected, harness.get().open(Robolectric.application, harness.uri));
    }

    @Test
    public void testVideoQueryReturnsVideoCursor() {
        Uri queryUri = MediaStore.Video.Thumbnails.EXTERNAL_CONTENT_URI;
        MediaStoreThumbFetcher.VideoThumbnailQuery query = new MediaStoreThumbFetcher.VideoThumbnailQuery();
        TestCursor testCursor = new SimpleTestCursor();
        Robolectric.shadowOf(Robolectric.application.getContentResolver()).setCursor(queryUri, testCursor);
        assertEquals(testCursor, query.query(Robolectric.application, harness.uri));
    }

    @Test
    public void testImageQueryReturnsImageCurosr() {
        Uri queryUri = MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI;
        MediaStoreThumbFetcher.ImageThumbnailQuery query = new MediaStoreThumbFetcher.ImageThumbnailQuery();
        TestCursor testCursor = new SimpleTestCursor();
        Robolectric.shadowOf(Robolectric.application.getContentResolver()).setCursor(queryUri, testCursor);
        assertEquals(testCursor, query.query(Robolectric.application, harness.uri));
    }

    private static class Harness {
        MatrixCursor cursor = new MatrixCursor(new String[1]);
        File file = new File("fake/uri");
        Uri uri = Uri.fromFile(file);
        MediaStoreThumbFetcher.ThumbnailQuery query = mock(MediaStoreThumbFetcher.ThumbnailQuery.class);
        MediaStoreThumbFetcher.FileService service = mock(MediaStoreThumbFetcher.FileService.class);

        public Harness() {
            cursor.addRow(new String[] { file.getAbsolutePath() });
            when(query.query(eq(Robolectric.application), eq(uri))).thenReturn(cursor);
            when(service.get(eq(file.getAbsolutePath()))).thenReturn(file);
            when(service.exists(eq(file))).thenReturn(true);
            when(service.length(eq(file))).thenReturn(1L);
        }

        public MediaStoreThumbFetcher.ThumbnailStreamOpener get() {
            return new MediaStoreThumbFetcher.ThumbnailStreamOpener(service, query);
        }
    }
}