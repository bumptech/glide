package com.bumptech.glide.integration.compose

import android.graphics.PointF
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.withSave
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.ScaleFactor
import androidx.compose.ui.layout.times
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.invalidateMeasurement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.integration.ktx.AsyncGlideSize
import com.bumptech.glide.integration.ktx.ExperimentGlideFlows
import com.bumptech.glide.integration.ktx.ImmediateGlideSize
import com.bumptech.glide.integration.ktx.InternalGlideApi
import com.bumptech.glide.integration.ktx.Placeholder
import com.bumptech.glide.integration.ktx.ResolvableGlideSize
import com.bumptech.glide.integration.ktx.Resource
import com.bumptech.glide.integration.ktx.Status
import com.bumptech.glide.integration.ktx.flowResolvable
import com.bumptech.glide.internalModel
import com.bumptech.glide.load.DataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlin.math.roundToInt

@ExperimentalGlideComposeApi
internal interface RequestListener {
  fun onStateChanged(model: Any?, painter: Painter?, requestState: RequestState)
}

@ExperimentalGlideComposeApi
internal fun Modifier.glideNode(
  requestBuilder: RequestBuilder<Drawable>,
  contentDescription: String? = null,
  alignment: Alignment? = null,
  contentScale: ContentScale? = null,
  alpha: Float? = null,
  colorFilter: ColorFilter? = null,
  transitionFactory: Transition.Factory? = null,
  requestListener: RequestListener? = null,
  draw: Boolean? = null,
  loadingPlaceholder: Painter? = null,
  errorPlaceholder: Painter? = null,
): Modifier {
  return this then GlideNodeElement(
    requestBuilder,
    contentScale ?: ContentScale.None,
    alignment ?: Alignment.Center,
    alpha,
    colorFilter,
    requestListener,
    draw,
    transitionFactory,
    loadingPlaceholder,
    errorPlaceholder,
  )
    .clipToBounds()
    .semantics {
      if (contentDescription != null) {
        this@semantics.contentDescription = contentDescription
      }
      role = Role.Image
    }
}

@ExperimentalGlideComposeApi
@OptIn(InternalGlideApi::class)
internal data class GlideNodeElement constructor(
  private val requestBuilder: RequestBuilder<Drawable>,
  private val contentScale: ContentScale,
  private val alignment: Alignment,
  private val alpha: Float?,
  private val colorFilter: ColorFilter?,
  private val requestListener: RequestListener?,
  private val draw: Boolean?,
  private val transitionFactory: Transition.Factory?,
  private val loadingPlaceholder: Painter?,
  private val errorPlaceholder: Painter?,
) : ModifierNodeElement<GlideNode>() {
  override fun create(): GlideNode {
    val result = GlideNode()
    update(result)
    return result
  }

  override fun update(node: GlideNode) {
    node.onNewRequest(
      requestBuilder,
      contentScale,
      alignment,
      alpha,
      colorFilter,
      requestListener,
      draw,
      transitionFactory,
      loadingPlaceholder,
      errorPlaceholder,
    )
  }

  override fun InspectorInfo.inspectableProperties() {
    name = "GlideNode"
    properties["model"] = requestBuilder.internalModel
    properties["size"] = requestBuilder.overrideSize() ?: "LayoutBased"
    properties["alignment"] = alignment
    properties["contentScale"] = contentScale
    properties["colorFilter"] = colorFilter
    properties["draw"] = draw
    properties["transition"] = when (transitionFactory) {
      is DoNotTransition.Factory -> "None"
      is CrossFade -> "CrossFade"
      else -> {
        "Custom: $transitionFactory"
      }
    }
  }
}

private val MAIN_HANDLER by lazy(LazyThreadSafetyMode.NONE) {
  Handler(Looper.getMainLooper())
}

