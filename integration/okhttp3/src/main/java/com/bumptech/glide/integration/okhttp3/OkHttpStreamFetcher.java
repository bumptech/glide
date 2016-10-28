package com.bumptech.glide.integration.okhttp3;

import android.util.Log;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.util.ContentLengthInputStream;
import com.bumptech.glide.util.Synthetic;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import okhttp3.Call;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Fetches an {@link InputStream} using the okhttp library.
 */
public class OkHttpStreamFetcher implements DataFetcher<InputStream> {
  private static final String TAG = "OkHttpFetcher";
  private final Call.Factory client;
  private final GlideUrl url;
  @Synthetic InputStream stream;
  @Synthetic ResponseBody responseBody;
  private volatile Call call;

  public OkHttpStreamFetcher(Call.Factory client, GlideUrl url) {
    this.client = client;
    this.url = url;
  }

  @Override
  public void loadData(Priority priority, final DataCallback<? super InputStream> callback) {
    Request.Builder requestBuilder = new Request.Builder().url(url.toStringUrl());
    for (Map.Entry<String, String> headerEntry : url.getHeaders().entrySet()) {
      String key = headerEntry.getKey();
      requestBuilder.addHeader(key, headerEntry.getValue());
    }
    Request request = requestBuilder.build();

    call = client.newCall(request);
    call.enqueue(new okhttp3.Callback() {
      @Override
      public void onFailure(Call call, IOException e) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
          Log.d(TAG, "OkHttp failed to obtain result", e);
        }
        callback.onLoadFailed(e);
      }

      @Override
      public void onResponse(Call call, Response response) throws IOException {
        responseBody = response.body();
        if (response.isSuccessful()) {
          long contentLength = response.body().contentLength();
          stream = ContentLengthInputStream.obtain(responseBody.byteStream(), contentLength);
        } else if (Log.isLoggable(TAG, Log.DEBUG)) {
          Log.d(TAG, "OkHttp got error response: " + response.code() + ", " + response.message());
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
      responseBody.close();
    }
  }

  @Override
  public void cancel() {
    Call local = call;
    if (local != null) {
      local.cancel();
    }
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
