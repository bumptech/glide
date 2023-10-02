package com.bumptech.glide.integration.compose.test

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.TypedValue
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.test.SemanticsMatcher
import androidx.test.core.app.ApplicationProvider
import com.bumptech.glide.integration.compose.DisplayedDrawableKey
import com.bumptech.glide.integration.compose.DisplayedPainterKey
import com.bumptech.glide.integration.ktx.InternalGlideApi
import com.bumptech.glide.integration.ktx.Size
import kotlin.math.roundToInt

private fun context(): Context = ApplicationProvider.getApplicationContext()

fun Int.dpToPixels() =
  TypedValue.applyDimension(
    TypedValue.COMPLEX_UNIT_DIP,
    this.toFloat(),
    Resources.getSystem().displayMetrics
  )
    .roundToInt()

@OptIn(InternalGlideApi::class)
fun Int.bitmapSize() = context().resources.getDrawable(this, context().theme).size()

@OptIn(InternalGlideApi::class)
fun Drawable.size() = (this as BitmapDrawable).bitmap.let { Size(it.width, it.height) }

fun expectDisplayedResource(resourceId: Int) =
  expectDisplayedDrawable(context().getDrawable(resourceId))

fun Drawable?.bitmapOrThrow(): Bitmap? = if (this == null) null else (this as BitmapDrawable).bitmap

@OptIn(InternalGlideApi::class)
fun expectDisplayedDrawableSize(expectedSize: Size): SemanticsMatcher =
  expectDisplayedDrawable(expectedSize) { it?.size() }

fun expectDisplayedDrawable(expectedValue: Drawable?): SemanticsMatcher =
  expectDisplayedDrawable(expectedValue.bitmapOrThrow(), ::compareBitmaps) { it.bitmapOrThrow() }


fun expectDisplayedPainter(expectedValue: Painter?): SemanticsMatcher =
  expectStateValue(
    DisplayedPainterKey, expectedValue, { first, second -> first == second }, { value -> value }
  )

// These are hacks. We're relying on the ordering of expected.equals(actual) so that our
// DeepEqualsImageBitmap's equals method will be used. This doesn't implement the equals/hashcode
// contract :/
fun expectDisplayedPainter(context: Context, @DrawableRes resourceId: Int): SemanticsMatcher {
  val imageBitmap = DeepEqualsImageBitmap(ImageBitmap.imageResource(context.resources, resourceId))
  val painter = BitmapPainter(imageBitmap)
  return expectDisplayedPainter(painter)
}

fun expectDisplayedPainter(expectedValue: Drawable?): SemanticsMatcher {
  val bitmapDrawable = expectedValue as BitmapDrawable
  val imageBitmap = DeepEqualsImageBitmap(bitmapDrawable.bitmap.asImageBitmap())
  return expectDisplayedPainter(BitmapPainter(imageBitmap))
}

class DeepEqualsImageBitmap(base: ImageBitmap) : ImageBitmap by base {
  override fun equals(other: Any?): Boolean {
    val compare = other as? ImageBitmap ?: return false
    return compare.width == width && compare.height == height
        && compare.hasAlpha == hasAlpha
        && compare.config == config
        && compare.colorSpace == colorSpace
        && equalPixels(this, other)
  }

  private fun ImageBitmap.readPixels(): IntArray {
    val pixels = IntArray(width * height)
    readPixels(pixels)
    return pixels
  }

  private fun equalPixels(first: ImageBitmap, second: ImageBitmap): Boolean {
    return first.readPixels().contentEquals(second.readPixels())
  }

  override fun hashCode(): Int {
    var result = width.hashCode()
    result *= 31 * height.hashCode()
    result *= 31 * hasAlpha.hashCode()
    result *= 31 * config.hashCode()
    result *= 31 * colorSpace.hashCode()
    result *= 31 * readPixels().contentHashCode()
    return result
  }
}

fun expectNoDrawable(): SemanticsMatcher = expectDisplayedDrawable(null)

private fun compareBitmaps(first: Bitmap?, second: Bitmap?): Boolean {
  if (first == null && second == null) {
    return true
  }
  if (first == null || second == null) {
    return false
  }
  return first.sameAs(second)
}

private fun <ValueT> expectDisplayedDrawable(
  expectedValue: ValueT,
  compare: (ValueT?, ValueT?) -> Boolean = { first, second -> first == second },
  transform: (Drawable?) -> ValueT,
): SemanticsMatcher =
  expectStateValue(DisplayedDrawableKey, expectedValue, compare) { transform(it) }

private fun <ValueT, TransformedValueT> expectStateValue(
  key: SemanticsPropertyKey<() -> ValueT?>,
  expectedValue: TransformedValueT,
  compare: (TransformedValueT?, TransformedValueT?) -> Boolean,
  transform: (ValueT?) -> TransformedValueT?,
): SemanticsMatcher =
  SemanticsMatcher("${key.name} = '$expectedValue'") {
    val value = transform(it.config.getOrElseNullable(key) { null }?.invoke())
    if (!compare(expectedValue, value)) {
      throw AssertionError("Expected: $expectedValue, but was: $value")
    }
    true
  }

fun expectSameInstance(expectedDrawable: Drawable) =
  SemanticsMatcher("${DisplayedDrawableKey.name} = '$expectedDrawable'") {
    val actualValue: Drawable? =
      it.config.getOrElseNullable(DisplayedDrawableKey) { null }?.invoke()
    if (actualValue !== expectedDrawable) {
      throw AssertionError("Expected: $expectedDrawable, but was: $actualValue")
    }
    true
  }
