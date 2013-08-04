package com.bumptech.glide.loader.model;

import android.content.Context;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.RequestQueue;
import com.android.volley.RetryPolicy;
import com.bumptech.glide.Glide;
import com.bumptech.glide.loader.stream.StreamLoader;
import com.bumptech.glide.loader.stream.VolleyStreamLoader;

/**
 * A base ModelLoader for using Volley to fetch an image from a model that
 * can readily be converted into a url
 */
public abstract class VolleyModelLoader<T> implements ModelLoader<T> {
    private final RequestQueue requestQueue;

    /**
     * A convenience constructor relying on the {@link Glide} singleton and it's {@link RequestQueue} via
     * {@link Glide#getRequestQueue(android.content.Context)}}
     *
     * @param context A context
     */
    public VolleyModelLoader(Context context) {
        this(Glide.get().getRequestQueue(context));
    }

    public VolleyModelLoader(RequestQueue requestQueue) {
        this.requestQueue = requestQueue;
    }

    @Override
    public StreamLoader getStreamLoader(T model, int width, int height) {
        return new VolleyStreamLoader(requestQueue, getUrl(model, width, height), getRetryPolicy());
    }

    /**
     * Get the url to load the image from
     *
     * @param model The model representing the image
     * @param width The width of the view where the image will be displayed
     * @param height The height of the view where the image will be displayed
     * @return A String url
     */
    protected abstract String getUrl(T model, int width, int height);

    protected RetryPolicy getRetryPolicy() {
        return new DefaultRetryPolicy();
    }
}
