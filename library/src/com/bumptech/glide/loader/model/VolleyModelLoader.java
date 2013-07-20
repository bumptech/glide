package com.bumptech.glide.loader.model;

import com.android.volley.RequestQueue;
import com.bumptech.glide.loader.stream.StreamLoader;
import com.bumptech.glide.loader.stream.VolleyStreamLoader;

/**
 * Created with IntelliJ IDEA.
 * User: sam
 * Date: 7/19/13
 * Time: 5:34 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class VolleyModelLoader<T> implements ModelLoader<T>{
    private final RequestQueue requestQueue;
    private VolleyStreamLoader current = null;

    public VolleyModelLoader(RequestQueue requestQueue) {
        this.requestQueue = requestQueue;
    }

    @Override
    public StreamLoader getStreamOpener(T model, int width, int height) {
        clear();
        current = new VolleyStreamLoader(requestQueue, getUrl(model, width, height));
        return current;
    }

    @Override
    public void clear() {
        if (current != null) {
            current.cancel();
            current = null;
        }
    }

    protected abstract String getUrl(T model, int width, int height);
}
