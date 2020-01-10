package com.bumptech.glide.integration.cronet;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.annotation.NonNull;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.HttpException;
import com.bumptech.glide.load.data.DataFetcher.DataCallback;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.bumptech.glide.load.model.LazyHeaders.Builder;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.MoreExecutors;
import java.net.HttpURLConnection;
import java.nio.ByteBuffer;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executor;
import org.chromium.net.CronetEngine;
import org.chromium.net.CronetException;
import org.chromium.net.UrlRequest;
import org.chromium.net.UrlRequest.Callback;
import org.chromium.net.UrlResponseInfo;
import org.chromium.net.impl.UrlResponseInfoImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link ChromiumUrlFetcher}. */
@RunWith(RobolectricTestRunner.class)
public class ChromiumUrlFetcherTest {
  @Mock private DataCallback<ByteBuffer> callback;
  @Mock private CronetEngine cronetEngine;
  @Mock private UrlRequest request;
  @Mock private UrlRequest.Builder mockUrlRequestBuilder;
  @Mock private ByteBufferParser<ByteBuffer> parser;
  @Mock private CronetRequestFactory cronetRequestFactory;

  private UrlRequest.Builder builder;
  private GlideUrl glideUrl;
  private ChromiumUrlFetcher<ByteBuffer> fetcher;
  private ChromiumRequestSerializer serializer;
  private ArgumentCaptor<UrlRequest.Callback> urlRequestListenerCaptor;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    when(parser.getDataClass()).thenReturn(ByteBuffer.class);
    when(parser.parse(any(ByteBuffer.class)))
        .thenAnswer(
            new Answer<ByteBuffer>() {
              @Override
              public ByteBuffer answer(InvocationOnMock invocation) throws Throwable {
                return (ByteBuffer) invocation.getArguments()[0];
              }
            });
    when(cronetEngine.newUrlRequestBuilder(
            anyString(), any(UrlRequest.Callback.class), any(Executor.class)))
        .thenReturn(mockUrlRequestBuilder);
    when(mockUrlRequestBuilder.build()).thenReturn(request);

    glideUrl = new GlideUrl("http://www.google.com");

