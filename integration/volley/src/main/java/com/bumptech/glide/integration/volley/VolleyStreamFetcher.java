package com.bumptech.glide.integration.volley;

import android.util.Log;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.bumptech.glide.Logs;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.GlideUrl;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * A DataFetcher backed by volley for fetching images via http.
 */
public class VolleyStreamFetcher implements DataFetcher<InputStream> {
  public static final VolleyRequestFactory DEFAULT_REQUEST_FACTORY = new VolleyRequestFactory() {
    @Override
    public Request<byte[]> create(String url, DataCallback<? super InputStream> callback,
        Request.Priority priority) {
      return new GlideRequest(url, callback, priority);
    }
  };

  private final RequestQueue requestQueue;
  private final VolleyRequestFactory requestFactory;
  private final GlideUrl url;
  private Request<byte[]> request;

  @SuppressWarnings("unused")
  public VolleyStreamFetcher(RequestQueue requestQueue, GlideUrl url) {
    this(requestQueue, url, DEFAULT_REQUEST_FACTORY);
  }

  public VolleyStreamFetcher(RequestQueue requestQueue, GlideUrl url,
      VolleyRequestFactory requestFactory) {
    this.requestQueue = requestQueue;
    this.url = url;
    this.requestFactory = requestFactory;
  }

  @Override
  public void loadData(Priority priority, DataCallback<? super InputStream> callback) {
    // Make sure the string url safely encodes non ascii characters.
    request = requestFactory.create(url.toString(), callback, glideToVolleyPriority(priority));
    requestQueue.add(request);
  }

  @Override
  public void cleanup() {
    // Do nothing.
  }

  @Override
  public String getId() {
    return url.toString();
  }

  @Override
  public void cancel() {
    request.cancel();
  }

  @Override
  public Class<InputStream> getDataClass() {
    return InputStream.class;
  }

  @Override
  public DataSource getDataSource() {
    return DataSource.REMOTE;
  }

  private static Request.Priority glideToVolleyPriority(Priority priority) {
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
   * Default {@link com.android.volley.Request} implementation for Glide that recives errors and
   * results on volley's background thread.
   */
  public static class GlideRequest extends Request<byte[]> {
    private DataCallback<? super InputStream> callback;
    private final Priority priority;

    public GlideRequest(String url, final DataCallback<? super  InputStream> callback,
        Priority priority) {
      super(Method.GET, url, null);
      this.callback = callback;
      this.priority = priority;
    }

    @Override
    public Priority getPriority() {
      return priority;
    }

    @Override
    protected VolleyError parseNetworkError(VolleyError volleyError) {
      if (Logs.isEnabled(Log.DEBUG)) {
        Logs.log(Log.DEBUG, "Volley failed to retrieve response", volleyError);
      }
      callback.onDataReady(null);
      return super.parseNetworkError(volleyError);
    }

    @Override
    protected Response<byte[]> parseNetworkResponse(NetworkResponse response) {
      callback.onDataReady(new ByteArrayInputStream(response.data));
      return Response.success(response.data, HttpHeaderParser.parseCacheHeaders(response));
    }

    @Override
    protected void deliverResponse(byte[] response) {
      // Do nothing.
    }
  }
}
