package com.bumptech.glide.integration.okhttp;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.GlideUrl;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;

import java.io.IOException;
import java.io.InputStream;

/**
 * Fetches an {@link InputStream} using the okhttp library.
 */
public class OkHttpStreamFetcher implements DataFetcher<InputStream> {
    private final OkHttpClient client;
    private final GlideUrl url;
    private volatile Request request;
    private InputStream stream;

    public OkHttpStreamFetcher(OkHttpClient client, GlideUrl url) {
        this.client = client;
        this.url = url;
    }

    @Override
    public InputStream loadData(Priority priority) throws Exception {
        request = new Request.Builder()
                .url(url.toString())
                .build();

        stream = client.newCall(request)
                .execute()
                .body()
                .byteStream();
        return stream;
    }

    @Override
    public void cleanup() {
        if (stream == null) {
            return;
        }
        try {
            stream.close();
        } catch (IOException e) {
            // Ignored
        }
    }

    @Override
    public String getId() {
        return url.toString();
    }

    @Override
    public void cancel() {
        // TODO: call cancel on the client when this method is called on a background thread. See #257
    }
}