    urlRequestListenerCaptor = ArgumentCaptor.forClass(UrlRequest.Callback.class);
    serializer = new ChromiumRequestSerializer(cronetRequestFactory, null /*dataLogger*/);
    fetcher = new ChromiumUrlFetcher<>(serializer, parser, glideUrl);
    builder =
        cronetEngine.newUrlRequestBuilder(
            glideUrl.toStringUrl(),
            mock(UrlRequest.Callback.class),
            MoreExecutors.directExecutor());
    when(cronetRequestFactory.newRequest(
            anyString(), anyInt(), anyHeaders(), urlRequestListenerCaptor.capture()))
        .thenReturn(builder);
    when(builder.build()).thenReturn(request);
  }

  @Test
  public void testLoadData_createsAndStartsRequest() {
    when(cronetRequestFactory.newRequest(
            eq(glideUrl.toStringUrl()),
            eq(UrlRequest.Builder.REQUEST_PRIORITY_LOWEST),
            anyHeaders(),
            any(UrlRequest.Callback.class)))
        .thenReturn(builder);

    fetcher.loadData(Priority.LOW, callback);

    verify(request).start();
  }

  @Test
  public void testLoadData_providesHeadersFromGlideUrl() {
    LazyHeaders.Builder headersBuilder = new Builder();
    headersBuilder.addHeader("key", "value");
    LazyHeaders headers = headersBuilder.build();

    glideUrl = new GlideUrl("http://www.google.com", headers);
    fetcher = new ChromiumUrlFetcher<>(serializer, parser, glideUrl);
    fetcher.loadData(Priority.LOW, callback);

    verify(cronetRequestFactory)
        .newRequest(
            Matchers.eq(glideUrl.toStringUrl()),
            anyInt(),
            Matchers.eq(headers.getHeaders()),
            any(UrlRequest.Callback.class));

    verify(request).start();
  }

  @Test
  public void testLoadData_withInProgressRequest_doesNotStartNewRequest() {
    ChromiumUrlFetcher<ByteBuffer> firstFetcher =
        new ChromiumUrlFetcher<>(serializer, parser, glideUrl);
    ChromiumUrlFetcher<ByteBuffer> secondFetcher =
        new ChromiumUrlFetcher<>(serializer, parser, glideUrl);

    firstFetcher.loadData(Priority.LOW, callback);
    secondFetcher.loadData(Priority.HIGH, callback);

    verify(cronetRequestFactory, times(1))
        .newRequest(
            Matchers.eq(glideUrl.toStringUrl()),
            anyInt(),
            anyMap(),
            any(UrlRequest.Callback.class));
  }

  @Test
  public void testLoadData_withInProgressRequest_isNotifiedWhenRequestCompletes() throws Exception {
    ChromiumUrlFetcher<ByteBuffer> firstFetcher =
        new ChromiumUrlFetcher<>(serializer, parser, glideUrl);
    ChromiumUrlFetcher<ByteBuffer> secondFetcher =
        new ChromiumUrlFetcher<>(serializer, parser, glideUrl);

    DataCallback<ByteBuffer> firstCb = mock(DataCallback.class);
    DataCallback<ByteBuffer> secondCb = mock(DataCallback.class);
    firstFetcher.loadData(Priority.LOW, firstCb);
    secondFetcher.loadData(Priority.HIGH, secondCb);

    succeed(getInfo(10, 200), urlRequestListenerCaptor.getValue(), ByteBuffer.allocateDirect(10));

    verify(firstCb, timeout(1000)).onDataReady(isA(ByteBuffer.class));
    verify(secondCb, timeout(1000)).onDataReady(isA(ByteBuffer.class));
  }

  @NonNull
  private UrlResponseInfo getInfo(int contentLength, int statusCode) {
    return new UrlResponseInfoImpl(
        ImmutableList.of(glideUrl.toStringUrl()),
        statusCode,
        "OK",
        ImmutableList.<Entry<String, String>>of(
            new SimpleImmutableEntry<>("Content-Length", Integer.toString(contentLength))),
        false,
        "",
        "");
  }

  @Test
  public void testCancel_withMultipleInProgressRequests_doesNotCancelChromiumRequest() {
    ChromiumUrlFetcher<ByteBuffer> firstFetcher =
        new ChromiumUrlFetcher<>(serializer, parser, glideUrl);
    ChromiumUrlFetcher<ByteBuffer> secondFetcher =
        new ChromiumUrlFetcher<>(serializer, parser, glideUrl);

    firstFetcher.loadData(Priority.LOW, callback);
    secondFetcher.loadData(Priority.HIGH, callback);

    firstFetcher.cancel();

    verify(request, never()).cancel();
  }

  @Test
  public void testCancel_afterCancellingAllInProgressRequests_cancelsChromiumRequest() {
    ChromiumUrlFetcher<ByteBuffer> firstFetcher =
        new ChromiumUrlFetcher<>(serializer, parser, glideUrl);
    ChromiumUrlFetcher<ByteBuffer> secondFetcher =
        new ChromiumUrlFetcher<>(serializer, parser, glideUrl);

    firstFetcher.loadData(Priority.LOW, callback);
    secondFetcher.loadData(Priority.HIGH, callback);

    firstFetcher.cancel();
    secondFetcher.cancel();

    verify(request).cancel();
  }

  @Test
  public void testCancel_withNoStartedRequest_doesNothing() {
    fetcher.cancel();
  }

  @Test
  public void testCancel_withStartedRequest_cancelsRequest() {
    fetcher.loadData(Priority.LOW, callback);

    fetcher.cancel();

    verify(request).cancel();
  }

  @Test
  public void testRequestComplete_withNonNullException_callsCallbackWithException() {
    CronetException expected = new CronetException("test", /*cause=*/ null) {};
    fetcher.loadData(Priority.LOW, callback);
    urlRequestListenerCaptor.getValue().onFailed(request, null, expected);

    verify(callback, timeout(1000)).onLoadFailed(eq(expected));
  }

  @Test
  public void testRequestComplete_withNon200StatusCode_callsCallbackWithException()
      throws Exception {
    UrlResponseInfo info = getInfo(0, HttpURLConnection.HTTP_INTERNAL_ERROR);
    fetcher.loadData(Priority.LOW, callback);
    UrlRequest.Callback urlCallback = urlRequestListenerCaptor.getValue();
    succeed(info, urlCallback, ByteBuffer.allocateDirect(0));
    ArgumentCaptor<HttpException> captor = ArgumentCaptor.forClass(HttpException.class);
    verify(callback, timeout(1000)).onLoadFailed(captor.capture());
    assertThat(captor.getValue())
        .hasMessageThat()
        .isEqualTo("Http request failed with status code: 500");
  }

  private void succeed(UrlResponseInfo info, Callback urlCallback, ByteBuffer byteBuffer)
      throws Exception {
    byteBuffer.position(byteBuffer.limit());
    urlCallback.onResponseStarted(request, info);
    urlCallback.onReadCompleted(request, info, byteBuffer);
    urlCallback.onSucceeded(request, info);
  }

  @Test
  public void testRequestComplete_withUnauthorizedStatusCode_callsCallbackWithAuthError()
      throws Exception {
    UrlResponseInfo info = getInfo(0, HttpURLConnection.HTTP_FORBIDDEN);
    fetcher.loadData(Priority.LOW, callback);
    UrlRequest.Callback urlCallback = urlRequestListenerCaptor.getValue();
    succeed(info, urlCallback, ByteBuffer.allocateDirect(0));

    verifyAuthError();
  }

  @Test
  public void testRequestComplete_whenCancelledAndUnauthorized_callsCallbackWithNullError()
      throws Exception {
    UrlResponseInfo info = getInfo(0, HttpURLConnection.HTTP_FORBIDDEN);
    fetcher.loadData(Priority.HIGH, callback);
    Callback urlCallback = urlRequestListenerCaptor.getValue();
    urlCallback.onResponseStarted(request, info);
    urlCallback.onCanceled(request, info);

    verify(callback, timeout(1000)).onLoadFailed(isNull(Exception.class));
  }

  private void verifyAuthError() {
    ArgumentCaptor<Exception> exceptionArgumentCaptor = ArgumentCaptor.forClass(Exception.class);
    verify(callback, timeout(1000)).onLoadFailed(exceptionArgumentCaptor.capture());
    HttpException exception = (HttpException) exceptionArgumentCaptor.getValue();
    assertThat(exception.getStatusCode()).isEqualTo(HttpURLConnection.HTTP_FORBIDDEN);
  }

  @Test
  public void testRequestComplete_with200AndCancelled_callsCallbackWithNullException()
      throws Exception {
    UrlResponseInfo info = getInfo(0, 200);
    fetcher.loadData(Priority.LOW, callback);
    Callback urlCallback = urlRequestListenerCaptor.getValue();
    urlCallback.onResponseStarted(request, info);
    urlCallback.onCanceled(request, info);

    verify(callback, timeout(1000)).onLoadFailed(isNull(Exception.class));
  }

  @Test
  public void testRequestComplete_with200NotCancelledMatchingLength_callsCallbackWithValidData()
      throws Exception {
    String data = "data";
    ByteBuffer expected = ByteBuffer.wrap(data.getBytes());
    ArgumentCaptor<ByteBuffer> captor = ArgumentCaptor.forClass(ByteBuffer.class);

    fetcher.loadData(Priority.LOW, callback);
    succeed(
        getInfo(expected.remaining(), 200),
        urlRequestListenerCaptor.getValue(),
        expected.duplicate());

    verify(callback, timeout(1000)).onDataReady(captor.capture());

    ByteBuffer received = captor.getValue();

    assertThat(
            new String(
                received.array(),
                received.arrayOffset() + received.position(),
                received.remaining()))
        .isEqualTo(data);
  }

  private static Map<String, String> anyHeaders() {
    return anyMap();
  }
}
