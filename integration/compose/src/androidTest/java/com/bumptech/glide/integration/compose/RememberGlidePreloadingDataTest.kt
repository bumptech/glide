@file:OptIn(ExperimentalGlideComposeApi::class, ExperimentalGlideComposeApi::class)

package com.bumptech.glide.integration.compose

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.integration.compose.test.GlideComposeRule
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.Target
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

class RememberGlidePreloadingDataTest {
  private val context: Context = ApplicationProvider.getApplicationContext()
  @get:Rule val glideComposeRule = GlideComposeRule()

  @Test
  fun rememberGlidePreloadingData_withoutScroll_preloadsNextItem() {
    glideComposeRule.setContent {
      val preloadingData = rememberOneItemAtATimePreloadingData()

      LazyRow(modifier = Modifier.testTag(listTestTag)) {
        items(preloadingData.size) { index ->
          preloadingData.triggerPreload(index)
          GlideImage(
            model = model,
            contentDescription = imageContentDescription(index),
            Modifier.fillParentMaxWidth(),
          )
        }
      }
    }

    assertThatModelIsInMemoryCache(preloadModels[1])
  }

  @Test
  fun glideLazyListPreloader_onScroll_preloadsAheadInDirectionOfScroll() {
    glideComposeRule.setContent {
      val preloadingData = rememberOneItemAtATimePreloadingData()
      LazyRow(modifier = Modifier.testTag(listTestTag)) {
        items(preloadingData.size) { index ->
          preloadingData.triggerPreload(index)
          GlideImage(
            model = model,
            contentDescription = imageContentDescription(index),
            Modifier.fillParentMaxWidth(),
          )
        }
      }
    }

    val scrollToIndex = 1
    glideComposeRule.onNode(hasTestTag(listTestTag)).performScrollToIndex(scrollToIndex)

    assertThatModelIsInMemoryCache(preloadModels[2])
  }

  @Test
  fun glideLazyListPreloader_withHeaderItem_onScroll_doesNotCrash() {
    glideComposeRule.setContent {
      val preloadingData = rememberOneItemAtATimePreloadingData()

      LazyRow(modifier = Modifier.testTag(listTestTag)) {
        item { Text(text = "Header") }
        items(preloadingData.size) { index ->
          preloadingData.triggerPreload(index)
          GlideImage(
            model = model,
            contentDescription = imageContentDescription(index),
            Modifier.fillParentMaxWidth(),
          )
        }
      }
    }

    // Scroll to the 0th image, accounting for the first header item.
    val scrollToIndex = 1
    glideComposeRule.onNode(hasTestTag(listTestTag)).performScrollToIndex(scrollToIndex)
    // Make sure the next image, the 1th, is in memory due to preloading.
    assertThatModelIsInMemoryCache(preloadModels[1])
  }

  @Test
  fun glideLazyListPreloader_whenDataChanges_onScroll_preloadsUpdatedData() {
    glideComposeRule.setContent {
      // Swap both to avoid confusing the preloader. The preloader doesn't notice or take into
      // account data set changes (this is a bug in the Java preloading API)...
      val currentPreloadModels = remember { mutableStateListOf<Int>() }
      val currentModels = remember { mutableStateListOf<Int>() }
      // Use a button to swap data because we can't mutate state in setContent easily from
      // outside
      // the method, nor can you call setContent multiple times.
      fun swapData() {
        currentPreloadModels.addAll(preloadModels)
        currentModels.addAll(preloadModels)
      }
      val preloadData =
        rememberGlidePreloadingData(
          data = currentPreloadModels,
          preloadImageSize = Target.SIZE_ORIGINAL.toSize(),
          numberOfItemsToPreload = 1,
          fixedVisibleItemCount = 1,
        ) { data: Int, requestBuilder: RequestBuilder<Drawable> ->
          requestBuilder.load(data).removeTheme()
        }

      TextButton(onClick = ::swapData) { Text(text = "Swap") }

      Column {
        LazyRow(
          modifier = Modifier.testTag(listTestTag),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          items(currentModels.size) { index ->
            // This mismatch between currentModels and preloadData may lead to errors in
            // the future
            // because items may be recomposed before the setContent method's function
            // is
            // recomposed. See https://chat.google.com/room/AAAAYRnp4-Y/AvFrBgb_peU for
            // a bunch of
            // detailed discussion.
            preloadData.triggerPreload(index)
            GlideImage(
              model = currentModels[index],
              contentDescription = imageContentDescription(index),
              Modifier.fillParentMaxWidth(),
            )
          }
        }
      }
    }

    glideComposeRule.onNodeWithText("Swap").performClick()
    glideComposeRule.waitForIdle()
    val scrollToIndex = 1
    glideComposeRule.onNode(hasTestTag(listTestTag)).performScrollToIndex(scrollToIndex)

    assertThatModelIsInMemoryCache(preloadModels[scrollToIndex + 1])
  }

