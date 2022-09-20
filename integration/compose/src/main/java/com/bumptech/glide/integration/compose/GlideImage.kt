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

/** Mutates and returns the given [RequestBuilder] to apply relevant options. */
public typealias RequestBuilderTransform<T> = (RequestBuilder<T>) -> RequestBuilder<T>

/**
 * Start a request by passing [model] to [RequestBuilder.load] using the given [requestManager] and
 * then applying the [requestBuilderTransform] function to add options or apply mutations if the
 * caller desires.
 *
 * [alignment], [contentScale], [alpha], [colorFilter] and [contentDescription] have the same
 * defaults (if any) and function identically to the parameters in [Image].
 *
 * If you want to restrict the size of this [Composable], use the given [modifier]. If you'd like to
 * force the size of the pixels you load to be different than the display area, use
 * [RequestBuilder.override]. Often you can get better performance by setting an explicit size so
 * that we do not have to wait for layout to fetch the image. If the size set via the [modifier] is
 * dependent on the content, Glide will probably end up loading the image using
 * [com.bumptech.glide.request.target.Target.SIZE_ORIGINAL]. Avoid `SIZE_ORIGINAL`, implicitly or
 * explicitly if you can. You may end up loading a substantially larger image than you need, which
 * will increase memory usage and may also increase latency.
 *
 * If you provide your own [requestManager] rather than using this method's default, consider using
 * [remember] at a higher level to avoid some amount of overhead of retrieving it each
 * re-composition.
 *
 * This method will inspect [contentScale] and apply a matching transformation if one exists. Any
 * automatically applied transformation can be overridden using [requestBuilderTransform]. Either
 * apply a specific transformation instead, or use [RequestBuilder.dontTransform]]
 *
 * Transitions set via [RequestBuilder.transition] are currently ignored.
 *
 * Note - this method is likely to change while we work on improving the API. Transitions are one
 * significant unexplored area. It's also possible we'll try and remove the [RequestBuilder] from
 * the direct API and instead allow all options to be set directly in the method.
 */
// TODO(judds): the API here is not particularly composeesque, we should consider alternatives
// to RequestBuilder (though thumbnail() may make that a challenge).
// TODO(judds): Consider how to deal with transitions.
@ExperimentalGlideComposeApi
@OptIn(InternalGlideApi::class)
@Composable
public fun GlideImage(
  model: Any?,
  contentDescription: String?,
  modifier: Modifier = Modifier,
  alignment: Alignment = Alignment.Center,
  contentScale: ContentScale = ContentScale.Fit,
  alpha: Float = DefaultAlpha,
  colorFilter: ColorFilter? = null,
  // TODO(judds): Consider defaulting to load the model here instead of always doing so below.
  requestBuilderTransform: RequestBuilderTransform<Drawable> = { it },
) {
  val requestManager: RequestManager = LocalContext.current.let { remember(it) { Glide.with(it) } }
  val requestBuilder =
    rememberRequestBuilderWithDefaults(model, requestManager, requestBuilderTransform, contentScale)
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
) =
  remember(overrideSize, modifier) {
    if (overrideSize != null) {
      SizeAndModifier(ImmediateGlideSize(overrideSize), modifier)
    } else {
      val sizeObserver = SizeObserver()
      SizeAndModifier(
        AsyncGlideSize(sizeObserver::getSize),
        modifier.sizeObservingModifier(sizeObserver)
      )
    }
  }

@Composable
private fun rememberRequestBuilderWithDefaults(
  model: Any?,
  requestManager: RequestManager,
  requestBuilderTransform: RequestBuilderTransform<Drawable>,
  contentScale: ContentScale
) =
  remember(model, requestManager, requestBuilderTransform, contentScale) {
    requestBuilderTransform(requestManager.load(model).contentScaleTransform(contentScale))
  }

private fun RequestBuilder<Drawable>.contentScaleTransform(
  contentScale: ContentScale
): RequestBuilder<Drawable> {
  return when (contentScale) {
    ContentScale.Crop -> {
      centerCrop()
    }
    ContentScale.Inside,
    ContentScale.Fit -> {
      // Outside compose, glide would use fitCenter() for FIT. But that's probably not a good
      // decision given how unimportant Bitmap re-use is relative to minimizing texture sizes now.
      // So instead we'll do something different and prefer not to upscale, which means using
      // centerInside(). The UI can still scale the view even if the Bitmap is smaller.
      centerInside()
    }
    else -> {
      this
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
private fun Modifier.sizeObservingModifier(sizeObserver: SizeObserver): Modifier =
  this.layout { measurable, constraints ->
    sizeObserver.setSize(constraints.inferredGlideSize())
    val placeable = measurable.measure(constraints)
    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
  }

internal val DisplayedDrawableKey =
  SemanticsPropertyKey<MutableState<Drawable?>>("DisplayedDrawable")
internal var SemanticsPropertyReceiver.displayedDrawable by DisplayedDrawableKey
