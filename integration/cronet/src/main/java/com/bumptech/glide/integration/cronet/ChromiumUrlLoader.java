package com.bumptech.glide.integration.cronet;

import androidx.annotation.Nullable;
import com.bumptech.glide.Priority;
import com.bumptech.glide.Registry;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.engine.executor.GlideExecutor;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;
import com.bumptech.glide.util.ByteBufferUtil;
import com.google.common.base.Preconditions;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.PriorityQueue;

/**
 * A {@link com.bumptech.glide.load.model.ModelLoader} for loading urls using cronet.
 *
 * <p>You can optionally pass an executor to the constructor for handling cronet callbacks in {@link
 * ChromiumRequestSerializer}. If the executor is not provided, it will be created for you.
 *
 * @param <T> The type of data this loader will load.
 */
public final class ChromiumUrlLoader<T> implements ModelLoader<GlideUrl, T> {

  /** Priority-ordered limiter bounding simultaneous in-flight Cronet requests across loaders. */
  public static final class ConcurrencyLimiter {
    private final int maxConcurrentRequests;
    private final PriorityQueue<PendingJob> pendingJobs = new PriorityQueue<>();
    private int inFlightRequests;
    private long nextSequenceNumber;

    private ConcurrencyLimiter(int max) {
      Preconditions.checkArgument(max > 0);
      this.maxConcurrentRequests = max;
    }

    /** Creates a limiter allowing at most {@code maxConcurrentRequests} simultaneous requests. */
    public static ConcurrencyLimiter create(int maxConcurrentRequests) {
      return new ConcurrencyLimiter(maxConcurrentRequests);
    }

    synchronized boolean tryAcquireOrEnqueue(PendingJob job, Priority initialPriority) {
      job.priority = initialPriority;
      job.sequenceNumber = nextSequenceNumber++;
      if (inFlightRequests < maxConcurrentRequests) {
        inFlightRequests++;
        return true;
      }
      job.enqueued = true;
      pendingJobs.offer(job);
      return false;
    }

    synchronized void maybePromote(PendingJob job, Priority newPriority) {
      if (job.enqueued && newPriority.compareTo(job.priority) < 0) {
        pendingJobs.remove(job);
        job.priority = newPriority;
        pendingJobs.offer(job);
      }
    }

    synchronized boolean removePending(PendingJob job) {
      boolean wasEnqueued = job.enqueued;
      job.enqueued = false;
      return wasEnqueued && pendingJobs.remove(job);
    }

    @Nullable
    synchronized PendingJob releaseAndPollNext() {
      for (PendingJob next = pendingJobs.poll(); next != null; next = pendingJobs.poll()) {
        next.enqueued = false;
        if (!next.isCancelled()) {
          return next;
        }
      }
      inFlightRequests--;
      return null;
    }

    abstract static class PendingJob implements Comparable<PendingJob> {
      Priority priority = Priority.NORMAL;
      long sequenceNumber;
      boolean enqueued;

      @Override
      public final int compareTo(PendingJob other) {
        int diff = this.priority.compareTo(other.priority);
        return diff != 0 ? diff : Long.compare(this.sequenceNumber, other.sequenceNumber);
      }

      abstract boolean isCancelled();

      @Nullable
      abstract PendingJob onDequeuedWithSlot();
    }
  }

  /**
   * Opt-in behavioral configuration for {@link ChromiumUrlLoader} factories.
   *
   * <p>All options default to {@code false} so existing callers retain legacy behavior unless
   * explicitly enabled.
   */
  public static final class Options {
    static final Options DEFAULT = builder().build();

    private final boolean guardActiveJobEviction;
    private final boolean discardBuffersOnCancel;
    private final boolean catchBufferOutOfMemoryError;
    @Nullable private final ConcurrencyLimiter concurrencyLimiter;

    private Options(Builder builder) {
      this.guardActiveJobEviction = builder.guardActiveJobEviction;
      this.discardBuffersOnCancel = builder.discardBuffersOnCancel;
      this.catchBufferOutOfMemoryError = builder.catchBufferOutOfMemoryError;
      this.concurrencyLimiter = builder.concurrencyLimiter;
    }

    /** Creates a new {@link Builder} with all opt-in options disabled by default. */
    public static Builder builder() {
      return new Builder();
    }

    boolean guardActiveJobEviction() {
      return guardActiveJobEviction;
    }

    boolean discardBuffersOnCancel() {
      return discardBuffersOnCancel;
    }

    boolean catchBufferOutOfMemoryError() {
      return catchBufferOutOfMemoryError;
    }

    @Nullable
    ConcurrencyLimiter concurrencyLimiter() {
      return concurrencyLimiter;
    }

    /** Builder for {@link Options}. */
    public static final class Builder {
      private boolean guardActiveJobEviction;
      private boolean discardBuffersOnCancel;
      private boolean catchBufferOutOfMemoryError;
      @Nullable private ConcurrencyLimiter concurrencyLimiter;

