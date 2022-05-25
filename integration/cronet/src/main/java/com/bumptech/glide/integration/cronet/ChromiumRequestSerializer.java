package com.bumptech.glide.integration.cronet;

import android.util.Log;
import androidx.annotation.Nullable;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.HttpException;
import com.bumptech.glide.load.engine.executor.GlideExecutor;
import com.bumptech.glide.load.engine.executor.GlideExecutor.UncaughtThrowableStrategy;
import com.bumptech.glide.load.model.GlideUrl;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import org.chromium.net.CronetException;
import org.chromium.net.UrlRequest;
import org.chromium.net.UrlRequest.Callback;
import org.chromium.net.UrlResponseInfo;

/**
 * Ensures that two simultaneous requests for exactly the same url make only a single http request.
 *
 * <p>Requests are started by Glide on multiple threads in a thread pool. An arbitrary number of
 * threads may attempt to start or cancel requests for one or more urls at once. Our goal is to
 * ensure:
 * <li>
 *
 *     <ul>
 *       A new request is made to cronet if a url is requested and no cronet request for that url is
 *       in progress
 * </ul>
 *
 * <ul>
 *   Subsequent requests for in progress urls do not make new requests to cronet, but are notified
 *   when the existing cronet request completes.
 * </ul>
 *
 * <ul>
 *   Cancelling a single request does not cancel the cronet request if multiple requests for the url
 *   have been made, but cancelling all requests for a url does cancel the cronet request.
 * </ul>
 */
final class ChromiumRequestSerializer {
  private static final String TAG = "ChromiumSerializer";

  private static final Map<Priority, Integer> GLIDE_TO_CHROMIUM_PRIORITY =
      new EnumMap<>(Priority.class);
  // Memoized so that all callers can share an instance.
  // Suppliers.memoize() is thread safe. See google3/java/com/google/common/base/Suppliers.java
  private static final Supplier<Executor> GLIDE_EXECUTOR_SUPPLIER =
      Suppliers.memoize(
          new Supplier<Executor>() {
            @Override
            public GlideExecutor get() {
              // Allow network operations, but use a single thread. See b/37684357.
              return GlideExecutor.newSourceExecutor(
                  1 /*threadCount*/, "chromium-serializer", UncaughtThrowableStrategy.DEFAULT);
            }
          });

  private abstract static class PriorityRunnable implements Runnable, Comparable<PriorityRunnable> {

    private final int priority;

    private PriorityRunnable(Priority priority) {
      this.priority = priority.ordinal();
    }

    @Override
    public final int compareTo(PriorityRunnable another) {
      if (another.priority > this.priority) {
        return -1;
      } else if (another.priority < this.priority) {
        return 1;
      }
      return 0;
    }
  }

  static {
    GLIDE_TO_CHROMIUM_PRIORITY.put(Priority.IMMEDIATE, UrlRequest.Builder.REQUEST_PRIORITY_HIGHEST);
    GLIDE_TO_CHROMIUM_PRIORITY.put(Priority.HIGH, UrlRequest.Builder.REQUEST_PRIORITY_MEDIUM);
    GLIDE_TO_CHROMIUM_PRIORITY.put(Priority.NORMAL, UrlRequest.Builder.REQUEST_PRIORITY_LOW);
    GLIDE_TO_CHROMIUM_PRIORITY.put(Priority.LOW, UrlRequest.Builder.REQUEST_PRIORITY_LOWEST);
  }

  private final JobPool jobPool = new JobPool();
  private final Map<GlideUrl, Job> jobs = new HashMap<>();
  private final CronetRequestFactory requestFactory;
  @Nullable private final DataLogger dataLogger;

  ChromiumRequestSerializer(CronetRequestFactory requestFactory, @Nullable DataLogger dataLogger) {
    this.requestFactory = requestFactory;
    this.dataLogger = dataLogger;
  }

