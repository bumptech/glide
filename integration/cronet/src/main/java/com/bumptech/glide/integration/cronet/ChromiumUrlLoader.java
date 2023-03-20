package com.bumptech.glide.integration.cronet;

import androidx.annotation.Nullable;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.engine.executor.GlideExecutor;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;
import com.bumptech.glide.util.ByteBufferUtil;
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
  private final ChromiumRequestSerializer requestSerializer;
  private final ByteBufferParser<T> parser;

  ChromiumUrlLoader(CronetRequestFactory requestFactory, ByteBufferParser<T> parser) {
    this(parser, requestFactory, null /*dataLogger*/);
  }

  ChromiumUrlLoader(
      ByteBufferParser<T> parser,
      CronetRequestFactory requestFactory,
      @Nullable DataLogger dataLogger) {
    this.parser = parser;
    requestSerializer =
        new ChromiumRequestSerializer(requestFactory, dataLogger, /* executor= */ null);
  }

  ChromiumUrlLoader(
      ByteBufferParser<T> parser,
      CronetRequestFactory requestFactory,
      @Nullable DataLogger dataLogger,
      @Nullable GlideExecutor executor) {
    this.parser = parser;
    requestSerializer = new ChromiumRequestSerializer(requestFactory, dataLogger, executor);
  }

  @Override
  public LoadData<T> buildLoadData(GlideUrl glideUrl, int width, int height, Options options) {
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

    private CronetRequestFactory requestFactory;
    @Nullable private final DataLogger dataLogger;
    @Nullable private final GlideExecutor executor;

    public StreamFactory(CronetRequestFactory requestFactory, @Nullable DataLogger dataLogger) {
      this.requestFactory = requestFactory;
      this.dataLogger = dataLogger;
      this.executor = null;
    }

    /**
     * @param executor See {@link ChromiumUrlLoader} for details.
     */
    public StreamFactory(
        CronetRequestFactory requestFactory,
        @Nullable DataLogger dataLogger,
        @Nullable GlideExecutor executor) {
      this.requestFactory = requestFactory;
      this.dataLogger = dataLogger;
      this.executor = executor;
    }

    @Override
    public ModelLoader<GlideUrl, InputStream> build(MultiModelLoaderFactory multiFactory) {
      return new ChromiumUrlLoader<>(/* parser= */ this, requestFactory, dataLogger, executor);
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

    private CronetRequestFactory requestFactory;
    @Nullable private final DataLogger dataLogger;
    @Nullable private final GlideExecutor executor;

    public ByteBufferFactory(CronetRequestFactory requestFactory, @Nullable DataLogger dataLogger) {
      this.requestFactory = requestFactory;
      this.dataLogger = dataLogger;
      this.executor = null;
    }

    /**
     * @param executor See {@link ChromiumUrlLoader} for details.
     */
    public ByteBufferFactory(
        CronetRequestFactory requestFactory,
        @Nullable DataLogger dataLogger,
        @Nullable GlideExecutor executor) {
      this.requestFactory = requestFactory;
      this.dataLogger = dataLogger;
      this.executor = executor;
    }

    @Override
    public ModelLoader<GlideUrl, ByteBuffer> build(MultiModelLoaderFactory multiFactory) {
      return new ChromiumUrlLoader<>(/* parser= */ this, requestFactory, dataLogger, executor);
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
