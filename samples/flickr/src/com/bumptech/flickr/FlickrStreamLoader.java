package com.bumptech.flickr;

import com.bumptech.flickr.api.Api;
import com.bumptech.flickr.api.Photo;
import com.bumptech.photos.loader.model.ModelStreamLoader;
import com.bumptech.photos.loader.opener.FileInputStreamsOpener;

import java.io.File;
import java.util.concurrent.Future;

/**
 * Created with IntelliJ IDEA.
 * User: sam
 * Date: 1/6/13
 * Time: 11:55 AM
 * To change this template use File | Settings | File Templates.
 */
public class FlickrStreamLoader implements ModelStreamLoader<Photo> {
    private final Api flickrApi;
    private final File cacheDir;
    private Future current;

    public FlickrStreamLoader(Api flickApi, File cacheDir) {
        this.flickrApi = flickApi;
        this.cacheDir = cacheDir;
    }

    @Override
    public Object fetchModelStreams(Photo model, int width, int height, final ModelStreamsReadyCallback cb) {
        clear();
        current = flickrApi.downloadPhoto(model, cacheDir, new Api.PhotoCallback() {
            @Override
            public void onDownloadComplete(String path) {
                cb.onStreamsReady(path, new FileInputStreamsOpener(path));
            }
        });
        return current;
    }

    @Override
    public void clear() {
        if (current != null) {
            current.cancel(false);
            current = null;
        }
    }
}
