package com.bumptech.glide.integration.compose

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.semantics
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.RequestManager
import com.bumptech.glide.integration.ktx.AsyncGlideSize
import com.bumptech.glide.integration.ktx.ImmediateGlideSize
import com.bumptech.glide.integration.ktx.InternalGlideApi
import com.bumptech.glide.integration.ktx.ResolvableGlideSize
import com.bumptech.glide.integration.ktx.Size

public typealias RequestBuilderTransform<T> = (RequestBuilder<T>) -> RequestBuilder<T>

// TODO(judds): the API here is not particularly composeesque, we should consider alternatives
// to RequestBuilder (though thumbnail() may make that a challenge). We should also consider an
// alternative way to pass through the Model so that it doesn't have to be set on the request
// builder and passed through.
@ExperimentalGlideComposeApi
@OptIn(InternalGlideApi::class)
@Composable
public fun GlideImage(
  model: Any?,
  modifier: Modifier = Modifier,
  requestManager: RequestManager = LocalContext.current.let{ remember(it) { Glide.with(it) } },
  contentDescription: String?,
  alignment: Alignment = Alignment.Center,
  contentScale: ContentScale = ContentScale.Fit,
  alpha: Float = DefaultAlpha,
  colorFilter: ColorFilter? = null,
  // TODO(judds): Consider defaulting to load the model here instead of always doing so below.
  requestBuilderTransform: RequestBuilderTransform<Drawable> = { it },
) {
  val requestBuilder =
    rememberRequestBuilderWithDefaults(
      model, requestManager, requestBuilderTransform, contentScale
    )
  val overrideSize: Size? = requestBuilder.overrideSize()
  val (size, finalModifier) = rememberSizeAndModifier(overrideSize, modifier)

  SizedGlideImage(
    requestBuilder = requestBuilder,
    size = size,
    modifier = finalModifier,
    contentDescription = contentDescription,
    alignment = alignment,
    contentScale = contentScale,
    alpha = alpha,
    colorFilter = colorFilter,
  )
}

@OptIn(InternalGlideApi::class)
private data class SizeAndModifier(val size: ResolvableGlideSize, val modifier: Modifier)

@OptIn(InternalGlideApi::class)
@Composable
private fun rememberSizeAndModifier(
  overrideSize: Size?,
  modifier: Modifier,
) = remember (overrideSize, modifier) {
  var sizeStealer: SizeStealer? = null
  val size: ResolvableGlideSize =
    if (overrideSize != null) {
      ImmediateGlideSize(overrideSize)
    } else {
      sizeStealer = SizeStealer()
      AsyncGlideSize(sizeStealer::getSize)
    }
  val finalModifier =
    when (size) {
      is AsyncGlideSize -> modifier.sizeStealingModifier(sizeStealer!!)
      is ImmediateGlideSize -> modifier
    }
  SizeAndModifier(size, finalModifier)
}

@Composable
private fun rememberRequestBuilderWithDefaults(
  model: Any?,
  requestManager: RequestManager,
  requestBuilderTransform: RequestBuilderTransform<Drawable>,
  contentScale: ContentScale
) = remember(model, requestManager, requestBuilderTransform, contentScale) {
  val requestBuilder = requestManager.load(model)
  setDefaultTransform(requestBuilder, contentScale)
  requestBuilderTransform(requestBuilder)
}

private fun setDefaultTransform(
  requestBuilder: RequestBuilder<Drawable>, contentScale: ContentScale
) : RequestBuilder<Drawable> {
  return when (contentScale) {
    ContentScale.Crop -> {
      requestBuilder.centerCrop()
    }
    ContentScale.Inside, ContentScale.Fit -> {
      // Outside compose, glide would use fitCenter() for FIT. But that's probably not a good
      // decision given how unimportant Bitmap re-use is relative to minimizing texture sizes now.
      // So instead we'll do something different and prefer not to upscale, which means using
      // centerInside(). The UI can still scale the view even if the Bitmap is smaller.
      requestBuilder.centerInside()
    }
    else -> {
      requestBuilder
    }
  }
  // TODO(judds): Think about how to handle the various fills
}

@OptIn(InternalGlideApi::class)
@Composable
private fun SizedGlideImage(
  requestBuilder: RequestBuilder<Drawable>,
  size: ResolvableGlideSize,
  modifier: Modifier,
  contentDescription: String?,
  alignment: Alignment,
  contentScale: ContentScale,
  alpha: Float,
  colorFilter: ColorFilter?,
) {
  val painter =
    rememberGlidePainter(
      requestBuilder = requestBuilder,
      size = size,
    )
  Image(
    painter = painter,
    contentDescription = contentDescription,
    alignment = alignment,
    contentScale = contentScale,
    alpha = alpha,
    colorFilter = colorFilter,
    modifier = modifier.then(Modifier.semantics { displayedDrawable = painter.currentDrawable }),
  )
}

@OptIn(InternalGlideApi::class)
@Composable
private fun rememberGlidePainter(
  requestBuilder: RequestBuilder<Drawable>,
  size: ResolvableGlideSize,
): GlidePainter {
  val scope = rememberCoroutineScope()
  // TODO(judds): Calling onRemembered here manually might make a minor improvement in how quickly
  //  the image load is started, but it also triggers a recomposition. I can't figure out why it
  //  triggers a recomposition
  return remember(requestBuilder, size) { GlidePainter(requestBuilder, size, scope) }
}

@OptIn(InternalGlideApi::class)
private fun Modifier.sizeStealingModifier(sizeStealer: SizeStealer): Modifier =
  this.layout { measurable, constraints ->
    sizeStealer.setSize(constraints.inferredGlideSize())
    val placeable = measurable.measure(constraints)
    layout(placeable.width, placeable.height) {
      placeable.place(0, 0)
    }
  }

internal val DisplayedDrawableKey = SemanticsPropertyKey<MutableState<Drawable?>>("DisplayedDrawable")
internal var SemanticsPropertyReceiver.displayedDrawable by DisplayedDrawableKey
