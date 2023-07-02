package com.bumptech.glide.integration.compose

import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.RequestManager
import com.bumptech.glide.integration.compose.GlidePainter.State
import com.bumptech.glide.integration.ktx.AsyncGlideSize
import com.bumptech.glide.integration.ktx.ExperimentGlideFlows
import com.bumptech.glide.integration.ktx.ImmediateGlideSize
import com.bumptech.glide.integration.ktx.InternalGlideApi
import com.bumptech.glide.integration.ktx.Placeholder
import com.bumptech.glide.integration.ktx.ResolvableGlideSize
import com.bumptech.glide.integration.ktx.Resource
import com.bumptech.glide.integration.ktx.Status
import com.bumptech.glide.integration.ktx.flowResolvable
import com.bumptech.glide.load.DataSource
import com.google.accompanist.drawablepainter.DrawablePainter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import java.lang.IllegalStateException

/**
 * Exposes a [Painter] and the painter's [State] so that they can be composed with other
 * Composables like [androidx.compose.foundation.Image].
 *
 * [GlideImage] is easier to use than this method and should be preferred unless you have a use case
 * that requires [State].
 *
 * See also the other variant of [rememberGlidePainter] and [rememberSizeAndModifier].
 *
 * You can use this class to display custom placeholders and/or animations. For example to crossfade
 * from a placeholder to a real image, you might do something like this:
 *
 * ```
 * val painter = rememberGlidePainter(model = item.uri) {
 *   it.override(targetImageSize)
 * }
 * when (painter.state.animate()) {
 *  AnimationState.Loading, AnimationState.Animate -> {
 *    Crossfade(painter.state == AnimationState.Loading) {
 *      if (it) {
 *        Image(
 *          painterResource(android.R.drawable.star_big_on),
 *          contentDescription,
 *          modifier,
 *        )
 *      } else {
 *        Image(painter, contentDescription, modifier)
 *      }
 *    }
 *  }
 *  AnimationState.Success -> {
 *    Image(painter, contentDescription, modifier)
 *  }
 *  AnimationState.Failed -> {
 *    Image(
 *      painterResource(android.R.drawable.star_big_off),
 *      contentDescription,
 *      modifier,
 *    )
 *  }
 * }
 * ```
 *
 * [animate] is a helper that uses [AnimationState.Animate] to indicate that an
 * animation should be performed because the [DataSource] is not [DataSource.MEMORY_CACHE], or
 * [AnimationState.Success] to indicate that the animation should be skipped. It's derived from
 * [State] so you can implement your own version if your requirements differ, or skip it
 * entirely.
 *
 * Unlike [GlideImage], the painter cannot automatically determine its size. If no override size
 * (by calling [RequestBuilder.override] in [requestBuilderTransform]) is provided, the painter will
 * wait until it is drawn to start the image load. This can lead to the image load never starting.
 * To ensure the painter is always able to determine a size and start the image load:
 *
 * 1. Use the other variant of [rememberGlidePainter] along with [rememberSizeAndModifier] to
 * resolve the size from layout.
 * 2. Ensure that the painter is always drawn (unlike in the example above) in another Composable
 * (e.g. [androidx.compose.foundation.Image] with a reasonable [Modifier].
 * 3. Using [requestBuilderTransform] to apply a specific [RequestBuilder.override] size to the
 * Glide request directly. If you choose this option, try to pick an override size based on the area
 * you want to display the image in or at least the screen size rather than using
 * [com.bumptech.glide.request.target.Target.SIZE_ORIGINAL].
 */
@ExperimentalGlideComposeApi
@OptIn(InternalGlideApi::class)
@Composable
public fun rememberGlidePainter(
    model: Any?,
    requestBuilderTransform: RequestBuilderTransform<Drawable> = { it },
): GlidePainter {
    val requestManager: RequestManager =
        LocalContext.current.let { remember(it) { Glide.with(it) } }

    val requestBuilder = remember(model, requestManager, requestBuilderTransform) {
        requestBuilderTransform(requestManager.load(model))
    }

    val overrideSize = requestBuilder.overrideSize()
    val size = rememberResolvableSize(overrideSize)
    return rememberGlidePainter(
        requestBuilder = requestBuilder,
        size = size,
        resolveSize = true,
    )
}

