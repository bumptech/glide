package com.bumptech.glide.volley;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.toolbox.HttpHeaderParser;
import com.bumptech.glide.Metadata;
import com.bumptech.glide.load.resource.ResourceFetcher;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * A ResourceFetcher backed by volley for fetching images via http.
 */
public class VolleyStreamFetcher implements ResourceFetcher<InputStream> {
    private final RequestQueue requestQueue;
    private final String url;
    private final RetryPolicy retryPolicy;
    private VolleyRequestFuture<InputStream> requestFuture;

    @SuppressWarnings("unused")
    public VolleyStreamFetcher(RequestQueue requestQueue, String url) {
        this(requestQueue, url, new DefaultRetryPolicy(), null);
    }

    public VolleyStreamFetcher(RequestQueue requestQueue, String url, RetryPolicy retryPolicy,
            VolleyRequestFuture<InputStream> requestFuture) {
        this.requestQueue = requestQueue;
        this.url = url;
        this.retryPolicy = retryPolicy;
        this.requestFuture = requestFuture;
        if (requestFuture == null) {
            this.requestFuture = VolleyRequestFuture.newFuture();
        }
    }

    @Override
    public InputStream loadResource(Metadata metadata) throws Exception {
        GlideRequest request = new GlideRequest(url, requestFuture, glideToVolleyPriority(metadata));

        request.setRetryPolicy(retryPolicy);
        requestFuture.setRequest(requestQueue.add(request));

        return requestFuture.get();
    }

    @Override
    public void cancel() {
        VolleyRequestFuture<InputStream> localFuture = requestFuture;
        if (localFuture != null) {
            localFuture.cancel(true);
        }
    }

    private static Request.Priority glideToVolleyPriority(Metadata metadata) {
        if (metadata == null) {
            metadata = Metadata.DEFAULT;
        }
        switch (metadata.getPriority()) {
            case LOW:
                return Request.Priority.LOW;
            case HIGH:
                return Request.Priority.HIGH;
            case IMMEDIATE:
                return Request.Priority.IMMEDIATE;
            default:
                return Request.Priority.NORMAL;

        }
    }

    private static class GlideRequest extends Request<byte[]> {
        private final VolleyRequestFuture<InputStream> future;
        private Priority priority;

        public GlideRequest(String url, VolleyRequestFuture<InputStream> future, Priority priority) {
            super(Method.GET, url, future);
            this.future = future;
            this.priority = priority;
        }

        @Override
        public Priority getPriority() {
            return priority;
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
