package com.bumptech.photos.imagemanager.loader;

import com.bumptech.photos.imagemanager.LoadedCallback;
import com.bumptech.photos.imagemanager.ImageManager;

/**
 * Created with IntelliJ IDEA.
 * User: sam
 * Date: 12/31/12
 * Time: 4:56 PM
 * To change this template use File | Settings | File Templates.
 */
public class AsIs<T> extends PhotoManagerLoader<T> {

    public AsIs(ImageManager imageManager) {
        super(imageManager);
    }

    @Override
    protected Object doFetchImage(String path, int width, int height, LoadedCallback cb) {
        return imageManager.getImage(path, cb);
    }
}
