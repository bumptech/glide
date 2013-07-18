package com.bumptech.flickr;

import com.bumptech.flickr.api.Api;
import com.bumptech.flickr.api.Photo;
import com.bumptech.glide.loader.model.DirectModelStreamLoader;
import com.bumptech.glide.loader.opener.HttpInputStreamOpener;
import com.bumptech.glide.loader.opener.StreamOpener;

/**
 * An implementation of ModelStreamLoader that leverages the StreamOpener class and the ExecutorService backing the
 * ImageManager to download the image and resize it in memory before saving the resized version
 * directly to the disk cache.
 */
public class DirectFlickrStreamLoader extends DirectModelStreamLoader<Photo>{

    @Override
    protected StreamOpener getStreamOpener(Photo model, int width, int height) {
        return new HttpInputStreamOpener(Api.getPhotoURL(model, width, height));
    }

    @Override
    protected String getId(Photo model) {
        return model.id;
    }
}
