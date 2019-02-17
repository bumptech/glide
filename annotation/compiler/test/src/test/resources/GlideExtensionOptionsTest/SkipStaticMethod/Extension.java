package com.bumptech.glide.test;

import android.support.annotation.NonNull;
import com.bumptech.glide.annotation.GlideExtension;
import com.bumptech.glide.annotation.GlideOption;
import com.bumptech.glide.request.BaseRequestOptions;

@GlideExtension
public final class Extension {

  private Extension() {
    // Utility class.
  }

  @NonNull
  @GlideOption(skipStaticMethod = true)
  public static BaseRequestOptions<?> test(BaseRequestOptions<?> requestOptions) {
    return requestOptions.centerCrop();
  }
}
