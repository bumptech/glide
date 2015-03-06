package com.bumptech.glide.load.data.mediastore;

import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.net.Uri;
import android.provider.MediaStore;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.data.DataFetcher;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.io.InputStream;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, emulateSdk = 18)
public class ThumbFetcherTest {

  @Mock ThumbnailStreamOpener opener;
  @Mock DataFetcher.DataCallback<InputStream> callback;
  @Mock InputStream expected;

  private ThumbFetcher fetcher;
  private Uri uri;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    uri = Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "123");
    fetcher = new ThumbFetcher(RuntimeEnvironment.application, uri, opener);
  }

  @Test
  public void testReturnsInputStreamFromThumbnailOpener() throws Exception {
    when(opener.open(eq(RuntimeEnvironment.application), eq(uri))).thenReturn(expected);

    fetcher.loadData(Priority.LOW, callback);
    verify(callback).onDataReady(isNotNull(InputStream.class));
  }

  @Test
  public void testClosesInputStreamFromThumbnailOpenerOnCleanup() throws Exception {
    when(opener.open(eq(RuntimeEnvironment.application), eq(uri))).thenReturn(expected);

    fetcher.loadData(Priority.HIGH, callback);

    fetcher.cleanup();
    verify(expected).close();
  }

  @Test
  public void testDoesNotThrowIfCleanupWithNullInputStream() {
    fetcher.cleanup();
  }
}