      private Builder() {}

      /**
       * When enabled, {@link ChromiumRequestSerializer} verifies job identity before removing a URL
       * entry from its active-request map on cancellation or completion, preventing a delayed
       * callback from a cancelled request from evicting a newly started request for the same URL.
       */
      public Builder setGuardActiveJobEviction(boolean enabled) {
        this.guardActiveJobEviction = enabled;
        return this;
      }

      /** When enabled, cancelling a request eagerly discards queued response chunks. */
      @CanIgnoreReturnValue
      public Builder setDiscardBuffersOnCancel(boolean enabled) {
        this.discardBuffersOnCancel = enabled;
        return this;
      }

      /** Catches {@link BufferQueue} allocation/coalesce OOMs and fails via Glide's callback. */
      @CanIgnoreReturnValue
      public Builder setCatchBufferOutOfMemoryError(boolean enabled) {
        this.catchBufferOutOfMemoryError = enabled;
        return this;
      }

      /**
       * Creates a {@link ConcurrencyLimiter} shared by loaders built from this {@link Options}
       * instance (use {@link #setConcurrencyLimiter} to share across {@link Options} instances).
       */
      @CanIgnoreReturnValue
      public Builder setMaxConcurrentRequests(int maxConcurrentRequests) {
        this.concurrencyLimiter = ConcurrencyLimiter.create(maxConcurrentRequests);
        return this;
      }

      /** Configures a shared {@link ConcurrencyLimiter} (or {@code null} for unbounded). */
      @CanIgnoreReturnValue
      public Builder setConcurrencyLimiter(@Nullable ConcurrencyLimiter concurrencyLimiter) {
        this.concurrencyLimiter = concurrencyLimiter;
        return this;
      }

      /** Builds an immutable {@link Options} instance. */
      public Options build() {
        return new Options(this);
      }
    }
  }

  /**
   * Replaces any existing {@link ChromiumUrlLoader} registrations for {@link ByteBuffer} and {@link
   * InputStream} in {@code registry} with new factories configured with {@code options}, preserving
   * each existing loader's {@link CronetRequestFactory}, {@link DataLogger}, and executor
   * configuration.
   *
   * <p>Replaces all registered {@code (GlideUrl, ByteBuffer)} factories before {@code (GlideUrl,
   * InputStream)} factories via {@link Registry#replace}, which appends each replacement in call
   * order so {@link ByteBufferFactory} remains ahead of {@link StreamFactory}.
   */
  @CanIgnoreReturnValue
  public static boolean applyOptions(Registry registry, Options options) {
    Preconditions.checkNotNull(registry);
    Preconditions.checkNotNull(options);
    List<ModelLoader<GlideUrl, ?>> existingLoaders;
    try {
      existingLoaders = registry.getModelLoaders(new GlideUrl("https://localhost"));
    } catch (Registry.NoModelLoaderAvailableException e) {
      return false;
    }
    ChromiumUrlLoader<?> streamLoader = null;
    ChromiumUrlLoader<?> byteBufferLoader = null;
    for (ModelLoader<GlideUrl, ?> loader : existingLoaders) {
      if (loader instanceof ChromiumUrlLoader<?>) {
        ChromiumUrlLoader<?> chromiumLoader = (ChromiumUrlLoader<?>) loader;
        Class<?> dataClass = chromiumLoader.parser.getDataClass();
        if (dataClass == InputStream.class && streamLoader == null) {
          streamLoader = chromiumLoader;
        } else if (dataClass == ByteBuffer.class && byteBufferLoader == null) {
          byteBufferLoader = chromiumLoader;
        }
      }
    }
    if (byteBufferLoader != null) {
      registry.replace(
          GlideUrl.class,
          ByteBuffer.class,
          new ByteBufferFactory(
              byteBufferLoader.requestSerializer.requestFactory,
              byteBufferLoader.requestSerializer.dataLogger,
              byteBufferLoader.executor,
              options));
    }
    if (streamLoader != null) {
      registry.replace(
          GlideUrl.class,
          InputStream.class,
          new StreamFactory(
              streamLoader.requestSerializer.requestFactory,
              streamLoader.requestSerializer.dataLogger,
              streamLoader.executor,
              options));
    }
    return streamLoader != null || byteBufferLoader != null;
  }

  private final ChromiumRequestSerializer requestSerializer;
  private final ByteBufferParser<T> parser;
  @Nullable private final GlideExecutor executor;

  ChromiumUrlLoader(CronetRequestFactory requestFactory, ByteBufferParser<T> parser) {
    this(parser, requestFactory, null /*dataLogger*/);
  }

  ChromiumUrlLoader(
      ByteBufferParser<T> parser,
      CronetRequestFactory requestFactory,
      @Nullable DataLogger dataLogger) {
    this(parser, requestFactory, dataLogger, /* executor= */ null, Options.DEFAULT);
  }

