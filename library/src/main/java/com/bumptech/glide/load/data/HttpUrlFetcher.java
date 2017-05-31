package com.bumptech.glide.load.data;

import android.text.TextUtils;
import android.util.Log;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.HttpException;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.util.ContentLengthInputStream;
import com.bumptech.glide.util.LogTime;
import com.bumptech.glide.util.Synthetic;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;

/**
 * A DataFetcher that retrieves an {@link java.io.InputStream} for a Url.
 */
public class HttpUrlFetcher implements DataFetcher<InputStream> {
  private static final String TAG = "HttpUrlFetcher";
  private static final int MAXIMUM_REDIRECTS = 5;
  // Visible for testing.
  static final HttpUrlConnectionFactory DEFAULT_CONNECTION_FACTORY =
      new DefaultHttpUrlConnectionFactory();

  private final GlideUrl glideUrl;
  private final int timeout;
  private final HttpUrlConnectionFactory connectionFactory;

  private HttpURLConnection urlConnection;
  private InputStream stream;
  private volatile boolean isCancelled;

  public HttpUrlFetcher(GlideUrl glideUrl, int timeout) {
    this(glideUrl, timeout, DEFAULT_CONNECTION_FACTORY);
  }

  // Visible for testing.
  HttpUrlFetcher(GlideUrl glideUrl, int timeout, HttpUrlConnectionFactory connectionFactory) {
    this.glideUrl = glideUrl;
    this.timeout = timeout;
    this.connectionFactory = connectionFactory;
  }

  @Override
  public void loadData(Priority priority, DataCallback<? super InputStream> callback) {
    long startTime = LogTime.getLogTime();
    final InputStream result;
    try {
      result = loadDataWithRedirects(glideUrl.toURL(), 0 /*redirects*/, null /*lastUrl*/,
          glideUrl.getHeaders());
    } catch (IOException e) {
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, "Failed to load data for url", e);
      }
      callback.onLoadFailed(e);
      return;
    }

    if (Log.isLoggable(TAG, Log.VERBOSE)) {
      Log.v(TAG, "Finished http url fetcher fetch in " + LogTime.getElapsedMillis(startTime)
          + " ms and loaded " + result);
    }
    callback.onDataReady(result);
  }

  private InputStream loadDataWithRedirects(URL url, int redirects, URL lastUrl,
      Map<String, String> headers) throws IOException {
    if (redirects >= MAXIMUM_REDIRECTS) {
      throw new HttpException("Too many (> " + MAXIMUM_REDIRECTS + ") redirects!");
    } else {
      // Comparing the URLs using .equals performs additional network I/O and is generally broken.
      // See http://michaelscharf.blogspot.com/2006/11/javaneturlequals-and-hashcode-make.html.
      try {
        if (lastUrl != null && url.toURI().equals(lastUrl.toURI())) {
          throw new HttpException("In re-direct loop");

        }
      } catch (URISyntaxException e) {
        // Do nothing, this is best effort.
      }
    }

    urlConnection = connectionFactory.build(url);
    for (Map.Entry<String, String> headerEntry : headers.entrySet()) {
      urlConnection.addRequestProperty(headerEntry.getKey(), headerEntry.getValue());
    }
    urlConnection.setConnectTimeout(timeout);
    urlConnection.setReadTimeout(timeout);
    urlConnection.setUseCaches(false);
    urlConnection.setDoInput(true);

    // Stop the urlConnection instance of HttpUrlConnection from following redirects so that
    // redirects will be handled by recursive calls to this method, loadDataWithRedirects.
    urlConnection.setInstanceFollowRedirects(false);

    // Connect explicitly to avoid errors in decoders if connection fails.
    urlConnection.connect();
    if (isCancelled) {
      return null;
    }
    final int statusCode = urlConnection.getResponseCode();
    if (statusCode / 100 == 2) {
      return getStreamForSuccessfulRequest(urlConnection);
    } else if (statusCode / 100 == 3) {
      String redirectUrlString = urlConnection.getHeaderField("Location");
      if (TextUtils.isEmpty(redirectUrlString)) {
        throw new HttpException("Received empty or null redirect url");
      }
      URL redirectUrl = new URL(url, redirectUrlString);
      return loadDataWithRedirects(redirectUrl, redirects + 1, url, headers);
    } else if (statusCode == -1) {
      throw new HttpException(statusCode);
    } else {
      throw new HttpException(urlConnection.getResponseMessage(), statusCode);
    }
  }

  private InputStream getStreamForSuccessfulRequest(HttpURLConnection urlConnection)
      throws IOException {
    if (TextUtils.isEmpty(urlConnection.getContentEncoding())) {
      int contentLength = urlConnection.getContentLength();
      stream = ContentLengthInputStream.obtain(urlConnection.getInputStream(), contentLength);
    } else {
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, "Got non empty content encoding: " + urlConnection.getContentEncoding());
      }
      stream = urlConnection.getInputStream();
    }
    return stream;
  }

  @Override
  public void cleanup() {
    if (stream != null) {
      try {
        stream.close();
      } catch (IOException e) {
        // Ignore
      }
    }
    if (urlConnection != null) {
      urlConnection.disconnect();
    }
  }

  @Override
  public void cancel() {
    // TODO: we should consider disconnecting the url connection here, but we can't do so
    // directly because cancel is often called on the main thread.
    isCancelled = true;
  }

  @Override
  public Class<InputStream> getDataClass() {
    return InputStream.class;
  }

  @Override
  public DataSource getDataSource() {
    return DataSource.REMOTE;
  }

  interface HttpUrlConnectionFactory {
    HttpURLConnection build(URL url) throws IOException;
  }

  private static class DefaultHttpUrlConnectionFactory implements HttpUrlConnectionFactory {

    @Synthetic
    DefaultHttpUrlConnectionFactory() { }

    @Override
    public HttpURLConnection build(URL url) throws IOException {
      return (HttpURLConnection) url.openConnection();
    }
  }
}
