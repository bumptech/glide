package com.bumptech.flickr;

import com.bumptech.flickr.api.Api;
import com.bumptech.flickr.api.Photo;
import com.bumptech.photos.loader.model.DirectModelStreamLoader;
import com.bumptech.photos.loader.opener.HttpInputStreamsOpener;
import com.bumptech.photos.loader.opener.StreamOpener;

/**
 * An implementation of ModelStreamLoader that leverages the StreamOpener class and the ExecutorService backing the
 * ImageManager to download the image and resize it in memory before saving the resized version
 * directly to the disk cache.
 */
public class DirectFlickrStreamLoader extends DirectModelStreamLoader<Photo>{
    private final Api flickrApi;

    public DirectFlickrStreamLoader(Api flickApi) {
        this.flickrApi = flickApi;
    }

    @Override
    protected StreamOpener getStreamOpener(Photo model, int width, int height) {
        return new HttpInputStreamsOpener(flickrApi.getPhotoURL(width, height, model));
    }

    @Override
    protected String getId(Photo model) {
        return model.id;
    }
}