  void startRequest(Priority priority, GlideUrl glideUrl, Listener listener) {
    boolean startNewRequest = false;
    Job job;
    synchronized (this) {
      job = jobs.get(glideUrl);
      if (job == null) {
        startNewRequest = true;
        job = jobPool.get(glideUrl);
        jobs.put(glideUrl, job);
      }
      job.addListener(listener);
    }

    if (startNewRequest) {
      if (Log.isLoggable(TAG, Log.VERBOSE)) {
        Log.v(TAG, "Fetching image url using cronet" + " url: " + glideUrl);
      }
      job.priority = priority;
      job.request =
          requestFactory
              .newRequest(
                  glideUrl.toStringUrl(),
                  GLIDE_TO_CHROMIUM_PRIORITY.get(priority),
                  glideUrl.getHeaders(),
                  job)
              .build();
      job.request.start();

      // It's possible we will be cancelled between adding the job to the job list and starting the
      // corresponding request. We don't want to hold a lock while starting the request, because
      // starting the request may block for a while and we need cancellation to happen quickly (it
      // happens on the main thread).
      if (job.isCancelled) {
        job.request.cancel();
      }
    }
  }

  void cancelRequest(GlideUrl glideUrl, Listener listener) {
    final Job job;
    synchronized (this) {
      job = jobs.get(glideUrl);
    }
    // Jobs may be cancelled before they are started.
    if (job != null) {
      job.removeListener(listener);
    }
  }

  private static IOException getExceptionIfFailed(
      UrlResponseInfo info, IOException e, boolean wasCancelled) {
    if (wasCancelled) {
      return null;
    } else if (e != null) {
      return e;
    } else if (info.getHttpStatusCode() != HttpURLConnection.HTTP_OK) {
      return new HttpException(info.getHttpStatusCode());
    }
    return null;
  }

  /**
   * Manages a single cronet request for a single url with one or more active listeners.
   *
   * <p>Cronet requests are cancelled when all listeners are removed.
   */
  private class Job extends Callback {
    private final List<Listener> listeners = new ArrayList<>(2);

    private GlideUrl glideUrl;
    private Priority priority;
    private long startTime;
    private UrlRequest request;
    private long endTimeMs;
    private long responseStartTimeMs;
    private volatile boolean isCancelled;
    private BufferQueue.Builder builder;

    void init(GlideUrl glideUrl) {
      startTime = System.currentTimeMillis();
      this.glideUrl = glideUrl;
    }

    void addListener(Listener listener) {
      synchronized (ChromiumRequestSerializer.this) {
        listeners.add(listener);
      }
    }

    void removeListener(Listener listener) {
      synchronized (ChromiumRequestSerializer.this) {
        // Note: multiple cancellation calls + a subsequent request for a url may mean we fail to
        // remove the listener here because that listener is actually for a previous request. Since
        // that race is harmless, we simply ignore it.
        listeners.remove(listener);
        if (listeners.isEmpty()) {
          isCancelled = true;
          jobs.remove(glideUrl);
        }
      }

      // The request may not have started yet, so request may be null.
      if (isCancelled) {
        UrlRequest localRequest = request;
        if (localRequest != null) {
          localRequest.cancel();
        }
      }
    }

    @Override
    public void onRedirectReceived(UrlRequest urlRequest, UrlResponseInfo urlResponseInfo, String s)
        throws Exception {
      urlRequest.followRedirect();
    }

    @Override
    public void onResponseStarted(UrlRequest request, UrlResponseInfo info) {
      responseStartTimeMs = System.currentTimeMillis();
      builder = BufferQueue.builder();
      request.read(builder.getFirstBuffer(info));
    }

    @Override
    public void onReadCompleted(
        UrlRequest urlRequest, UrlResponseInfo urlResponseInfo, ByteBuffer byteBuffer)
        throws Exception {
      request.read(builder.getNextBuffer(byteBuffer));
    }

    @Override
    public void onSucceeded(UrlRequest request, final UrlResponseInfo info) {
      GLIDE_EXECUTOR_SUPPLIER
          .get()
          .execute(
              new PriorityRunnable(priority) {
                @Override
                public void run() {
                  onRequestFinished(
                      info,
                      null /*exception*/,
                      false /*wasCancelled*/,
                      builder.build().coalesceToBuffer());
                }
              });
    }

    @Override
    public void onFailed(
        UrlRequest urlRequest, final UrlResponseInfo urlResponseInfo, final CronetException e) {
      GLIDE_EXECUTOR_SUPPLIER
          .get()
          .execute(
              new PriorityRunnable(priority) {
                @Override
                public void run() {
                  onRequestFinished(urlResponseInfo, e, false /*wasCancelled*/, null /*buffer*/);
                }
              });
    }

