package com.bumptech.flickr;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.bumptech.glide.loader.stream.StreamLoader;

import java.io.ByteArrayInputStream;
import java.net.URL;

/**
 * Created with IntelliJ IDEA.
 * User: sam
 * Date: 7/19/13
 * Time: 8:51 AM
 * To change this template use File | Settings | File Templates.
 */
public class VolleyStreamLoader implements StreamLoader {
    private final RequestQueue requestQueue;
    private final URL url;
    private Request current = null;

    public VolleyStreamLoader(RequestQueue requestQueue, URL url) {
        this.requestQueue = requestQueue;
        this.url = url;
    }

    @Override
    public void loadStream(final StreamReadyCallback cb) {
        current = requestQueue.add(new Request<ByteArrayInputStream>(Request.Method.GET, url.toString(), new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                cb.onException(error);
            }
        }) {
            @Override
            protected Response<ByteArrayInputStream> parseNetworkResponse(NetworkResponse response) {
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