  ChromiumUrlLoader(
      ByteBufferParser<T> parser,
      CronetRequestFactory requestFactory,
      @Nullable DataLogger dataLogger,
      @Nullable GlideExecutor executor) {
    this(parser, requestFactory, dataLogger, executor, Options.DEFAULT);
  }

  ChromiumUrlLoader(
      ByteBufferParser<T> parser,
      CronetRequestFactory requestFactory,
      @Nullable DataLogger dataLogger,
      @Nullable GlideExecutor executor,
      Options options) {
    this.parser = parser;
    this.executor = executor;
    requestSerializer =
        new ChromiumRequestSerializer(
            requestFactory, dataLogger, executor, Preconditions.checkNotNull(options));
  }

  @Override
  public LoadData<T> buildLoadData(
      GlideUrl glideUrl, int width, int height, com.bumptech.glide.load.Options options) {
    DataFetcher<T> fetcher = new ChromiumUrlFetcher<>(requestSerializer, parser, glideUrl);
    return new LoadData<>(glideUrl, fetcher);
  }

  @Override
  public boolean handles(GlideUrl glideUrl) {
    return true;
  }

  /** Loads {@link InputStream}s for {@link GlideUrl}s using cronet. */
  public static final class StreamFactory
      implements ModelLoaderFactory<GlideUrl, InputStream>, ByteBufferParser<InputStream> {

    private final CronetRequestFactory requestFactory;
    @Nullable private final DataLogger dataLogger;
    @Nullable private final GlideExecutor executor;
    private final Options options;

    public StreamFactory(CronetRequestFactory requestFactory, @Nullable DataLogger dataLogger) {
      this(requestFactory, dataLogger, /* executor= */ null, Options.DEFAULT);
    }

    /**
     * @param executor See {@link ChromiumUrlLoader} for details.
     */
    public StreamFactory(
        CronetRequestFactory requestFactory,
        @Nullable DataLogger dataLogger,
        @Nullable GlideExecutor executor) {
      this(requestFactory, dataLogger, executor, Options.DEFAULT);
    }

    /**
     * @param executor See {@link ChromiumUrlLoader} for details.
     * @param options Opt-in behavioral options for request serialization.
     */
    public StreamFactory(
        CronetRequestFactory requestFactory,
        @Nullable DataLogger dataLogger,
        @Nullable GlideExecutor executor,
        Options options) {
      this.requestFactory = requestFactory;
      this.dataLogger = dataLogger;
      this.executor = executor;
      this.options = Preconditions.checkNotNull(options);
    }

    @Override
    public ModelLoader<GlideUrl, InputStream> build(MultiModelLoaderFactory multiFactory) {
      return new ChromiumUrlLoader<>(
          /* parser= */ this, requestFactory, dataLogger, executor, options);
    }

    @Override
    public void teardown() {}

    @Override
    public InputStream parse(ByteBuffer byteBuffer) {
      return ByteBufferUtil.toStream(byteBuffer);
    }

    @Override
    public Class<InputStream> getDataClass() {
      return InputStream.class;
    }
  }

  /** Loads {@link ByteBuffer}s for {@link GlideUrl}s using cronet. */
  public static final class ByteBufferFactory
      implements ModelLoaderFactory<GlideUrl, ByteBuffer>, ByteBufferParser<ByteBuffer> {

    private final CronetRequestFactory requestFactory;
    @Nullable private final DataLogger dataLogger;
    @Nullable private final GlideExecutor executor;
    private final Options options;

    public ByteBufferFactory(CronetRequestFactory requestFactory, @Nullable DataLogger dataLogger) {
      this(requestFactory, dataLogger, /* executor= */ null, Options.DEFAULT);
    }

    /**
     * @param executor See {@link ChromiumUrlLoader} for details.
     */
    public ByteBufferFactory(
        CronetRequestFactory requestFactory,
        @Nullable DataLogger dataLogger,
        @Nullable GlideExecutor executor) {
      this(requestFactory, dataLogger, executor, Options.DEFAULT);
    }

    /**
     * @param executor See {@link ChromiumUrlLoader} for details.
     * @param options Opt-in behavioral options for request serialization.
     */
    public ByteBufferFactory(
        CronetRequestFactory requestFactory,
        @Nullable DataLogger dataLogger,
        @Nullable GlideExecutor executor,
        Options options) {
      this.requestFactory = requestFactory;
      this.dataLogger = dataLogger;
      this.executor = executor;
      this.options = Preconditions.checkNotNull(options);
    }

    @Override
    public ModelLoader<GlideUrl, ByteBuffer> build(MultiModelLoaderFactory multiFactory) {
      return new ChromiumUrlLoader<>(
          /* parser= */ this, requestFactory, dataLogger, executor, options);
    }

    @Override
    public void teardown() {
      // Do nothing.
    }

    @Override
    public ByteBuffer parse(ByteBuffer byteBuffer) {
      return byteBuffer;
    }

    @Override
    public Class<ByteBuffer> getDataClass() {
      return ByteBuffer.class;
    }
  }
}
