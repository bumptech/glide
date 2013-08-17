package com.bumptech.glide.volley;

import android.content.Context;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.RetryPolicy;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.loader.model.GenericLoaderFactory;
import com.bumptech.glide.loader.model.ModelLoader;
import com.bumptech.glide.loader.model.ModelLoaderFactory;
import com.bumptech.glide.loader.stream.StreamLoader;

import java.net.URL;

/**
 *  A simple model loader for fetching images for a given url
 */
public class VolleyUrlLoader implements ModelLoader<URL> {
    public static class Factory implements ModelLoaderFactory<URL> {
        private RequestQueue requestQueue;

        public Factory() { }

        public Factory(RequestQueue requestQueue) {
            this.requestQueue = requestQueue;
        }

        protected RequestQueue getRequestQueue(Context context) {
            if (requestQueue == null) {
                requestQueue = Volley.newRequestQueue(context);
            }
            return requestQueue;
        }

        @Override
        public ModelLoader<URL> build(Context context, GenericLoaderFactory factories) {
            return new VolleyUrlLoader(getRequestQueue(context));
        }

        @Override
        public Class<? extends ModelLoader<URL>> loaderClass() {
            return VolleyUrlLoader.class;
        }

        @Override
        public void teardown() {
            if (requestQueue != null) {
                requestQueue.stop();
                requestQueue.cancelAll(new RequestQueue.RequestFilter() {
                    @Override
                    public boolean apply(Request<?> request) {
                        return true;
                    }
                });
                requestQueue = null;
            }
        }
    }

    private final RequestQueue requestQueue;

    public VolleyUrlLoader(RequestQueue requestQueue) {
        this.requestQueue = requestQueue;
    }

    @Override
    public StreamLoader getStreamLoader(URL url, int width, int height) {
        return new VolleyStreamLoader(requestQueue, url.toString(), getRetryPolicy());
    }

    @Override
    public String getId(URL url) {
        return url.toString();
    }

    protected RetryPolicy getRetryPolicy() {
        return new DefaultRetryPolicy();
    }
}
