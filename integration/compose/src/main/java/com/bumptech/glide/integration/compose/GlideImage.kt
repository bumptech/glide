package com.bumptech.glide.integration.compose

import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.DataSource

/** Mutates and returns the given [RequestBuilder] to apply relevant options. */
public typealias RequestBuilderTransform<T> = (RequestBuilder<T>) -> RequestBuilder<T>

/**
 * Start a request by passing [model] to [RequestBuilder.load] and then applying the
 * [requestBuilderTransform] function to add options or apply mutations if the caller desires.
 *
 * [alignment], [contentScale], [alpha], [colorFilter] and [contentDescription] have the same
 * defaults (if any) and function identically to the parameters in [Image].
 *
 * Set the size this [Composable] using the given [modifier]. Use fixed sizes when you can for
 * better performance and to avoid layout jank when images are loaded. If you cannot use a fixed
 * size, try to at least set a bounded size. If the size set via the [modifier] is
 * dependent on the content, Glide will probably end up loading the image using
 * [com.bumptech.glide.request.target.Target.SIZE_ORIGINAL]. Avoid `SIZE_ORIGINAL`, implicitly or
 * explicitly if you can. You may end up loading a substantially larger image than you need, which
 * will increase memory usage and may also increase latency.
 *
 * You can force the size of the image you load to be different than the display area using
 * [RequestBuilder.override].
 *
 * [contentScale] will apply to both [loading] and [error] placeholders, as well as the the primary
 * request. If you'd like different scaling behavior for placeholders vs the primary request, use
 * [contentScale] to scale the placeholders and [requestBuilderTransform] to set a different
 * `Transformation` for the image load. [contentScale] will also be inspected to apply a matching
 * default `Transformation` if one exists. Any automatically applied `Transformation` based on
 * [contentScale] can be overridden using [requestBuilderTransform] via [RequestBuilder.transform]
 * or [RequestBuilder.dontTransform].
 *
 * [requestBuilderTransform] is overridden by any overlapping parameter defined in this method if
 * that parameter is non-null. For example, [loading] and [failure], if non-null will be used in
 * place of any placeholder set by [requestBuilderTransform] using [RequestBuilder.placeholder] or
 * [RequestBuilder.error]. Transitions set via [RequestBuilder.transition] are ignored.
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
 * @param transition An optional [Transition.Factory] that can animate the transition from a
 * placeholder to a loaded image. The transition will only be called once, when the load transitions
 * from showing the placeholder to showing the first resource. The transition will persist across
 * multiple resources if you're using thumbnail, but will not be called for each successive resource
 * in the request chain. The transition factory will not be called across different requests if
 * multiple are made. The transition will not be called if you use [placeholder] or [failure] with
 * the deprecated [Composable] API. See [CrossFade]
 */
// TODO(judds): the API here is not particularly composeesque, we should consider alternatives
// to RequestBuilder (though thumbnail() may make that a challenge).
@ExperimentalGlideComposeApi
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
  transition: Transition.Factory? = null,
  // TODO(judds): Consider defaulting to load the model here instead of always doing so below.
  requestBuilderTransform: RequestBuilderTransform<Drawable> = { it },
) {
  val requestManager: RequestManager = LocalContext.current.let { remember(it) { Glide.with(it) } }
  val requestBuilder =
    rememberRequestBuilderWithDefaults(model, requestManager, requestBuilderTransform, contentScale)
      .let { loading?.apply(it::placeholder, it::placeholder) ?: it }
      .let { failure?.apply(it::error, it::error) ?: it }


  // TODO(judds): It seems like we should be able to use the production paths for
  // resource / drawables as well as Composables. It's not totally clear what part of the prod code
  // isn't supported.
  if (LocalInspectionMode.current && loading?.isResourceOrDrawable() == true) {
    PreviewResourceOrDrawable(loading, contentDescription, modifier)
    return
  }

  // TODO(sam): Remove this branch when GlideSubcomposition has been out for a bit.
  val loadingComposable = loading?.maybeComposable()
  val failureComposable = failure?.maybeComposable()
  if (loadingComposable != null || failureComposable != null) {
    GlideSubcomposition(model, modifier, { requestBuilder }) {
      if (state == RequestState.Loading && loadingComposable != null) {
        loadingComposable()
      } else if (state == RequestState.Failure && failureComposable != null) {
        failureComposable()
      } else {
        Image(
          painter,
          contentDescription,
          modifier,
          alignment,
          contentScale,
          alpha,
          colorFilter,
        )
      }
    }
  } else {
    SimpleLayout(
      modifier
        .glideNode(
          requestBuilder,
          contentDescription,
          alignment,
          contentScale,
          alpha,
          colorFilter,
          transition,
        )
    )
  }
}

/**
 * Provides the current state of the request and a [Painter] to draw it.
 */
@ExperimentalGlideComposeApi
public interface GlideSubcompositionScope {
  /** The current state of the request, slightly simplified over Glide's standard request state. */
  public val state: RequestState

  /**
   * A painter that will draw the placeholder or resource matching the current request state. If no
   * placeholder or resource is available currently, the painter will draw transparent.
   */
  public val painter: Painter
}

@ExperimentalGlideComposeApi
internal class GlideSubcompositionScopeImpl(
  private val drawable: Drawable?,
  override val state: RequestState
) : GlideSubcompositionScope {

  override val painter: Painter
    get() = drawable?.toPainter() ?: ColorPainter(Color.Transparent)

}

