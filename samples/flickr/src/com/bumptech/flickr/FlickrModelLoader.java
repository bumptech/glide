package com.bumptech.flickr;

import com.android.volley.RequestQueue;
import com.bumptech.flickr.api.Api;
import com.bumptech.flickr.api.Photo;
import com.bumptech.glide.loader.model.VolleyModelLoader;

/**
 * An implementation of ModelStreamLoader that leverages the StreamOpener class and the ExecutorService backing the
 * ImageManager to download the image and resize it in memory before saving the resized version
 * directly to the disk cache.
 */
public class FlickrModelLoader extends VolleyModelLoader<Photo> {
    public FlickrModelLoader(RequestQueue requestQueue) {
        super(requestQueue);
    }

    @Override
    public String getId(Photo model) {
        return model.id;
    }

    @Override
    protected String getUrl(Photo model, int width, int height) {
        return Api.getPhotoURL(model, width, height);
    }
}
