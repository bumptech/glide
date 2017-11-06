package com.bumptech.glide.test;

import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.annotation.GlideExtension;
import com.bumptech.glide.annotation.GlideOption;
import com.bumptech.glide.annotation.GlideType;
import com.bumptech.glide.request.RequestOptions;

@GlideExtension
public final class ExtensionWithType {

  private ExtensionWithType() {
    // Utility class.
  }

  @GlideType(Number.class)
  public static void asInteger(RequestBuilder<Number> builder) {}
}
