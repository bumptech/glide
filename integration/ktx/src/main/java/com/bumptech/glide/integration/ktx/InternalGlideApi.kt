package com.bumptech.glide.integration.ktx

@RequiresOptIn(
  level = RequiresOptIn.Level.ERROR,
  message =
    "An internal only API not intended for public use, may change, break or be removed" +
      " at any time without warning."
)
@Retention(AnnotationRetention.BINARY)
@kotlin.annotation.Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
public annotation class InternalGlideApi
