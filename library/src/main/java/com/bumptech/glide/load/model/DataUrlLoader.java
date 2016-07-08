package com.bumptech.glide.load.model;

import android.util.Base64;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.signature.ObjectKey;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * A simple model loader for loading data from a Data URL String.
 *
 * Data URIs use the "data" scheme.
 *
 * <p>See http://www.ietf.org/rfc/rfc2397.txt  for a complete description of the 'data' URL scheme.
 *
 * <p>Briefly, a 'data' URL has the form: <pre>data:[mediatype][;base64],some_data</pre>
 *
 * @param <Data> The type of data that can be opened.
 */
public final class DataUrlLoader<Data> implements ModelLoader<String, Data> {

  private static final String DATA_SCHEME_IMAGE = "data:image";
  private static final String BASE64_TAG = ";base64";
  private final DataDecoder<Data> dataDecoder;

  public DataUrlLoader(DataDecoder<Data> dataDecoder) {
    this.dataDecoder = dataDecoder;
  }

  @Override
  public LoadData<Data> buildLoadData(String model, int width, int height, Options options) {
    return new LoadData<>(new ObjectKey(model), new DataUriFetcher<Data>(model, dataDecoder));
  }

  @Override
  public boolean handles(String url) {
    return url.startsWith(DATA_SCHEME_IMAGE);
  }

  /**
   * Allows decoding a specific type of data from a Data URL String.
   *
   * @param <Data> The type of data that can be opened.
   */
  public interface DataDecoder<Data> {

    Data decode(String uri) throws IllegalArgumentException;

    void close(Data data) throws IOException;

    Class<Data> getDataClass();
  }

  private static final class DataUriFetcher<Data> implements DataFetcher<Data> {

    private final String dataUri;
    private final DataDecoder<Data> reader;
    private Data data;

    public DataUriFetcher(String dataUri, DataDecoder<Data> reader) {
      this.dataUri = dataUri;
      this.reader = reader;
    }

    @Override
    public void loadData(Priority priority, DataCallback<? super Data> callback) {
      try {
        data = reader.decode(dataUri);
        callback.onDataReady(data);
      } catch (IllegalArgumentException e) {
        callback.onLoadFailed(e);
      }
    }

    @Override
    public void cleanup() {
      try {
        reader.close(data);
      } catch (IOException e) {
        // Ignored.
      }
    }

    @Override
    public void cancel() {
      // Do nothing.
    }

    @Override
    public Class<Data> getDataClass() {
      return reader.getDataClass();
    }

    @Override
    public DataSource getDataSource() {
      return DataSource.LOCAL;
    }
  }

  /**
   * Factory for loading {@link InputStream} from Data URL string.
   */
  public static final class StreamFactory implements ModelLoaderFactory<String, InputStream> {

    private final DataDecoder<InputStream> opener;

    public StreamFactory() {
      opener = new DataDecoder<InputStream>() {
        @Override
        public InputStream decode(String url) {
          if (!url.startsWith(DATA_SCHEME_IMAGE)) {
            throw new IllegalArgumentException("Not a valid image data URL.");
          }

          int commaIndex = url.indexOf(',');
          if (commaIndex == -1) {
            throw new IllegalArgumentException("Missing comma in data URL.");
          }

          String beforeComma = url.substring(0, commaIndex);
          if (!beforeComma.endsWith(BASE64_TAG)) {
            throw new IllegalArgumentException("Not a base64 image data URL.");
          }

          String afterComma = url.substring(commaIndex + 1);
          byte[] bytes = Base64.decode(afterComma, Base64.DEFAULT);

          return new ByteArrayInputStream(bytes);
        }

        @Override
        public void close(InputStream inputStream) throws IOException {
          inputStream.close();
        }

        @Override
        public Class<InputStream> getDataClass() {
          return InputStream.class;
        }
      };
    }

    @Override
    public final ModelLoader<String, InputStream> build(MultiModelLoaderFactory multiFactory) {
      return new DataUrlLoader<>(opener);
    }

    @Override
    public final void teardown() {
      // Do nothing.
    }
  }
}
