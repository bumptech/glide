package com.bumptech.glide.test;

import androidx.annotation.NonNull;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.annotation.GlideExtension;
import com.bumptech.glide.annotation.GlideType;

@GlideExtension
public final class ExtensionWithType {

  private ExtensionWithType() {
    // Utility class.
  }

  @NonNull
  @GlideType(Number.class)
  public static RequestBuilder<Number> asNumber(RequestBuilder<Number> builder) {
    return builder;
  }
}
