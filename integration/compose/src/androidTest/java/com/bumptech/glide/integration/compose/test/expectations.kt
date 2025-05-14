@file:OptIn(InternalGlideApi::class)

package com.bumptech.glide.integration.compose.test

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.TypedValue
import androidx.compose.runtime.MutableState
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.test.SemanticsMatcher
import androidx.test.core.app.ApplicationProvider
import com.bumptech.glide.integration.compose.DisplayedDrawableKey
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

fun Int.bitmapSize() = context().resources.getDrawable(this, context().theme).size()

fun Drawable.size() = (this as BitmapDrawable).bitmap.let { Size(it.width, it.height) }

fun expectDisplayedResource(resourceId: Int) =
  expectDisplayedDrawable(context().getDrawable(resourceId))

fun Drawable?.bitmapOrThrow(): Bitmap? = if (this == null) null else (this as BitmapDrawable).bitmap

fun expectDisplayedDrawableSize(expectedSize: Size): SemanticsMatcher =
  expectDisplayedDrawable(expectedSize) { it?.size() }

fun expectDisplayedDrawable(expectedValue: Drawable?): SemanticsMatcher =
  expectDisplayedDrawable(expectedValue.bitmapOrThrow(), ::compareBitmaps) { it.bitmapOrThrow() }

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
  key: SemanticsPropertyKey<MutableState<ValueT?>>,
  expectedValue: TransformedValueT,
  compare: (TransformedValueT?, TransformedValueT?) -> Boolean,
  transform: (ValueT?) -> TransformedValueT?,
): SemanticsMatcher =
  SemanticsMatcher("${key.name} = '$expectedValue'") {
    val value = transform(it.config.getOrElseNullable(key) { null }?.value)
    if (!compare(value, expectedValue)) {
      throw AssertionError("Expected: $expectedValue, but was: $value")
    }
    true
  }

fun expectSameInstance(expectedDrawable: Drawable) =
  SemanticsMatcher("${DisplayedDrawableKey.name} = '$expectedDrawable'") {
    val actualValue: Drawable? = it.config.getOrElseNullable(DisplayedDrawableKey) { null }?.value
    if (actualValue !== expectedDrawable) {
      throw AssertionError("Expected: $expectedDrawable, but was: $actualValue")
    }
    true
  }
