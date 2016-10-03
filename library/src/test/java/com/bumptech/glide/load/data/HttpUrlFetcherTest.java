package com.bumptech.glide.load.data;

import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.model.GlideUrl;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 18)
public class HttpUrlFetcherTest {
  @Mock HttpURLConnection urlConnection;
  @Mock HttpUrlFetcher.HttpUrlConnectionFactory connectionFactory;
  @Mock GlideUrl glideUrl;
  @Mock InputStream stream;
  @Mock DataFetcher.DataCallback<InputStream> callback;

  private static final int TIMEOUT_MS = 100;
  private HttpUrlFetcher fetcher;

  @Before
  public void setUp() throws IOException {
    MockitoAnnotations.initMocks(this);
    URL url = new URL("http://www.google.com");

    when(connectionFactory.build(eq(url))).thenReturn(urlConnection);
    when(urlConnection.getInputStream()).thenReturn(stream);
    when(urlConnection.getResponseCode()).thenReturn(200);
    when(glideUrl.toURL()).thenReturn(url);

    fetcher = new HttpUrlFetcher(glideUrl, TIMEOUT_MS, connectionFactory);
  }

  @Test
  public void testSetsReadTimeout() throws IOException {
    fetcher.loadData(Priority.HIGH, callback);
    verify(urlConnection).setReadTimeout(eq(TIMEOUT_MS));
  }

  @Test
  public void testSetsConnectTimeout() throws IOException {
    fetcher.loadData(Priority.IMMEDIATE, callback);
    verify(urlConnection).setConnectTimeout(eq(TIMEOUT_MS));
  }

  @Test
  public void testReturnsNullIfCancelledBeforeConnects() throws IOException {
    InputStream notExpected = new ByteArrayInputStream(new byte[0]);
    when(urlConnection.getInputStream()).thenReturn(notExpected);

    fetcher.cancel();
    fetcher.loadData(Priority.LOW, callback);
    verify(callback).onDataReady(isNull(InputStream.class));
  }

  @Test
  public void testDisconnectsUrlOnCleanup() throws IOException {
    fetcher.loadData(Priority.HIGH, callback);
    fetcher.cleanup();

    verify(urlConnection).disconnect();
  }

  @Test
  public void testDoesNotThrowIfCleanupCalledBeforeStarted() {
    fetcher.cleanup();
  }

  @Test
  public void testDoesNotThrowIfCancelCalledBeforeStart() {
    fetcher.cancel();
  }

  @Test
  public void testCancelDoesNotDisconnectIfAlreadyConnected()
      throws IOException {
    fetcher.loadData(Priority.HIGH, callback);
    fetcher.cancel();

    verify(urlConnection, never()).disconnect();
  }

  @Test
  public void testClosesStreamInCleanupIfNotNull() throws IOException {
    fetcher.loadData(Priority.HIGH, callback);
    fetcher.cleanup();

    verify(stream).close();
  }

  @Test
  public void testClosesStreamBeforeDisconnectingConnection() throws IOException {
    fetcher.loadData(Priority.NORMAL, callback);
    fetcher.cleanup();

    InOrder order = inOrder(stream, urlConnection);
    order.verify(stream).close();
    order.verify(urlConnection).disconnect();
  }
}
