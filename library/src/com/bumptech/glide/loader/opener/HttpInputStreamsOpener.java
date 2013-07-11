package com.bumptech.glide.loader.opener;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created with IntelliJ IDEA.
 * User: sam
 * Date: 7/10/13
 * Time: 11:21 PM
 * To change this template use File | Settings | File Templates.
 */
public class HttpInputStreamsOpener implements StreamOpener {
    private final URL url;
    private HttpURLConnection[] urlConnections = new HttpURLConnection[2];

    public HttpInputStreamsOpener(URL url) {
        this.url = url;
    }

    @Override
    public Streams openStreams() throws IOException {
        urlConnections = new HttpURLConnection[] {(HttpURLConnection) url.openConnection(), (HttpURLConnection) url.openConnection()};
        for (HttpURLConnection urlConnection : urlConnections) {
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setDoInput(true);
            urlConnection.setDoOutput(false);
            urlConnection.setUseCaches(false);
            urlConnection.setRequestProperty("Connection", "close");
            urlConnection.connect();
        }
        return new Streams(urlConnections[0].getInputStream(), urlConnections[1].getInputStream());
    }

    @Override
    public void cleanup() {
        for (HttpURLConnection urlConnection : urlConnections) {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }
}
