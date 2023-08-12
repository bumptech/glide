package com.bumptech.glide.integration.compose

import android.graphics.PointF
import android.graphics.drawable.Drawable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.graphics.asAndroidColorFilter
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
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
import com.bumptech.glide.util.Preconditions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlin.math.roundToInt

@ExperimentalGlideComposeApi
@OptIn(InternalGlideApi::class)
internal fun Modifier.glideNode(
  requestBuilder: RequestBuilder<Drawable>,
  size: ImmediateGlideSize?,
  contentDescription: String?,
  alignment: Alignment,
  contentScale: ContentScale,
  alpha: Float = DefaultAlpha,
  colorFilter: ColorFilter?,
): Modifier =
  this then GlideNodeElement(
    requestBuilder,
    size,
    contentScale,
    alignment,
    colorFilter,
  ) then
      clipToBounds() then
      alpha(alpha) then
      if (contentDescription != null) {
        semantics {
          this@semantics.contentDescription = contentDescription
          role = Role.Image
        }
      } else {
        Modifier
      }

@ExperimentalGlideComposeApi
@OptIn(InternalGlideApi::class)
internal data class GlideNodeElement constructor(
  private val requestBuilder: RequestBuilder<Drawable>,
  private val fixedSize: ImmediateGlideSize?,
  private val contentScale: ContentScale,
  private val alignment: Alignment,
  private val colorFilter: ColorFilter?,
) : ModifierNodeElement<GlideNode>() {
  override fun create(): GlideNode {
    val result = GlideNode()
    result.onNewRequest(
      requestBuilder,
      fixedSize,
      contentScale,
      alignment,
      colorFilter,
    )
    return result
  }

  override fun update(node: GlideNode) {
    node.onNewRequest(
      requestBuilder,
      fixedSize,
      contentScale,
      alignment,
      colorFilter,
    )
  }

  override fun InspectorInfo.inspectableProperties() {
    name = "GlideNode"
    properties["model"] = requestBuilder.internalModel
    properties["size"] = fixedSize ?: "LayoutBased"
    properties["alignment"] = alignment
    properties["contentScale"] = contentScale
    properties["colorFilter"] = colorFilter
  }
}

