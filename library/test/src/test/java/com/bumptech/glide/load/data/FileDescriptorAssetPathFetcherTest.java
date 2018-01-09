package com.bumptech.glide.load.data;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.os.ParcelFileDescriptor;
import com.bumptech.glide.Priority;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 18)
public class FileDescriptorAssetPathFetcherTest {

  @Mock private AssetManager assetManager;
  @Mock private AssetFileDescriptor assetFileDescriptor;
  @Mock private DataFetcher.DataCallback<ParcelFileDescriptor> callback;

  private FileDescriptorAssetPathFetcher fetcher;
  private ParcelFileDescriptor expected;

  @Before
  public void setUp() throws IOException {
    MockitoAnnotations.initMocks(this);
    String assetPath = "/some/asset/path";
    fetcher = new FileDescriptorAssetPathFetcher(assetManager, assetPath);
    expected = mock(ParcelFileDescriptor.class);
    when(assetFileDescriptor.getParcelFileDescriptor()).thenReturn(expected);
    when(assetManager.openFd(eq(assetPath))).thenReturn(assetFileDescriptor);
  }

  @Test
  public void testOpensInputStreamForPathWithAssetManager() throws Exception {
    fetcher.loadData(Priority.NORMAL, callback);
    verify(callback).onDataReady(eq(expected));
  }

  @Test
  public void testClosesOpenedInputStreamOnCleanup() throws Exception {
    fetcher.loadData(Priority.NORMAL, callback);
    fetcher.cleanup();

    verify(expected).close();
  }

  @Test
  public void testDoesNothingOnCleanupIfNoDataLoaded() throws IOException {
    fetcher.cleanup();
    verify(expected, never()).close();
  }

  @Test
  public void testDoesNothingOnCancel() throws Exception {
    fetcher.loadData(Priority.NORMAL, callback);
    fetcher.cancel();
    verify(expected, never()).close();
  }
}
