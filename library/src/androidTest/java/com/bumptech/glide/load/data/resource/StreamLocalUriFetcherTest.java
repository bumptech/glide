package com.bumptech.glide.load.data.resource;

import static org.mockito.Matchers.isNotNull;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.net.Uri;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.data.StreamLocalUriFetcher;

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
public class StreamLocalUriFetcherTest {
  @Mock DataFetcher.DataCallback<InputStream> callback;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testLoadsInputStream() throws Exception {
    final Context context = RuntimeEnvironment.application;
    Uri uri = Uri.parse("android.resource://com.bumptech.glide.tests/raw/ic_launcher");
    StreamLocalUriFetcher fetcher = new StreamLocalUriFetcher(context, uri);
    fetcher.loadData(Priority.NORMAL, callback);
    verify(callback).onDataReady(isNotNull(InputStream.class));
  }
}