/**
 * A size associated with a [Modifier] that will be resolved during layout.
 */
@OptIn(InternalGlideApi::class)
public data class AsyncLayoutSize(internal val size: AsyncGlideSize)

/**
 * Similar to the other variant of [rememberGlidePainter] except that this method uses the given
 * [size] to determine the size for the image load.
 *
 * [GlideImage] is easier to use than this method and should be preferred unless you have a use case
 * that requires [State].
 *
 * Using this method and [rememberSizeAndModifier] together looks like this:
 *
 * ```
 * val (size, updatedModifier) = rememberSizeAndModifier(modifier)
 * val painter = rememberGlidePainter(model = item.uri, size)
 * when (painter.state.animate()) {
 *  AnimationState.Loading, AnimationState.Animate -> {
 *    Crossfade(painter.state == AnimationState.Loading) {
 *      if (it) {
 *        Image(
 *          painterResource(android.R.drawable.star_big_on),
 *          contentDescription,
 *          updatedModifier,
 *        )
 *      } else {
 *        Image(painter, contentDescription, updatedModifier)
 *      }
 *    }
 *  }
 *  AnimationState.Success -> {
 *    Image(painter, contentDescription, updatedModifier)
 *  }
 *  AnimationState.Failed -> {
 *    Image(
 *      painterResource(android.R.drawable.star_big_off),
 *      contentDescription,
 *      updatedModifier,
 *    )
 *  }
 * }
 * ```
 *
 * If you're using fixed size buckets across the app, or have some other reason to use
 * [RequestBuilder.override], then this method is not helpful and you should use the other
 * [rememberGlidePainter] variant instead. If an override size is set, this method will throw.
 *
 * Otherwise, using this method can improve efficiency by ensuring you load an image whose size
 * matches the layout it's displayed in. In addition, this variant will always resolve a size and
 * be able to start an image load if the given [size] is resolved via its associated [Modifier].
 * See [rememberSizeAndModifier].
 *
 * @throws IllegalArgumentException if [requestBuilderTransform] sets an [RequestBuilder.override]
 * size.
 */
@ExperimentalGlideComposeApi
@OptIn(InternalGlideApi::class)
@Composable
public fun rememberGlidePainter(
    model: Any?,
    size: AsyncLayoutSize,
    requestBuilderTransform: RequestBuilderTransform<Drawable> = { it },
): GlidePainter {
    val requestManager: RequestManager =
        LocalContext.current.let { remember(it) { Glide.with(it) } }

    val requestBuilder = remember(model, requestManager, requestBuilderTransform) {
        requestBuilderTransform(requestManager.load(model))
    }
    if (requestBuilder.overrideSize() != null) {
        throw IllegalArgumentException(
            "Using rememberGlidePainterAndSize with a fixed override size " +
                    "(${requestBuilder.overrideSize()}) is redundant, use rememberGlidePainter instead"
        )
    }

    return rememberGlidePainter(
        requestBuilder = requestBuilder,
        size = size.size,
        // Let the Modifier resolve the size
        resolveSize = false,
    )
}

/**
 * Returns a [AsyncLayoutSize] that can be provided to [rememberGlidePainter] and an updated
 * copy of the given [Modifier]. The returned modifier must be applied to some other Composable
 * whose layout size is the size of the image you want.
 *
 * Using this method along with [rememberGlidePainter] allows you to avoid specifying an override
 * size, saving memory and speeding up future image loads.
 */
