package com.bumptech.glide.integration.cronet;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.Priority;
import com.bumptech.glide.integration.cronet.ChromiumUrlLoader.Options;
import com.bumptech.glide.load.HttpException;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.data.DataFetcher.DataCallback;
import com.bumptech.glide.load.engine.executor.GlideExecutor;
import com.bumptech.glide.load.engine.executor.GlideExecutor.UncaughtThrowableStrategy;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.bumptech.glide.load.model.LazyHeaders.Builder;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.MoreExecutors;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.ByteBuffer;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.chromium.net.CronetEngine;
import org.chromium.net.CronetException;
import org.chromium.net.UrlRequest;
import org.chromium.net.UrlRequest.Callback;
import org.chromium.net.UrlResponseInfo;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link ChromiumUrlFetcher}. */
@RunWith(RobolectricTestRunner.class)
public class ChromiumUrlFetcherTest {

  @Rule public final MockitoRule mocks = MockitoJUnit.rule();
  @Mock private DataCallback<ByteBuffer> callback;
  @Mock private CronetEngine cronetEngine;
  @Mock private UrlRequest request;
  @Mock private UrlRequest.Builder mockUrlRequestBuilder;
  @Mock private ByteBufferParser<ByteBuffer> parser;
  @Mock private CronetRequestFactory cronetRequestFactory;
  @Mock private DataCallback<ByteBuffer> firstCallback;
  @Mock private DataCallback<ByteBuffer> secondCallback;

  private UrlRequest.Builder builder;
  private GlideUrl glideUrl;
  private ChromiumUrlFetcher<ByteBuffer> fetcher;
  private ChromiumRequestSerializer serializer;
  private ArgumentCaptor<UrlRequest.Callback> urlRequestListenerCaptor;

