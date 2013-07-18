package com.bumptech.flickr;

import com.android.volley.Request;
import com.bumptech.flickr.api.Api;
import com.bumptech.flickr.api.Photo;
import com.bumptech.glide.loader.model.BaseModelStreamLoader;
import com.bumptech.glide.loader.opener.FileInputStreamOpener;

import java.io.File;

/**
 * An implementation of a ModelStreamLoader that uses a separate class to download images to disk and then uses
 * the path where the image is downloaded to create an input stream opener. Demonstrates one way of integrating this
 * library with existing apis for download or otherwise retrieving images
 *
 */
public class FlickrStreamLoader extends BaseModelStreamLoader<Photo>{
    private final Api flickrApi;
    private final File cacheDir;
    private Request current = null;

    public FlickrStreamLoader(Api flickrApi, File cacheDir) {
        this.flickrApi = flickrApi;
        this.cacheDir = cacheDir;
    }

    @Override
    protected void doFetchModelStreams(final Photo model, int width, int height, final ModelStreamReadyCallback cb) {
        clear();
        current = flickrApi.downloadPhoto(model, cacheDir, new Api.PhotoCallback() {
            @Override
            public void onDownloadComplete(String path) {
                cb.onStreamReady(model.id, new FileInputStreamOpener(path));
            }
        });
    }

    @Override
    public void clear() {
        if (current != null) {
            current.cancel();
            current = null;
        }
    }
}
