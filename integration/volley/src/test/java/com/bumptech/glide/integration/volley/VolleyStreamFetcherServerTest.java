package com.bumptech.glide.integration.volley;

import static com.bumptech.glide.testutil.TestUtil.assertStreamOf;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.os.SystemClock;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.Headers;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowSystemClock;

/**
 * Tests {@link com.bumptech.glide.integration.volley.VolleyStreamFetcher} against server responses.
 */
@RunWith(RobolectricTestRunner.class)
@Config(
    manifest = Config.NONE,
    sdk = 18,
    shadows = VolleyStreamFetcherServerTest.FakeSystemClock.class)
public class VolleyStreamFetcherServerTest {
  private static final String DEFAULT_PATH = "/fakepath";

  @Mock private DataFetcher.DataCallback<InputStream> callback;

  private MockWebServer mockWebServer;
  private RequestQueue requestQueue;
  private ArgumentCaptor<InputStream> streamCaptor;
  private CountDownLatch waitForResponseLatch;

  @Before
  public void setUp() throws IOException {
    MockitoAnnotations.initMocks(this);

    waitForResponseLatch = new CountDownLatch(1);
    doAnswer(new CountDown()).when(callback).onDataReady(any(InputStream.class));
    doAnswer(new CountDown()).when(callback).onLoadFailed(any(Exception.class));
    requestQueue = Volley.newRequestQueue(RuntimeEnvironment.application);
    mockWebServer = new MockWebServer();
    mockWebServer.start();

    streamCaptor = ArgumentCaptor.forClass(InputStream.class);
  }

  @After
  public void tearDown() throws IOException {
    mockWebServer.shutdown();
    requestQueue.stop();
  }

  @Test
  public void testReturnsInputStreamOnStatusOk() throws Exception {
    String expected = "fakedata";
    mockWebServer.enqueue(new MockResponse().setBody(expected).setResponseCode(200));
    DataFetcher<InputStream> fetcher = getFetcher();
    fetcher.loadData(Priority.HIGH, callback);
    waitForResponseLatch.await();
    verify(callback).onDataReady(streamCaptor.capture());
    assertStreamOf(expected, streamCaptor.getValue());
  }

  @Test
  public void testHandlesRedirect301s() throws Exception {
    String expected = "fakedata";
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(301)
            .setHeader("Location", mockWebServer.url("/redirect").toString()));
    mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(expected));
    getFetcher().loadData(Priority.LOW, callback);
    waitForResponseLatch.await();
    verify(callback).onDataReady(streamCaptor.capture());
    assertStreamOf(expected, streamCaptor.getValue());
  }

  @Test
  public void testHandlesRedirect302s() throws Exception {
    String expected = "fakedata";
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(302)
            .setHeader("Location", mockWebServer.url("/redirect").toString()));
    mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(expected));
    getFetcher().loadData(Priority.LOW, callback);
    waitForResponseLatch.await();
    verify(callback).onDataReady(streamCaptor.capture());
    assertStreamOf(expected, streamCaptor.getValue());
  }

  @Test
  public void testHandlesUpToFiveRedirects() throws Exception {
    int numRedirects = 4;
    String expected = "redirectedData";
    String redirectBase = "/redirect";
    for (int i = 0; i < numRedirects; i++) {
      mockWebServer.enqueue(
          new MockResponse()
              .setResponseCode(301)
              .setHeader("Location", mockWebServer.url(redirectBase + i).toString()));
    }
    mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(expected));

    getFetcher().loadData(Priority.NORMAL, callback);
    waitForResponseLatch.await();
    verify(callback).onDataReady(streamCaptor.capture());
    assertStreamOf(expected, streamCaptor.getValue());

    assertThat(mockWebServer.takeRequest().getPath()).contains(DEFAULT_PATH);
    for (int i = 0; i < numRedirects; i++) {
      assertThat(mockWebServer.takeRequest().getPath()).contains(redirectBase + i);
    }
  }

  @Test
  public void testCallsLoadFailedIfRedirectLocationIsEmpty() throws Exception {
    for (int i = 0; i < 2; i++) {
      mockWebServer.enqueue(new MockResponse().setResponseCode(301));
    }

    getFetcher().loadData(Priority.NORMAL, callback);
    waitForResponseLatch.await();
    verify(callback).onLoadFailed(isA(VolleyError.class));
  }

  @Test
  public void testCallsLoadFailedIfStatusCodeIsNegativeOne() throws Exception {
    mockWebServer.enqueue(new MockResponse().setResponseCode(-1));
    getFetcher().loadData(Priority.LOW, callback);
    waitForResponseLatch.await();
    verify(callback).onLoadFailed(isA(VolleyError.class));
  }

  @Test
  public void testCallsLoadFailedAfterTooManyRedirects() throws Exception {
    for (int i = 0; i < 20; i++) {
      mockWebServer.enqueue(
          new MockResponse()
              .setResponseCode(301)
              .setHeader("Location", mockWebServer.url("/redirect" + i).toString()));
    }
    getFetcher().loadData(Priority.NORMAL, callback);
    waitForResponseLatch.await();
    verify(callback).onLoadFailed(isA(VolleyError.class));
  }

  @Test
  public void testCallsLoadFailedIfStatusCodeIs500() throws Exception {
    mockWebServer.enqueue(new MockResponse().setResponseCode(500).setBody("error"));
    getFetcher().loadData(Priority.NORMAL, callback);
    waitForResponseLatch.await();
    verify(callback).onLoadFailed(isA(VolleyError.class));
  }

  @Test
  public void testCallsLoadFailedIfStatusCodeIs400() throws Exception {
    mockWebServer.enqueue(new MockResponse().setResponseCode(400).setBody("error"));
    getFetcher().loadData(Priority.LOW, callback);
    waitForResponseLatch.await();
    verify(callback).onLoadFailed(isA(VolleyError.class));
  }

  @Test
  public void testAppliesHeadersInGlideUrl() throws Exception {
    mockWebServer.enqueue(new MockResponse().setResponseCode(200));
    String headerField = "field";
    String headerValue = "value";
    Map<String, String> headersMap = new HashMap<>();
    headersMap.put(headerField, headerValue);
    Headers headers = mock(Headers.class);
    when(headers.getHeaders()).thenReturn(headersMap);

    getFetcher(headers).loadData(Priority.HIGH, callback);
    waitForResponseLatch.await();

    assertThat(mockWebServer.takeRequest().getHeader(headerField)).isEqualTo(headerValue);
  }

  private DataFetcher<InputStream> getFetcher() {
    return getFetcher(Headers.DEFAULT);
  }

  private DataFetcher<InputStream> getFetcher(Headers headers) {
    URL url = mockWebServer.url(DEFAULT_PATH).url();
    return new VolleyStreamFetcher(requestQueue, new GlideUrl(url.toString(), headers));
  }

  private class CountDown implements Answer<Void> {

    @Override
    public Void answer(InvocationOnMock invocation) throws Throwable {
      waitForResponseLatch.countDown();
      return null;
    }
  }

  /** A shadow clock that doesn't rely on running on an Android thread with a Looper. */
  @Implements(SystemClock.class)
  public static class FakeSystemClock extends ShadowSystemClock {

    // Used by Shadow.
    @SuppressWarnings("unused")
    @Implementation
    public static long elapsedRealtime() {
      // The default is to return something using the main looper, which doesn't exist on
      // Volley's threads.
      return System.currentTimeMillis();
    }
  }
}
