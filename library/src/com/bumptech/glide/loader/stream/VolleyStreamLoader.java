package com.bumptech.glide.loader.stream;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;

import java.io.ByteArrayInputStream;

/**
 * A StreamLoader backed by volley for fetching images via http.
 */
public class VolleyStreamLoader implements StreamLoader {
    private final RequestQueue requestQueue;
    private final String url;
    private Request current = null;

    public VolleyStreamLoader(RequestQueue requestQueue, String url) {
        this.requestQueue = requestQueue;
        this.url = url;
    }

    @Override
    public void loadStream(final StreamReadyCallback cb) {
        current = requestQueue.add(new Request<Void>(Request.Method.GET, url, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                cb.onException(error);
            }
        }) {
            @Override
            protected Response<Void> parseNetworkResponse(NetworkResponse response) {
                //We actually are going to do the parsing in the callback, so we we're going to call it here where it
                // will be executed on a background thread.
                cb.onStreamReady(new ByteArrayInputStream(response.data));
                return Response.success(null, getCacheEntry());
            }

            @Override
            protected void deliverResponse(Void response) { }
        });
    }

    @Override
    public void cancel() {
        final Request local = current;
        if (local != null) {
            local.cancel();
            current = null;
        }
    }
}
