@file:OptIn(ExperimentalGlideComposeApi::class, InternalGlideApi::class)

package com.bumptech.glide.integration.compose

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.compose.test.bitmapSize
import com.bumptech.glide.integration.compose.test.expectDisplayedDrawable
import com.bumptech.glide.integration.compose.test.expectDisplayedDrawableSize
import com.bumptech.glide.integration.ktx.InternalGlideApi
import com.bumptech.glide.integration.ktx.Size
import com.bumptech.glide.load.engine.executor.GlideIdlingResourceInit
import com.bumptech.glide.testutil.TearDownGlide
import java.util.concurrent.atomic.AtomicReference
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class GlideComposeTest {
  private val context: Context = ApplicationProvider.getApplicationContext()
  @get:Rule(order = 1) val composeRule = createComposeRule()
  @get:Rule(order = 2) val tearDownGlide = TearDownGlide()

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
  fun glideImage_withChangingModel_refreshes() {
    val description = "test"

    val firstDrawable: Drawable = context.getDrawable(android.R.drawable.star_big_off)!!
    val secondDrawable: Drawable = context.getDrawable(android.R.drawable.star_big_on)!!

    composeRule.setContent {
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

    composeRule.waitForIdle()
    composeRule.onNodeWithText("Swap").performClick()
    composeRule.waitForIdle()

    composeRule
      .onNodeWithContentDescription(description)
      .assert(expectDisplayedDrawable(secondDrawable))
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
      .assert(expectDisplayedDrawable(fullsizeDrawable))
  }
}
