package com.bumptech.glide.samples.flickr;

import com.bumptech.glide.samples.flickr.api.Photo;

import java.util.List;

public interface PhotoViewer {
    public void onPhotosUpdated(List<Photo> photos);
}