@ExperimentalGlideComposeApi
@OptIn(InternalGlideApi::class)
internal class GlideNode : DrawModifierNode, LayoutModifierNode, SemanticsModifierNode,
  Modifier.Node() {
  private lateinit var requestBuilder: RequestBuilder<Drawable>
  private lateinit var contentScale: ContentScale
  private lateinit var alignment: Alignment
  private lateinit var resolvableGlideSize: ResolvableGlideSize
  private var alpha: Float = DefaultAlpha
  private var colorFilter: ColorFilter? = null
  private var transitionFactory: Transition.Factory = DoNotTransition.Factory
  private var draw: Boolean = true
  private var requestListener: RequestListener? = null

  private var currentJob: Job? = null
  private var primary: Primary? = null

  private var loadingPlaceholder: Painter? = null
  private var errorPlaceholder: Painter? = null

  // Only used for debugging
  private var state: RequestState = RequestState.Loading
  private var placeholder: Painter? = null
  private var isFirstResource = true

  // Avoid allocating Point/PointFs during draw
  private var placeholderPositionAndSize: CachedPositionAndSize? = null
  private var drawablePositionAndSize: CachedPositionAndSize? = null

  private var hasFixedSize: Boolean = false
  private var inferredGlideSize: com.bumptech.glide.integration.ktx.Size? = null

  private var transition: Transition = DoNotTransition


  private val callback: Drawable.Callback by lazy {
    object : Drawable.Callback {
      override fun invalidateDrawable(d: Drawable) {
        invalidateDraw()
      }

      override fun scheduleDrawable(d: Drawable, what: Runnable, time: Long) {
        MAIN_HANDLER.postAtTime(what, time)
      }

      override fun unscheduleDrawable(d: Drawable, what: Runnable) {
        MAIN_HANDLER.removeCallbacks(what)
      }
    }
  }

  private fun RequestBuilder<*>.maybeImmediateSize() =
    this.overrideSize()?.let { ImmediateGlideSize(it) }

  fun onNewRequest(
    requestBuilder: RequestBuilder<Drawable>,
    contentScale: ContentScale,
    alignment: Alignment,
    alpha: Float?,
    colorFilter: ColorFilter?,
    requestListener: RequestListener?,
    draw: Boolean?,
    transitionFactory: Transition.Factory?,
    loadingPlaceholder: Painter?,
    errorPlaceholder: Painter?,
  ) {
    // Other attributes can be refreshed by re-drawing rather than restarting a request
    val restartLoad =
      !this::requestBuilder.isInitialized ||
          requestBuilder != this.requestBuilder
          // TODO(sam): Avoid restarting the entire load if we just change the placeholder. State
          // management makes this complicated and this might not be a common use case, so we
          // haven't yet done so.
          || loadingPlaceholder != this.loadingPlaceholder
          || errorPlaceholder != this.errorPlaceholder

    this.requestBuilder = requestBuilder
    this.contentScale = contentScale
    this.alignment = alignment
    this.alpha = alpha ?: DefaultAlpha
    this.colorFilter = colorFilter
    this.requestListener = requestListener
    this.draw = draw ?: true
    this.transitionFactory = transitionFactory ?: DoNotTransition.Factory
    this.loadingPlaceholder = loadingPlaceholder
    this.errorPlaceholder = errorPlaceholder
    this.resolvableGlideSize =
      requestBuilder.maybeImmediateSize()
        ?: inferredGlideSize?.let { ImmediateGlideSize(it) }
            ?: AsyncGlideSize()

    if (restartLoad) {
      clear()
      updatePrimary(null)

      // If we're not attached, we'll be measured when we eventually are attached.
      if (isAttached) {
        launchRequest(requestBuilder)
      }
    } else {
      invalidateDraw()
    }
  }

  private val Size.isValidWidth
    get() = isSpecified && width.isValidDimension

  private val Size.isValidHeight
    get() = isSpecified && height.isValidDimension

  private val Float.isValidDimension
    get() = this > 0f && isFinite()

  private val Size.isValid
    get() = isValidWidth && isValidHeight

  private fun Size.roundToInt() = IntSize(width.roundToInt(), height.roundToInt())

  private fun IntOffset.toPointF() = PointF(x.toFloat(), y.toFloat())

  internal data class CachedPositionAndSize(val position: PointF, val size: Size)

  private fun ContentDrawScope.drawOne(
    painter: Painter?,
    cache: CachedPositionAndSize?,
    drawOne: DrawScope.(Size) -> Unit
  ): CachedPositionAndSize? {
    if (painter == null) {
      return null
    }
    val currentPositionAndSize = if (cache != null) {
      cache
    } else {
      val srcWidth = if (painter.intrinsicSize.isValidWidth) {
        painter.intrinsicSize.width
      } else {
        size.width
      }
      val srcHeight = if (painter.intrinsicSize.isValidHeight) {
        painter.intrinsicSize.height
      } else {
        size.height
      }
      val srcSize = Size(srcWidth, srcHeight)

      val scaledSize = if (size.isValid) {
        contentScale.computeScaleFactor(srcSize, size).times(srcSize)
      } else {
        Size.Zero
      }

      CachedPositionAndSize(
        alignment.align(
          scaledSize.roundToInt(),
          size.roundToInt(),
          layoutDirection
        ).toPointF(),
        scaledSize,
      )
    }

    clipRect {
      translate(currentPositionAndSize.position.x, currentPositionAndSize.position.y) {
        drawOne.invoke(this, currentPositionAndSize.size)
      }
    }
    return currentPositionAndSize
  }

  override fun ContentDrawScope.draw() {
    if (draw) {
      val drawPlaceholder = transition.drawPlaceholder ?: DoNotTransition.drawPlaceholder
      // If we're only showing the placeholder, it should just be drawn as the primary image.
      // If we've loaded a full image and we have a placeholder, then we should try to draw both so
      // that the transition can decide what to do.
      if (placeholder != primary && transition != DoNotTransition) {
        placeholder?.let { painter ->
          drawContext.canvas.withSave {
            placeholderPositionAndSize = drawOne(painter, placeholderPositionAndSize) { size ->
              drawPlaceholder.invoke(this, painter, size, alpha, colorFilter)
            }
          }
        }
      }

      primary?.painter?.let { painter ->
        drawContext.canvas.withSave {
          drawablePositionAndSize = drawOne(painter, drawablePositionAndSize) { size ->
            transition.drawCurrent.invoke(this, painter, size, alpha, colorFilter)
          }
        }
      }
    }

    // Allow chaining of DrawModifiers
    drawContent()
  }

  override val shouldAutoInvalidate: Boolean
    get() = false

  override fun onAttach() {
    super.onAttach()
    if (currentJob == null) {
      launchRequest(requestBuilder)
    }
  }

  override fun onReset() {
    super.onReset()
    clear()
    updatePrimary(null)
  }

  @OptIn(ExperimentGlideFlows::class)
  private fun CoroutineScope.maybeAnimate(instant: Resource<Drawable>) {
    if (instant.dataSource == DataSource.MEMORY_CACHE
      || !isFirstResource
      || transitionFactory == DoNotTransition.Factory
    ) {
      isFirstResource = false
      transition = DoNotTransition
      return
    }
    isFirstResource = false
    transition = transitionFactory.build()
    launch {
      transition.transition {
        invalidateDraw()
      }
    }
  }

  @OptIn(ExperimentGlideFlows::class, InternalGlideApi::class, ExperimentalComposeUiApi::class)
  private fun launchRequest(requestBuilder: RequestBuilder<Drawable>) =
    // Launch via sideEffect because onAttach is called before onNewRequest and onNewRequest is not
    // always called. That means in onAttach if we launch the request, we might restart an old
    // request only to have it immediately replaced by a new request, causing jank. Or if we don't
    // launch the new requests in onAttach, then onNewRequest might not be called and we won't show
    // the old image.
    // sideEffect is called after all changes in the tree, so we can always queue a new request, but
    // drop any for old requests by comparing requests builders.
    sideEffect {
      // The request changed while our sideEffect was queued, which should also have triggered
      // another sideEffect. Wait for that one instead.
      if (this.requestBuilder != requestBuilder) {
        return@sideEffect
      }
      // We've raced with another sideEffect, our previous check means that the request hasn't
      // changed and this check means we're already loading that image, so we have nothing useful to
      // to do.
      if (currentJob != null) {
        return@sideEffect
      }
      currentJob = (coroutineScope + Dispatchers.Main.immediate).launch {
        placeholder = null
        placeholderPositionAndSize = null

        requestBuilder.flowResolvable(resolvableGlideSize).collect {
          val (state, primary) =
            when (it) {
              is Resource -> {
                maybeAnimate(it)
                Pair(RequestState.Success(it.dataSource), Primary.PrimaryDrawable(it.resource))
              }

              is Placeholder -> {
                val state = when (it.status) {
                  Status.RUNNING, Status.CLEARED -> RequestState.Loading
                  Status.FAILED -> RequestState.Failure
                  Status.SUCCEEDED -> throw IllegalStateException()
                }
                // Prefer the override Painters if set.
                val painter = when (state) {
                  is RequestState.Loading -> loadingPlaceholder
                  is RequestState.Failure -> errorPlaceholder
                  is RequestState.Success -> throw IllegalStateException()
                }
                val primary = if (painter != null) {
                  Primary.PrimaryPainter(painter)
                } else {
                  Primary.PrimaryDrawable(it.placeholder)
                }
                placeholder = primary.painter
                placeholderPositionAndSize = null
                Pair(state, primary)
              }
            }
          updatePrimary(primary)
          requestListener?.onStateChanged(requestBuilder.internalModel, primary.painter, state)
          this@GlideNode.state = state
          if (hasFixedSize) {
            invalidateDraw()
          } else {
            invalidateMeasurement()
          }
        }
      }
    }

  private sealed class Primary {
    class PrimaryDrawable(override val drawable: Drawable?) : Primary() {
      override val painter = drawable?.toPainter()
      override fun onUnset() {
        drawable?.callback = null
        drawable?.setVisible(false, false)
        (drawable as? Animatable)?.stop()
      }

      override fun onSet(callback: Drawable.Callback) {
        drawable?.callback = callback
        drawable?.setVisible(true, true)
        (drawable as? Animatable)?.start()
      }
    }

    class PrimaryPainter(override val painter: Painter?) : Primary() {
      override val drawable = null
      override fun onUnset() {}
      override fun onSet(callback: Drawable.Callback) {}
    }

    abstract val painter: Painter?
    abstract val drawable: Drawable?

    abstract fun onUnset()

    abstract fun onSet(callback: Drawable.Callback)
  }

  private fun updatePrimary(newPrimary: Primary?) {
    this.primary?.onUnset()
    this.primary = newPrimary
    newPrimary?.onSet(callback)
    drawablePositionAndSize = null
  }

  override fun onDetach() {
    super.onDetach()
    clear()
    if (transition != DoNotTransition) {
      coroutineScope.launch {
        transition.stop()
      }
    }
  }

  private fun clear() {
    isFirstResource = true
    currentJob?.cancel()
    currentJob = null
    state = RequestState.Loading
    updatePrimary(null)
  }

  @OptIn(InternalGlideApi::class)
  override fun MeasureScope.measure(
    measurable: Measurable,
    constraints: Constraints
  ): MeasureResult {
    placeholderPositionAndSize = null
    drawablePositionAndSize = null
    hasFixedSize = constraints.hasFixedSize()
    inferredGlideSize = constraints.inferredGlideSize()

    when (val currentSize = resolvableGlideSize) {
      is AsyncGlideSize -> {
        inferredGlideSize?.also { currentSize.setSize(it) }
      }

      is ImmediateGlideSize -> {}
    }
    val placeable = measurable.measure(modifyConstraints(constraints))
    return layout(placeable.width, placeable.height) {
      placeable.placeRelative(0, 0)
    }
  }

  private fun Constraints.hasFixedSize() = hasFixedWidth && hasFixedHeight

  private fun modifyConstraints(constraints: Constraints): Constraints {
    if (constraints.hasFixedSize()) {
      return constraints.copy(
        minWidth = constraints.maxWidth,
        minHeight = constraints.maxHeight
      )
    }

    val intrinsicSize = primary?.painter?.intrinsicSize ?: return constraints

    val intrinsicWidth =
      if (constraints.hasFixedWidth) {
        constraints.maxWidth
      } else if (intrinsicSize.isValidWidth) {
        intrinsicSize.width.roundToInt()
      } else {
        constraints.minWidth
      }

    val intrinsicHeight =
      if (constraints.hasFixedHeight) {
        constraints.maxHeight
      } else if (intrinsicSize.isValidHeight) {
        intrinsicSize.height.roundToInt()
      } else {
        constraints.minHeight
      }

    val constrainedWidth = constraints.constrainWidth(intrinsicWidth)
    val constrainedHeight = constraints.constrainHeight(intrinsicHeight)

    val srcSize = Size(intrinsicWidth.toFloat(), intrinsicHeight.toFloat())
    val scaleFactor = contentScale.computeScaleFactor(
      srcSize, Size(constrainedWidth.toFloat(), constrainedHeight.toFloat())
    )
    if (scaleFactor == ScaleFactor.Unspecified) {
      return constraints
    }
    val scaledSize = srcSize * scaleFactor

    val minWidth = constraints.constrainWidth(scaledSize.width.roundToInt())
    val minHeight = constraints.constrainHeight(scaledSize.height.roundToInt())
    return constraints.copy(minWidth = minWidth, minHeight = minHeight)
  }

  override fun SemanticsPropertyReceiver.applySemantics() {
    displayedDrawable = { primary?.drawable }
    displayedPainter = { primary?.painter }
  }

  override fun equals(other: Any?): Boolean {
    if (other is GlideNode) {
      return requestBuilder == other.requestBuilder
          && contentScale == other.contentScale
          && alignment == other.alignment
          && colorFilter == other.colorFilter
          && requestListener == other.requestListener
          && draw == other.draw
          && transitionFactory == other.transitionFactory
          && alpha == other.alpha
          && loadingPlaceholder == other.loadingPlaceholder
          && errorPlaceholder == other.errorPlaceholder
    }
    return false
  }

  override fun hashCode(): Int {
    var result = requestBuilder.hashCode()
    result = 31 * result + contentScale.hashCode()
    result = 31 * result + alignment.hashCode()
    result = 31 * result + colorFilter.hashCode()
    result = 31 * result + draw.hashCode()
    result = 31 * result + requestListener.hashCode()
    result = 31 * result + transitionFactory.hashCode()
    result = 31 * result + alpha.hashCode()
    result = 31 * result + loadingPlaceholder.hashCode()
    result = 31 * result + errorPlaceholder.hashCode()
    return result
  }
}

internal val DisplayedDrawableKey =
  SemanticsPropertyKey<() -> Drawable?>("DisplayedDrawable")
internal var SemanticsPropertyReceiver.displayedDrawable by DisplayedDrawableKey

internal val DisplayedPainterKey =
  SemanticsPropertyKey<() -> Painter?>("DisplayedPainter")
internal var SemanticsPropertyReceiver.displayedPainter by DisplayedPainterKey
