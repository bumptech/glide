package com.bumptech.glide.load.data;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.model.GlideUrl;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * A DataFetcher that retrieves an {@link java.io.InputStream} for a Url.
 */
public class HttpUrlFetcher implements DataFetcher<InputStream> {
    private static final int MAXIMUM_REDIRECTS = 5;
    private static final HttpUrlConnectionFactory DEFAULT_FACTORY = new DefaultHttpUrlConnectionFactory();

    private final HttpUrlConnectionFactory factory;
    private final GlideUrl glideUrl;
    private HttpURLConnection urlConnection;
    private volatile boolean isCancelled;

    public HttpUrlFetcher(GlideUrl glideUrl) {
        this(glideUrl, DEFAULT_FACTORY);
    }

    HttpUrlFetcher(GlideUrl glideUrl, HttpUrlConnectionFactory factory) {
        this.glideUrl = glideUrl;
        this.factory = factory;
    }

    @Override
    public InputStream loadData(Priority priority) throws Exception {
        return loadDataWithRedirects(glideUrl.toURL(), 0 /*redirects*/, null /*lastUrl*/);
    }

    private InputStream loadDataWithRedirects(URL url, int redirects, URL lastUrl) throws IOException {
        if (redirects >= MAXIMUM_REDIRECTS) {
            throw new IOException("Too many (> " + MAXIMUM_REDIRECTS + ") redirects!");
        } else if (url.equals(lastUrl)) {
            throw new IOException("In re-direct loop");
        }
        urlConnection = factory.build(url);
        urlConnection.setConnectTimeout(2500);
        urlConnection.setReadTimeout(2500);
        urlConnection.setUseCaches(false);
        urlConnection.setDoInput(true);

        // Connect explicitly to avoid errors in decoders if connection fails.
        urlConnection.connect();
        if (isCancelled) {
            return null;
        }
        final int statusCode = urlConnection.getResponseCode();
        if (statusCode / 100 == 2) {
            return urlConnection.getInputStream();
        } else if (statusCode / 100 == 3) {
            String redirectUrlString = urlConnection.getHeaderField("Location");
            URL redirectUrl = new URL(redirectUrlString);
            return loadDataWithRedirects(redirectUrl, redirects + 1, url);
        } else {
            if (statusCode == -1) {
                throw new IOException("Unable to retrieve response code from HttpUrlConnection.");
            }
            throw new IOException("Request failed " + statusCode + ": " + urlConnection.getResponseMessage());
        }
    }

    @Override
    public void cleanup() {
        if (urlConnection != null) {
            urlConnection.disconnect();
        }
    }

    @Override
    public String getId() {
        return glideUrl.toString();
    }

    @Override
    public void cancel() {
        // TODO: we should consider disconnecting the url connection here, but we can't do so directly because cancel is
        // often called on the main thread.
        isCancelled = true;
    }

    interface HttpUrlConnectionFactory {
        public HttpURLConnection build(URL url) throws IOException;
    }

    private static class DefaultHttpUrlConnectionFactory implements HttpUrlConnectionFactory {
        @Override
        public HttpURLConnection build(URL url) throws IOException {
            return (HttpURLConnection) url.openConnection();
        }
    }

}
