package com.bumptech.glide.integration.volley;

import android.util.Log;
import androidx.annotation.NonNull;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.GlideUrl;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

/** A DataFetcher backed by volley for fetching images via http. */
// Public API.
@SuppressWarnings("WeakerAccess")
public class VolleyStreamFetcher implements DataFetcher<InputStream> {
  private static final String TAG = "VolleyStreamFetcher";
  public static final VolleyRequestFactory DEFAULT_REQUEST_FACTORY =
      new VolleyRequestFactory() {
        @Override
        public Request<byte[]> create(
            String url,
            DataCallback<? super InputStream> callback,
            Request.Priority priority,
            Map<String, String> headers) {
          return new GlideRequest(url, callback, priority, headers);
        }
      };

  private final RequestQueue requestQueue;
  private final VolleyRequestFactory requestFactory;
  private final GlideUrl url;
  private volatile Request<byte[]> request;

  @SuppressWarnings("unused")
  public VolleyStreamFetcher(RequestQueue requestQueue, GlideUrl url) {
    this(requestQueue, url, DEFAULT_REQUEST_FACTORY);
  }

  public VolleyStreamFetcher(
      RequestQueue requestQueue, GlideUrl url, VolleyRequestFactory requestFactory) {
    this.requestQueue = requestQueue;
    this.url = url;
    this.requestFactory = requestFactory;
  }

  @Override
  public void loadData(
      @NonNull Priority priority, @NonNull DataCallback<? super InputStream> callback) {
    request =
        requestFactory.create(
            url.toStringUrl(), callback, glideToVolleyPriority(priority), url.getHeaders());
    requestQueue.add(request);
  }

  @Override
  public void cleanup() {
    // Do nothing.
  }

  @Override
  public void cancel() {
    Request<byte[]> local = request;
    if (local != null) {
      local.cancel();
    }
  }

  @NonNull
  @Override
  public Class<InputStream> getDataClass() {
    return InputStream.class;
  }

  @NonNull
  @Override
  public DataSource getDataSource() {
    return DataSource.REMOTE;
  }

  private static Request.Priority glideToVolleyPriority(@NonNull Priority priority) {
    switch (priority) {
      case LOW:
        return Request.Priority.LOW;
      case HIGH:
        return Request.Priority.HIGH;
      case IMMEDIATE:
        return Request.Priority.IMMEDIATE;
      default:
        return Request.Priority.NORMAL;
    }
  }

  /**
   * Default {@link com.android.volley.Request} implementation for Glide that receives errors and
   * results on volley's background thread.
   */
  // Public API.
  @SuppressWarnings("unused")
  public static class GlideRequest extends Request<byte[]> {
    private final DataCallback<? super InputStream> callback;
    private final Priority priority;
    private final Map<String, String> headers;

    public GlideRequest(String url, DataCallback<? super InputStream> callback, Priority priority) {
      this(url, callback, priority, Collections.<String, String>emptyMap());
    }

    public GlideRequest(
        String url,
        DataCallback<? super InputStream> callback,
        Priority priority,
        Map<String, String> headers) {
      super(Method.GET, url, null);
      this.callback = callback;
      this.priority = priority;
      this.headers = headers;
    }

    @Override
    public Map<String, String> getHeaders() {
      return headers;
    }

    @Override
    public Priority getPriority() {
      return priority;
    }

    @Override
    protected VolleyError parseNetworkError(VolleyError volleyError) {
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, "Volley failed to retrieve response", volleyError);
      }
      if (!isCanceled()) {
        callback.onLoadFailed(volleyError);
      }
      return super.parseNetworkError(volleyError);
    }

    @Override
    protected Response<byte[]> parseNetworkResponse(NetworkResponse response) {
      if (!isCanceled()) {
        callback.onDataReady(new ByteArrayInputStream(response.data));
      }
      return Response.success(response.data, HttpHeaderParser.parseCacheHeaders(response));
    }

    @Override
    protected void deliverResponse(byte[] response) {
      // Do nothing.
    }
  }
}
