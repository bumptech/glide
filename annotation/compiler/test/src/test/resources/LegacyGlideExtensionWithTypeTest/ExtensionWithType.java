package com.bumptech.glide.test;

import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.annotation.GlideExtension;
import com.bumptech.glide.annotation.GlideType;

@GlideExtension
public final class ExtensionWithType {

  private ExtensionWithType() {
    // Utility class.
  }

  @GlideType(Number.class)
  public static void asInteger(RequestBuilder<Number> builder) {}
}
