package com.bumptech.glide.load.data.mediastore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.MediaStore;
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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.fakes.RoboCursor;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 18)
public class ThumbnailStreamOpenerTest {
  private Harness harness;

  @Before
  public void setUp() {
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
    cursor.addRow(new Object[] { "" });
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
    Shadows.shadowOf(RuntimeEnvironment.application.getContentResolver())
        .registerInputStream(harness.uri, expected);
    assertEquals(expected, harness.get().open(harness.uri));
  }

  @Test
  public void testVideoQueryReturnsVideoCursor() {
    Uri queryUri = MediaStore.Video.Thumbnails.EXTERNAL_CONTENT_URI;
    ThumbFetcher.VideoThumbnailQuery query =
        new ThumbFetcher.VideoThumbnailQuery(getContentResolver());
    RoboCursor testCursor = new RoboCursor();
    Shadows.shadowOf(RuntimeEnvironment.application.getContentResolver())
        .setCursor(queryUri, testCursor);
    assertEquals(testCursor, query.query(harness.uri));
  }

  @Test
  public void testImageQueryReturnsImageCursor() {
    Uri queryUri = MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI;
    ThumbFetcher.ImageThumbnailQuery query =
        new ThumbFetcher.ImageThumbnailQuery(getContentResolver());
    RoboCursor testCursor = new RoboCursor();
    Shadows.shadowOf(RuntimeEnvironment.application.getContentResolver())
        .setCursor(queryUri, testCursor);
    assertEquals(testCursor, query.query(harness.uri));
  }

  private static ContentResolver getContentResolver() {
    return RuntimeEnvironment.application.getContentResolver();
  }

  private static class Harness {
    MatrixCursor cursor = new MatrixCursor(new String[1]);
    File file = new File("fake/uri");
    Uri uri = Uri.fromFile(file);
    ThumbnailQuery query = mock(ThumbnailQuery.class);
    FileService service = mock(FileService.class);
    ArrayPool byteArrayPool = new LruArrayPool();

    public Harness() {
      cursor.addRow(new String[] { file.getAbsolutePath() });
      when(query.query(eq(uri))).thenReturn(cursor);
      when(service.get(eq(file.getAbsolutePath()))).thenReturn(file);
      when(service.exists(eq(file))).thenReturn(true);
      when(service.length(eq(file))).thenReturn(1L);
    }

    public ThumbnailStreamOpener get() {
      List<ImageHeaderParser> parsers = new ArrayList<ImageHeaderParser>();
      parsers.add(new DefaultImageHeaderParser());
      return new ThumbnailStreamOpener(
          parsers, service, query, byteArrayPool, getContentResolver());
    }
  }
}
