package com.bumptech.glide.test;

import androidx.annotation.NonNull;
import com.bumptech.glide.annotation.GlideExtension;
import com.bumptech.glide.annotation.GlideOption;
import com.bumptech.glide.request.BaseRequestOptions;

@GlideExtension
public final class ExtensionWithOption {

  private ExtensionWithOption() {
    // Utility class.
  }

  @NonNull
  @GlideOption
  public static BaseRequestOptions<?> squareThumb(BaseRequestOptions<?> requestOptions) {
    return requestOptions.centerCrop();
  }
}
