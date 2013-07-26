package com.bumptech.glide.loader.model;

import com.android.volley.RequestQueue;
import com.bumptech.glide.loader.stream.StreamLoader;
import com.bumptech.glide.loader.stream.VolleyStreamLoader;

import java.net.URL;

/**
 * A simple model loader for urls
 */
public class UrlLoader implements ModelLoader<URL> {
    private final RequestQueue requestQueue;

    public UrlLoader(RequestQueue requestQueue) {
        this.requestQueue = requestQueue;
    }

    @Override
    public StreamLoader getStreamLoader(URL model, int width, int height) {
        return new VolleyStreamLoader(requestQueue, model.toString());
    }

    //this may need to be overridden if multiple urls can be used to retrieve the same image
    @Override
    public String getId(URL model) {
        return model.toString();
    }

    @Override
    public void clear() { }
}
