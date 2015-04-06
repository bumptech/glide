package com.bumptech.glide.load.data;

import android.text.TextUtils;
import android.util.Log;

import com.bumptech.glide.Logs;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.util.ContentLengthInputStream;
import com.bumptech.glide.util.LogTime;

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
  private static final String CONTENT_LENGTH_HEADER = "Content-Length";
  private static final int MAXIMUM_REDIRECTS = 5;
  private static final int DEFAULT_TIMEOUT_MS = 2500;
  // Visible for testing.
  static final HttpUrlConnectionFactory DEFAULT_CONNECTION_FACTORY =
      new DefaultHttpUrlConnectionFactory();

  private final GlideUrl glideUrl;
  private final int timeout;
  private final HttpUrlConnectionFactory connectionFactory;

  private HttpURLConnection urlConnection;
  private InputStream stream;
  private volatile boolean isCancelled;

  public HttpUrlFetcher(GlideUrl glideUrl) {
    this(glideUrl, DEFAULT_TIMEOUT_MS, DEFAULT_CONNECTION_FACTORY);
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
    InputStream result = null;
    try {
      result = loadDataWithRedirects(glideUrl.toURL(), 0 /*redirects*/, null /*lastUrl*/,
          glideUrl.getHeaders());
    } catch (IOException e) {
      if (Logs.isEnabled(Log.DEBUG)) {
        Logs.log(Log.DEBUG, "Failed to load data for url", e);
      }
    }
    if (Logs.isEnabled(Log.VERBOSE)) {
      Logs.log(Log.VERBOSE, "Finished http url fetcher fetch in "
          + LogTime.getElapsedMillis(startTime) + " ms and loaded "  + result);
    }
    callback.onDataReady(result);
  }

  private InputStream loadDataWithRedirects(URL url, int redirects, URL lastUrl,
      Map<String, String> headers) throws IOException {
    if (redirects >= MAXIMUM_REDIRECTS) {
      throw new IOException("Too many (> " + MAXIMUM_REDIRECTS + ") redirects!");
    } else {
      // Comparing the URLs using .equals performs additional network I/O and is generally broken.
      // See http://michaelscharf.blogspot.com/2006/11/javaneturlequals-and-hashcode-make.html.
      try {
        if (lastUrl != null && url.toURI().equals(lastUrl.toURI())) {
            throw new IOException("In re-direct loop");
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

    // Connect explicitly to avoid errors in decoders if connection fails.
    urlConnection.connect();
    if (isCancelled) {
        return null;
    }
    final int statusCode = urlConnection.getResponseCode();
    if (statusCode / 100 == 2) {
      String contentLength = urlConnection.getHeaderField(CONTENT_LENGTH_HEADER);
      stream = ContentLengthInputStream.obtain(urlConnection.getInputStream(), contentLength);
      return stream;
    } else if (statusCode / 100 == 3) {
      String redirectUrlString = urlConnection.getHeaderField("Location");
      if (TextUtils.isEmpty(redirectUrlString)) {
          throw new IOException("Received empty or null redirect url");
      }
      URL redirectUrl = new URL(url, redirectUrlString);
      return loadDataWithRedirects(redirectUrl, redirects + 1, url, headers);
    } else {
      if (statusCode == -1) {
          throw new IOException("Unable to retrieve response code from HttpUrlConnection.");
      }
      throw new IOException("Request failed " + statusCode + ": "
          + urlConnection.getResponseMessage());
    }
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
    @Override
    public HttpURLConnection build(URL url) throws IOException {
      return (HttpURLConnection) url.openConnection();
    }
  }
}
