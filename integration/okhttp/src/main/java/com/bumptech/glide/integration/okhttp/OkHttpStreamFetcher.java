package com.bumptech.glide.integration.okhttp;

import android.util.Log;

import com.bumptech.glide.Logs;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.GlideUrl;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.io.InputStream;

/**
 * Fetches an {@link InputStream} using the okhttp library.
 */
public class OkHttpStreamFetcher implements DataFetcher<InputStream> {
  private final OkHttpClient client;
  private final GlideUrl url;
  private InputStream stream;

  public OkHttpStreamFetcher(OkHttpClient client, GlideUrl url) {
    this.client = client;
    this.url = url;
  }

  @Override
  public void loadData(Priority priority, final DataCallback<? super InputStream> callback)
      throws IOException {
    Request request = new Request.Builder().url(url.toString()).build();

    client.newCall(request).enqueue(new com.squareup.okhttp.Callback() {
      @Override
      public void onFailure(Request request, IOException e) {
        if (Logs.isEnabled(Log.DEBUG)) {
          Logs.log(Log.DEBUG, "OkHttp failed to obtain result", e);
        }
        callback.onDataReady(null);
      }

      @Override
      public void onResponse(Response response) throws IOException {
        stream = response.body().byteStream();
        callback.onDataReady(stream);
      }
    });
  }

  @Override
  public void cleanup() {
    if (stream == null) {
      return;
    }
    try {
      stream.close();
    } catch (IOException e) {
      // Ignored
    }
  }

  @Override
  public String getId() {
    return url.toString();
  }

  @Override
  public void cancel() {
    // TODO: call cancel on the client when this method is called on a background thread. See #257
  }

  @Override
  public Class<InputStream> getDataClass() {
    return InputStream.class;
  }

  @Override
  public DataSource getDataSource() {
    return DataSource.REMOTE;
  }
}
