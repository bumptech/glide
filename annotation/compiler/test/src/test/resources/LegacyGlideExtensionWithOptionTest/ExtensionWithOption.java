package com.bumptech.glide.test;

import com.bumptech.glide.annotation.GlideExtension;
import com.bumptech.glide.annotation.GlideOption;
import com.bumptech.glide.request.RequestOptions;

@GlideExtension
public final class ExtensionWithOption {

  private ExtensionWithOption() {
    // Utility class.
  }

  @GlideOption
  public static void squareThumb(RequestOptions requestOptions) {
    requestOptions.centerCrop();
  }
}