@OptIn(InternalGlideApi::class)
@ExperimentalGlideComposeApi
@Composable
public fun rememberSizeAndModifier(
    modifier: Modifier,
): Pair<AsyncLayoutSize, Modifier> =
    remember(modifier) {
        val size = AsyncGlideSize()
        Pair(
            AsyncLayoutSize(size),
            modifier.sizeObservingModifier(size)
        )
    }

@OptIn(InternalGlideApi::class)
private fun Modifier.sizeObservingModifier(size: AsyncGlideSize): Modifier =
    this.layout { measurable, constraints ->
        val inferredSize = constraints.inferredGlideSize()
        if (inferredSize != null) {
            size.setSize(inferredSize)
        }
        val placeable = measurable.measure(constraints)
        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }

@OptIn(InternalGlideApi::class, ExperimentalGlideComposeApi::class)
@Composable
internal fun rememberGlidePainter(
    requestBuilder: RequestBuilder<Drawable>,
    size: ResolvableGlideSize,
    resolveSize: Boolean,
): GlidePainterImpl {
    val scope = rememberCoroutineScope()
    val result = remember(requestBuilder, size) {
        GlidePainterImpl(
            requestBuilder,
            size,
            resolveSize,
            scope
        )
    }
    result.onRemembered()
    return result
}

/**
 * A helper enum that translates [State] into something easier to use with custom
 * animations. See [animate].
 */
@ExperimentalGlideComposeApi
public enum class AnimationState {
    Loading,

    /**
     * The request has finished successfully and should animate.
     */
    Animate,

    /**
     * The request has finished successfully but should not animate because it was loaded from
     * [DataSource.MEMORY_CACHE].
     */
    Success,
    Failed;
}

/**
 * An optional helper that simplifies writing animations with [State] by using
 * [AnimationState.Animate] and [AnimationState.Success] to distinguish when an animation should
 * occur.
 *
 * If you use thumbnails, this API will return [AnimationState.Animate] or [AnimationState.Success]
 * based on the data source of the first image to be loaded. If the first image to be loaded is
 * from the memory cache, this method will always return [AnimationState.Animate]. Otherwise it will
 * always return [AnimationState.Success]. This behavior is based on [State.Success].
 */
@ExperimentalGlideComposeApi
public fun State.animate(): AnimationState =
    when (this) {
        is State.Failure -> AnimationState.Failed
        is State.Loading -> AnimationState.Loading
        is State.Success ->
            if (dataSource != DataSource.MEMORY_CACHE) {
                AnimationState.Animate
            } else {
                AnimationState.Success
            }
    }


/**
 * Paints images from Glide, see [rememberGlidePainter] for details.
 */
@ExperimentalGlideComposeApi
public abstract class GlidePainter : Painter() {
    /**
     * The current [State] of the painter and its corresponding image load(s).
     *
     * See [rememberGlidePainter] for details and examples
     */
    public abstract var state: State
        internal set

    /**
     * The current state of a request associated with a Glide painter.
     *
     * This state is a bit of a simplification over Glide's real state. In particular [Success] is
     * used in any case where we have an image, even if that image is the thumbnail of a full request
     * where the full request has failed. From the point of view of the UI this is usually reasonable
     * and a significant simplification of this API.
     */
    @ExperimentalGlideComposeApi
    public sealed class State {

        @ExperimentalGlideComposeApi
        public object Loading : State()

        /**
         * Indicates the load finished successfully (or at least one thumbnail was loaded, see the details
         * on [State]).
         *
         * @param dataSource The data source the latest image was loaded from. If your request uses one
         * or more thumbnails this value may change as each successive thumbnail is loaded.
         */
        @ExperimentalGlideComposeApi
        public data class Success(
            val dataSource: DataSource,
        ) : State()

        @ExperimentalGlideComposeApi
        public object Failure : State()
    }
}

