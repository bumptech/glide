@file:OptIn(ExperimentalGlideComposeApi::class, InternalGlideApi::class)

package com.bumptech.glide.integration.compose

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.ktx.InternalGlideApi
import com.bumptech.glide.integration.ktx.Size
import com.bumptech.glide.load.engine.executor.GlideIdlingResourceInit
import java.util.concurrent.atomic.AtomicReference
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class GlideComposeTest {
  private val context: Context = ApplicationProvider.getApplicationContext()
  @get:Rule val composeRule = createComposeRule()

  @Before
  fun setUp() {
    GlideIdlingResourceInit.initGlide(composeRule)
  }

  @Test
  fun glideImage_noModifierSize_resourceDrawable_displaysDrawable() {
    val description = "test"
    val resourceId = android.R.drawable.star_big_on
    composeRule.setContent { GlideImage(model = resourceId, contentDescription = description) }

    composeRule.waitForIdle()

    val expectedSize = resourceId.bitmapSize()
    composeRule
      .onNodeWithContentDescription(description)
      .assert(expectDisplayedDrawableSize(expectedSize))
  }

  @Test
  fun glideImage_withSizeLargerThanImage_noTransformSet_doesNotUpscaleImage() {
    val description = "test"
    val resourceId = android.R.drawable.star_big_on
    composeRule.setContent {
      GlideImage(
        model = resourceId,
        contentDescription = description,
        modifier = Modifier.size(300.dp, 300.dp)
      )
    }

    composeRule.waitForIdle()

    val expectedSize = resourceId.bitmapSize()
    composeRule
      .onNodeWithContentDescription(description)
      .assert(expectDisplayedDrawableSize(expectedSize))
  }

  @Test
  fun glideImage_withSizeLargerThanImage_upscaleTransformSet_upscalesImage() {
    val viewDimension = 300
    val description = "test"
    val sizeRef = AtomicReference<Size>()
    composeRule.setContent {
      GlideImage(
        model = android.R.drawable.star_big_on,
        requestBuilderTransform = { it.fitCenter() },
        contentDescription = description,
        modifier = Modifier.size(viewDimension.dp, viewDimension.dp)
      )

      with(LocalDensity.current) {
        val pixels = viewDimension.dp.roundToPx()
        sizeRef.set(Size(pixels, pixels))
      }
    }

    composeRule.waitForIdle()

    val pixels = sizeRef.get()
    composeRule
      .onNodeWithContentDescription(description)
      .assert(expectDisplayedDrawableSize(pixels))
  }

  @Test
  fun glideImage_withThumbnail_prefersFullSizeImage() {
    val description = "test"
    val thumbnailDrawable = context.getDrawable(android.R.drawable.star_big_off)
    val fullsizeDrawable = context.getDrawable(android.R.drawable.star_big_on)

    val fullsizeBitmap = (fullsizeDrawable as BitmapDrawable).bitmap

    composeRule.setContent {
      GlideImage(
        model = fullsizeDrawable,
        requestBuilderTransform = { it.thumbnail(Glide.with(context).load(thumbnailDrawable)) },
        contentDescription = description,
      )
    }

    composeRule.waitForIdle()

    composeRule
      .onNodeWithContentDescription(description)
      .assert(expectDisplayedDrawable(fullsizeBitmap) { (it as BitmapDrawable).bitmap })
  }

  private fun Int.bitmapSize() = context.resources.getDrawable(this, context.theme).size()
}

private fun Drawable.size() = (this as BitmapDrawable).bitmap.let { Size(it.width, it.height) }

private fun expectDisplayedDrawableSize(widthPixels: Int, heightPixels: Int): SemanticsMatcher =
  expectDisplayedDrawable(Size(widthPixels, heightPixels)) { it?.size() }

private fun expectDisplayedDrawableSize(expectedSize: Size): SemanticsMatcher =
  expectDisplayedDrawable(expectedSize) { it?.size() }

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
