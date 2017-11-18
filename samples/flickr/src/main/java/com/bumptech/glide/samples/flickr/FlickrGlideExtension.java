package com.bumptech.glide.samples.flickr;

import com.bumptech.glide.annotation.GlideExtension;
import com.bumptech.glide.annotation.GlideOption;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.samples.flickr.api.Api;

/**
 * Extension methods for the Flickr sample's generated API.
 */
// Required by Glide's annotation processor.
@SuppressWarnings("WeakerAccess")
@GlideExtension
public final class FlickrGlideExtension {

  private FlickrGlideExtension() {
    // Utility class.
  }

  @GlideOption
  public static RequestOptions squareThumb(RequestOptions requestOptions) {
    return requestOptions
        .centerCrop();
  }

  @GlideOption
  public static RequestOptions squareMiniThumb(RequestOptions requestOptions) {
    return requestOptions.centerCrop().override(Api.SQUARE_THUMB_SIZE);
  }

  @GlideOption(override = GlideOption.OVERRIDE_EXTEND)
  public static RequestOptions centerCrop(RequestOptions options) {
    return options;
    // Empty.
  }
}
