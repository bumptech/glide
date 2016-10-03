package com.bumptech.glide.load.data.resource;

import static org.mockito.Matchers.isA;
import static org.mockito.Matchers.isNotNull;
import static org.mockito.Mockito.verify;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.data.StreamLocalUriFetcher;
import com.bumptech.glide.tests.ContentResolverShadow;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.internal.ShadowExtractor;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 18, shadows = { ContentResolverShadow.class })
public class StreamLocalUriFetcherTest {
  @Mock DataFetcher.DataCallback<InputStream> callback;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testLoadResource_returnsInputStream() throws Exception {
    Context context = RuntimeEnvironment.application;
    Uri uri = Uri.parse("file://nothing");

    ContentResolver contentResolver = context.getContentResolver();
    ContentResolverShadow shadow = (ContentResolverShadow) ShadowExtractor.extract(contentResolver);
    shadow.registerInputStream(uri, new ByteArrayInputStream(new byte[0]));

    StreamLocalUriFetcher fetcher = new StreamLocalUriFetcher(context.getContentResolver(), uri);
    fetcher.loadData(Priority.NORMAL, callback);
    verify(callback).onDataReady(isNotNull(InputStream.class));
  }

  @Test
  public void testLoadResource_withNullInputStream_callsLoadFailed() {
    Context context = RuntimeEnvironment.application;
    Uri uri = Uri.parse("file://nothing");

    ContentResolver contentResolver = context.getContentResolver();
    ContentResolverShadow shadow = (ContentResolverShadow) ShadowExtractor.extract(contentResolver);

    shadow.registerInputStream(uri, null /*inputStream*/);

    StreamLocalUriFetcher fetcher = new StreamLocalUriFetcher(context.getContentResolver(), uri);
    fetcher.loadData(Priority.LOW, callback);

    verify(callback).onLoadFailed(isA(FileNotFoundException.class));
  }
}
