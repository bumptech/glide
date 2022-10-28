package com.bumptech.glide.integration.compose

import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.integration.ktx.ExperimentGlideFlows
import com.bumptech.glide.integration.ktx.InternalGlideApi
import com.bumptech.glide.integration.ktx.Placeholder
import com.bumptech.glide.integration.ktx.ResolvableGlideSize
import com.bumptech.glide.integration.ktx.Resource
import com.bumptech.glide.integration.ktx.Status
import com.bumptech.glide.integration.ktx.flowResolvable
import com.google.accompanist.drawablepainter.DrawablePainter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus

// This class is inspired by a similar implementation in the excellent Coil library
// (https://github.com/coil-kt/coil), specifically:
// https://github.com/coil-kt/coil/blob/main/coil-compose-base/src/main/java/coil/compose/AsyncImagePainter.kt
@Stable
internal class GlidePainter
@OptIn(InternalGlideApi::class)
constructor(
  private val requestBuilder: RequestBuilder<Drawable>,
  private val size: ResolvableGlideSize,
  scope: CoroutineScope,
) : Painter(), RememberObserver {
  @OptIn(ExperimentGlideFlows::class) internal var status: Status by mutableStateOf(Status.CLEARED)
  internal val currentDrawable: MutableState<Drawable?> = mutableStateOf(null)
  private var alpha: Float by mutableStateOf(DefaultAlpha)
  private var colorFilter: ColorFilter? by mutableStateOf(null)
  private var delegate: Painter? by mutableStateOf(null)
  private val scope =
    scope + SupervisorJob(parent = scope.coroutineContext.job) + Dispatchers.Main.immediate

  override val intrinsicSize: Size
    get() = delegate?.intrinsicSize ?: Size.Unspecified

  override fun DrawScope.onDraw() {
    delegate?.apply { draw(size, alpha, colorFilter) }
  }

  override fun onAbandoned() {
    (delegate as? RememberObserver)?.onAbandoned()
  }

  override fun onForgotten() {
    (delegate as? RememberObserver)?.onForgotten()
  }

  override fun onRemembered() {
    (delegate as? RememberObserver)?.onRemembered()
    launchRequest()
  }

  @OptIn(ExperimentGlideFlows::class, InternalGlideApi::class)
  private fun launchRequest() {
    this.scope.launch {
      requestBuilder.flowResolvable(size).collect {
        updateDelegate(
          when (it) {
            is Resource -> it.resource
            is Placeholder -> it.placeholder
          }
        )
        status = it.status
      }
    }
  }

  private fun Drawable.toPainter() =
    when (this) {
      is BitmapDrawable -> BitmapPainter(bitmap.asImageBitmap())
      is ColorDrawable -> ColorPainter(Color(color))
      else -> DrawablePainter(mutate())
    }

  private fun updateDelegate(drawable: Drawable?) {
    val newDelegate = drawable?.toPainter()
    val oldDelegate = delegate
    if (newDelegate !== oldDelegate) {
      (oldDelegate as? RememberObserver)?.onForgotten()
      (newDelegate as? RememberObserver)?.onRemembered()
      currentDrawable.value = drawable
      delegate = newDelegate
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
