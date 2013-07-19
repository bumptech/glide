package com.bumptech.glide.loader.stream;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * A simple http stream loader for fetching a url
 */
public class HttpStreamLoader implements StreamLoader {
    private final URL url;

    public HttpStreamLoader(String url) throws MalformedURLException {
        this(new URL(url));
    }

    public HttpStreamLoader(URL url) {
        this.url = url;
    }

    @Override
    public void loadStream(StreamReadyCallback cb) {
        HttpURLConnection urlConnection = null;
        try {
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setDoInput(true);
            urlConnection.setDoOutput(false);
            urlConnection.setUseCaches(false);
            urlConnection.setRequestProperty("Connection", "close");
            urlConnection.connect();
            cb.onStreamReady(urlConnection.getInputStream());
        } catch (IOException e) {
            cb.onException(e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

    /**
     * Since {@link #loadStream(com.bumptech.glide.loader.stream.StreamLoader.StreamReadyCallback)} calls its
     * callback synchronously, not much we can do here.
     */
    @Override
    public void cancel() { }
}
