@file:OptIn(ExperimentalGlideComposeApi::class, ExperimentalCoroutinesApi::class)

package com.bumptech.glide.integration.compose

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.ScaleFactor
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.testing.TestLifecycleOwner
import com.bumptech.glide.integration.compose.test.Constants
import com.bumptech.glide.integration.compose.test.GlideComposeRule
import com.bumptech.glide.integration.compose.test.assertDisplaysInstance
import com.bumptech.glide.integration.compose.test.onNodeWithDefaultContentDescription
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Tests Issue #4943.
 *
 * Transformable types are tested in [GlideImageDefaultTransformationTest].
 */
@RunWith(Parameterized::class)
class GlideImageCustomDrawableTransformationTest(
  private val contentScale: ContentScale,
  // We need a shorter test name than the ContentScale class name to make google3 happy, so we
  // add an extra parameter. Unfortunately that means we need to list it in the constructor even
  // though it's only used by Parameters to create the test name.
  @Suppress("unused") private val name: String,
) {
  @get:Rule val glideComposeRule = GlideComposeRule()

  @Test
  fun glideImage_nonBitmapDrawable_doesNotThrow() = runTest {
    val customDrawable = FakeDrawable()

    glideComposeRule.setContent { GlideImageWithCustomDrawable(customDrawable) }

    glideComposeRule.onNodeWithDefaultContentDescription().assertDisplaysInstance(customDrawable)
  }

  @Test
  fun glideImage_animatableDrawable_doesNotThrow() = runTest {
    val customDrawable = FakeAnimatableDrawable()

    glideComposeRule.setContent { GlideImageWithCustomDrawable(customDrawable) }

    glideComposeRule.onNodeWithDefaultContentDescription().assertDisplaysInstance(customDrawable)
  }

  @Test
  fun glideImage_animatableDrawable_stopsAnimationWhenLifecycleNotStarted() = runTest {
    val customDrawable = FakeAnimatableDrawable()
    val testLifecycleOwner = TestLifecycleOwner(initialState = Lifecycle.State.STARTED)

    glideComposeRule.setContent {
      CompositionLocalProvider(LocalLifecycleOwner provides testLifecycleOwner) {
        GlideImageWithCustomDrawable(customDrawable)
      }
    }
    assertThat(customDrawable.animating).isTrue()
    testLifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
    assertThat(customDrawable.animating).isFalse()
  }

  @Composable
  private fun GlideImageWithCustomDrawable(customDrawable: FakeDrawable) {
    GlideImage(
      model = customDrawable,
      contentScale = contentScale,
      contentDescription = Constants.DEFAULT_DESCRIPTION,
      modifier = Modifier.size(200.dp, 100.dp),
    )
  }

  companion object {
    // Add a second parameter purely to make the test name shorter, see the comment on the test
    // constructor argument for details.
    @JvmStatic
    @Parameterized.Parameters(name = "{1}")
    fun data() =
      arrayOf(
        arrayOf(ContentScale.Crop, "Crop"),
        arrayOf(ContentScale.FillBounds, "FillBounds"),
        arrayOf(ContentScale.FillHeight, "FillHeight"),
        arrayOf(ContentScale.FillWidth, "FillWidth"),
        arrayOf(ContentScale.Fit, "Fit"),
        arrayOf(ContentScale.Inside, "Inside"),
        arrayOf(ContentScale.None, "None"),
        arrayOf(
          object : ContentScale {
            override fun computeScaleFactor(srcSize: Size, dstSize: Size): ScaleFactor =
              ContentScale.Fit.computeScaleFactor(srcSize, dstSize)
          },
          "Custom",
        ),
      )
  }
}

@Suppress("DeprecatedCallableAddReplaceWith")
private open class FakeDrawable : Drawable() {
  override fun draw(p0: Canvas) {}

  override fun setAlpha(p0: Int) = throw UnsupportedOperationException()

  override fun setColorFilter(p0: ColorFilter?) = throw UnsupportedOperationException()

  @Deprecated("Deprecated in Java")
  override fun getOpacity(): Int = throw UnsupportedOperationException()
}

private class FakeAnimatableDrawable : FakeDrawable(), Animatable {
  var animating: Boolean? = null

  override fun start() {
    animating = true
  }

  override fun stop() {
    animating = false
  }

  override fun isRunning(): Boolean = throw UnsupportedOperationException()
}