  @Test
  fun glideLazyListPreloader_withHeaderItems_andPositionFunction_onScroll_preloadsTheFirstItem() {
    val numHeaderItems = 3
    glideComposeRule.setContent {
      val data = rememberOneItemAtATimePreloadingData()
      LazyRow(modifier = Modifier.testTag(listTestTag)) {
        repeat(numHeaderItems) { item { Text(text = "Header$it") } }
        items(data.size) { index ->
          data.triggerPreload(index)
          GlideImage(
            model = model,
            contentDescription = imageContentDescription(index),
            Modifier.fillParentMaxWidth(),
          )
        }
      }
    }

    val imageIndex = 1
    val scrollToIndex = numHeaderItems + imageIndex
    glideComposeRule.onNode(hasTestTag(listTestTag)).performScrollToIndex(scrollToIndex)

    assertThatModelIsInMemoryCache(preloadModels[imageIndex + 1])
  }

  @Test
  fun rememberGlidePreloadingData_onDispose_cancelsAndClearsPreloads() {
    var showPreloader by mutableStateOf(true)
    val modelToPreload = preloadModels[1] // Preloader loads ahead, so triggering at 0 loads 1

    glideComposeRule.setContent {
      if (showPreloader) {
        val preloadingData =
          rememberGlidePreloadingData(
            data = preloadModels,
            preloadImageSize = Target.SIZE_ORIGINAL.toSize(),
            numberOfItemsToPreload = 1,
            fixedVisibleItemCount = 1,
          ) { model, requestBuilder ->
            requestBuilder.load(model).removeTheme()
          }
        preloadingData.triggerPreload(0)
      }
    }

    // Verify it loaded into memory
    assertThatModelIsInMemoryCache(modelToPreload)

    // Dispose the preloader
    showPreloader = false
    glideComposeRule.waitForIdle()

    // Clear memory cache. Active resources (leaked) will NOT be cleared.
    // Inactive resources (cleared) WILL be cleared.
    glideComposeRule.runOnUiThread { Glide.get(context).clearMemory() }
    glideComposeRule.waitForIdle()

    // Verify if it's still in memory.
    // If it leaked, it's still active, so this will succeed.
    // If it was fixed, it was cleared, so this will fail.
    val isStillInMemory =
      try {
        val future =
          Glide.with(context)
            .load(modelToPreload)
            .removeTheme()
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .onlyRetrieveFromCache(true)
            .submit()
        future.get()
        Glide.with(context).clear(future)
        true
      } catch (e: Exception) {
        false
      }

    assertThat(isStillInMemory).isFalse()
  }

  // Ignore the preload request because we want to test that the preloader loaded a model
  // and not be confused by our UI loading a model. Do not ignore the preload request
  // builder in real code!
  @Composable
  private fun <DataT> GlidePreloadingData<DataT>.triggerPreload(index: Int) = this[index].first

  @Composable
  private fun rememberOneItemAtATimePreloadingData(): GlidePreloadingData<Int> {
    return rememberGlidePreloadingData(
      data = preloadModels,
      preloadImageSize = Target.SIZE_ORIGINAL.toSize(),
      numberOfItemsToPreload = 1,
      fixedVisibleItemCount = 1,
    ) { model, requestBuilder ->
      requestBuilder.load(model).removeTheme()
    }
  }

  private fun assertThatModelIsInMemoryCache(@DrawableRes model: Int) {
    // Wait for previous async image loads to finish
    glideComposeRule.waitForIdle()
    val future =
      Glide.with(context)
        .load(model)
        .removeTheme()
        .diskCacheStrategy(DiskCacheStrategy.NONE)
        .onlyRetrieveFromCache(true)
        .submit()
    val nextPreloadModel: Drawable = future.get()
    assertThat(nextPreloadModel).isNotNull()
    // Clear the future target to release the resource from "active" to "cached" state,
    // otherwise it remains pinned in memory by this verification request.
    Glide.with(context).clear(future)
  }

  // We're loading the same resource across two different Contexts. One is the Context from the
  // instrumentation package, the other is the package under test. Each Context has it's own
  // Theme,
  // neither of which are equal to each other. So that we can verify an item is loaded into
  // memory,
  // we remove the themes from all requests that we need to have matching cache keys.
  private fun <T> RequestBuilder<T>.removeTheme() = theme(null)

  private companion object {
    const val model = android.R.drawable.star_big_on

    // Use different preload and non-preload models so that we can assert on which items are
    // preloaded and not loaded by the list. This is bad practice in production code and would
    // waste
    // resources while doing nothing useful in a real app.
    val preloadModels =
      listOf(
        android.R.drawable.btn_minus,
        android.R.drawable.btn_radio,
        android.R.drawable.btn_star,
      )

    const val listTestTag = "listTestTag"

    fun imageContentDescription(index: Int) = "Image $index"
  }
}

private fun Int.toSize() = this.toFloat().let { Size(it, it) }
