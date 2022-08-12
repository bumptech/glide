package com.bumptech.glide.load.data.mediastore;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.MediaStore;
import androidx.test.core.app.ApplicationProvider;
import com.bumptech.glide.load.ImageHeaderParser;
import com.bumptech.glide.load.engine.bitmap_recycle.ArrayPool;
import com.bumptech.glide.load.engine.bitmap_recycle.LruArrayPool;
import com.bumptech.glide.load.resource.bitmap.DefaultImageHeaderParser;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.fakes.RoboCursor;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 18)
public class ThumbnailStreamOpenerTest {
  private Harness harness;

  @Rule public final TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Before
  public void setUp() throws Exception {
    harness = new Harness();
  }

  @Test
  public void testReturnsNullIfCursorIsNull() throws FileNotFoundException {
    when(harness.query.query(eq(harness.uri))).thenReturn(null);
    assertNull(harness.get().open(harness.uri));
  }

  @Test
  public void testReturnsNullIfCursorIsEmpty() throws FileNotFoundException {
    when(harness.query.query(eq(harness.uri))).thenReturn(new MatrixCursor(new String[1]));
    assertNull(harness.get().open(harness.uri));
  }

  @Test
  public void testReturnsNullIfCursorHasEmptyPath() throws FileNotFoundException {
    MatrixCursor cursor = new MatrixCursor(new String[1]);
    cursor.addRow(new Object[] {""});
    when(harness.query.query(eq(harness.uri))).thenReturn(cursor);
    assertNull(harness.get().open(harness.uri));
  }

  @Test
  public void testReturnsNullIfFileDoesNotExist() throws FileNotFoundException {
    when(harness.service.get(anyString())).thenReturn(harness.file);
    when(harness.service.exists(eq(harness.file))).thenReturn(false);
    assertNull(harness.get().open(harness.uri));
  }

  @Test
  public void testReturnNullIfFileLengthIsZero() throws FileNotFoundException {
    when(harness.service.get(anyString())).thenReturn(harness.file);
    when(harness.service.length(eq(harness.file))).thenReturn(0L);
    assertNull(harness.get().open(harness.uri));
  }

  @Test
  public void testClosesCursor() throws FileNotFoundException {
    harness.get().open(harness.uri);
    assertTrue(harness.cursor.isClosed());
  }

  @Test
  public void testReturnsOpenedInputStreamWhenFileFound() throws FileNotFoundException {
    InputStream expected = new ByteArrayInputStream(new byte[0]);
    Shadows.shadowOf(ApplicationProvider.getApplicationContext().getContentResolver())
        .registerInputStream(harness.uri, expected);
    assertEquals(expected, harness.get().open(harness.uri));
  }

  @Test
  public void open_returnsNull_whenQueryThrowsSecurityException() throws FileNotFoundException {
    when(harness.query.query(any(Uri.class))).thenThrow(new SecurityException());
    assertThat(harness.get().open(harness.uri)).isNull();
  }

  @Test
  public void testVideoQueryReturnsVideoCursor() {
    Uri queryUri = MediaStore.Video.Thumbnails.EXTERNAL_CONTENT_URI;
    ThumbFetcher.VideoThumbnailQuery query =
        new ThumbFetcher.VideoThumbnailQuery(getContentResolver());
    RoboCursor testCursor = new RoboCursor();
    Shadows.shadowOf(ApplicationProvider.getApplicationContext().getContentResolver())
        .setCursor(queryUri, testCursor);
    assertEquals(testCursor, query.query(harness.uri));
  }

  @Test
  public void testImageQueryReturnsImageCursor() {
    Uri queryUri = MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI;
    ThumbFetcher.ImageThumbnailQuery query =
        new ThumbFetcher.ImageThumbnailQuery(getContentResolver());
    RoboCursor testCursor = new RoboCursor();
    Shadows.shadowOf(ApplicationProvider.getApplicationContext().getContentResolver())
        .setCursor(queryUri, testCursor);
    assertEquals(testCursor, query.query(harness.uri));
  }

  private static ContentResolver getContentResolver() {
    return ApplicationProvider.getApplicationContext().getContentResolver();
  }

  private class Harness {
    final MatrixCursor cursor = new MatrixCursor(new String[1]);
    final File file = temporaryFolder.newFile();
    final Uri uri = Uri.fromFile(file);
    final ThumbnailQuery query = mock(ThumbnailQuery.class);
    final FileService service = mock(FileService.class);
    final ArrayPool byteArrayPool = new LruArrayPool();

    Harness() throws Exception {
      cursor.addRow(new String[] {file.getAbsolutePath()});
      when(query.query(eq(uri))).thenReturn(cursor);
      when(service.get(eq(file.getAbsolutePath()))).thenReturn(file);
      when(service.exists(eq(file))).thenReturn(true);
      when(service.length(eq(file))).thenReturn(1L);
    }

    public ThumbnailStreamOpener get() {
      List<ImageHeaderParser> parsers = new ArrayList<>();
      parsers.add(new DefaultImageHeaderParser());
      return new ThumbnailStreamOpener(
          parsers, service, query, byteArrayPool, getContentResolver());
    }
  }
}