@ExperimentalGlideComposeApi
@OptIn(InternalGlideApi::class)
internal class GlideNode : DrawModifierNode, LayoutModifierNode, SemanticsModifierNode,
  Modifier.Node() {
  private lateinit var requestBuilder: RequestBuilder<Drawable>
  private lateinit var contentScale: ContentScale
  private lateinit var alignment: Alignment
  private var fixedSize: ImmediateGlideSize? = null
  private var colorFilter: ColorFilter? = null

  private var currentJob: Job? = null
  private var drawable: Drawable? = null
  private var state: RequestState = RequestState.Loading
  private var resolvableGlideSize: ResolvableGlideSize? = null

  fun onNewRequest(
    requestBuilder: RequestBuilder<Drawable>,
    fixedSize: ImmediateGlideSize?,
    contentScale: ContentScale,
    alignment: Alignment,
    colorFilter: ColorFilter?,
  ) {
    // Other attributes can be refreshed by re-drawing rather than restarting a request
    val restartLoad =
      !this::requestBuilder.isInitialized ||
          requestBuilder != this.requestBuilder
          || fixedSize != this.fixedSize

    this.requestBuilder = requestBuilder
    this.fixedSize = fixedSize
    this.contentScale = contentScale
    this.alignment = alignment
    this.colorFilter = colorFilter

    if (restartLoad) {
      clear()
      updateDrawable(null)

      // If we're not attached, we'll be measured when we eventually are attached.
      if (isAttached) {
        // If we don't have a fixed size, we need a new layout pass to figure out how large the
        // image should be. Ideally we'd retain the old resolved glide size unless some other
        // modifier node had already requested measurement. Since we can't tell if measurement is
        // requested, we can either re-use the old resolvableGlideSize, which will be incorrect if
        // measurement was requested. Or we can invalidate resolvableGlideSize and ensure that it's
        // resolved by requesting measurement ourselves. Requesting is less efficient, but more
        // correct.
        // TODO(sam): See if we can find a reasonable way to remove this behavior, or be more
        //  targeted.
        if (fixedSize == null) {
          invalidateMeasurement()
        }
        launchRequest(requestBuilder)
      }
    } else {
      drawable?.colorFilter = colorFilter?.asAndroidColorFilter()
      invalidateDraw()
    }
  }

  private val Int.isValidDimension
    get() = this > 0

  private val Float.isValidDimension
    get() = this > 0f

  private val Size.isValid
    get() = width.isValidDimension && height.isValidDimension

  private fun Size.roundToInt() = IntSize(width.roundToInt(), height.roundToInt())

  private fun IntOffset.toPointF() = PointF(x.toFloat(), y.toFloat())

  override fun ContentDrawScope.draw() {
    val drawable = drawable ?: return
    val srcWidth = if (drawable.intrinsicWidth.isValidDimension) {
      drawable.intrinsicWidth.toFloat()
    } else {
      size.width
    }
    val srcHeight = if (drawable.intrinsicHeight.isValidDimension) {
      drawable.intrinsicHeight.toFloat()
    } else {
      size.height
    }
    val srcSize = Size(srcWidth, srcHeight)

    val scaledSize = if (size.isValid) {
      contentScale.computeScaleFactor(srcSize, size).times(srcSize).roundToInt()
    } else {
      Size.Zero.roundToInt()
    }

    drawable.setBounds(0, 0, scaledSize.width, scaledSize.height)
    val alignedPosition: PointF = alignment.align(
      IntSize(scaledSize.width, scaledSize.height),
      IntSize(size.width.roundToInt(), size.height.roundToInt()),
      layoutDirection
    ).toPointF()

    translate(alignedPosition.x, alignedPosition.y) {
      drawable.draw(drawContext.canvas.nativeCanvas)
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
        this@GlideNode.resolvableGlideSize = fixedSize ?: AsyncGlideSize()

        requestBuilder.flowResolvable(resolvableGlideSize!!).collect {
          val (state, drawable) =
            when (it) {
              is Resource -> Pair(RequestState.Success(it.dataSource), it.resource)
              is Placeholder -> {
                val drawable = it.placeholder
                val state = when (it.status) {
                  Status.CLEARED, Status.RUNNING -> RequestState.Loading
                  Status.FAILED -> RequestState.Failure
                  Status.SUCCEEDED -> throw IllegalStateException()
                }
                Pair(state, drawable)
              }
            }
          updateDrawable(drawable)
          this@GlideNode.state = state
          invalidateDraw()
        }
      }
    }

  private fun updateDrawable(drawable: Drawable?) {
    this.drawable?.callback = null
    this.drawable = drawable
    drawable?.colorFilter = colorFilter?.asAndroidColorFilter()
    drawable?.callback = object : Drawable.Callback {
      override fun invalidateDrawable(who: Drawable) {
        invalidateDraw()
      }

      override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) {}
      override fun unscheduleDrawable(who: Drawable, what: Runnable) {}
    }
  }

  override fun onDetach() {
    super.onDetach()
    clear()
  }

  private fun clear() {
    resolvableGlideSize = null
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
    when (val currentSize = resolvableGlideSize) {
      is AsyncGlideSize -> {
        val inferredSize = constraints.inferredGlideSize()
        if (inferredSize != null) {
          currentSize.setSize(inferredSize)
        }
      }
      // Do nothing.
      is ImmediateGlideSize -> {}
      null -> {}
    }
    val placeable = measurable.measure(constraints)
    return layout(constraints.maxWidth, constraints.maxHeight) { placeable.placeRelative(0, 0) }
  }

  override fun SemanticsPropertyReceiver.applySemantics() {
    displayedDrawable = { drawable }
  }

  override fun equals(other: Any?): Boolean {
    if (other is GlideNode) {
      return requestBuilder == other.requestBuilder
          && fixedSize == other.fixedSize
          && contentScale == other.contentScale
          && alignment == other.alignment
          && colorFilter == other.colorFilter
    }
    return false
  }

  override fun hashCode(): Int {
    var result = requestBuilder.hashCode()
    result = 31 * result + fixedSize.hashCode()
    result = 31 * result + contentScale.hashCode()
    result = 31 * result + alignment.hashCode()
    result = 31 * result + colorFilter.hashCode()
    return result
  }
}

internal val DisplayedDrawableKey =
  SemanticsPropertyKey<() -> Drawable?>("DisplayedDrawable")
internal var SemanticsPropertyReceiver.displayedDrawable by DisplayedDrawableKey
