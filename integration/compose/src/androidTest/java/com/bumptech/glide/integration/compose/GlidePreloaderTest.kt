@file:OptIn(ExperimentalGlideComposeApi::class)

package com.bumptech.glide.integration.compose

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.core.app.ApplicationProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.compose.test.GlideComposeRule
import com.bumptech.glide.request.target.Target
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

class GlidePreloaderTest {
  private val context: Context = ApplicationProvider.getApplicationContext()
  @get:Rule val glideComposeRule = GlideComposeRule()

  @Test
  fun glideLazyListPreloader_withoutScroll_preloadsNextItem() {
    glideComposeRule.setContent {
      val state = rememberLazyListState()
      LazyRow(state = state, modifier = Modifier.testTag(listTestTag)) {
        itemsIndexed(models) { index, drawableResourceId ->
          GlideImage(
            model = drawableResourceId,
            contentDescription = imageContentDescription(index),
            Modifier.fillParentMaxWidth(),
          )
        }
      }

      PreloadOneItemGlideLazyListPreloader(state)
    }

    val nextPreloadModel: Drawable =
      Glide.with(context).load(preloadModels[2]).onlyRetrieveFromCache(true).submit().get()

    assertThat(nextPreloadModel).isNotNull()
  }

  @Test
  fun glideLazyListPreloader_onScroll_preloadsAheadInDirectionOfScroll() {
    glideComposeRule.setContent {
      val state = rememberLazyListState()
      LazyRow(state = state, modifier = Modifier.testTag(listTestTag)) {
        itemsIndexed(models) { index, drawableResourceId ->
          GlideImage(
            model = drawableResourceId,
            contentDescription = imageContentDescription(index),
            Modifier.fillParentMaxWidth(),
          )
        }
      }

      PreloadOneItemGlideLazyListPreloader(state)
    }

    val scrollToIndex = 1
    glideComposeRule.onNode(hasTestTag(listTestTag)).performScrollToIndex(scrollToIndex)

    val nextPreloadModel: Drawable =
      Glide.with(context).load(preloadModels[2]).onlyRetrieveFromCache(true).submit().get()

    assertThat(nextPreloadModel).isNotNull()
  }

  @Test
  fun glideLazyListPreloader_withHeaderItem_onScroll_doesNotCrash() {
    glideComposeRule.setContent {
      val state = rememberLazyListState()
      LazyRow(state = state, modifier = Modifier.testTag(listTestTag)) {
        item { Text(text = "Header") }
        itemsIndexed(models) { index, drawableResourceId ->
          GlideImage(
            model = drawableResourceId,
            contentDescription = imageContentDescription(index),
            Modifier.fillParentMaxWidth(),
          )
        }
      }

      PreloadOneItemGlideLazyListPreloader(state)
    }

    val scrollToIndex = 1
    glideComposeRule.onNode(hasTestTag(listTestTag)).performScrollToIndex(scrollToIndex)

    val nextPreloadModel: Drawable =
      Glide.with(context).load(preloadModels[2]).onlyRetrieveFromCache(true).submit().get()

    assertThat(nextPreloadModel).isNotNull()
  }

  @Suppress("TestFunctionName") // Not a Test...
  @Composable
  private fun PreloadOneItemGlideLazyListPreloader(state: LazyListState) =
    GlideLazyListPreloader(
      state = state,
      data = preloadModels,
      size = Size(Target.SIZE_ORIGINAL.toFloat(), Target.SIZE_ORIGINAL.toFloat()),
      numberOfItemsToPreload = 1,
      requestBuilderTransform = { resourceId, requestBuilder -> requestBuilder.load(resourceId) })

  companion object {
    val models =
      listOf(
        android.R.drawable.star_big_on,
        android.R.drawable.star_big_off,
        android.R.drawable.btn_plus,
      )
    // Use different preload and non-preload models so that we can assert on which items are
    // preloaded and not loaded by the list. This is bad practice in production code and would waste
    // resources while doing nothing useful in a real app.
    val preloadModels =
      listOf(
        android.R.drawable.btn_minus,
        android.R.drawable.btn_radio,
        android.R.drawable.btn_star,
      )

    val listTestTag = "listTestTag"
    fun imageContentDescription(index: Int) = "Image $index"
  }
}

