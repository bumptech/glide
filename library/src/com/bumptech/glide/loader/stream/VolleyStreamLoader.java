package com.bumptech.glide.loader.stream;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;

import java.io.ByteArrayInputStream;

/**
 * A StreamLoader backed by volley for fetching images via http.
 */
public class VolleyStreamLoader implements StreamLoader {
    private final RequestQueue requestQueue;
    private final String url;
    private final RetryPolicy retryPolicy;
    private Request current = null;

    @SuppressWarnings("unused")
    public VolleyStreamLoader(RequestQueue requestQueue, String url) {
        this(requestQueue, url, new DefaultRetryPolicy());
    }

    public VolleyStreamLoader(RequestQueue requestQueue, String url, RetryPolicy retryPolicy) {
        this.requestQueue = requestQueue;
        this.url = url;
        this.retryPolicy = retryPolicy;
    }

    @Override
    public void loadStream(final StreamReadyCallback cb) {
        Request<Void> request = new GlideRequest(url, cb);
        request.setRetryPolicy(retryPolicy);
        current = requestQueue.add(request);
    }

    @Override
    public void cancel() {
        final Request local = current;
        if (local != null) {
            local.cancel();
            current = null;
        }
    }

    private static class GlideRequest extends Request<Void> {
        private final StreamReadyCallback cb;

        public GlideRequest(String url, final StreamReadyCallback cb) {
            super(Method.GET, url, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    cb.onException(error);
                }
            });
            this.cb = cb;
        }

        @Override
        protected Response<Void> parseNetworkResponse(NetworkResponse response) {
            cb.onStreamReady(new ByteArrayInputStream(response.data));
            return Response.success(null, getCacheEntry());
        }

        @Override
        protected void deliverResponse(Void response) { }
    }
}