  @Before
  public void setUp() {
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
    serializer =
        new ChromiumRequestSerializer(
            cronetRequestFactory, /* dataLogger= */ null, /* executor= */ null);
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
  public void loadData_createsAndStartsRequest() {
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
  public void loadData_providesHeadersFromGlideUrl() {
    LazyHeaders.Builder headersBuilder = new Builder();
    headersBuilder.addHeader("key", "value");
    LazyHeaders headers = headersBuilder.build();

    glideUrl = new GlideUrl("http://www.google.com", headers);
    fetcher = new ChromiumUrlFetcher<>(serializer, parser, glideUrl);
    fetcher.loadData(Priority.LOW, callback);

    verify(cronetRequestFactory)
        .newRequest(
            ArgumentMatchers.eq(glideUrl.toStringUrl()),
            anyInt(),
            ArgumentMatchers.eq(headers.getHeaders()),
            any(UrlRequest.Callback.class));

    verify(request).start();
  }

  @Test
  public void loadData_withInProgressRequest_doesNotStartNewRequest() {
    ChromiumUrlFetcher<ByteBuffer> firstFetcher =
        new ChromiumUrlFetcher<>(serializer, parser, glideUrl);
    ChromiumUrlFetcher<ByteBuffer> secondFetcher =
        new ChromiumUrlFetcher<>(serializer, parser, glideUrl);

    firstFetcher.loadData(Priority.LOW, callback);
    secondFetcher.loadData(Priority.HIGH, callback);

    verify(cronetRequestFactory, times(1))
        .newRequest(
            ArgumentMatchers.eq(glideUrl.toStringUrl()),
            anyInt(),
            ArgumentMatchers.<String, String>anyMap(),
            any(UrlRequest.Callback.class));
  }

  @Test
  public void loadData_withInProgressRequest_isNotifiedWhenRequestCompletes() throws Exception {
    ChromiumUrlFetcher<ByteBuffer> firstFetcher =
        new ChromiumUrlFetcher<>(serializer, parser, glideUrl);
    ChromiumUrlFetcher<ByteBuffer> secondFetcher =
        new ChromiumUrlFetcher<>(serializer, parser, glideUrl);

    firstFetcher.loadData(Priority.LOW, firstCallback);
    secondFetcher.loadData(Priority.HIGH, secondCallback);

    succeed(getInfo(10, 200), urlRequestListenerCaptor.getValue(), ByteBuffer.allocateDirect(10));

    verify(firstCallback, timeout(1000)).onDataReady(isA(ByteBuffer.class));
    verify(secondCallback, timeout(1000)).onDataReady(isA(ByteBuffer.class));
  }

  @NonNull
  private UrlResponseInfo getInfo(final int contentLength, final int statusCode) {
    return new UrlResponseInfo() {

      @Override
      public String getUrl() {
        return glideUrl.toStringUrl();
      }

      @Override
      public List<String> getUrlChain() {
        return ImmutableList.of(getUrl());
      }

      @Override
      public int getHttpStatusCode() {
        return statusCode;
      }

      @Override
      public String getHttpStatusText() {
        return "OK";
      }

      @Override
      public List<Map.Entry<String, String>> getAllHeadersAsList() {
        return ImmutableList.<Map.Entry<String, String>>of(
            new SimpleImmutableEntry<>("Content-Length", Integer.toString(contentLength)));
      }

      @Override
      public Map<String, List<String>> getAllHeaders() {
        ImmutableMap.Builder<String, List<String>> builder = ImmutableMap.builder();
        for (Map.Entry<String, String> entry : getAllHeadersAsList()) {
          builder.put(entry.getKey(), ImmutableList.copyOf(entry.getValue().split(",")));
        }
        return builder.build();
      }

      @Override
      public boolean wasCached() {
        return false;
      }

      @Override
      public String getNegotiatedProtocol() {
        return "";
      }

      @Override
      public String getProxyServer() {
        return "";
      }

      @Override
      public long getReceivedByteCount() {
        return 0;
      }
    };
  }

  @Test
  public void cancel_withMultipleInProgressRequests_doesNotCancelChromiumRequest() {
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
  public void cancel_afterCancellingAllInProgressRequests_cancelsChromiumRequest() {
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
  public void cancel_withNoStartedRequest_doesNothing() {
    fetcher.cancel();
  }

  @Test
  public void cancel_withStartedRequest_cancelsRequest() {
    fetcher.loadData(Priority.LOW, callback);

    fetcher.cancel();

    verify(request).cancel();
  }

  @Test
  public void requestComplete_withNonNullException_callsCallbackWithException() {
    CronetException expected =
        new CronetException("test", /* cause= */ null) {
          static final long serialVersionUID = 1;
        };
    fetcher.loadData(Priority.LOW, callback);
    urlRequestListenerCaptor.getValue().onFailed(request, null, expected);

    verify(callback, timeout(1000)).onLoadFailed(eq(expected));
  }

  @Test
  public void requestComplete_withNon200StatusCode_callsCallbackWithException() throws Exception {
    UrlResponseInfo info = getInfo(0, HttpURLConnection.HTTP_INTERNAL_ERROR);
    fetcher.loadData(Priority.LOW, callback);
    UrlRequest.Callback urlCallback = urlRequestListenerCaptor.getValue();
    succeed(info, urlCallback, ByteBuffer.allocateDirect(0));
    ArgumentCaptor<HttpException> captor = ArgumentCaptor.forClass(HttpException.class);
    verify(callback, timeout(1000)).onLoadFailed(captor.capture());
    assertThat(captor.getValue())
        .hasMessageThat()
        .isEqualTo("Http request failed, status code: 500");
  }

  private void succeed(UrlResponseInfo info, Callback urlCallback, ByteBuffer byteBuffer)
      throws Exception {
    byteBuffer.position(byteBuffer.limit());
    urlCallback.onResponseStarted(request, info);
    urlCallback.onReadCompleted(request, info, byteBuffer);
    urlCallback.onSucceeded(request, info);
  }

  @Test
  public void requestComplete_withUnauthorizedStatusCode_callsCallbackWithAuthError()
      throws Exception {
    UrlResponseInfo info = getInfo(0, HttpURLConnection.HTTP_FORBIDDEN);
    fetcher.loadData(Priority.LOW, callback);
    UrlRequest.Callback urlCallback = urlRequestListenerCaptor.getValue();
    succeed(info, urlCallback, ByteBuffer.allocateDirect(0));

    verifyAuthError();
  }

  @Test
  public void requestComplete_whenCancelledAndUnauthorized_callsCallbackWithNullError()
      throws Exception {
    UrlResponseInfo info = getInfo(0, HttpURLConnection.HTTP_FORBIDDEN);
    fetcher.loadData(Priority.HIGH, callback);
    Callback urlCallback = urlRequestListenerCaptor.getValue();
    urlCallback.onResponseStarted(request, info);
    urlCallback.onCanceled(request, info);

    verify(callback, timeout(1000)).onLoadFailed(ArgumentMatchers.<Exception>isNull());
  }

  private void verifyAuthError() {
    ArgumentCaptor<Exception> exceptionArgumentCaptor = ArgumentCaptor.forClass(Exception.class);
    verify(callback, timeout(1000)).onLoadFailed(exceptionArgumentCaptor.capture());
    HttpException exception = (HttpException) exceptionArgumentCaptor.getValue();
    assertThat(exception.getStatusCode()).isEqualTo(HttpURLConnection.HTTP_FORBIDDEN);
  }

  @Test
  public void requestComplete_with200AndCancelled_callsCallbackWithNullException()
      throws Exception {
    UrlResponseInfo info = getInfo(0, 200);
    fetcher.loadData(Priority.LOW, callback);
    Callback urlCallback = urlRequestListenerCaptor.getValue();
    urlCallback.onResponseStarted(request, info);
    urlCallback.onCanceled(request, info);

    verify(callback, timeout(1000)).onLoadFailed(ArgumentMatchers.<Exception>isNull());
  }

  @Test
  public void requestComplete_with200NotCancelledMatchingLength_callsCallbackWithValidData()
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

  @Test
  public void testCancelAndImmediateRestart_withGuardActiveJobEvictionEnabled_keepsSecondRequest() {
    DataLogger dataLogger = mock(DataLogger.class);
    Options options = Options.builder().setGuardActiveJobEviction(true).build();
    ModelLoader<GlideUrl, ByteBuffer> loader =
        new ChromiumUrlLoader.ByteBufferFactory(
                cronetRequestFactory, dataLogger, /* executor= */ null, options)
            .build(mock(MultiModelLoaderFactory.class));
    ModelLoader<GlideUrl, InputStream> streamLoader =
        new ChromiumUrlLoader.StreamFactory(
                cronetRequestFactory, dataLogger, /* executor= */ null, options)
            .build(mock(MultiModelLoaderFactory.class));
    assertThat(streamLoader.handles(glideUrl)).isTrue();

    UrlRequest firstRequest = mock(UrlRequest.class);
    UrlRequest secondRequest = mock(UrlRequest.class);
    when(builder.build()).thenReturn(firstRequest, secondRequest);

    com.bumptech.glide.load.Options loadOptions = new com.bumptech.glide.load.Options();
    DataFetcher<ByteBuffer> firstFetcher =
        loader.buildLoadData(glideUrl, 10, 10, loadOptions).fetcher;
    DataFetcher<ByteBuffer> secondFetcher =
        loader.buildLoadData(glideUrl, 10, 10, loadOptions).fetcher;

    firstFetcher.loadData(Priority.HIGH, firstCallback);
    Callback firstUrlCallback = urlRequestListenerCaptor.getAllValues().get(0);

    // Cancel firstFetcher and immediately start secondFetcher for the exact same URL before
    // Cronet delivers onCanceled for firstRequest.
    firstFetcher.cancel();
    verify(firstRequest).cancel();

    secondFetcher.loadData(Priority.HIGH, secondCallback);
    verify(secondRequest).start();

    // Deliver late onCanceled for firstRequest and wait for firstJob.onRequestFinished to run.
    UrlResponseInfo canceledInfo = getInfo(100, 200);
    firstUrlCallback.onCanceled(firstRequest, canceledInfo);
    verify(dataLogger, timeout(1000))
        .logNetworkData(eq(canceledInfo), anyLong(), anyLong(), anyLong());

    // Cancelling secondFetcher must still find secondJob in jobs and cancel secondRequest.
    secondFetcher.cancel();
    verify(secondRequest).cancel();
  }

  @Test
  public void testCancelAndImmediateRestart_withDefaultOptions_leavesLegacyEvictionUnchanged() {
    DataLogger dataLogger = mock(DataLogger.class);
    Options defaultOptions = Options.builder().build();
    assertThat(defaultOptions.guardActiveJobEviction()).isFalse();
    ChromiumRequestSerializer legacySerializer =
        new ChromiumRequestSerializer(
            cronetRequestFactory, dataLogger, /* executor= */ null, defaultOptions);

    UrlRequest firstRequest = mock(UrlRequest.class);
    UrlRequest secondRequest = mock(UrlRequest.class);
    when(builder.build()).thenReturn(firstRequest, secondRequest);

    ChromiumUrlFetcher<ByteBuffer> firstFetcher =
        new ChromiumUrlFetcher<>(legacySerializer, parser, glideUrl);
    ChromiumUrlFetcher<ByteBuffer> secondFetcher =
        new ChromiumUrlFetcher<>(legacySerializer, parser, glideUrl);

    firstFetcher.loadData(Priority.HIGH, firstCallback);
    Callback firstUrlCallback = urlRequestListenerCaptor.getAllValues().get(0);

    firstFetcher.cancel();
    verify(firstRequest).cancel();

    secondFetcher.loadData(Priority.HIGH, secondCallback);
    verify(secondRequest).start();

    UrlResponseInfo canceledInfo = getInfo(100, 200);
    firstUrlCallback.onCanceled(firstRequest, canceledInfo);
    verify(dataLogger, timeout(1000))
        .logNetworkData(eq(canceledInfo), anyLong(), anyLong(), anyLong());

    secondFetcher.cancel();
    verify(secondRequest, never()).cancel();
  }

  @Test
  public void
      testStartRequest_whenJobPooledAndCancelledBeforePostStartCheck_cancelsWithoutNullPointerException()
          throws Exception {
    final AtomicReference<Thread> serializerWorker = new AtomicReference<>();
    DataLogger dataLogger =
        new DataLogger() {
          @Override
          public void logNetworkData(
              @Nullable UrlResponseInfo responseInfo,
              long startTimeMs,
              long responseStartTimeMs,
              long endTimeMs) {
            serializerWorker.set(Thread.currentThread());
          }
        };
    GlideExecutor customExecutor =
        GlideExecutor.newSourceExecutor(1, "test-serializer", UncaughtThrowableStrategy.THROW);
    final ChromiumRequestSerializer pooledSerializer =
        new ChromiumRequestSerializer(
            cronetRequestFactory,
            dataLogger,
            customExecutor,
            Options.builder().setGuardActiveJobEviction(true).build());
    final GlideUrl secondGlideUrl = new GlideUrl("http://www.example.com");
    final ChromiumUrlFetcher<ByteBuffer> firstFetcher =
        new ChromiumUrlFetcher<>(pooledSerializer, parser, glideUrl);
    final ChromiumUrlFetcher<ByteBuffer> secondFetcher =
        new ChromiumUrlFetcher<>(pooledSerializer, parser, secondGlideUrl);

    final UrlRequest firstRequest = mock(UrlRequest.class);
    final UrlRequest secondRequest = mock(UrlRequest.class);
    final UrlRequest.Builder secondBuilder = mock(UrlRequest.Builder.class);
    when(builder.build()).thenReturn(firstRequest);
    when(cronetRequestFactory.newRequest(
            eq(secondGlideUrl.toStringUrl()),
            anyInt(),
            anyHeaders(),
            urlRequestListenerCaptor.capture()))
        .thenReturn(secondBuilder);

    final CountDownLatch secondBuildEntered = new CountDownLatch(1);
    final CountDownLatch allowSecondBuildToFinish = new CountDownLatch(1);
    when(secondBuilder.build())
        .thenAnswer(
            new Answer<UrlRequest>() {
              @Override
              public UrlRequest answer(InvocationOnMock invocation) throws Throwable {
                secondBuildEntered.countDown();
                assertThat(allowSecondBuildToFinish.await(2, TimeUnit.SECONDS)).isTrue();
                return secondRequest;
              }
            });

    final AtomicReference<Thread> secondThreadRef = new AtomicReference<>();
    doAnswer(
            new Answer<Void>() {
              @Override
              public Void answer(InvocationOnMock invocation) throws Throwable {
                Callback firstJobCallback = urlRequestListenerCaptor.getAllValues().get(0);
                firstJobCallback.onCanceled(firstRequest, getInfo(0, 200));
                long deadlineNanos = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
                while (serializerWorker.get() == null
                    || serializerWorker.get().getState() != Thread.State.WAITING) {
                  assertThat(System.nanoTime() < deadlineNanos).isTrue();
                  Thread.sleep(1);
                }

                Thread secondThread =
                    new Thread(
                        new Runnable() {
                          @Override
                          public void run() {
                            secondFetcher.loadData(Priority.HIGH, secondCallback);
                          }
                        });
                secondThreadRef.set(secondThread);
                secondThread.start();
                assertThat(secondBuildEntered.await(2, TimeUnit.SECONDS)).isTrue();
                assertThat(urlRequestListenerCaptor.getAllValues().get(1))
                    .isSameInstanceAs(firstJobCallback);

                // Cancelling secondFetcher while secondBuilder.build() is still blocked sets
                // job.isCancelled = true while job.request remains null on the reused pooled Job.
                secondFetcher.cancel();
                return null;
              }
            })
        .when(firstRequest)
        .start();

    try {
      firstFetcher.loadData(Priority.HIGH, firstCallback);
      verify(firstRequest, never()).cancel();

      allowSecondBuildToFinish.countDown();
      Thread secondThread = secondThreadRef.get();
      assertThat(secondThread).isNotNull();
      secondThread.join(2000);
      assertThat(secondThread.isAlive()).isFalse();
      verify(secondRequest, times(1)).cancel();
    } finally {
      allowSecondBuildToFinish.countDown();
      Thread secondThread = secondThreadRef.get();
      if (secondThread != null) {
        secondThread.join(1000);
      }
      customExecutor.shutdownNow();
    }
  }

  @Test
  public void testDuplicateCancel_withGuardActiveJobEvictionEnabled_cancelsCronetRequestOnlyOnce() {
    ChromiumRequestSerializer guardedSerializer =
        new ChromiumRequestSerializer(
            cronetRequestFactory,
            /* dataLogger= */ null,
            /* executor= */ null,
            Options.builder().setGuardActiveJobEviction(true).build());
    ChromiumUrlFetcher<ByteBuffer> guardedFetcher =
        new ChromiumUrlFetcher<>(guardedSerializer, parser, glideUrl);

    guardedFetcher.loadData(Priority.HIGH, callback);
    guardedFetcher.cancel();
    guardedFetcher.cancel();

    verify(request, times(1)).cancel();
  }

  private static Map<String, String> anyHeaders() {
    return anyMap();
  }
}