    @Override
    public void onCanceled(UrlRequest urlRequest, @Nullable final UrlResponseInfo urlResponseInfo) {
      GLIDE_EXECUTOR_SUPPLIER
          .get()
          .execute(
              new PriorityRunnable(priority) {
                @Override
                public void run() {
                  onRequestFinished(
                      urlResponseInfo, null /*exception*/, true /*wasCancelled*/, null /*buffer*/);
                }
              });
    }

    private void onRequestFinished(
        UrlResponseInfo info,
        @Nullable CronetException e,
        boolean wasCancelled,
        ByteBuffer buffer) {
      synchronized (ChromiumRequestSerializer.this) {
        jobs.remove(glideUrl);
      }

      Exception exception = getExceptionIfFailed(info, e, wasCancelled);
      boolean isSuccess = exception == null && !wasCancelled;

      endTimeMs = System.currentTimeMillis();

      maybeLogResult(isSuccess, exception, wasCancelled, buffer);
      if (isSuccess) {
        notifySuccess(buffer);
      } else {
        notifyFailure(exception);
      }

      if (dataLogger != null) {
        dataLogger.logNetworkData(info, startTime, responseStartTimeMs, endTimeMs);
      }
      builder = null;

      jobPool.put(this);
    }

    private void notifySuccess(ByteBuffer buffer) {
      ByteBuffer toNotify = buffer;
      /* Locking here isn't necessary and is potentially dangerous. There's an optimization in
       * Glide that avoids re-posting results if the callback onRequestComplete triggers is called
       * on the calling thread. If that were ever to happen here (the request is cached in memory?),
       * this might block all requests for a while. Locking isn't necessary because the Job is
       * removed from the serializer's job set at the beginning of onRequestFinished. After that
       * point, whatever thread we're on is the only one that has access to the Job. Subsequent
       * requests for the same image would trigger an additional RPC/Job. */
      for (int i = 0, size = listeners.size(); i < size; i++) {
        Listener listener = listeners.get(i);
        listener.onRequestComplete(toNotify);
        toNotify = (ByteBuffer) toNotify.asReadOnlyBuffer().position(0);
      }
    }

    private void notifyFailure(Exception exception) {
      /* Locking here isn't necessary and is potentially dangerous. There's an optimization in
       * Glide that avoids re-posting results if the callback onRequestComplete triggers is called
       * on the calling thread. If that were ever to happen here (the request is cached in memory?),
       * this might block all requests for a while. Locking isn't necessary because the Job is
       * removed from the serializer's job set at the beginning of onRequestFinished. After that
       * point, whatever thread we're on is the only one that has access to the Job. Subsequent
       * requests for the same image would trigger an additional RPC/Job. */
      for (int i = 0, size = listeners.size(); i < size; i++) {
        Listener listener = listeners.get(i);
        listener.onRequestFailed(exception);
      }
    }

    private void maybeLogResult(
        boolean isSuccess, Exception exception, boolean wasCancelled, ByteBuffer buffer) {
      if (isSuccess && Log.isLoggable(TAG, Log.VERBOSE)) {
        Log.v(
            TAG,
            "Successfully completed request"
                + ", url: "
                + glideUrl
                + ", duration: "
                + (System.currentTimeMillis() - startTime)
                + ", file size: "
                + (buffer.limit() / 1024)
                + "kb");
      } else if (!isSuccess && Log.isLoggable(TAG, Log.ERROR) && !wasCancelled) {
        Log.e(TAG, "Request failed", exception);
      }
    }

    private void clearListeners() {
      synchronized (ChromiumRequestSerializer.this) {
        listeners.clear();
        request = null;
        isCancelled = false;
      }
    }
  }

  private class JobPool {
    private static final int MAX_POOL_SIZE = 50;
    private final ArrayDeque<Job> pool = new ArrayDeque<>();

    public synchronized Job get(GlideUrl glideUrl) {
      Job job = pool.poll();
      if (job == null) {
        job = new Job();
      }
      job.init(glideUrl);
      return job;
    }

    public void put(Job job) {
      job.clearListeners();
      synchronized (this) {
        if (pool.size() < MAX_POOL_SIZE) {
          pool.offer(job);
        }
      }
    }
  }

  interface Listener {
    void onRequestComplete(ByteBuffer byteBuffer);

    void onRequestFailed(@Nullable Exception e);
  }
}
