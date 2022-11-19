@file:OptIn(ExperimentalGlideComposeApi::class, InternalGlideApi::class)

package com.bumptech.glide.integration.compose

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.compose.test.GlideComposeRule
import com.bumptech.glide.integration.compose.test.assertDisplays
import com.bumptech.glide.integration.compose.test.bitmapSize
import com.bumptech.glide.integration.compose.test.dpToPixels
import com.bumptech.glide.integration.compose.test.expectDisplayedDrawable
import com.bumptech.glide.integration.compose.test.expectDisplayedDrawableSize
import com.bumptech.glide.integration.ktx.InternalGlideApi
import com.bumptech.glide.integration.ktx.Size
import java.util.concurrent.atomic.AtomicReference
import org.junit.Rule
import org.junit.Test

class GlideImageTest {
  private val context: Context = ApplicationProvider.getApplicationContext()
  @get:Rule val glideComposeRule = GlideComposeRule()

  @Test
  fun glideImage_noModifierSize_resourceDrawable_displaysDrawable() {
    val description = "test"
    val resourceId = android.R.drawable.star_big_on
    glideComposeRule.setContent { GlideImage(model = resourceId, contentDescription = description) }

    glideComposeRule.waitForIdle()

    val expectedSize = resourceId.bitmapSize()
    glideComposeRule
      .onNodeWithContentDescription(description)
      .assert(expectDisplayedDrawableSize(expectedSize))
  }

  @Test
  fun glideImage_withSizeLargerThanImage_noTransformSet_doesNotUpscaleImage() {
    val description = "test"
    val resourceId = android.R.drawable.star_big_on
    glideComposeRule.setContent {
      GlideImage(
        model = resourceId,
        contentDescription = description,
        modifier = Modifier.size(300.dp, 300.dp)
      )
    }

    glideComposeRule.waitForIdle()

    val expectedSize = resourceId.bitmapSize()
    glideComposeRule
      .onNodeWithContentDescription(description)
      .assert(expectDisplayedDrawableSize(expectedSize))
  }

  @Test
  fun glideImage_withChangingModel_refreshes() {
    val description = "test"

    val firstDrawable: Drawable = context.getDrawable(android.R.drawable.star_big_off)!!
    val secondDrawable: Drawable = context.getDrawable(android.R.drawable.star_big_on)!!

    glideComposeRule.setContent {
      val model = remember { mutableStateOf(firstDrawable) }

      fun swapModel() {
        model.value = secondDrawable
      }

      Column {
        TextButton(onClick = ::swapModel) { Text(text = "Swap") }
        GlideImage(
          model = model.value,
          modifier = Modifier.size(100.dp),
          contentDescription = description
        )
      }
    }

    glideComposeRule.waitForIdle()
    glideComposeRule.onNodeWithText("Swap").performClick()
    glideComposeRule.waitForIdle()

    glideComposeRule
      .onNodeWithContentDescription(description)
      .assert(expectDisplayedDrawable(secondDrawable))
  }

