package com.bumptech.glide.samples.flickr;

import android.support.annotation.NonNull;
import com.bumptech.glide.annotation.GlideExtension;
import com.bumptech.glide.annotation.GlideOption;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.samples.flickr.api.Api;

/**
 * Extension methods for the Flickr sample's generated API.
 */
// Required by Glide's annotation processor.
@SuppressWarnings({"WeakerAccess", "unused"})
@GlideExtension
public final class FlickrGlideExtension {

  private FlickrGlideExtension() {
    // Utility class.
  }

  @NonNull
  @GlideOption
  public static RequestOptions squareThumb(RequestOptions requestOptions) {
    return requestOptions.centerCrop();
  }

  @NonNull
  @GlideOption
  public static RequestOptions squareMiniThumb(RequestOptions requestOptions) {
    return requestOptions.centerCrop().override(Api.SQUARE_THUMB_SIZE);
  }
}
