package com.bumptech.glide.integration.compose

import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.semantics
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.RequestManager
import com.bumptech.glide.integration.ktx.AsyncGlideSize
import com.bumptech.glide.integration.ktx.ExperimentGlideFlows
import com.bumptech.glide.integration.ktx.ImmediateGlideSize
import com.bumptech.glide.integration.ktx.InternalGlideApi
import com.bumptech.glide.integration.ktx.ResolvableGlideSize
import com.bumptech.glide.integration.ktx.Size
import com.bumptech.glide.integration.ktx.Status
import com.google.accompanist.drawablepainter.rememberDrawablePainter

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
 *
 * [requestBuilderTransform] is overridden by any overlapping parameter defined in this method if
 * that parameter is non-null. For example, [loading] and [failure], if non-null will be used in
 * place of any placeholder set by [requestBuilderTransform] using [RequestBuilder.placeholder] or
 * [RequestBuilder.error].
 *
 * @param loading A [Placeholder] that will be displayed while the request is loading. Specifically
 * it's used if the request is cleared ([com.bumptech.glide.request.target.Target.onLoadCleared]) or
 * loading ([com.bumptech.glide.request.target.Target.onLoadStarted]. There's a subtle difference in
 * behavior depending on which type of [Placeholder] you use. The resource and `Drawable` variants
 * will be displayed if the request fails and no other failure handling is specified, but the
 * `Composable` will not.
 * @param failure A [Placeholder] that will be displayed if the request fails. Specifically it's
 * used when [com.bumptech.glide.request.target.Target.onLoadFailed] is called. If
 * [RequestBuilder.error] is called in [requestBuilderTransform] with a valid [RequestBuilder] (as
 * opposed to resource id or [Drawable]), this [Placeholder] will not be used unless the `error`
 * [RequestBuilder] also fails. This parameter does not override error [RequestBuilder]s, only error
 * resource ids and/or [Drawable]s.
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
  // TODO(judds): Consider using separate GlideImage* methods instead of sealed classes.
  // See http://shortn/_x79pjkMZIH for an internal discussion.
  loading: Placeholder? = null,
  failure: Placeholder? = null,
  // TODO(judds): Consider defaulting to load the model here instead of always doing so below.
  requestBuilderTransform: RequestBuilderTransform<Drawable> = { it },
) {
  val requestManager: RequestManager = LocalContext.current.let { remember(it) { Glide.with(it) } }
  val requestBuilder =
    rememberRequestBuilderWithDefaults(model, requestManager, requestBuilderTransform, contentScale)
      .let { loading?.apply(it::placeholder, it::placeholder) ?: it }
      .let { failure?.apply(it::error, it::error) ?: it }

  val overrideSize: Size? = requestBuilder.overrideSize()
  val (size, finalModifier) = rememberSizeAndModifier(overrideSize, modifier)

  // TODO(judds): It seems like we should be able to use the production paths for
  // resource / drawables as well as Composables. It's not totally clear what part of the prod code
  // isn't supported.
  if (LocalInspectionMode.current && loading?.isResourceOrDrawable() == true) {
    PreviewResourceOrDrawable(loading, contentDescription, modifier)
    return
  }

  SizedGlideImage(
    requestBuilder = requestBuilder,
    size = size,
    modifier = finalModifier,
    contentDescription = contentDescription,
    alignment = alignment,
    contentScale = contentScale,
    alpha = alpha,
    colorFilter = colorFilter,
    placeholder = loading?.maybeComposable(),
    failure = failure?.maybeComposable(),
  )
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun PreviewResourceOrDrawable(
  loading: Placeholder,
  contentDescription: String?,
  modifier: Modifier,
) {
  val drawable =
    when(loading) {
      is Placeholder.OfDrawable -> loading.drawable
      is Placeholder.OfResourceId -> LocalContext.current.getDrawable(loading.resourceId)
      is Placeholder.OfComposable ->
        throw IllegalArgumentException("Composables should go through the production codepath")
    }
  Image(
    painter = rememberDrawablePainter(drawable),
    modifier = modifier,
    contentDescription = contentDescription,
  )
}

/**
 * Used to specify a [Drawable] to use in conjunction with [GlideImage]'s `loading` or `failure`
 * parameters.
 *
 * Ideally [drawable] is non-null, but because [android.content.Context.getDrawable] can return
 * null, we allow it here. `placeholder(null)` has the same override behavior as if a non-null
 * `Drawable` were provided.
 */
@ExperimentalGlideComposeApi
public fun placeholder(drawable: Drawable?): Placeholder = Placeholder.OfDrawable(drawable)

/**
 * Used to specify a resource id to use in conjunction with [GlideImage]'s `loading` or `failure`
 * parameters.
 *
 * In addition to being slightly simpler than manually fetching a [Drawable] and passing it to
 * [placeholder], this method can be more efficient because the [Drawable] will only be loaded when
 * needed.
 */
@ExperimentalGlideComposeApi
public fun placeholder(@DrawableRes resourceId: Int): Placeholder =
  Placeholder.OfResourceId(resourceId)

/**
 * Used to specify a [Composable] function to use in conjunction with [GlideImage]'s `loading` or
 * `failure` parameter.
 *
 * Providing a nested [GlideImage] is not recommended. Use [RequestBuilder.thumbnail] or
 * [RequestBuilder.error] as an alternative.
 */
@ExperimentalGlideComposeApi
public fun placeholder(composable: @Composable () -> Unit): Placeholder =
  Placeholder.OfComposable(composable)

/**
 * Content to display during a particular state of a Glide Request, for example while the request is
 * loading or if the request fails.
 *
 * `of(Drawable)` and `of(resourceId)` trigger fewer recompositions than `of(@Composable () ->
 * Unit)` so you should only use the Composable variant if you require something more complex than a
 * simple color or a static image.
 *
 * `of(@Composable () -> Unit)` will display the [Composable] inside a [Box] whose modifier is the
 * one provided to [GlideImage]. Doing so allows Glide to infer the requested size if one is not
 * explicitly specified on the request itself.
 */
@ExperimentalGlideComposeApi
public sealed class Placeholder {
  internal class OfDrawable(internal val drawable: Drawable?) : Placeholder()
  internal class OfResourceId(@DrawableRes internal val resourceId: Int) : Placeholder()
  internal class OfComposable(internal val composable: @Composable () -> Unit) : Placeholder()

  internal fun isResourceOrDrawable() =
    when (this) {
      is OfDrawable -> true
      is OfResourceId -> true
      is OfComposable -> false
    }

  internal fun maybeComposable(): (@Composable () -> Unit)? =
    when (this) {
      is OfComposable -> this.composable
      else -> null
    }

  internal fun <T> apply(
    resource: (Int) -> RequestBuilder<T>,
    drawable: (Drawable?) -> RequestBuilder<T>
  ): RequestBuilder<T> =
    when (this) {
      is OfDrawable -> drawable(this.drawable)
      is OfResourceId -> resource(this.resourceId)
      // Clear out any previously set placeholder.
      else -> drawable(null)
    }
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
      optionalCenterCrop()
    }
    ContentScale.Inside,
    ContentScale.Fit -> {
      // Outside compose, glide would use fitCenter() for FIT. But that's probably not a good
      // decision given how unimportant Bitmap re-use is relative to minimizing texture sizes now.
      // So instead we'll do something different and prefer not to upscale, which means using
      // centerInside(). The UI can still scale the view even if the Bitmap is smaller.
      optionalCenterInside()
    }
    else -> {
      this
    }
  }
  // TODO(judds): Think about how to handle the various fills
}

