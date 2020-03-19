package com.bumptech.glide.load.data;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.HttpException;
import com.bumptech.glide.load.model.GlideUrl;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 18)
public class HttpUrlFetcherTest {
  @Mock private HttpURLConnection urlConnection;
  @Mock private HttpUrlFetcher.HttpUrlConnectionFactory connectionFactory;
  @Mock private GlideUrl glideUrl;
  @Mock private InputStream stream;
  @Mock private DataFetcher.DataCallback<InputStream> callback;

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
  public void loadData_whenConnectThrowsFileNotFound_notifiesCallbackWithHttpErrorCode()
      throws IOException {
    int statusCode = 400;
    doThrow(new FileNotFoundException()).when(urlConnection).connect();
    when(urlConnection.getResponseCode()).thenReturn(statusCode);

    fetcher.loadData(Priority.HIGH, callback);

    HttpException exception = (HttpException) getCallbackException();
    assertThat(exception.getStatusCode()).isEqualTo(statusCode);
  }

  @Test
  public void loadData_whenGetInputStreamThrows_notifiesCallbackWithStatusCode()
      throws IOException {
    int statusCode = 400;
    doThrow(new IOException()).when(urlConnection).getInputStream();
    when(urlConnection.getResponseCode()).thenReturn(statusCode);

    fetcher.loadData(Priority.HIGH, callback);

    HttpException exception = (HttpException) getCallbackException();
    assertThat(exception.getStatusCode()).isEqualTo(statusCode);
  }

  @Test
  public void loadData_whenConnectAndGetResponseCodeThrow_notifiesCallbackWithInvalidStatusCode()
      throws IOException {
    doThrow(new FileNotFoundException()).when(urlConnection).connect();
    when(urlConnection.getResponseCode()).thenThrow(new IOException());

    fetcher.loadData(Priority.HIGH, callback);

    HttpException exception = (HttpException) getCallbackException();
    assertThat(exception.getStatusCode()).isEqualTo(HttpUrlFetcher.INVALID_STATUS_CODE);
  }

  @Test
  public void loadData_whenRedirectUrlIsMalformed_notifiesCallbackWithStatusCode()
      throws IOException {
    int statusCode = 300;

    when(urlConnection.getHeaderField(eq(HttpUrlFetcher.REDIRECT_HEADER_FIELD)))
        .thenReturn("gg://www.google.com");
    when(urlConnection.getResponseCode()).thenReturn(statusCode);

    fetcher.loadData(Priority.HIGH, callback);

    HttpException exception = (HttpException) getCallbackException();
    assertThat(exception.getStatusCode()).isEqualTo(statusCode);
    assertThat(exception.getCause()).isInstanceOf(MalformedURLException.class);
  }

  private Exception getCallbackException() {
    ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
    verify(callback).onLoadFailed(captor.capture());
    return captor.getValue();
  }

  @Test
  public void testSetsReadTimeout() {
    fetcher.loadData(Priority.HIGH, callback);
    verify(urlConnection).setReadTimeout(eq(TIMEOUT_MS));
  }

  @Test
  public void testSetsConnectTimeout() {
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
  public void testDisconnectsUrlOnCleanup() {
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
  public void testCancelDoesNotDisconnectIfAlreadyConnected() {
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
