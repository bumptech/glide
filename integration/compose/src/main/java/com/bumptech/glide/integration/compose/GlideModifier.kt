package com.bumptech.glide.integration.compose

import android.graphics.PointF
import android.graphics.drawable.Drawable
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
import com.bumptech.glide.util.Preconditions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlin.math.roundToInt

@ExperimentalGlideComposeApi
internal interface RequestListener {
  fun onStateChanged(model: Any?, drawable: Drawable?, requestState: RequestState)
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
  ) then
      clipToBounds() then
      if (contentDescription != null) {
        semantics {
          this@semantics.contentDescription = contentDescription
          role = Role.Image
        }
      } else {
        Modifier
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
  private var painter: Painter? = null

  // Only used for debugging
  private var drawable: Drawable? = null
  private var state: RequestState = RequestState.Loading
  private var placeholder: Painter? = null
  private var isFirstResource = true

  // Avoid allocating Point/PointFs during draw
  private var placeholderPositionAndSize: CachedPositionAndSize? = null
  private var drawablePositionAndSize: CachedPositionAndSize? = null

  private var hasFixedSize: Boolean = false
  private var inferredGlideSize: com.bumptech.glide.integration.ktx.Size? = null

  private var transition: Transition = DoNotTransition

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
  ) {
    // Other attributes can be refreshed by re-drawing rather than restarting a request
    val restartLoad =
      !this::requestBuilder.isInitialized ||
          requestBuilder != this.requestBuilder

    this.requestBuilder = requestBuilder
    this.contentScale = contentScale
    this.alignment = alignment
    this.alpha = alpha ?: DefaultAlpha
    this.colorFilter = colorFilter
    this.requestListener = requestListener
    this.draw = draw ?: true
    this.transitionFactory = transitionFactory ?: DoNotTransition.Factory
    this.resolvableGlideSize =
      requestBuilder.maybeImmediateSize()
        ?: inferredGlideSize?.let { ImmediateGlideSize(it) }
            ?: AsyncGlideSize()

    if (restartLoad) {
      clear()
      updateDrawable(null)

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
      placeholder?.let { painter ->
        drawContext.canvas.withSave {
          placeholderPositionAndSize = drawOne(painter, placeholderPositionAndSize) { size ->
            drawPlaceholder.invoke(this, painter, size, alpha, colorFilter)
          }
        }
      }

      painter?.let { painter ->
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
    updateDrawable(null)
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
      if (this.requestBuilder != requestBuilder) {
        return@sideEffect
      }
      Preconditions.checkArgument(currentJob == null)
      currentJob = (coroutineScope + Dispatchers.Main.immediate).launch {
        placeholder = null
        placeholderPositionAndSize = null

        requestBuilder.flowResolvable(resolvableGlideSize).collect {
          val (state, drawable) =
            when (it) {
              is Resource -> {
                maybeAnimate(it)
                Pair(RequestState.Success(it.dataSource), it.resource)
              }

              is Placeholder -> {
                val drawable = it.placeholder
                val state = when (it.status) {
                  Status.RUNNING, Status.CLEARED -> RequestState.Loading
                  Status.FAILED -> RequestState.Failure
                  Status.SUCCEEDED -> throw IllegalStateException()
                }
                placeholder = drawable?.toPainter()
                placeholderPositionAndSize = null
                Pair(state, drawable)
              }
            }
          updateDrawable(drawable)
          requestListener?.onStateChanged(requestBuilder.internalModel, drawable, state)
          this@GlideNode.state = state
          if (hasFixedSize) {
            invalidateDraw()
          } else {
            invalidateMeasurement()
          }
        }
      }
    }

  private fun updateDrawable(drawable: Drawable?) {
    this.drawable = drawable
    painter = drawable?.toPainter()
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
    updateDrawable(null)
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

    val intrinsicSize = painter?.intrinsicSize ?: return constraints

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
    val scaledSize =
      srcSize * contentScale.computeScaleFactor(
        srcSize, Size(constrainedWidth.toFloat(), constrainedHeight.toFloat())
      )

    val minWidth = constraints.constrainWidth(scaledSize.width.roundToInt())
    val minHeight = constraints.constrainHeight(scaledSize.height.roundToInt())
    return constraints.copy(minWidth = minWidth, minHeight = minHeight)
  }

  override fun SemanticsPropertyReceiver.applySemantics() {
    displayedDrawable = { drawable }
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
    return result
  }
}

internal val DisplayedDrawableKey =
  SemanticsPropertyKey<() -> Drawable?>("DisplayedDrawable")
internal var SemanticsPropertyReceiver.displayedDrawable by DisplayedDrawableKey
