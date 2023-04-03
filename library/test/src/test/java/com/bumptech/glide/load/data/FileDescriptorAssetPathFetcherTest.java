package com.bumptech.glide.load.data;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
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
@Config(sdk = 18)
public class FileDescriptorAssetPathFetcherTest {

  @Mock private AssetManager assetManager;
  @Mock private AssetFileDescriptor assetFileDescriptor;
  @Mock private DataFetcher.DataCallback<AssetFileDescriptor> callback;

  private FileDescriptorAssetPathFetcher fetcher;

  @Before
  public void setUp() throws IOException {
    MockitoAnnotations.initMocks(this);
    String assetPath = "/some/asset/path";
    fetcher = new FileDescriptorAssetPathFetcher(assetManager, assetPath);
    when(assetManager.openFd(eq(assetPath))).thenReturn(assetFileDescriptor);
  }

  @Test
  public void testOpensInputStreamForPathWithAssetManager() throws Exception {
    fetcher.loadData(Priority.NORMAL, callback);
    verify(callback).onDataReady(eq(assetFileDescriptor));
  }

  @Test
  public void testClosesOpenedInputStreamOnCleanup() throws Exception {
    fetcher.loadData(Priority.NORMAL, callback);
    fetcher.cleanup();

    verify(assetFileDescriptor).close();
  }

  @Test
  public void testDoesNothingOnCleanupIfNoDataLoaded() throws IOException {
    fetcher.cleanup();
    verify(assetFileDescriptor, never()).close();
  }

  @Test
  public void testDoesNothingOnCancel() throws Exception {
    fetcher.loadData(Priority.NORMAL, callback);
    fetcher.cancel();
    verify(assetFileDescriptor, never()).close();
  }
}
