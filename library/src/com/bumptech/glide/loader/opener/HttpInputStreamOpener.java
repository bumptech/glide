package com.bumptech.glide.loader.opener;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created with IntelliJ IDEA.
 * User: sam
 * Date: 7/10/13
 * Time: 11:21 PM
 * To change this template use File | Settings | File Templates.
 */
public class HttpInputStreamOpener implements StreamOpener {
    private final URL url;
    private HttpURLConnection urlConnection = null;

    public HttpInputStreamOpener(URL url) {
        this.url = url;
    }

    @Override
    public InputStream openStream() throws IOException {
        urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setDoInput(true);
        urlConnection.setDoOutput(false);
        urlConnection.setUseCaches(false);
        urlConnection.setRequestProperty("Connection", "close");
        urlConnection.connect();
        return urlConnection.getInputStream();
    }

    @Override
    public void cleanup() {
        if (urlConnection != null) {
            urlConnection.disconnect();
        }
    }
}
