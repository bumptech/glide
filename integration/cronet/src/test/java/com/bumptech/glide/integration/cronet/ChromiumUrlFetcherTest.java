package com.bumptech.glide.integration.cronet;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.util.concurrent.Uninterruptibles.awaitUninterruptibly;
import static org.junit.Assert.assertThrows;
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
import static org.mockito.Mockito.spy;
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
import com.google.common.util.concurrent.Uninterruptibles;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.ByteBuffer;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;
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

  @Test
  public void discardableBuilder_buildAfterDiscard_throwsIllegalStateException() {
    BufferQueue.DiscardableBuilder builder = BufferQueue.discardableBuilder();
    builder.discard();
    assertThrows(IllegalStateException.class, builder::build);
  }

  @Test
  public void discardableBuilder_getNextBufferAfterDiscard_throwsIllegalStateException() {
    BufferQueue.DiscardableBuilder builder = BufferQueue.discardableBuilder();
    builder.discard();
    ByteBuffer lastBuffer = ByteBuffer.allocateDirect(1);
    assertThrows(IllegalStateException.class, () -> builder.getNextBuffer(lastBuffer));
  }

  @Test
  public void discardableBuilder_getNextBufferAfterBuild_throwsIllegalStateExceptionWithCause() {
    BufferQueue.DiscardableBuilder builder = BufferQueue.discardableBuilder();
    BufferQueue unused = builder.build();
    ByteBuffer lastBuffer = ByteBuffer.allocateDirect(1);
    IllegalStateException thrown =
        assertThrows(IllegalStateException.class, () -> builder.getNextBuffer(lastBuffer));
    assertThat(thrown).hasCauseThat().isNotNull();
  }

  @Test
  public void discardableBuilder_discardDuringChunkAllocation_doesNotBlockCaller()
      throws Exception {
    CountDownLatch entered = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    BufferQueue.DiscardableBuilder builder =
        BufferQueue.discardableBuilder(capacity -> {
          entered.countDown();
          assertThat(awaitUninterruptibly(release, 5, TimeUnit.SECONDS)).isTrue();
          return ByteBuffer.allocateDirect(capacity);
        });
    ByteBuffer fullChunk = ByteBuffer.allocate(0);
    FutureTask<IllegalStateException> nextBufferTask =
        new FutureTask<>(() ->
            assertThrows(IllegalStateException.class, () -> builder.getNextBuffer(fullChunk)));
    new Thread(nextBufferTask).start();
    assertThat(entered.await(2, TimeUnit.SECONDS)).isTrue();
    builder.discard();
    release.countDown();
    assertThat(nextBufferTask.get(2, TimeUnit.SECONDS)).isNotNull();
  }

  @Test
  public void cancelWithDiscardEnabled_discardsLateReadsAndPreservesNewerJob() throws Exception {
    DataLogger logger = mock(DataLogger.class);
    Options opts = Options.builder().setDiscardBuffersOnCancel(true).build();
    ChromiumRequestSerializer ser =
        new ChromiumRequestSerializer(cronetRequestFactory, logger, null, opts);
    DataFetcher<ByteBuffer> first = new ChromiumUrlFetcher<>(ser, parser, glideUrl);
    first.loadData(Priority.HIGH, callback);
    Callback cb = urlRequestListenerCaptor.getValue();
    cb.onResponseStarted(request, getInfo(8, 200));
    first.cancel();
    cb.onReadCompleted(request, getInfo(8, 200), ByteBuffer.allocate(8));
    verify(request).read(any(ByteBuffer.class));
    first.loadData(Priority.HIGH, secondCallback);
    cb.onCanceled(request, getInfo(8, 200));
    verify(logger, timeout(1000)).logNetworkData(any(), anyLong(), anyLong(), anyLong());
    first.cancel();
    verify(request, times(3)).cancel();
  }

  private static GlideExecutor singleThreadExecutor() {
    return GlideExecutor.newSourceBuilder()
        .setThreadCount(1)
        .setUncaughtThrowableStrategy(UncaughtThrowableStrategy.THROW)
        .build();
  }

  private static ByteBuffer directBufferAtPosition(int capacity, int position) {
    ByteBuffer buffer = ByteBuffer.allocateDirect(capacity);
    buffer.position(position);
    return buffer;
  }

  private int limitAfterQueuedCancel(Options opts) throws Exception {
    CountDownLatch gate = new CountDownLatch(1);
    GlideExecutor exec = singleThreadExecutor();
    exec.execute(() -> Uninterruptibles.awaitUninterruptibly(gate));
    ByteBuffer chunk2 = directBufferAtPosition(15, 10);
    try {
      ChromiumRequestSerializer ser =
          new ChromiumRequestSerializer(cronetRequestFactory, null, exec, opts);
      ChromiumUrlFetcher<ByteBuffer> lowFetcher = new ChromiumUrlFetcher<>(ser, parser, glideUrl);
      lowFetcher.loadData(Priority.LOW, callback);
      Callback cb = urlRequestListenerCaptor.getValue();
      UrlResponseInfo info = getInfo(20, 200);
      cb.onResponseStarted(request, info);
      cb.onReadCompleted(request, info, directBufferAtPosition(20, 20));
      cb.onReadCompleted(request, info, chunk2);
      cb.onSucceeded(request, info);
      lowFetcher.cancel();
      gate.countDown();
      exec.shutdown();
      assertThat(exec.awaitTermination(2, TimeUnit.SECONDS)).isTrue();
      return chunk2.limit();
    } finally {
      gate.countDown();
      exec.shutdownNow();
    }
  }

  @Test
  public void onSucceeded_whenCancelledBeforeSerializerRuns_skipsCoalesceOnlyWhenEnabled()
      throws Exception {
    assertThat(limitAfterQueuedCancel(Options.builder().setDiscardBuffersOnCancel(true).build()))
        .isEqualTo(15);
    assertThat(limitAfterQueuedCancel(Options.builder().build())).isEqualTo(10);
  }

  @Test
  public void onFailed_afterCancel_withDiscardEnabled_treatsAsCancelled() throws Exception {
    UrlResponseInfo normalInfo = getInfo(8, 200);
    for (boolean enabled : new boolean[] {true, false}) {
      CountDownLatch started = new CountDownLatch(1);
      CountDownLatch gate = new CountDownLatch(1);
      GlideExecutor exec = singleThreadExecutor();
      exec.execute(() -> {
        started.countDown();
        Uninterruptibles.awaitUninterruptibly(gate);
      });
      AtomicReference<UrlResponseInfo> firstLogged = new AtomicReference<>();
      DataLogger logger = (info, start, resp, end) -> firstLogged.compareAndSet(null, info);
      Options opts = Options.builder().setDiscardBuffersOnCancel(enabled).build();
      ChromiumRequestSerializer ser =
          new ChromiumRequestSerializer(cronetRequestFactory, logger, exec, opts);
      UrlResponseInfo failedInfo = spy(getInfo(20, HttpURLConnection.HTTP_INTERNAL_ERROR));
      try {
        assertThat(started.await(2, TimeUnit.SECONDS)).isTrue();
        int base = urlRequestListenerCaptor.getAllValues().size();
        new ChromiumUrlFetcher<>(ser, parser, new GlideUrl("http://a"))
            .loadData(Priority.NORMAL, firstCallback);
        succeed(
            normalInfo, urlRequestListenerCaptor.getAllValues().get(base), ByteBuffer.allocate(8));
        ChromiumUrlFetcher<ByteBuffer> lowFetcher =
            new ChromiumUrlFetcher<>(ser, parser, new GlideUrl("http://b"));
        lowFetcher.loadData(Priority.LOW, callback);
        Callback lowCb = urlRequestListenerCaptor.getAllValues().get(base + 1);
        lowCb.onResponseStarted(request, failedInfo);
        lowFetcher.cancel();
        lowCb.onFailed(request, failedInfo, /* error= */ null);
        gate.countDown();
        exec.shutdown();
        assertThat(exec.awaitTermination(2, TimeUnit.SECONDS)).isTrue();
      } finally {
        gate.countDown();
        exec.shutdownNow();
      }
      assertThat(firstLogged.get()).isSameInstanceAs(enabled ? failedInfo : normalInfo);
      verify(failedInfo, times(enabled ? 0 : 2)).getHttpStatusCode();
    }
    verify(callback, never()).onLoadFailed(any(Exception.class));
  }

  @Test
  public void onSucceeded_whenCoalesceThrowsOom_guardsOnlyWhenCatchEnabled() throws Exception {
    BufferQueue.BufferAllocator fail30 = mock(BufferQueue.BufferAllocator.class);
    when(fail30.allocate(anyInt())).thenAnswer(i -> ByteBuffer.allocateDirect(i.getArgument(0)));
    when(fail30.allocate(30)).thenThrow(new OutOfMemoryError("boom"));
    AtomicReference<Throwable> uncaught = new AtomicReference<>();
    CountDownLatch latch = new CountDownLatch(1);
    UncaughtThrowableStrategy trap =
        err -> {
          uncaught.set(err);
          latch.countDown();
        };
    GlideExecutor offExec =
        GlideExecutor.newSourceBuilder()
            .setThreadCount(1)
            .setUncaughtThrowableStrategy(trap)
            .build();
    UrlResponseInfo info = getInfo(20, 200);
    Options onOpts = Options.builder().setCatchBufferOutOfMemoryError(true).build();
    ChromiumRequestSerializer offSer =
        new ChromiumRequestSerializer(cronetRequestFactory, null, offExec, Options.DEFAULT, fail30);
    ChromiumRequestSerializer onSer =
        new ChromiumRequestSerializer(cronetRequestFactory, null, null, onOpts, fail30);
    ChromiumUrlFetcher<ByteBuffer> offFirst = new ChromiumUrlFetcher<>(offSer, parser, glideUrl);
    try {
      for (int idx = 0; idx < 2; idx++) {
        ChromiumUrlFetcher<ByteBuffer> f =
            idx == 0 ? offFirst : new ChromiumUrlFetcher<>(onSer, parser, glideUrl);
        f.loadData(Priority.HIGH, idx == 0 ? firstCallback : callback);
        Callback cb = urlRequestListenerCaptor.getAllValues().get(idx);
        cb.onResponseStarted(request, info);
        cb.onReadCompleted(request, info, directBufferAtPosition(20, 20));
        cb.onReadCompleted(request, info, directBufferAtPosition(10, 10));
        cb.onSucceeded(request, info);
      }
      assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
      assertThat(uncaught.get()).isInstanceOf(OutOfMemoryError.class);
      ChromiumUrlFetcher<ByteBuffer> offSecond = new ChromiumUrlFetcher<>(offSer, parser, glideUrl);
      offSecond.loadData(Priority.HIGH, secondCallback);
      verify(request, times(2)).start();
      offFirst.cancel();
      offSecond.cancel();
      verify(request).cancel();
    } finally {
      offExec.shutdownNow();
    }
    verify(callback, timeout(1000)).onLoadFailed(isA(IOException.class));
    new ChromiumUrlFetcher<>(onSer, parser, glideUrl).loadData(Priority.HIGH, secondCallback);
    verify(request, times(3)).start();
  }

  @Test
  @SuppressWarnings("unchecked") // Raw DataCallback mock.
  public void startResponseAndRead_whenOomThrown_guardsOnlyWhenEnabledAndCleansJobs()
      throws Exception {
    OutOfMemoryError oom = new OutOfMemoryError("simulated OOM");
    UrlResponseInfo info = getInfo(20, 200);
    when(builder.build()).thenThrow(oom).thenReturn(request);
    assertThrows(OutOfMemoryError.class, () -> fetcher.loadData(Priority.HIGH, firstCallback));
    new ChromiumUrlFetcher<>(serializer, parser, glideUrl).loadData(Priority.HIGH, secondCallback);
    verify(cronetRequestFactory).newRequest(any(), anyInt(), any(), any());
    verify(request, never()).start();
    succeed(info, urlRequestListenerCaptor.getAllValues().get(0), ByteBuffer.allocateDirect(20));
    verify(firstCallback, timeout(1000)).onDataReady(any());
    verify(secondCallback, timeout(1000)).onDataReady(any());

    BufferQueue.BufferAllocator alloc = mock(BufferQueue.BufferAllocator.class);
    ByteBuffer buf20 = ByteBuffer.allocateDirect(20);
    when(alloc.allocate(anyInt()))
        .thenThrow(oom)
        .thenReturn(buf20)
        .thenThrow(oom)
        .thenReturn(buf20);
    ByteBuffer chunk = directBufferAtPosition(20, 20);
    ChromiumRequestSerializer defaultSer =
        new ChromiumRequestSerializer(cronetRequestFactory, null, null, Options.DEFAULT, alloc);
    new ChromiumUrlFetcher<>(defaultSer, parser, glideUrl)
        .loadData(Priority.HIGH, mock(DataCallback.class));
    Callback defaultCb = urlRequestListenerCaptor.getAllValues().get(1);
    assertThrows(OutOfMemoryError.class, () -> defaultCb.onResponseStarted(request, info));
    defaultCb.onResponseStarted(request, info);
    assertThrows(OutOfMemoryError.class, () -> defaultCb.onReadCompleted(request, info, chunk));
    new ChromiumUrlFetcher<>(defaultSer, parser, glideUrl)
        .loadData(Priority.HIGH, mock(DataCallback.class));
    verify(request).start();

    Options onOpts = Options.builder().setCatchBufferOutOfMemoryError(true).build();
    assertThat(limitAfterQueuedCancel(onOpts)).isEqualTo(10);
    when(builder.build()).thenThrow(oom).thenReturn(request);
    when(alloc.allocate(anyInt())).thenThrow(oom).thenReturn(buf20).thenThrow(oom);
    ChromiumRequestSerializer guarded =
        new ChromiumRequestSerializer(cronetRequestFactory, null, null, onOpts, alloc);
    for (int step : new int[] {0, 1, 2}) {
      DataCallback<ByteBuffer> loadCb = mock(DataCallback.class);
      new ChromiumUrlFetcher<>(guarded, parser, glideUrl).loadData(Priority.HIGH, loadCb);
      if (step > 0) {
        Callback cronetCb = urlRequestListenerCaptor.getAllValues().get(step + 3);
        cronetCb.onResponseStarted(request, info);
        if (step == 2) {
          chunk.position(20);
          cronetCb.onReadCompleted(request, info, chunk);
        }
        verify(request, times(step + 1)).cancel();
        cronetCb.onCanceled(request, info);
      }
      ArgumentCaptor<Exception> err = ArgumentCaptor.forClass(Exception.class);
      verify(loadCb, timeout(1000)).onLoadFailed(err.capture());
      assertThat((IOException) err.getValue()).hasCauseThat().isEqualTo(oom);
    }
    verify(request, times(4)).start();
  }

  @Test
  @SuppressWarnings("unchecked") // Raw DataCallback mock.
  public void catchOomAloneAndWithDiscard_preservesJobIdentityAndGuardsDiscardableBuilder()
      throws Exception {
    OutOfMemoryError oom = new OutOfMemoryError("simulated OOM");
    UrlResponseInfo info = getInfo(20, 200);
    BufferQueue.BufferAllocator alloc = mock(BufferQueue.BufferAllocator.class);
    when(alloc.allocate(anyInt()))
        .thenThrow(oom)
        .thenReturn(ByteBuffer.allocateDirect(20), ByteBuffer.allocateDirect(20));
    UrlRequest req1 = mock(UrlRequest.class);
    UrlRequest req2 = mock(UrlRequest.class);
    UrlRequest req3 = mock(UrlRequest.class);
    when(builder.build()).thenReturn(req1, req2, req3, request);

    DataLogger logger = mock(DataLogger.class);
    Options catchOnly = Options.builder().setCatchBufferOutOfMemoryError(true).build();
    ChromiumRequestSerializer catchSer =
        new ChromiumRequestSerializer(cronetRequestFactory, logger, null, catchOnly, alloc);
    DataFetcher<ByteBuffer> first = new ChromiumUrlFetcher<>(catchSer, parser, glideUrl);
    first.loadData(Priority.HIGH, mock(DataCallback.class));
    Callback cronetCb1 = urlRequestListenerCaptor.getAllValues().get(0);
    cronetCb1.onResponseStarted(req1, info);
    verify(req1).cancel();
    first.cancel();

    DataFetcher<ByteBuffer> second = new ChromiumUrlFetcher<>(catchSer, parser, glideUrl);
    second.loadData(Priority.HIGH, mock(DataCallback.class));
    Callback cronetCb2 = urlRequestListenerCaptor.getAllValues().get(1);
    cronetCb1.onCanceled(req1, info);
    verify(logger, timeout(1000)).logNetworkData(any(), anyLong(), anyLong(), anyLong());
    first.cancel();
    second.cancel();
    verify(req2).cancel();
    cronetCb2.onCanceled(req2, info);
    verify(logger, timeout(1000).times(2)).logNetworkData(any(), anyLong(), anyLong(), anyLong());
    new ChromiumUrlFetcher<>(catchSer, parser, new GlideUrl("http://reused"))
        .loadData(Priority.HIGH, mock(DataCallback.class));
    verify(req3).start();
    verify(req3, never()).cancel();

    Options bothOpts =
        Options.builder()
            .setDiscardBuffersOnCancel(true)
            .setCatchBufferOutOfMemoryError(true)
            .build();
    String[] expectedMessages = {
      "Initial buffer alloc failed", "Chunk buffer alloc failed", "Coalesce failed",
    };
    when(alloc.allocate(anyInt()))
        .thenThrow(oom)
        .thenReturn(ByteBuffer.allocateDirect(20))
        .thenThrow(oom)
        .thenReturn(ByteBuffer.allocateDirect(20), ByteBuffer.allocateDirect(15))
        .thenThrow(oom);
    ChromiumRequestSerializer bothSer =
        new ChromiumRequestSerializer(cronetRequestFactory, null, null, bothOpts, alloc);
    for (int step : new int[] {0, 1, 2}) {
      DataCallback<ByteBuffer> loadCb = mock(DataCallback.class);
      new ChromiumUrlFetcher<>(bothSer, parser, new GlideUrl("http://both/" + step))
          .loadData(Priority.HIGH, loadCb);
      Callback cb = urlRequestListenerCaptor.getAllValues().get(step + 3);
      cb.onResponseStarted(request, info);
      if (step == 0) {
        cb.onCanceled(request, info);
      } else if (step == 1) {
        cb.onReadCompleted(request, info, directBufferAtPosition(20, 20));
        cb.onCanceled(request, info);
      } else {
        cb.onReadCompleted(request, info, directBufferAtPosition(20, 20));
        cb.onReadCompleted(request, info, directBufferAtPosition(15, 10));
        cb.onSucceeded(request, info);
      }
      ArgumentCaptor<Exception> err = ArgumentCaptor.forClass(Exception.class);
      verify(loadCb, timeout(1000)).onLoadFailed(err.capture());
      assertThat(err.getValue()).hasMessageThat().isEqualTo(expectedMessages[step]);
      assertThat((IOException) err.getValue()).hasCauseThat().isEqualTo(oom);
    }
  }

  private static Map<String, String> anyHeaders() {
    return anyMap();
  }
}
