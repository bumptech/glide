package com.bumptech.glide.load.model;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.signature.EmptySignature;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * A base class to convert byte arrays to input streams so they can be decoded. This class is
 * abstract because there is no simple/quick way to generate an id from the bytes themselves, so
 * subclass must include an id.
 *
 * @param <Data> The type of data that will be loaded from a given byte array.
 */
public class ByteArrayLoader<Data> implements ModelLoader<byte[], Data> {
  private final Converter<Data> converter;

  public ByteArrayLoader(Converter<Data> converter) {
    this.converter = converter;
  }

  @Override
  public LoadData<Data> buildLoadData(byte[] model, int width, int height,
      Options options) {
    // TODO: compare the actual bytes?
    return new LoadData<>(EmptySignature.obtain(), new Fetcher<>(model, converter));
  }

  @Override
  public boolean handles(byte[] model) {
    return true;
  }

  /**
   * Converts between a byte array a desired model class.
   * @param <Data> The type of data to convert to.
   */
  public interface Converter<Data> {
    Data convert(byte[] model);
    Class<Data> getDataClass();
  }

  private static class Fetcher<Data> implements DataFetcher<Data> {
    private final byte[] model;
    private final Converter<Data> converter;

    public Fetcher(byte[] model, Converter<Data> converter) {
      this.model = model;
      this.converter = converter;
    }

    @Override
    public void loadData(Priority priority, DataCallback<? super Data> callback) {
      Data result = converter.convert(model);
      callback.onDataReady(result);
    }

    @Override
    public void cleanup() {
      // Do nothing.
    }

    @Override
    public void cancel() {
      // Do nothing.
    }

    @Override
    public Class<Data> getDataClass() {
      return converter.getDataClass();
    }

    @Override
    public DataSource getDataSource() {
      return DataSource.LOCAL;
    }
  }

  /**
   * Factory for {@link com.bumptech.glide.load.model.ByteArrayLoader} and
   * {@link java.nio.ByteBuffer}.
   */
  public static class ByteBufferFactory implements ModelLoaderFactory<byte[], ByteBuffer> {

    @Override
    public ModelLoader<byte[], ByteBuffer> build(MultiModelLoaderFactory multiFactory) {
      return new ByteArrayLoader<>(new Converter<ByteBuffer>() {
        @Override
        public ByteBuffer convert(byte[] model) {
          return ByteBuffer.wrap(model);
        }

        @Override
        public Class<ByteBuffer> getDataClass() {
          return ByteBuffer.class;
        }
      });
    }

    @Override
    public void teardown() {
      // Do nothing.
    }
  }

  /**
   * Factory for {@link ByteArrayLoader} and {@link java.io.InputStream}.
   */
  public static class StreamFactory implements ModelLoaderFactory<byte[], InputStream> {

    @Override
    public ModelLoader<byte[], InputStream> build(MultiModelLoaderFactory multiFactory) {
      return new ByteArrayLoader<>(new Converter<InputStream>() {
        @Override
        public InputStream convert(byte[] model) {
          return new ByteArrayInputStream(model);
        }

        @Override
        public Class<InputStream> getDataClass() {
          return InputStream.class;
        }
      });
    }

    @Override
    public void teardown() {
      // Do nothing.
    }
  }
}
