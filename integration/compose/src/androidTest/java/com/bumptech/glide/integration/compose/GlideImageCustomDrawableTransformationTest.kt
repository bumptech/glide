@file:OptIn(ExperimentalGlideComposeApi::class, ExperimentalCoroutinesApi::class)

package com.bumptech.glide.integration.compose

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.ScaleFactor
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.test.Constants
import com.bumptech.glide.integration.compose.test.GlideComposeRule
import com.bumptech.glide.integration.compose.test.assertDisplaysInstance
import com.bumptech.glide.integration.compose.test.onNodeWithDefaultContentDescription
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
class GlideImageCustomDrawableTransformationTest(private val contentScale: ContentScale) {
  @get:Rule val glideComposeRule = GlideComposeRule()

  @Test
  fun glideImage_nonBitmapDrawable_doesNotThrow() = runTest {
    val customDrawable = FakeDrawable()

    glideComposeRule.setContent {
      GlideImage(
        model = customDrawable,
        contentScale = contentScale,
        contentDescription = Constants.DEFAULT_DESCRIPTION,
        modifier = Modifier.size(100.dp, 200.dp)
      )
    }

    glideComposeRule.onNodeWithDefaultContentDescription().assertDisplaysInstance(customDrawable)
  }

  @Test
  fun glideImage_animatableDrawable_doesNotThrow() = runTest {
    val customDrawable = FakeAnimatableDrawable()

    glideComposeRule.setContent {
      GlideImage(
        model = customDrawable,
        contentScale = contentScale,
        contentDescription = Constants.DEFAULT_DESCRIPTION,
        modifier = Modifier.size(200.dp, 100.dp)
      )
    }

    glideComposeRule.onNodeWithDefaultContentDescription().assertDisplaysInstance(customDrawable)
  }

  companion object {
    @JvmStatic
    @Parameterized.Parameters(name = "{0}: contentScale")
    fun data() = arrayOf(
      ContentScale.Crop,
      ContentScale.FillBounds,
      ContentScale.FillHeight,
      ContentScale.FillWidth,
      ContentScale.Fit,
      ContentScale.Inside,
      ContentScale.None,
      object : ContentScale {
        override fun computeScaleFactor(srcSize: Size, dstSize: Size): ScaleFactor =
          ContentScale.Fit.computeScaleFactor(srcSize, dstSize)
      },
    )
  }
}

@Suppress("DeprecatedCallableAddReplaceWith")
private open class FakeDrawable : Drawable() {
  override fun draw(p0: Canvas) {
  }
  override fun setAlpha(p0: Int) = throw UnsupportedOperationException()
  override fun setColorFilter(p0: ColorFilter?) = throw UnsupportedOperationException()
  @Deprecated("Deprecated in Java")
  override fun getOpacity(): Int = throw UnsupportedOperationException()
}

private class FakeAnimatableDrawable : FakeDrawable(), Animatable {
  override fun start() {}
  override fun stop() {}
  override fun isRunning(): Boolean = throw UnsupportedOperationException()
}
