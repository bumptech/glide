package com.bumptech.glide.loader.model;

import com.android.volley.RequestQueue;
import com.bumptech.glide.loader.stream.StreamLoader;
import com.bumptech.glide.loader.stream.VolleyStreamLoader;

/**
 * A base ModelLoader for using Volley to fetch an image from a model that
 * can readily be converted into a url
 */
public abstract class VolleyModelLoader<T> extends BaseModelLoader<T> {
    private final RequestQueue requestQueue;

    public VolleyModelLoader(RequestQueue requestQueue) {
        this.requestQueue = requestQueue;
    }

    @Override
    protected StreamLoader buildStreamLoader(T model, int width, int height) {
        return new VolleyStreamLoader(requestQueue, getUrl(model, width, height));
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
}