/**
 * The current state of a request associated with a Glide painter.
 *
 * This state is a bit of a simplification over Glide's real state. In particular [Success] is
 * used in any case where we have an image, even if that image is the thumbnail of a full request
 * where the full request has failed. From the point of view of the UI this is usually reasonable
 * and a significant simplification of this API.
 */
@ExperimentalGlideComposeApi
public sealed class RequestState {

  @ExperimentalGlideComposeApi
  public object Loading : RequestState()

  /**
   * Indicates the load finished successfully (or at least one thumbnail was loaded, see the details
   * on [RequestState]).
   *
   * @param dataSource The data source the latest image was loaded from. If your request uses one
   * or more thumbnails this value may change as each successive thumbnail is loaded.
   */
  @ExperimentalGlideComposeApi
  public data class Success(
    val dataSource: DataSource,
  ) : RequestState()

  @ExperimentalGlideComposeApi
  public object Failure : RequestState()
}

/**
 * Starts an image load with Glide, exposing the state of the load via [GlideSubcompositionScope]
 * to allow complex subcompositions or animations that depend on the load's state.
 *
 * [GlideImage] is significantly more efficient and easier to use than this method. GlideImage
 * should be preferred over GlideSubcomposition whenever possible. Using GlideSubcomposition in a
 * scrolling list will cause multiple recompositions per image, significantly degrading performance.
 * The use case for this method is as a fallback for cases where you cannot animate or compose your
 * layout without knowing the status of the image load request.
 *
 * All that said, you can use this class to display custom placeholders and/or animations. For
 * example to start an animation when a load completes, you might do something like:
 *
 * ```
 * GlideSubcomposition(model = uri, modifier) {
 *   when (state) {
 *     RequestState.Loading -> ShowLoadingUi()
 *     RequestState.Failure -> ShowFailureUi()
 *     is RequestState.Success -> {
 *       if (state.dataSource != DataSource.MEMORY_CACHE) {
 *         ShowSomeComplexAnimation()
 *       } else {
 *         DoSomethingNormal()
 *       }
 *     }
 *   }
 * }
 * ```
 *
 * [RequestState.Success] contains the [DataSource] where the image was loaded from so that you can
 * avoid animating or otherwise change your composition if the image was loaded from the memory
 * cache. Typically you do not want to animate loads from the memory cache.
 *
 * If your [requestBuilderTransform] does not have an [overrideSize] set, this method will wrap your
 * subcomposition in [Box] and use the size of that `Box` to determine the
 * size to use when loading the image. The box's modifier will be set to the [modifier] you provide.
 * As with [GlideImage] try to ensure that you either set a reasonable [RequestBuilder.override]
 * size using [requestBuilderTransform] or that you provide a [modifier] that will cause this
 * composition to go through layout with a reasonable size. Failing to do so may result in the image
 * load never starting, or in an unreasonably large amount of memory being used. Loading overly
 * large images in memory can also impact scrolling performance.
 */
@ExperimentalGlideComposeApi
@Composable
public fun GlideSubcomposition(
  model: Any?,
  modifier: Modifier = Modifier,
  requestBuilderTransform: RequestBuilderTransform<Drawable> = { it },
  content: @Composable GlideSubcompositionScope.() -> Unit
) {
  val requestManager: RequestManager = LocalContext.current.let { remember(it) { Glide.with(it) } }
  val requestBuilder =
    remember(model, requestManager, requestBuilderTransform) {
      requestBuilderTransform(requestManager.load(model))
    }

  val requestState: MutableState<RequestState> =
    remember(model, requestManager, requestBuilderTransform) {
      mutableStateOf(RequestState.Loading)
    }
  val drawable: MutableState<Drawable?> = remember(model, requestManager, requestBuilderTransform) {
    mutableStateOf(null)
  }

  val requestListener: StateTrackingListener =
    remember(model, requestManager, requestBuilderTransform) {
      StateTrackingListener(
        requestState,
        drawable
      )
    }

  val scope = GlideSubcompositionScopeImpl(drawable.value, requestState.value)

  Box(
    modifier
      .glideNode(
        requestBuilder,
        draw = false,
        requestListener = requestListener,
      )
  ) {
    scope.content()
  }
}

@ExperimentalGlideComposeApi
private class StateTrackingListener(
  val state: MutableState<RequestState>,
  val drawable: MutableState<Drawable?>
) : RequestListener {

  override fun onStateChanged(model: Any?, drawable: Drawable?, requestState: RequestState) {
    state.value = requestState
    this.drawable.value = drawable
  }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun PreviewResourceOrDrawable(
  loading: Placeholder,
  contentDescription: String?,
  modifier: Modifier,
) {
  val drawable =
    when (loading) {
      is Placeholder.OfDrawable -> loading.drawable
      is Placeholder.OfResourceId -> LocalContext.current.getDrawable(loading.resourceId)
      is Placeholder.OfComposable ->
        throw IllegalArgumentException("Composables should go through the production codepath")
    }
  Image(
    painter = remember(drawable) { drawable.toPainter() },
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
@Deprecated(
  "Using this method forces recomposition when the image load state changes." +
      " If you require this behavior use GlideSubcomposition instead",
  level = DeprecationLevel.WARNING
)
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


@Composable
private fun SimpleLayout(
  modifier: Modifier,
) {
  Layout(
    modifier
  ) { _, constraints ->
    layout(constraints.minWidth, constraints.minHeight) {}
  }
}
