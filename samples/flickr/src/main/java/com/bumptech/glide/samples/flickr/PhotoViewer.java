package com.bumptech.glide.samples.flickr;

import com.bumptech.glide.samples.flickr.api.Photo;
import java.util.List;

/**
 * An interface for an object that displays {@link com.bumptech.glide.samples.flickr.api.Photo}
 * objects.
 */
interface PhotoViewer {
  /**
   * Called whenever new {@link com.bumptech.glide.samples.flickr.api.Photo}s are loaded.
   *
   * @param photos The loaded photos.
   */
  void onPhotosUpdated(List<Photo> photos);
}
