package com.bumptech.glide.load.model;

import android.util.Base64;
import androidx.annotation.NonNull;
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
 * <p>Data URIs use the "data" scheme.
 *
 * <p>See http://www.ietf.org/rfc/rfc2397.txt for a complete description of the 'data' URL scheme.
 *
 * <p>Briefly, a 'data' URL has the form:
 *
 * <pre>data:[mediatype][;base64],some_data</pre>
 *
 * @param <Model> The type of Model that we can retrieve data for, e.g. {@link String}.
 * @param <Data> The type of data that can be opened, e.g. {@link InputStream}.
 */
public final class DataUrlLoader<Model, Data> implements ModelLoader<Model, Data> {

  private static final String DATA_SCHEME_IMAGE = "data:image";
  private static final String BASE64_TAG = ";base64";
  private final DataDecoder<Data> dataDecoder;

  // Public API.
  @SuppressWarnings("WeakerAccess")
  public DataUrlLoader(DataDecoder<Data> dataDecoder) {
    this.dataDecoder = dataDecoder;
  }

  @Override
  public LoadData<Data> buildLoadData(
      @NonNull Model model, int width, int height, @NonNull Options options) {
    return new LoadData<>(
        new ObjectKey(model), new DataUriFetcher<>(model.toString(), dataDecoder));
  }

  @Override
  public boolean handles(@NonNull Model model) {
    // We expect Model to be a Uri or a String, both of which implement toString() efficiently. We
    // should reconsider this implementation before adding any new Model types.
    return model.toString().startsWith(DATA_SCHEME_IMAGE);
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

    DataUriFetcher(String dataUri, DataDecoder<Data> reader) {
      this.dataUri = dataUri;
      this.reader = reader;
    }

    @Override
    public void loadData(@NonNull Priority priority, @NonNull DataCallback<? super Data> callback) {
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

    @NonNull
    @Override
    public Class<Data> getDataClass() {
      return reader.getDataClass();
    }

    @NonNull
    @Override
    public DataSource getDataSource() {
      return DataSource.LOCAL;
    }
  }

  /**
   * Factory for loading {@link InputStream}s from data uris.
   *
   * @param <Model> The type of Model we can obtain data for, e.g. String.
   */
  public static final class StreamFactory<Model> implements ModelLoaderFactory<Model, InputStream> {

    private final DataDecoder<InputStream> opener;

    public StreamFactory() {
      opener =
          new DataDecoder<InputStream>() {
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

    @NonNull
    @Override
    public ModelLoader<Model, InputStream> build(@NonNull MultiModelLoaderFactory multiFactory) {
      return new DataUrlLoader<>(opener);
    }

    @Override
    public void teardown() {
      // Do nothing.
    }
  }
}
