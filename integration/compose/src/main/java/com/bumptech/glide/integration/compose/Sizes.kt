@file:OptIn(InternalGlideApi::class)

package com.bumptech.glide.integration.compose

import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.integration.ktx.InternalGlideApi
import com.bumptech.glide.integration.ktx.Size
import com.bumptech.glide.integration.ktx.isValidGlideDimension
import kotlinx.coroutines.CompletableDeferred
import kotlin.math.roundToInt

internal class SizeObserver {
  private val size = CompletableDeferred<Size>()

  fun setSize(size: Size) {
    this.size.complete(size)
  }

  suspend fun getSize(): Size {
    return size.await()
  }
}

internal fun RequestBuilder<out Any?>.overrideSize(): Size? =
  if (isOverrideSizeSet()) {
    Size(overrideWidth, overrideHeight)
  } else {
    null
  }

internal fun RequestBuilder<out Any?>.isOverrideSizeSet(): Boolean =
  overrideWidth.isValidGlideDimension() && overrideHeight.isValidGlideDimension()

internal fun androidx.compose.ui.geometry.Size.toGlideSize(): Size? {
  val width = width.roundToInt();
  val height = height.roundToInt();
  if (!width.isValidGlideDimension() || !height.isValidGlideDimension()) {
    return null;
  }
  return Size(width, height);
}
