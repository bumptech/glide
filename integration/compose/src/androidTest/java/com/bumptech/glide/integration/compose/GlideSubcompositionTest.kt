package com.bumptech.glide.integration.compose

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.compose.test.GlideComposeRule
import com.bumptech.glide.load.DataSource
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalGlideComposeApi::class)
class GlideSubcompositionTest {
  val context: Context = ApplicationProvider.getApplicationContext()

  @get:Rule
  val glideComposeRule = GlideComposeRule()

  @Test
  fun glideSubcomposition_withoutSize_startsWithStateLoading() {
    var currentState: RequestState? = null
    glideComposeRule.setContent {
      GlideSubcomposition(model = android.R.drawable.star_big_on) {
        if (currentState == null) {
          currentState = state
        }
      }
    }
    assertThat(currentState).isEqualTo(RequestState.Loading)
  }

  @Test
  fun glideSubcomposition_withOverrideSize_loadsImage() {
    var currentState: RequestState? = null
    glideComposeRule.setContent {
      GlideSubcomposition(
        android.R.drawable.star_big_on,
        requestBuilderTransform = { it.override(50) }
      ) {
        currentState = state
      }
    }
    glideComposeRule.waitForIdle()
    assertThat(currentState).isInstanceOf(RequestState.Success::class.java)
  }

  @Test
  fun glideSubcomposition_whenDrawnWithSize_loadsImage() {
    var currentState: RequestState? = null
    glideComposeRule.setContent {
      GlideSubcomposition(model = android.R.drawable.star_big_on) {
        currentState = state
        Image(
          painter = painter,
          contentDescription = "",
        )
      }
    }
    glideComposeRule.waitForIdle()
    assertThat(currentState).isInstanceOf(RequestState.Success::class.java)
  }

  @Test
  fun glideSubcomposition_withLayoutSize_startsWithStateLoading() {
    var currentState: RequestState? = null
    glideComposeRule.setContent {
      GlideSubcomposition(model = android.R.drawable.star_big_on, Modifier.size(10.dp)) {
        if (currentState == null) {
          currentState = state
        }
        Image(
          painter = painter,
          contentDescription = "",
        )
      }
    }
    assertThat(currentState).isEqualTo(RequestState.Loading)
  }

  @Test
  fun glideSubcomposition_withLayoutSize_appliedToBox_loadsImage() {
    var currentState: RequestState? = null
    glideComposeRule.setContent {
      GlideSubcomposition(model = android.R.drawable.star_big_on, Modifier.size(10.dp)) {
        currentState = state
        Box(Modifier.size(10.dp))
      }
    }
    glideComposeRule.waitForIdle()
    assertThat(currentState).isInstanceOf(RequestState.Success::class.java)
  }

  @Test
  fun glideSubcomposition_withOverrideSize_andInvalidImage_setsStateToFailed() {
    var currentState: RequestState? = null
    glideComposeRule.setContent {
      GlideSubcomposition(model = 1234, requestBuilderTransform = { it.override(50) }) {
        currentState = state
      }
    }
    glideComposeRule.waitForIdle()
    assertThat(currentState).isEqualTo(RequestState.Failure)
  }

  @Test
  fun glideSubcomposition_withLayoutSize_andInvalidImage_setsStateToFailed() {
    var currentState: RequestState? = null
    glideComposeRule.setContent {
      GlideSubcomposition(model = 1234, Modifier.size(10.dp)) {
        currentState = state
      }
    }
    glideComposeRule.waitForIdle()
    assertThat(currentState).isEqualTo(RequestState.Failure)
  }

  @Test
  fun glideSubcomposition_onLoadFromSource_setsDataSourceToSource() {
    var dataSource: DataSource? = null
    glideComposeRule.setContent {
      GlideSubcomposition(
        model = android.R.drawable.star_big_on,
        requestBuilderTransform = { it.override(50) }
      ) {
        val currentState = state
        if (currentState is RequestState.Success) {
          dataSource = currentState.dataSource
        }
      }
    }
    glideComposeRule.waitForIdle()
    assertThat(dataSource).isEqualTo(DataSource.LOCAL)
  }

  @Test
  fun glideSubcomposition_onLoadFromMemory_setsDataSourceToMemory() {
    var dataSource: DataSource? = null
    val resourceId = android.R.drawable.star_big_on
    val overrideSize = 50
    // TODO: Compose always uses the generic paths to load models, so it skips options that are
    // set by default by Glide's various class specific .load() method overrides.
    val future = Glide.with(context).load(resourceId as Any).override(overrideSize).submit()
    glideComposeRule.waitForIdle()
    future.get()
    glideComposeRule.setContent {
      GlideSubcomposition(
        model = resourceId,
        requestBuilderTransform = { it.override(overrideSize) }
      ) {
        val currentState = state
        if (currentState is RequestState.Success) {
          dataSource = currentState.dataSource
        }
      }
    }

    glideComposeRule.waitForIdle()
    assertThat(dataSource).isEqualTo(DataSource.MEMORY_CACHE)
  }
}

