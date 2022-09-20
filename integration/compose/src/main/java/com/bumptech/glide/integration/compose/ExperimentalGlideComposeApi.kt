package com.bumptech.glide.integration.compose

@RequiresOptIn(
  level = RequiresOptIn.Level.ERROR,
  message =
    "Glide's Compose integration is experimental. APIs may change or be removed without" +
      " warning."
)
@Retention(AnnotationRetention.BINARY)
@kotlin.annotation.Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
public annotation class ExperimentalGlideComposeApi
