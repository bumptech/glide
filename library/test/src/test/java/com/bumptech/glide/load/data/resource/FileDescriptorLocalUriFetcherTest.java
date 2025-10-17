package com.bumptech.glide.load.data.resource;

import static com.bumptech.glide.RobolectricConstants.ROBOLECTRIC_SDK;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import androidx.test.core.app.ApplicationProvider;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.data.FileDescriptorLocalUriFetcher;
import com.bumptech.glide.load.data.mediastore.MediaStoreUtil;
import com.bumptech.glide.tests.ContentResolverShadow;
import java.io.FileNotFoundException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;

@RunWith(RobolectricTestRunner.class)
@Config(
    sdk = ROBOLECTRIC_SDK,
    shadows = {ContentResolverShadow.class})
public class FileDescriptorLocalUriFetcherTest {

  @Mock private DataFetcher.DataCallback<ParcelFileDescriptor> callback;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testLoadResource_returnsFileDescriptor() throws Exception {
    Context context = ApplicationProvider.getApplicationContext();
    Uri uri = Uri.parse("file://nothing");

    ContentResolver contentResolver = context.getContentResolver();
    ContentResolverShadow shadow = Shadow.extract(contentResolver);

    AssetFileDescriptor assetFileDescriptor = mock(AssetFileDescriptor.class);
    ParcelFileDescriptor parcelFileDescriptor = mock(ParcelFileDescriptor.class);
    when(assetFileDescriptor.getParcelFileDescriptor()).thenReturn(parcelFileDescriptor);
    shadow.registerFileDescriptor(uri, assetFileDescriptor);

    FileDescriptorLocalUriFetcher fetcher =
        new FileDescriptorLocalUriFetcher(context.getContentResolver(), uri, false);
    fetcher.loadData(Priority.NORMAL, callback);
    verify(callback).onDataReady(eq(parcelFileDescriptor));
  }

  @Test
  public void testLoadResource_mediaUri_returnsFileDescriptor() throws Exception {
    Context context = ApplicationProvider.getApplicationContext();
    Uri uri = Uri.parse("content://media");

    ContentResolver contentResolver = context.getContentResolver();

    AssetFileDescriptor assetFileDescriptor = mock(AssetFileDescriptor.class);
    ParcelFileDescriptor parcelFileDescriptor = mock(ParcelFileDescriptor.class);
    when(assetFileDescriptor.getParcelFileDescriptor()).thenReturn(parcelFileDescriptor);

    FileDescriptorLocalUriFetcher fetcher =
        new FileDescriptorLocalUriFetcher(
            context.getContentResolver(), uri, /* useMediaStoreApisIfAvailable */ true);

    try (MockedStatic<MediaStoreUtil> utils = Mockito.mockStatic(MediaStoreUtil.class)) {
      utils.when(MediaStoreUtil::isMediaStoreOpenFileApisAvailable).thenReturn(true);
      utils.when(() -> MediaStoreUtil.isMediaStoreUri(uri)).thenReturn(true);
      utils
          .when(() -> MediaStoreUtil.openAssetFileDescriptor(uri, contentResolver))
          .thenReturn(assetFileDescriptor);
      fetcher.loadData(Priority.NORMAL, callback);
      verify(callback).onDataReady(eq(parcelFileDescriptor));
    }
  }

  @Test
  public void testLoadResource_withNullFileDescriptor_callsLoadFailed() {
    Context context = ApplicationProvider.getApplicationContext();
    Uri uri = Uri.parse("file://nothing");

    ContentResolver contentResolver = context.getContentResolver();
    ContentResolverShadow shadow = Shadow.extract(contentResolver);
    shadow.registerFileDescriptor(uri, null /*fileDescriptor*/);

    FileDescriptorLocalUriFetcher fetcher =
        new FileDescriptorLocalUriFetcher(
            context.getContentResolver(), uri, /* useMediaStoreApisIfAvailable */ false);
    fetcher.loadData(Priority.NORMAL, callback);
    verify(callback).onLoadFailed(isA(FileNotFoundException.class));
  }
}
