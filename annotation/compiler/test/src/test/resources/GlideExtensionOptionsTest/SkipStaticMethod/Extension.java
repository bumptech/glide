package com.bumptech.glide.test;

import com.bumptech.glide.annotation.GlideExtension;
import com.bumptech.glide.annotation.GlideOption;
import com.bumptech.glide.request.RequestOptions;

@GlideExtension
public final class Extension {

  private Extension() {
    // Utility class.
  }

  @GlideOption(skipStaticMethod = true)
  public static RequestOptions test(RequestOptions requestOptions) {
    return requestOptions.centerCrop();
  }
}
