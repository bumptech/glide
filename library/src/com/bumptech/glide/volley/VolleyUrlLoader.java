package com.bumptech.glide.volley;

import android.content.Context;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.RequestQueue;
import com.android.volley.RetryPolicy;
import com.bumptech.glide.loader.GlideUrl;
import com.bumptech.glide.loader.bitmap.model.GenericLoaderFactory;
import com.bumptech.glide.loader.bitmap.model.ModelLoader;
import com.bumptech.glide.loader.bitmap.model.ModelLoaderFactory;
import com.bumptech.glide.loader.bitmap.resource.ResourceFetcher;

import java.io.InputStream;

/**
 *  A simple model loader for fetching images for a given url
 */
public class VolleyUrlLoader implements ModelLoader<GlideUrl, InputStream> {

    public static class Factory implements ModelLoaderFactory<GlideUrl, InputStream> {
        private RequestQueue requestQueue;

        public Factory(RequestQueue requestQueue) {
            this.requestQueue = requestQueue;
        }

        @Override
        public ModelLoader<GlideUrl, InputStream> build(Context context, GenericLoaderFactory factories) {
            return new VolleyUrlLoader(requestQueue);
        }

        @Override
        public void teardown() { }
    }

    private final RequestQueue requestQueue;

    public VolleyUrlLoader(RequestQueue requestQueue) {
        this.requestQueue = requestQueue;
    }

    @Override
    public ResourceFetcher<InputStream> getResourceFetcher(GlideUrl url, int width, int height) {
        return new VolleyStreamFetcher(requestQueue, url.toString(), getRetryPolicy());
    }

    @Override
    public String getId(GlideUrl url) {
        return url.toString();
    }

    protected RetryPolicy getRetryPolicy() {
        return new DefaultRetryPolicy();
    }
}
