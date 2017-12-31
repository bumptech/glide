package com.bumptech.glide.test;

import android.support.annotation.NonNull;
import com.bumptech.glide.annotation.GlideExtension;
import com.bumptech.glide.annotation.GlideOption;
import com.bumptech.glide.request.RequestOptions;

@GlideExtension
public final class Extension {

  private Extension() {
    // Utility class.
  }

  @NonNull
  @GlideOption(override = GlideOption.OVERRIDE_REPLACE)
  public static RequestOptions centerCrop(RequestOptions requestOptions) {
    return requestOptions.centerCrop();
  }
}
