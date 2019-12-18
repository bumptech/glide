package com.bumptech.glide.integration.cronet;

import android.util.Log;
import androidx.annotation.NonNull;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.GlideUrl;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.chromium.net.CronetException;
import org.chromium.net.ExperimentalCronetEngine;
import org.chromium.net.UrlRequest;
import org.chromium.net.UrlRequest.Builder;
import org.chromium.net.UrlResponseInfo;

/** Fetches an {@link InputStream} using the okhttp library. */
public class CronetStreamFetcher implements DataFetcher<InputStream> {
  private static final String TAG = "CronetStreamFetcher";
  private static final String THREAD_NAME = "CronetExecutor";
  private static final int THREAD_POOL_SIZE = 32;
  private static final int MAX_QUEUE_SIZE = 32;

  private final ExperimentalCronetEngine engine;
  private final Executor executor;
  private final GlideUrl url;
  private volatile UrlRequest request;
  static final Executor DEFAULT_REQUEST_EXECUTOR = createThreadPool();

  @SuppressWarnings("unused")
  public CronetStreamFetcher(ExperimentalCronetEngine engine, GlideUrl url) {
    this(engine, url, DEFAULT_REQUEST_EXECUTOR);
  }

  public CronetStreamFetcher(ExperimentalCronetEngine engine, GlideUrl url, Executor executor) {
    this.engine = engine;
    this.url = url;
    this.executor = executor;
  }

  @Override
  public void loadData(
      @NonNull Priority priority, @NonNull DataCallback<? super InputStream> callback) {
    UrlRequest.Builder builder =
        engine.newUrlRequestBuilder(url.toString(), new CronetCallback(callback), executor);
    builder.setPriority(glideToVolleyPriority(priority)).setHttpMethod("GET");
    Map<String, String> headers = url.getHeaders();
    Set<String> keySet = headers.keySet();
    for (String key : keySet) {
      String value = headers.get(key);
      builder.addHeader(key, value);
    }

    request = builder.build();
    request.start();
  }

  @Override
  public void cleanup() {
    // Do nothing.
  }

  @Override
  public void cancel() {
    request.cancel();
  }

  @NonNull
  @Override
  public Class<InputStream> getDataClass() {
    return InputStream.class;
  }

  @NonNull
  @Override
  public DataSource getDataSource() {
    return DataSource.REMOTE;
  }

  private static int glideToVolleyPriority(@NonNull Priority priority) {
    switch (priority) {
      case LOW:
        return Builder.REQUEST_PRIORITY_LOWEST;
      case HIGH:
        return Builder.REQUEST_PRIORITY_MEDIUM;
      case IMMEDIATE:
        return Builder.REQUEST_PRIORITY_HIGHEST;
      default:
        return Builder.REQUEST_PRIORITY_LOW;
    }
  }

  private static Executor createThreadPool() {
    RejectedExecutionHandler waitToAddPolicy =
        (r, executor) ->
            Log.w(
                TAG,
                "Thread pool is full or has been shutdown, runnable["
                    + r
                    + "] is rejected by "
                    + executor);
    ThreadFactory threadFactory =
        r -> {
          SecurityManager s = System.getSecurityManager();
          ThreadGroup group =
              (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
          Thread t = new Thread(group, r, THREAD_NAME, 0);
          if (t.isDaemon()) {
            t.setDaemon(false);
          }
          if (t.getPriority() != Thread.NORM_PRIORITY) {
            t.setPriority(Thread.NORM_PRIORITY);
          }

          return t;
        };

    BlockingQueue<Runnable> taskQueue = new ArrayBlockingQueue<>(MAX_QUEUE_SIZE);
    ThreadPoolExecutor executor =
        new ThreadPoolExecutor(
            THREAD_POOL_SIZE,
            THREAD_POOL_SIZE,
            3,
            TimeUnit.MINUTES,
            taskQueue,
            threadFactory,
            waitToAddPolicy);
    executor.allowCoreThreadTimeOut(true);
    return executor;
  }

  /** UrlRequest callback, get the response body form http. */
  public static class CronetCallback extends UrlRequest.Callback {
    private final DataCallback<? super InputStream> callback;
    private ByteBuffer mByteBuffer = ByteBuffer.allocateDirect(102400);
    private ByteArrayOutputStream mBytesReceived = new ByteArrayOutputStream();
    private WritableByteChannel mReceiveChannel = Channels.newChannel(mBytesReceived);

    CronetCallback(DataCallback<? super InputStream> callback) {
      this.callback = callback;
    }

    @Override
    public void onRedirectReceived(UrlRequest urlRequest, UrlResponseInfo urlResponseInfo, String s)
        throws Exception {
      urlRequest.followRedirect();
    }

    @Override
    public void onResponseStarted(UrlRequest urlRequest, UrlResponseInfo urlResponseInfo)
        throws Exception {
      urlRequest.read(mByteBuffer);
    }

    @Override
    public void onReadCompleted(
        UrlRequest urlRequest, UrlResponseInfo urlResponseInfo, ByteBuffer byteBuffer)
        throws Exception {
      try {
        byteBuffer.flip();
        mReceiveChannel.write(byteBuffer);
        byteBuffer.clear();
      } catch (IOException e) {
        Log.e(TAG, Log.getStackTraceString(e));
      }

      urlRequest.read(mByteBuffer);
    }

    @Override
    public void onSucceeded(UrlRequest urlRequest, UrlResponseInfo urlResponseInfo) {
      if (callback != null) {
        byte[] data = mBytesReceived.toByteArray();
        callback.onDataReady(new ByteArrayInputStream(data));
      }
    }

    @Override
    public void onFailed(
        UrlRequest urlRequest, UrlResponseInfo urlResponseInfo, CronetException e) {
      if (callback != null) {
        callback.onLoadFailed(e);
      }
    }

    @Override
    public void onCanceled(UrlRequest request, UrlResponseInfo info) {}
  }
}
