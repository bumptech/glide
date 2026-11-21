package com.bumptech.glide.integration.cronet;

import android.util.Log;
import androidx.annotation.GuardedBy;
import androidx.annotation.Nullable;
import com.bumptech.glide.Priority;
import com.bumptech.glide.integration.cronet.ChromiumUrlLoader.Options;
import com.bumptech.glide.load.HttpException;
import com.bumptech.glide.load.engine.executor.GlideExecutor;
import com.bumptech.glide.load.engine.executor.GlideExecutor.UncaughtThrowableStrategy;
import com.bumptech.glide.load.model.GlideUrl;
import com.google.common.base.Preconditions;
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
 *     </ul>
 *
 *     <ul>
 *       Subsequent requests for in progress urls do not make new requests to cronet, but are
 *       notified when the existing cronet request completes.
 *     </ul>
 *
 *     <ul>
 *       Cancelling a single request does not cancel the cronet request if multiple requests for the
 *       url have been made, but cancelling all requests for a url does cancel the cronet request.
 *     </ul>
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

  private final JobPool jobPool;

  @GuardedBy("this")
  private final Map<GlideUrl, Job> jobs = new HashMap<>();

  private final CronetRequestFactory requestFactory;
  @Nullable private final DataLogger dataLogger;
  private final Options options;
  private final BufferQueue.BufferAllocator bufferAllocator;

  ChromiumRequestSerializer(
      CronetRequestFactory requestFactory,
      @Nullable DataLogger dataLogger,
      @Nullable final GlideExecutor executor) {
    this(requestFactory, dataLogger, executor, Options.DEFAULT);
  }

  ChromiumRequestSerializer(
      CronetRequestFactory requestFactory,
      @Nullable DataLogger dataLogger,
      @Nullable final GlideExecutor executor,
      Options options) {
    this(requestFactory, dataLogger, executor, options, ByteBuffer::allocateDirect);
  }

  ChromiumRequestSerializer(
      CronetRequestFactory requestFactory,
      @Nullable DataLogger dataLogger,
      @Nullable final GlideExecutor executor,
      Options options,
      BufferQueue.BufferAllocator bufferAllocator) {
    this.requestFactory = requestFactory;
    this.dataLogger = dataLogger;
    this.options = Preconditions.checkNotNull(options);
    this.bufferAllocator = Preconditions.checkNotNull(bufferAllocator);
    if (executor == null) {
      this.jobPool = new JobPool(GLIDE_EXECUTOR_SUPPLIER);
    } else {
      this.jobPool =
          new JobPool(
              new Supplier<Executor>() {
                @Override
                public Executor get() {
                  return executor;
                }
              });
    }
  }

  private boolean guardJobIdentity() {
    return options.guardActiveJobEviction()
        || options.discardBuffersOnCancel()
        || options.catchBufferOutOfMemoryError();
  }

  void startRequest(Priority priority, GlideUrl glideUrl, Listener listener) {
    if (options.discardBuffersOnCancel()) {
      Preconditions.checkNotNull(priority);
    }
    boolean startNewRequest = false;
    Job job;
    final int jobGeneration;
    synchronized (this) {
      job = jobs.get(glideUrl);
      if (job == null) {
        startNewRequest = true;
        boolean unpooled = options.discardBuffersOnCancel();
        job = unpooled ? jobPool.createUnpooled(glideUrl) : jobPool.get(glideUrl);
        jobs.put(glideUrl, job);
      }
      jobGeneration = job.addListenerAndGetGeneration(listener);
    }

    if (startNewRequest) {
      if (Log.isLoggable(TAG, Log.VERBOSE)) {
        Log.v(TAG, "Fetching image url using cronet" + " url: " + glideUrl);
      }
      job.priority = priority;
      final UrlRequest cronetRequest;
      if (options.catchBufferOutOfMemoryError()) {
        try {
          cronetRequest = buildAndStartCronetRequest(job);
        } catch (RuntimeException | OutOfMemoryError error) {
          job.onStartFailed(error);
          return;
        }
      } else {
        cronetRequest =
            requestFactory
                .newRequest(
                    glideUrl.toStringUrl(),
                    GLIDE_TO_CHROMIUM_PRIORITY.get(priority),
                    glideUrl.getHeaders(),
                    job)
                .build();
        cronetRequest.start();
      }

      // It's possible we will be cancelled between adding the job to the job list and starting the
      // corresponding request. We don't want to hold a lock while starting the request, because
      // starting the request may block for a while and we need cancellation to happen quickly (it
      // happens on the main thread). Publish cronetRequest or detect pre-start cancellation under
      // ChromiumRequestSerializer.this, checking jobGeneration in case start() already finished and
      // returned this Job to jobPool.
      if (job.publishRequestOrShouldCancel(jobGeneration, cronetRequest)) {
        if (options.discardBuffersOnCancel()) {
          job.discardBuilder();
        }
        cronetRequest.cancel();
      }
    }
  }

  private UrlRequest buildAndStartCronetRequest(Job job) {
    UrlRequest cronetRequest =
        requestFactory
            .newRequest(
                job.glideUrl.toStringUrl(),
                GLIDE_TO_CHROMIUM_PRIORITY.get(job.priority),
                job.glideUrl.getHeaders(),
                job)
            .build();
    cronetRequest.start();
    return cronetRequest;
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

    // Incremented in clearListeners() each time this Job is returned to JobPool for reuse so
    // startRequest() can detect if a fast-finishing request already recycled this Job.
    @GuardedBy("ChromiumRequestSerializer.this")
    private int generation;

    @GuardedBy("ChromiumRequestSerializer.this")
    @Nullable
    private UrlRequest request;

    private long endTimeMs;
    private long responseStartTimeMs;

    private volatile boolean isCancelled;
    private volatile IOException readFailureException;

    @GuardedBy("ChromiumRequestSerializer.this")
    private boolean isFinished;

    private BufferQueue.Builder builder;

    @GuardedBy("this")
    @Nullable
    private BufferQueue.DiscardableBuilder discardableBuilder;

    private final Supplier<Executor> executorSupplier;

    Job(Supplier<Executor> executorSupplier) {
      this.executorSupplier = executorSupplier;
    }

    void init(GlideUrl glideUrl) {
      startTime = System.currentTimeMillis();
      this.glideUrl = glideUrl;
    }

    int addListenerAndGetGeneration(Listener listener) {
      synchronized (ChromiumRequestSerializer.this) {
        listeners.add(listener);
        return generation;
      }
    }

    boolean publishRequestOrShouldCancel(int expectedGeneration, UrlRequest cronetRequest) {
      synchronized (ChromiumRequestSerializer.this) {
        if (isFinished) {
          return false;
        }
        if (guardJobIdentity() && generation != expectedGeneration) {
          // generation only increments in clearListeners() when onRequestFinished returns this Job
          // to jobPool:
          // 1. cronetRequest has already completed, failed, or been cancelled, so we no longer need
          //    to retain a reference to it.
          // 2. We must not publish cronetRequest into this.request (which now belongs to the pooled
          //    or reused Job) or return true to cancel it again.
          return false;
        }
        if (isCancelled) {
          return true;
        }
        request = cronetRequest;
        return false;
      }
    }

    void onStartFailed(Throwable cause) {
      List<Listener> toNotify = null;
      synchronized (ChromiumRequestSerializer.this) {
        if (!isFinished && !isCancelled) {
          isFinished = true;
          if (jobs.get(glideUrl) == this) {
            jobs.remove(glideUrl);
          }
          toNotify = new ArrayList<>(listeners);
        }
      }
      Exception err = cause instanceof Exception ? (Exception) cause : new IOException(cause);
      for (int i = 0; toNotify != null && i < toNotify.size(); i++) {
        toNotify.get(i).onRequestFailed(err);
      }
    }

    private void recordReadFailureAndCancel(UrlRequest req, String msg, Throwable cause) {
      if (!isCancelled) {
        readFailureException = new IOException(msg, cause);
      }
      builder = null;
      if (options.discardBuffersOnCancel()) {
        discardBuilder();
      }
      req.cancel();
    }

    synchronized void discardBuilder() {
      if (discardableBuilder != null) {
        discardableBuilder.discard();
        discardableBuilder = null;
      }
    }

    void removeListener(Listener listener) {
      UrlRequest requestToCancel = null;
      synchronized (ChromiumRequestSerializer.this) {
        // Note: multiple cancellation calls + a subsequent request for a url may mean we fail to
        // remove the listener here because that listener is actually for a previous request. Since
        // that race is harmless, we simply ignore it.
        if (options.discardBuffersOnCancel() && (isFinished || isCancelled)) {
          return;
        }
        if (options.catchBufferOutOfMemoryError() && isFinished) {
          return;
        }
        boolean removed = listeners.remove(listener);
        if (guardJobIdentity() && !removed) {
          return;
        }
        if (listeners.isEmpty()) {
          isCancelled = true;
          requestToCancel = request;
          request = null;
          // Only remove the entry if it still maps to this Job (see onRequestFinished).
          if (!guardJobIdentity() || jobs.get(glideUrl) == this) {
            jobs.remove(glideUrl);
          }
        }
      }

      if (options.discardBuffersOnCancel() && isCancelled) {
        discardBuilder();
      }
      // The request may not have started yet, so requestToCancel may be null.
      if (requestToCancel != null) {
        requestToCancel.cancel();
      }
    }

    @Override
    public void onRedirectReceived(UrlRequest urlRequest, UrlResponseInfo urlResponseInfo, String s)
        throws Exception {
      if (options.discardBuffersOnCancel() && isCancelled) {
        discardBuilder();
        urlRequest.cancel();
        return;
      }
      urlRequest.followRedirect();
    }

    @Override
    public void onResponseStarted(UrlRequest request, UrlResponseInfo info) {
      responseStartTimeMs = System.currentTimeMillis();
      if (options.discardBuffersOnCancel()) {
        if (shouldDiscardOnCancel()) {
          request.cancel();
          return;
        }
        BufferQueue.DiscardableBuilder newBuilder = BufferQueue.discardableBuilder(bufferAllocator);
        final ByteBuffer firstBuffer;
        if (options.catchBufferOutOfMemoryError()) {
          try {
            firstBuffer = newBuilder.getFirstBuffer(info);
          } catch (RuntimeException | OutOfMemoryError error) {
            newBuilder.discard();
            recordReadFailureAndCancel(request, "Initial buffer alloc failed", error);
            return;
          }
        } else {
          firstBuffer = newBuilder.getFirstBuffer(info);
        }
        synchronized (this) {
          discardableBuilder = newBuilder;
        }
        if (shouldDiscardOnCancel()) {
          discardBuilder();
          request.cancel();
          return;
        }
        request.read(firstBuffer);
        return;
      }
      if (options.catchBufferOutOfMemoryError()) {
        BufferQueue.Builder newBuilder = BufferQueue.builder(bufferAllocator);
        final ByteBuffer firstBuffer;
        try {
          firstBuffer = newBuilder.getFirstBuffer(info);
        } catch (RuntimeException | OutOfMemoryError error) {
          recordReadFailureAndCancel(request, "Initial buffer alloc failed", error);
          return;
        }
        builder = newBuilder;
        request.read(firstBuffer);
        return;
      }
      builder = BufferQueue.builder(bufferAllocator);
      request.read(builder.getFirstBuffer(info));
    }

    @Override
    public void onReadCompleted(
        UrlRequest urlRequest, UrlResponseInfo urlResponseInfo, ByteBuffer byteBuffer)
        throws Exception {
      if (options.discardBuffersOnCancel()) {
        BufferQueue.DiscardableBuilder localBuilder;
        synchronized (this) {
          localBuilder = shouldDiscardOnCancel() ? null : discardableBuilder;
        }
        try {
          ByteBuffer next = localBuilder == null ? null : localBuilder.getNextBuffer(byteBuffer);
          if (next != null && !shouldDiscardOnCancel()) {
            urlRequest.read(next);
            return;
          }
        } catch (IllegalStateException e) {
          if (!shouldDiscardOnCancel()) {
            if (options.catchBufferOutOfMemoryError()) {
              recordReadFailureAndCancel(urlRequest, "Chunk buffer alloc failed", e);
              return;
            }
            throw e;
          }
        } catch (RuntimeException | OutOfMemoryError error) {
          if (!options.catchBufferOutOfMemoryError()) {
            throw error;
          }
          recordReadFailureAndCancel(urlRequest, "Chunk buffer alloc failed", error);
          return;
        }
        discardBuilder();
        urlRequest.cancel();
        return;
      }
      if (options.catchBufferOutOfMemoryError()) {
        final ByteBuffer next;
        try {
          next = builder == null ? null : builder.getNextBuffer(byteBuffer);
        } catch (RuntimeException | OutOfMemoryError error) {
          recordReadFailureAndCancel(urlRequest, "Chunk buffer alloc failed", error);
          return;
        }
        if (next != null) {
          urlRequest.read(next);
        }
        return;
      }
      urlRequest.read(builder.getNextBuffer(byteBuffer));
    }

    boolean shouldDiscardOnCancel() {
      return options.discardBuffersOnCancel() && isCancelled;
    }

    private Priority completionPriority(boolean expedite) {
      return options.discardBuffersOnCancel() && expedite ? Priority.IMMEDIATE : priority;
    }

    @Nullable
    private synchronized BufferQueue.DiscardableBuilder claimBuilderForSucceeded() {
      BufferQueue.DiscardableBuilder b = discardableBuilder;
      discardableBuilder = null;
      if (shouldDiscardOnCancel() && b != null) {
        b.discard();
        return null;
      }
      return b;
    }

    private void finishCoalesce(BufferQueue queue, UrlResponseInfo info) {
      if (!options.catchBufferOutOfMemoryError()) {
        onRequestFinished(info, null, shouldDiscardOnCancel(), queue.coalesceToBuffer());
        return;
      }
      final ByteBuffer coalesced;
      try {
        coalesced = queue.coalesceToBuffer();
      } catch (RuntimeException | OutOfMemoryError t) {
        onRequestFinished(info, new IOException("Coalesce failed", t), isCancelled, null);
        return;
      }
      onRequestFinished(info, null, shouldDiscardOnCancel(), coalesced);
    }

    private void runSucceeded(UrlResponseInfo info) {
      if (options.discardBuffersOnCancel()) {
        BufferQueue.DiscardableBuilder localBuilder = claimBuilderForSucceeded();
        if (localBuilder == null) {
          IOException failure = options.catchBufferOutOfMemoryError() ? readFailureException : null;
          onRequestFinished(info, failure, failure == null || isCancelled, null);
          return;
        }
        finishCoalesce(localBuilder.build(), info);
        return;
      }
      if (options.catchBufferOutOfMemoryError()) {
        if (builder == null) {
          IOException failure = readFailureException;
          onRequestFinished(info, failure, failure == null || isCancelled, null);
          return;
        }
        finishCoalesce(builder.build(), info);
        return;
      }
      onRequestFinished(info, null, false, builder.build().coalesceToBuffer());
    }

    @Override
    public void onSucceeded(UrlRequest request, final UrlResponseInfo info) {
      if (shouldDiscardOnCancel()) {
        discardBuilder();
      }
      executorSupplier
          .get()
          .execute(
              new PriorityRunnable(completionPriority(isCancelled)) {
                @Override
                public void run() {
                  runSucceeded(info);
                }
              });
    }

    @Override
    public void onFailed(
        UrlRequest urlRequest, final UrlResponseInfo urlResponseInfo, final CronetException e) {
      if (options.discardBuffersOnCancel()) {
        discardBuilder();
      }
      executorSupplier
          .get()
          .execute(
              new PriorityRunnable(completionPriority(true)) {
                @Override
                public void run() {
                  onRequestFinished(urlResponseInfo, e, shouldDiscardOnCancel(), null);
                }
              });
    }

    @Override
    public void onCanceled(UrlRequest urlRequest, @Nullable final UrlResponseInfo urlResponseInfo) {
      if (options.discardBuffersOnCancel()) {
        discardBuilder();
      }
      executorSupplier
          .get()
          .execute(
              new PriorityRunnable(completionPriority(true)) {
                @Override
                public void run() {
                  if (options.catchBufferOutOfMemoryError()) {
                    IOException failure = readFailureException;
                    onRequestFinished(
                        urlResponseInfo, failure, failure == null || isCancelled, null);
                    return;
                  }
                  onRequestFinished(urlResponseInfo, null, true, null);
                }
              });
    }

    private void onRequestFinished(
        UrlResponseInfo info, @Nullable IOException e, boolean wasCancelled, ByteBuffer buffer) {
      synchronized (ChromiumRequestSerializer.this) {
        if (options.discardBuffersOnCancel() || options.catchBufferOutOfMemoryError()) {
          isFinished = true;
          request = null;
        }
        // When a Job is cancelled, removeListener() removes glideUrl from jobs immediately on the
        // caller thread, while Cronet invokes onCanceled() (or a racing completion callback)
        // asynchronously later. If glideUrl was requested again in that window, jobs.get(glideUrl)
        // already points to the newer active Job and must not be evicted by this finishing Job.
        if (!guardJobIdentity() || jobs.get(glideUrl) == this) {
          jobs.remove(glideUrl);
        }
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
      if (options.discardBuffersOnCancel()) {
        discardBuilder();
        return;
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
      } else if (!isSuccess && Log.isLoggable(TAG, Log.WARN) && !wasCancelled) {
        Log.w(TAG, "Request failed, url: " + glideUrl, exception);
      }
    }

    /**
     * Resets state when {@link JobPool#put(Job)} returns this {@link Job} to the pool for reuse.
     */
    private void clearListeners() {
      synchronized (ChromiumRequestSerializer.this) {
        generation++;
        listeners.clear();
        request = null;
        isCancelled = false;
        if (options.catchBufferOutOfMemoryError()) {
          isFinished = false;
          readFailureException = null;
        }
      }
    }
  }

  private class JobPool {
    private static final int MAX_POOL_SIZE = 50;
    private final ArrayDeque<Job> pool = new ArrayDeque<>();
    private final Supplier<Executor> executorSupplier;

    public JobPool(Supplier<Executor> executorSupplier) {
      this.executorSupplier = executorSupplier;
    }

    Job createUnpooled(GlideUrl glideUrl) {
      Job job = new Job(executorSupplier);
      job.init(glideUrl);
      return job;
    }

    public synchronized Job get(GlideUrl glideUrl) {
      Job job = pool.poll();
      if (job == null) {
        job = new Job(executorSupplier);
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
