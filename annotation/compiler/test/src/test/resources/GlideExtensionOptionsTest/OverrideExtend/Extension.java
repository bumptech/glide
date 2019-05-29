package com.bumptech.glide.test;

import androidx.annotation.NonNull;
import com.bumptech.glide.annotation.GlideExtension;
import com.bumptech.glide.annotation.GlideOption;
import com.bumptech.glide.request.BaseRequestOptions;

@GlideExtension
public final class Extension {

  private Extension() {
    // Utility class.
  }

  @NonNull
  @GlideOption(override = GlideOption.OVERRIDE_EXTEND)
  public static BaseRequestOptions<?> centerCrop(BaseRequestOptions<?> requestOptions) {
    return requestOptions.centerCrop();
  }
}
