package com.bumptech.photos.photomanager.loader;

import com.bumptech.photos.photomanager.LoadedCallback;
import com.bumptech.photos.photomanager.PhotoManager;

/**
 * Created with IntelliJ IDEA.
 * User: sam
 * Date: 12/31/12
 * Time: 4:56 PM
 * To change this template use File | Settings | File Templates.
 */
public class AsIs<T> extends PhotoManagerLoader<T> {

    public AsIs(PhotoManager photoManager) {
        super(photoManager);
    }

    @Override
    protected Object doFetchImage(String path, int width, int height, LoadedCallback cb) {
        return photoManager.getImage(path, cb);
    }
}