// This class is inspired by a similar implementation in the excellent Coil library
// (https://github.com/coil-kt/coil), specifically:
// https://github.com/coil-kt/coil/blob/main/coil-compose-base/src/main/java/coil/compose/AsyncImagePainter.kt
@Stable
@ExperimentalGlideComposeApi
internal class GlidePainterImpl
@OptIn(InternalGlideApi::class)
constructor(
    private val requestBuilder: RequestBuilder<Drawable>,
    private val resolvableSize: ResolvableGlideSize,
    private val resolveSize: Boolean,
    scope: CoroutineScope,
) : GlidePainter(), RememberObserver {
    private var _state: State = State.Loading
        set(value) {
            field = value
            state = value
        }
    private var _painter: Painter? = null
        set(value) {
            field = value
            painter = value
        }
    public override var state: State by mutableStateOf(State.Loading)
    private var painter: Painter? by mutableStateOf(null)
    internal val currentDrawable: MutableState<Drawable?> = mutableStateOf(null)

    private var alpha: Float by mutableStateOf(DefaultAlpha)
    private var colorFilter: ColorFilter? by mutableStateOf(null)
    private val scope =
        scope + SupervisorJob(parent = scope.coroutineContext.job) + Dispatchers.Main.immediate
    private var currentJob: Job? = null

    override val intrinsicSize: Size
        get() = _painter?.intrinsicSize ?: Size.Unspecified

    @OptIn(InternalGlideApi::class)
    override fun DrawScope.onDraw() {
        if (resolveSize) {
            when (resolvableSize) {
                is AsyncGlideSize -> {
                    size.toGlideSize()?.let { resolvableSize.setSize(it) }
                }
                // Do nothing.
                is ImmediateGlideSize -> {}
            }
        }
        painter?.apply { draw(size, alpha, colorFilter) }
    }

    override fun onAbandoned() {
        (_painter as? RememberObserver)?.onAbandoned()
    }

    override fun onForgotten() {
        (_painter as? RememberObserver)?.onForgotten()
        currentJob?.cancel()
        currentJob = null
    }

    override fun onRemembered() {
        (_painter as? RememberObserver)?.onRemembered()
        if (currentJob == null) {
            currentJob = launchRequest()
        }
    }

    @OptIn(ExperimentGlideFlows::class, InternalGlideApi::class)
    private fun launchRequest() = this.scope.launch {
        requestBuilder.flowResolvable(resolvableSize).collect {
            val (glidePainterState, painter) = when (it) {
                is Resource -> {
                    currentDrawable.value = it.resource
                    Pair(State.Success(it.dataSource), it.resource.toPainter())
                }

                is Placeholder -> {
                    currentDrawable.value = it.placeholder
                    val painter = it.placeholder?.toPainter()
                    when (it.status) {
                        Status.CLEARED -> Pair(State.Loading, painter)
                        Status.RUNNING -> Pair(State.Loading, painter)
                        Status.FAILED -> Pair(State.Failure, painter)
                        Status.SUCCEEDED -> throw IllegalStateException()
                    }
                }
            }
            updateState(glidePainterState, painter)
        }
    }

    private fun Drawable.toPainter() =
        when (this) {
            is BitmapDrawable -> BitmapPainter(bitmap.asImageBitmap())
            is ColorDrawable -> ColorPainter(Color(color))
            else -> DrawablePainter(mutate())
        }

    private fun updateState(glidePainterState: State, painter: Painter?) {
        val previous = _painter

        if (previous !== painter) {
            (previous as? RememberObserver)?.onForgotten()
            (painter as? RememberObserver)?.onRemembered()
        }
        _painter = painter

        // Avoid updating the DataSource for multiple successful Glide loads (ie when using thumbnails).
        // This makes the API a bit less flexible, but avoids recompositions when only the data source
        // changes.
        if (glidePainterState is State.Success && _state is State.Success) {
            return
        }
        if (glidePainterState != _state) {
            _state = glidePainterState
        }
    }

    override fun applyAlpha(alpha: Float): Boolean {
        this.alpha = alpha
        return true
    }

    override fun applyColorFilter(colorFilter: ColorFilter?): Boolean {
        this.colorFilter = colorFilter
        return true
    }
}