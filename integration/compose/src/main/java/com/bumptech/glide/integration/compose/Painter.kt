package com.bumptech.glide.integration.compose

import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.View
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asAndroidColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.withSave
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.toSize
import kotlin.math.roundToInt

internal fun Drawable?.toPainter(): Painter =
  when (this) {
    is BitmapDrawable -> BitmapPainter(bitmap.asImageBitmap())
    is ColorDrawable -> ColorPainter(Color(color))
    null -> ColorPainter(Color.Transparent)
    else -> DrawablePainter(mutate())
  }

private class DrawablePainter(
  val drawable: Drawable
) : Painter() {
  init {
    if (drawable.isIntrinsicSizeValid) {
      drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
    }
  }

  private var drawableIntrinsicSize = drawable.intrinsicSize

  private val Drawable.isIntrinsicSizeValid
    get() = intrinsicWidth >= 0 && intrinsicHeight >= 0

  private val Drawable.intrinsicSize: Size
    get() = if (isIntrinsicSizeValid) {
        IntSize(intrinsicWidth, intrinsicHeight).toSize()
      } else {
        Size.Unspecified
      }

  override fun applyAlpha(alpha: Float): Boolean {
    drawable.alpha = (alpha * 255).roundToInt().coerceIn(0, 255)
    return true
  }

  override fun applyColorFilter(colorFilter: ColorFilter?): Boolean {
    drawable.colorFilter = colorFilter?.asAndroidColorFilter()
    return true
  }

  override fun applyLayoutDirection(layoutDirection: LayoutDirection): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      return drawable.setLayoutDirection(
        when (layoutDirection) {
          LayoutDirection.Ltr -> View.LAYOUT_DIRECTION_LTR
          LayoutDirection.Rtl -> View.LAYOUT_DIRECTION_RTL
        }
      )
    }
    return false
  }

  override val intrinsicSize: Size get() = drawableIntrinsicSize

  override fun DrawScope.onDraw() {
    drawIntoCanvas { canvas ->
      drawable.setBounds(0, 0, size.width.roundToInt(), size.height.roundToInt())

      canvas.withSave {
        drawable.draw(canvas.nativeCanvas)
      }
    }
  }

  override fun equals(other: Any?): Boolean {
    val compare = other as? DrawablePainter ?: return false
    return drawable == compare.drawable
  }

  override fun hashCode(): Int {
    return drawable.hashCode()
  }
}
