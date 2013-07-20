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
        current = requestQueue.add(new Request<ByteArrayInputStream>(Request.Method.GET, url, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                cb.onException(error);
            }
        }) {
            @Override
            protected Response<ByteArrayInputStream> parseNetworkResponse(NetworkResponse response) {
                //this may be less than ideal, since we can't downsample the image as it is read,
                //but we don't have a choice if we want to use Volley
                return Response.success(new ByteArrayInputStream(response.data), getCacheEntry());
            }

            @Override
            protected void deliverResponse(ByteArrayInputStream response) {
                cb.onStreamReady(response);
            }
        });
    }

    @Override
    public void cancel() {
        if (current != null) {
            current.cancel();
            current = null;
        }
    }
}
