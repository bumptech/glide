package com.bumptech.flickr;

import com.bumptech.flickr.api.Photo;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: sam
 * Date: 1/10/13
 * Time: 11:45 AM
 * To change this template use File | Settings | File Templates.
 */
public interface PhotoViewer {
    public void onPhotosUpdated(List<Photo> photos);
}
