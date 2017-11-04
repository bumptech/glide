package com.bumptech.glide.test;

import com.bumptech.glide.annotation.GlideExtension;
import com.bumptech.glide.annotation.GlideOption;
import com.bumptech.glide.request.RequestOptions;

@GlideExtension
public final class Extension {

  private Extension() {
    // Utility class.
  }

  @GlideOption(override = GlideOption.OVERRIDE_REPLACE)
  public static void centerCrop(RequestOptions requestOptions) {
    requestOptions.centerCrop();
  }
}
