@file:OptIn(ExperimentalGlideComposeApi::class)

package com.bumptech.glide.integration.compose

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.dp
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

    assertThatModelIsInMemoryCache(preloadModels[2])
  }

  fun assertThatModelIsInMemoryCache(@DrawableRes model: Int){
    // Wait for previous aysnc image loads to finish
    glideComposeRule.waitForIdle()
    val nextPreloadModel: Drawable =
      Glide.with(context).load(model).onlyRetrieveFromCache(true).submit().get()
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

    assertThatModelIsInMemoryCache(preloadModels[2])
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

    assertThatModelIsInMemoryCache(preloadModels[2])
  }

  @Test
  fun glideLazyListPreloader_whenDataChanges_onScroll_preloadsUpdatedData() {
    glideComposeRule.setContent {
      // Swap both to avoid confusing the preloader. The preloader doesn't notice or take into
      // account data set changes (this is a bug in the Java preloading API)...
      val currentPreloadModels = remember { mutableStateOf(listOf<Int>()) }
      val currentModels = remember { mutableStateOf(listOf<Int>()) }
      // Use a button to swap data because we can't mutate state in setContent easily from outside
      // the method, nor can you call setContent multiple times.
      fun swapData() {
        currentPreloadModels.value = preloadModels
        currentModels.value = models
      }
      val state = rememberLazyListState()
      Column {
        TextButton(onClick = ::swapData) { Text(text = "Swap") }
        LazyRow(state = state,
                modifier = Modifier.testTag(listTestTag),
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          item { Text(text = "Header") }
          itemsIndexed(currentModels.value) { index, drawableResourceId ->
            GlideImage(
              model = drawableResourceId,
              contentDescription = imageContentDescription(index),
              Modifier.fillParentMaxWidth(),
            )
          }
        }
      }

      PreloadOneItemGlideLazyListPreloader(state, data = currentPreloadModels.value)
    }

    glideComposeRule.onNodeWithText("Swap").performClick()
    val scrollToIndex = 1
    glideComposeRule.onNode(hasTestTag(listTestTag)).performScrollToIndex(scrollToIndex)

    assertThatModelIsInMemoryCache(preloadModels[2])
  }

  @Suppress("TestFunctionName") // Not a Test...
  @Composable
  private fun PreloadOneItemGlideLazyListPreloader(
    state: LazyListState,
    data: List<Int> = preloadModels,
  ) =
    GlideLazyListPreloader(
      state = state,
      data = data,
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

