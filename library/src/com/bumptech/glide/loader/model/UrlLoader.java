package com.bumptech.glide.loader.model;

import com.bumptech.glide.loader.stream.HttpStreamLoader;
import com.bumptech.glide.loader.stream.StreamLoader;

import java.net.URL;

/**
 * A simple model loader for urls
 */
public class UrlLoader implements ModelLoader<URL> {
    @Override
    public StreamLoader getStreamOpener(URL model, int width, int height) {
        return new HttpStreamLoader(model);
    }

    //this may need to be overridden if multiple urls can be used to retrieve the same imgae
    @Override
    public String getId(URL model) {
        return model.toString();
    }

    @Override
    public void clear() { }
}