  @Test
  fun glideImage_withSizeLargerThanImage_upscaleTransformSet_upscalesImage() {
    val viewDimension = 300
    val description = "test"
    val sizeRef = AtomicReference<Size>()
    glideComposeRule.setContent {
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

    glideComposeRule.waitForIdle()

    val pixels = sizeRef.get()
    glideComposeRule
      .onNodeWithContentDescription(description)
      .assert(expectDisplayedDrawableSize(pixels))
  }

  @Test
  fun glideImage_withThumbnail_prefersFullSizeImage() {
    val description = "test"
    val thumbnailDrawable = context.getDrawable(android.R.drawable.star_big_off)
    val fullsizeDrawable = context.getDrawable(android.R.drawable.star_big_on)

    glideComposeRule.setContent {
      GlideImage(
        model = fullsizeDrawable,
        requestBuilderTransform = { it.thumbnail(Glide.with(context).load(thumbnailDrawable)) },
        contentDescription = description,
      )
    }

    glideComposeRule.waitForIdle()
    glideComposeRule
      .onNodeWithContentDescription(description)
      .assert(expectDisplayedDrawable(fullsizeDrawable))
  }

  @Test
  fun glideImage_withZeroSize_doesNotStartLoad() {
    val description = "test"
    glideComposeRule.setContent {
      Box(modifier = Modifier.size(0.dp)) {
        GlideImage(
          model = android.R.drawable.star_big_on,
          contentDescription = description,
        )
      }
    }
    glideComposeRule.waitForIdle()
    glideComposeRule
      .onNodeWithContentDescription(description)
      .assertDisplays(null)
  }


  @Test
  fun glideImage_withNegativeSize_doesNotStartLoad() {
    val description = "test"
    glideComposeRule.setContent {
      Box(modifier = Modifier.size((-10).dp)) {
        GlideImage(
          model = android.R.drawable.star_big_on,
          contentDescription = description,
        )
      }
    }
    glideComposeRule.waitForIdle()
    glideComposeRule
      .onNodeWithContentDescription(description)
      .assertDisplays(null)
  }

  @Test
  fun glideImage_withZeroWidth_validHeight_doesNotStartLoad() {
    val description = "test"
    glideComposeRule.setContent {
      Box(modifier = Modifier.size(0.dp, 10.dp)) {
        GlideImage(
          model = android.R.drawable.star_big_on,
          contentDescription = description,
        )
      }
    }
    glideComposeRule.waitForIdle()
    glideComposeRule
      .onNodeWithContentDescription(description)
      .assertDisplays(null)
  }

  @Test
  fun glideImage_withValidWidth_zeroHeight_doesNotStartLoad() {
    val description = "test"
    glideComposeRule.setContent {
      Box(modifier = Modifier.size(10.dp, 0.dp)) {
        GlideImage(
          model = android.R.drawable.star_big_on,
          contentDescription = description,
        )
      }
    }
    glideComposeRule.waitForIdle()
    glideComposeRule
      .onNodeWithContentDescription(description)
      .assertDisplays(null)
  }

  @Test
  fun glideImage_withZeroSize_thenValidSize_startsLoadWithValidSize() {
    val description = "test"
    val resourceId = android.R.drawable.star_big_on
    val validSizeDp = 10
    glideComposeRule.setContent {
      val currentSize = remember { mutableStateOf(0.dp) }
      fun swapSize() {
        currentSize.value = validSizeDp.dp
      }

      TextButton(onClick = ::swapSize) { Text(text = "Swap") }
      Box(modifier = Modifier.size(currentSize.value)) {
        GlideImage(
          model = resourceId,
          contentDescription = description,
        )
      }
    }
    glideComposeRule.waitForIdle()
    glideComposeRule.onNodeWithText("Swap").performClick()
    glideComposeRule.waitForIdle()

    glideComposeRule
      .onNodeWithContentDescription(description)
      .assert(expectDisplayedDrawableSize(Size(validSizeDp.dpToPixels(), validSizeDp.dpToPixels())))
  }

  @Test
  fun glideImage_withZeroSize_thenMultipleValidSizes_startsLoadWithFirstValidSize() {
    val description = "test"
    val resourceId = android.R.drawable.star_big_on
    val validSizeDps = listOf(10, 20, 30, 40)
    glideComposeRule.setContent {
      val currentSize = remember { mutableStateOf(0.dp) }
      val currentSizeIndex = remember { mutableStateOf(0) }
      fun swapSize() {
        currentSize.value = validSizeDps[currentSizeIndex.value].dp
        currentSizeIndex.value++
      }

      TextButton(onClick = ::swapSize) { Text(text = "Swap") }
      Box(modifier = Modifier.size(currentSize.value)) {
        GlideImage(
          model = resourceId,
          contentDescription = description,
        )
      }
    }
    repeat(validSizeDps.size) {
      glideComposeRule.waitForIdle()
      glideComposeRule.onNodeWithText("Swap").performClick()
    }
    glideComposeRule.waitForIdle()

    val expectedSize = validSizeDps[0]
    glideComposeRule
      .onNodeWithContentDescription(description)
      .assert(
        expectDisplayedDrawableSize(Size(expectedSize.dpToPixels(), expectedSize.dpToPixels()))
      )
  }
}
