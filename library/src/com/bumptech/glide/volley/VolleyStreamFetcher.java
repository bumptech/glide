package com.bumptech.glide.volley;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.RequestFuture;
import com.bumptech.glide.loader.bitmap.resource.ResourceFetcher;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * A ResourceFetcher backed by volley for fetching images via http.
 */
public class VolleyStreamFetcher implements ResourceFetcher<InputStream> {
    private final RequestQueue requestQueue;
    private final String url;
    private final RetryPolicy retryPolicy;
    private Request current = null;
    private RequestFuture<InputStream> requestFuture;

    @SuppressWarnings("unused")
    public VolleyStreamFetcher(RequestQueue requestQueue, String url) {
        this(requestQueue, url, new DefaultRetryPolicy());
    }

    public VolleyStreamFetcher(RequestQueue requestQueue, String url, RetryPolicy retryPolicy) {
        this.requestQueue = requestQueue;
        this.url = url;
        this.retryPolicy = retryPolicy;
    }

    @Override
    public InputStream loadResource() throws Exception {
        requestFuture = RequestFuture.newFuture();
        GlideRequest request = new GlideRequest(url, requestFuture);

        request.setRetryPolicy(retryPolicy);
        request.setShouldCache(true);
        current = requestQueue.add(request);
        requestFuture.setRequest(current);

        return requestFuture.get();
    }

    @Override
    public void cancel() {
        final Request local = current;
        RequestFuture localFuture = requestFuture;
        if (localFuture != null) {
            localFuture.cancel(false);
            requestFuture = null;
        }
        if (local != null) {
            local.cancel();
            current = null;
        }
    }

    private static class GlideRequest extends Request<byte[]> {
        private final RequestFuture<InputStream> future;

        public GlideRequest(String url, RequestFuture<InputStream> future) {
            super(Method.GET, url, future);
            this.future = future;
        }

        @Override
        protected Response<byte[]> parseNetworkResponse(NetworkResponse response) {
            return Response.success(response.data, HttpHeaderParser.parseCacheHeaders(response));
        }

        @Override
        protected void deliverResponse(byte[] response) {
            future.onResponse(new ByteArrayInputStream(response));
        }
    }
}
