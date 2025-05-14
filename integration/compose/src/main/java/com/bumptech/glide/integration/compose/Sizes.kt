@file:OptIn(InternalGlideApi::class)

package com.bumptech.glide.integration.compose

import androidx.compose.ui.unit.Constraints
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.integration.ktx.InternalGlideApi
import com.bumptech.glide.integration.ktx.Size
import com.bumptech.glide.integration.ktx.isValidGlideDimension
import com.bumptech.glide.request.target.Target
import kotlinx.coroutines.CompletableDeferred

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

internal fun Constraints.inferredGlideSize(): Size? {
  val width = if (hasBoundedWidth) maxWidth else Target.SIZE_ORIGINAL
  val height = if (hasBoundedHeight) maxHeight else Target.SIZE_ORIGINAL
  if (!width.isValidGlideDimension() || !height.isValidGlideDimension()) {
    return null
  }
  return Size(width, height)
}

