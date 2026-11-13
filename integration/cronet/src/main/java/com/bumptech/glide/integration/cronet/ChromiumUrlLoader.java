package com.bumptech.glide.integration.cronet;

import androidx.annotation.Nullable;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.engine.executor.GlideExecutor;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;
import com.bumptech.glide.util.ByteBufferUtil;
import com.google.common.base.Preconditions;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * A {@link com.bumptech.glide.load.model.ModelLoader} for loading urls using cronet.
 *
 * <p>You can optionally pass an executor to the constructor for handling cronet callbacks in {@link
 * ChromiumRequestSerializer}. If the executor is not provided, it will be created for you.
 *
 * @param <T> The type of data this loader will load.
 */
public final class ChromiumUrlLoader<T> implements ModelLoader<GlideUrl, T> {

  /**
   * Opt-in behavioral configuration for {@link ChromiumUrlLoader} factories.
   *
   * <p>All options default to {@code false} so existing callers retain legacy behavior unless
   * explicitly enabled.
   */
  public static final class Options {
    static final Options DEFAULT = builder().build();

    private final boolean guardActiveJobEviction;

    private Options(Builder builder) {
      this.guardActiveJobEviction = builder.guardActiveJobEviction;
    }

    /** Creates a new {@link Builder} with all opt-in options disabled by default. */
    public static Builder builder() {
      return new Builder();
    }

    boolean guardActiveJobEviction() {
      return guardActiveJobEviction;
    }

    /** Builder for {@link Options}. */
    public static final class Builder {
      private boolean guardActiveJobEviction;

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

      /** Builds an immutable {@link Options} instance. */
      public Options build() {
        return new Options(this);
      }
    }
  }

  private final ChromiumRequestSerializer requestSerializer;
  private final ByteBufferParser<T> parser;

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
