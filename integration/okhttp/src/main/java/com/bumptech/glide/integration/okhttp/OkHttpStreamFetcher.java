package com.bumptech.glide.integration.okhttp;

import android.util.Log;

import com.bumptech.glide.Logs;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.util.ContentLengthInputStream;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Fetches an {@link InputStream} using the okhttp library.
 */
public class OkHttpStreamFetcher implements DataFetcher<InputStream> {
  private static final String CONTENT_LENGTH_HEADER = "Content-Length";
  private final OkHttpClient client;
  private final GlideUrl url;
  private InputStream stream;
  private ResponseBody responseBody;

  public OkHttpStreamFetcher(OkHttpClient client, GlideUrl url) {
    this.client = client;
    this.url = url;
  }

  @Override
  public void loadData(Priority priority, final DataCallback<? super InputStream> callback) {
    Request.Builder requestBuilder = new Request.Builder().url(url.toStringUrl());
    for (Map.Entry<String, String> headerEntry : url.getHeaders().entrySet()) {
      requestBuilder.addHeader(headerEntry.getKey(), headerEntry.getValue());
    }
    Request request = requestBuilder.build();

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
        if (response.isSuccessful()) {
          String contentLength = response.header(CONTENT_LENGTH_HEADER);
          responseBody = response.body();
          stream = ContentLengthInputStream.obtain(responseBody.byteStream(), contentLength);
        } else if (Logs.isEnabled(Log.DEBUG)) {
          Logs.log(Log.DEBUG, "OkHttp got error response: " + response.code() + ", "
              + response.message());
        }
        callback.onDataReady(stream);
      }
    });
  }

  @Override
  public void cleanup() {
    try {
      if (stream != null) {
        stream.close();
      }
    } catch (IOException e) {
      // Ignored
    }
    if (responseBody != null) {
      try {
        responseBody.close();
      } catch (IOException e) {
        // Ignored.
      }
    }
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
