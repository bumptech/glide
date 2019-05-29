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
  @GlideOption(memoizeStaticMethod = true)
  public static BaseRequestOptions<?> test(BaseRequestOptions<?> requestOptions) {
    return requestOptions.centerCrop();
  }
}
