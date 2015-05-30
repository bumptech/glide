package com.bumptech.glide.samples.flickr.api;

import android.os.Parcelable;

/** An interface representing a query in Flickr's API that returns a list of photos. */
public interface Query extends Parcelable {
  /** A user facing description of the query. */
  String getDescription();
  /** The url to use to execute the query. */
  String getUrl();
}
