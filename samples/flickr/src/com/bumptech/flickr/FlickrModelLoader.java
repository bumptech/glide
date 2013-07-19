package com.bumptech.flickr;

import com.android.volley.RequestQueue;
import com.bumptech.flickr.api.Api;
import com.bumptech.flickr.api.Photo;
import com.bumptech.glide.loader.stream.StreamLoader;
import com.bumptech.glide.loader.model.ModelLoader;

/**
 * An implementation of ModelStreamLoader that leverages the StreamOpener class and the ExecutorService backing the
 * ImageManager to download the image and resize it in memory before saving the resized version
 * directly to the disk cache.
 */
public class FlickrModelLoader implements ModelLoader<Photo> {
    private final RequestQueue requestQueue;
    private StreamLoader current;

    public FlickrModelLoader(RequestQueue requestQueue) {
        this.requestQueue = requestQueue;
    }

    @Override
    public StreamLoader getStreamOpener(Photo model, int width, int height) {
        clear();
        current = new VolleyStreamLoader(requestQueue, Api.getPhotoURL(model, width, height));
        return current;
    }

    @Override
    public String getId(Photo model) {
        return model.id;
    }

    @Override
    public void clear() {
        if (current != null) {
            current.cancel();
            current = null;
        }
    }
}
