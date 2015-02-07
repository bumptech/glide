package com.bumptech.glide.integration.volley;

import android.content.Context;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.GenericLoaderFactory;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;

import java.io.InputStream;

/**
 *  A simple model loader for fetching media over http/https using Volley.
 */
public class VolleyUrlLoader implements ModelLoader<GlideUrl, InputStream> {

    /**
     * The default factory for {@link VolleyUrlLoader}s.
     */
    public static class Factory implements ModelLoaderFactory<GlideUrl, InputStream> {
        private static RequestQueue internalQueue;
        private RequestQueue requestQueue;
        private final VolleyRequestFactory requestFactory;

        private static RequestQueue getInternalQueue(Context context) {
            if (internalQueue == null) {
                synchronized (Factory.class) {
                    if (internalQueue == null) {
                        internalQueue = Volley.newRequestQueue(context);
                    }
                }
            }
            return internalQueue;
        }

        /**
         * Constructor for a new Factory that runs requests using a static singleton request queue.
         */
        public Factory(Context context) {
            this(getInternalQueue(context));
        }

        /**
         * Constructor for a new Factory that runs requests using the given {@link RequestQueue}.
         */
        public Factory(RequestQueue requestQueue) {
            this(requestQueue, VolleyStreamFetcher.DEFAULT_REQUEST_FACTORY);
        }

        /**
         * Constructor for a new Factory with a custom Volley request factory that runs requests
         * using the given {@link RequestQueue}.
         */
        public Factory(RequestQueue requestQueue, VolleyRequestFactory requestFactory) {
            this.requestFactory = requestFactory;
            this.requestQueue = requestQueue;
        }

        @Override
        public ModelLoader<GlideUrl, InputStream> build(Context context, GenericLoaderFactory factories) {
            return new VolleyUrlLoader(requestQueue, requestFactory);
        }

        @Override
        public void teardown() {
            // Do nothing, this instance doesn't own the request queue.
        }
    }

    private final RequestQueue requestQueue;
    private final VolleyRequestFactory requestFactory;

    public VolleyUrlLoader(RequestQueue requestQueue) {
        this(requestQueue, VolleyStreamFetcher.DEFAULT_REQUEST_FACTORY);
    }

    public VolleyUrlLoader(RequestQueue requestQueue, VolleyRequestFactory requestFactory) {
        this.requestQueue = requestQueue;
        this.requestFactory = requestFactory;
    }

    @Override
    public DataFetcher<InputStream> getResourceFetcher(GlideUrl url, int width, int height) {
        return new VolleyStreamFetcher(
            requestQueue, url, new VolleyRequestFuture<InputStream>(), requestFactory);
    }
}