@OptIn(InternalGlideApi::class, ExperimentGlideFlows::class)
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
  placeholder: @Composable (() -> Unit)?,
  failure: @Composable (() -> Unit)?,
) {
  // Use a Box so we can infer the size if the request doesn't have an explicit size.
  @Composable fun @Composable () -> Unit.boxed() = Box(modifier = modifier) { this@boxed() }

  val painter =
    rememberGlidePainter(
      requestBuilder = requestBuilder,
      size = size,
    )
  if (placeholder != null && painter.status.showPlaceholder()) {
    placeholder.boxed()
  } else if (failure != null && painter.status == Status.FAILED) {
    failure.boxed()
  } else {
    Image(
      painter = painter,
      contentDescription = contentDescription,
      alignment = alignment,
      contentScale = contentScale,
      alpha = alpha,
      colorFilter = colorFilter,
      modifier = modifier.then(Modifier.semantics { displayedDrawable = painter.currentDrawable })
    )
  }
}

@OptIn(ExperimentGlideFlows::class)
private fun Status.showPlaceholder(): Boolean =
  when (this) {
    Status.RUNNING -> true
    Status.CLEARED -> true
    else -> false
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
    val inferredSize = constraints.inferredGlideSize()
    if (inferredSize != null) {
      sizeObserver.setSize(inferredSize)
    }
    val placeable = measurable.measure(constraints)
    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
  }

internal val DisplayedDrawableKey =
  SemanticsPropertyKey<MutableState<Drawable?>>("DisplayedDrawable")
internal var SemanticsPropertyReceiver.displayedDrawable by DisplayedDrawableKey
