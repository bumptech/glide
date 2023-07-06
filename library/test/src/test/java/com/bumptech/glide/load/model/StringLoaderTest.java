package com.bumptech.glide.load.model;

import static com.bumptech.glide.RobolectricConstants.ROBOLECTRIC_SDK;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import android.net.Uri;
import androidx.test.core.app.ApplicationProvider;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.tests.Util;
import com.bumptech.glide.util.Preconditions;
import java.io.File;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** Tests for the {@link com.bumptech.glide.load.model.StringLoader} class. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = ROBOLECTRIC_SDK)
public class StringLoaderTest {
  // Not a magic number, just an arbitrary non zero value.
  private static final int IMAGE_SIDE = 100;

  @Mock private ModelLoader<Uri, Object> uriLoader;
  @Mock private DataFetcher<Object> fetcher;
  @Mock private Key key;

  private StringLoader<Object> loader;
  private Options options;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    options = new Options();
    when(uriLoader.handles(any(Uri.class))).thenReturn(true);
    loader = new StringLoader<>(uriLoader);
  }

  @Test
  public void testHandlesPaths() {
    // TODO fix drive letter parsing somehow
    assumeTrue("it will fail with schema being the drive letter (C:\\... -> C)", !Util.isWindows());

    File f = ApplicationProvider.getApplicationContext().getCacheDir();
    Uri expected = Uri.fromFile(f);
    when(uriLoader.buildLoadData(eq(expected), eq(IMAGE_SIDE), eq(IMAGE_SIDE), eq(options)))
        .thenReturn(new ModelLoader.LoadData<>(key, fetcher));

    assertTrue(loader.handles(f.getAbsolutePath()));
    assertEquals(
        fetcher,
        Preconditions.checkNotNull(
                loader.buildLoadData(f.getAbsolutePath(), IMAGE_SIDE, IMAGE_SIDE, options))
            .fetcher);
  }

  @Test
  public void testCanHandleComplexFilePaths() {
    String testPath =
        "/storage/emulated/0/DCIM/Camera/IMG_20140520_100001:nopm:.jpg,mimeType=image/jpeg,"
            + "2448x3264,orientation=0,date=Tue";
    Uri expected = Uri.fromFile(new File(testPath));
    when(uriLoader.buildLoadData(eq(expected), eq(IMAGE_SIDE), eq(IMAGE_SIDE), eq(options)))
        .thenReturn(new ModelLoader.LoadData<>(key, fetcher));

    assertTrue(loader.handles(testPath));
    assertEquals(
        fetcher,
        Preconditions.checkNotNull(loader.buildLoadData(testPath, IMAGE_SIDE, IMAGE_SIDE, options))
            .fetcher);
  }

  @Test
  public void testHandlesFileUris() {
    File f = ApplicationProvider.getApplicationContext().getCacheDir();

    Uri expected = Uri.fromFile(f);
    when(uriLoader.buildLoadData(eq(expected), eq(IMAGE_SIDE), eq(IMAGE_SIDE), eq(options)))
        .thenReturn(new ModelLoader.LoadData<>(key, fetcher));

    assertTrue(loader.handles(f.getAbsolutePath()));
    assertEquals(
        fetcher,
        Preconditions.checkNotNull(
                loader.buildLoadData(expected.toString(), IMAGE_SIDE, IMAGE_SIDE, options))
            .fetcher);
  }

  @Test
  public void testHandlesResourceUris() {
    Uri resourceUri = Uri.parse("android.resource://com.bumptech.glide.tests/raw/ic_launcher");

    when(uriLoader.buildLoadData(eq(resourceUri), eq(IMAGE_SIDE), eq(IMAGE_SIDE), eq(options)))
        .thenReturn(new ModelLoader.LoadData<>(key, fetcher));

    assertTrue(loader.handles(resourceUri.toString()));
    assertEquals(
        fetcher,
        Preconditions.checkNotNull(
                loader.buildLoadData(resourceUri.toString(), IMAGE_SIDE, IMAGE_SIDE, options))
            .fetcher);
  }

  @Test
  public void testHandlesHttp() {
    String url = "http://www.google.com";

    Uri expected = Uri.parse(url);
    when(uriLoader.buildLoadData(eq(expected), eq(IMAGE_SIDE), eq(IMAGE_SIDE), eq(options)))
        .thenReturn(new ModelLoader.LoadData<>(key, fetcher));

    assertTrue(loader.handles(url));
    assertEquals(
        fetcher,
        Preconditions.checkNotNull(loader.buildLoadData(url, IMAGE_SIDE, IMAGE_SIDE, options))
            .fetcher);
  }

  @Test
  public void testHandlesHttps() {
    String url = "https://www.google.com";

    Uri expected = Uri.parse(url);
    when(uriLoader.buildLoadData(eq(expected), eq(IMAGE_SIDE), eq(IMAGE_SIDE), eq(options)))
        .thenReturn(new ModelLoader.LoadData<>(key, fetcher));

    assertTrue(loader.handles(url));
    assertEquals(
        fetcher,
        Preconditions.checkNotNull(loader.buildLoadData(url, IMAGE_SIDE, IMAGE_SIDE, options))
            .fetcher);
  }

  @Test
  public void testHandlesContent() {
    String content = "content://com.bumptech.glide";

    Uri expected = Uri.parse(content);
    when(uriLoader.buildLoadData(eq(expected), eq(IMAGE_SIDE), eq(IMAGE_SIDE), eq(options)))
        .thenReturn(new ModelLoader.LoadData<>(key, fetcher));

    assertTrue(loader.handles(content));
    assertEquals(
        fetcher,
        Preconditions.checkNotNull(loader.buildLoadData(content, IMAGE_SIDE, IMAGE_SIDE, options))
            .fetcher);
  }

  @Test
  public void testGetResourceFetcher_withEmptyString_returnsNull() {
    assertThat(loader.buildLoadData("", IMAGE_SIDE, IMAGE_SIDE, options)).isNull();
    assertThat(loader.buildLoadData("    ", IMAGE_SIDE, IMAGE_SIDE, options)).isNull();
    assertThat(loader.buildLoadData("  \n", IMAGE_SIDE, IMAGE_SIDE, options)).isNull();
  }
}
