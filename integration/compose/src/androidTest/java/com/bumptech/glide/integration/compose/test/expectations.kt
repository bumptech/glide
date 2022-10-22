@file:OptIn(InternalGlideApi::class)

package com.bumptech.glide.integration.compose.test

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.test.SemanticsMatcher
import androidx.test.core.app.ApplicationProvider
import com.bumptech.glide.integration.compose.DisplayedDrawableKey
import com.bumptech.glide.integration.ktx.InternalGlideApi
import com.bumptech.glide.integration.ktx.Size

private val context = ApplicationProvider.getApplicationContext<Context>()

fun Int.bitmapSize() = context.resources.getDrawable(this, context.theme).size()

fun Drawable.size() = (this as BitmapDrawable).bitmap.let { Size(it.width, it.height) }

fun expectDisplayedResource(resourceId: Int) =
  expectDisplayedDrawable(context.getDrawable(resourceId))

fun Drawable?.bitmapOrThrow(): Bitmap? = if (this == null) null else (this as BitmapDrawable).bitmap

fun expectDisplayedDrawableSize(expectedSize: Size): SemanticsMatcher =
  expectDisplayedDrawable(expectedSize) { it?.size() }

fun expectDisplayedDrawable(
  expectedValue: Drawable?
): SemanticsMatcher =
  expectDisplayedDrawable(expectedValue.bitmapOrThrow()) { it.bitmapOrThrow() }

fun expectNoDrawable(): SemanticsMatcher = expectDisplayedDrawable(null)

private fun <ValueT> expectDisplayedDrawable(
  expectedValue: ValueT,
  transform: (Drawable?) -> ValueT
): SemanticsMatcher = expectStateValue(DisplayedDrawableKey, expectedValue) { transform(it) }

private fun <ValueT, TransformedValueT> expectStateValue(
  key: SemanticsPropertyKey<MutableState<ValueT?>>,
  expectedValue: TransformedValueT,
  transform: (ValueT?) -> TransformedValueT?
): SemanticsMatcher =
  SemanticsMatcher("${key.name} = '$expectedValue'") {
    val value = transform(it.config.getOrElseNullable(key) { null }?.value)
    if (value != expectedValue) {
      throw AssertionError("Expected: $expectedValue, but was: $value")
    }
    true
  }
