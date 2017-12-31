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
  @GlideOption(staticMethodName = "testSomething")
  public static RequestOptions test(RequestOptions requestOptions) {
    return requestOptions.centerCrop();
  }
}
