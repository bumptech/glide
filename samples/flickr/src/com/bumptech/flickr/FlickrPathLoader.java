package com.bumptech.flickr;

import com.bumptech.flickr.api.Api;
import com.bumptech.flickr.api.Photo;
import com.bumptech.photos.loader.path.BasePathLoader;

import java.io.File;

/**
 * Created with IntelliJ IDEA.
 * User: sam
 * Date: 1/6/13
 * Time: 11:55 AM
 * To change this template use File | Settings | File Templates.
 */
public class FlickrPathLoader extends BasePathLoader<Photo> {
    private final Api flickrApi;
    private final File cacheDir;

    public FlickrPathLoader(Api flickApi, File cacheDir) {
        this.flickrApi = flickApi;
        this.cacheDir = cacheDir;
    }

    @Override
    protected void doFetchPath(Photo model, int width, int height, final PathReadyCallback cb) {
        flickrApi.downloadPhoto(model, cacheDir, new Api.PhotoCallback() {
            @Override
            public void onDownloadComplete(String path) {
                cb.onPathReady(path);
            }
        });
    }
}
