@file:OptIn(
  ExperimentalCoroutinesApi::class,
  ExperimentGlideFlows::class,
  ExperimentalGlideComposeApi::class
)

package com.bumptech.glide.integration.compose

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.integration.compose.test.Constants
import com.bumptech.glide.integration.compose.test.GlideComposeRule
import com.bumptech.glide.integration.compose.test.assertDisplays
import com.bumptech.glide.integration.compose.test.dpToPixels
import com.bumptech.glide.integration.compose.test.onNodeWithDefaultContentDescription
import com.bumptech.glide.integration.ktx.ExperimentGlideFlows
import com.bumptech.glide.integration.ktx.Resource
import com.bumptech.glide.integration.ktx.Status
import com.bumptech.glide.integration.ktx.flow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

/** Non-transformable types are tested in [GlideImageCustomDrawableTransformationTest] */
class GlideImageDefaultTransformationTest {
  private val context: Context = ApplicationProvider.getApplicationContext()
  @get:Rule val glideComposeRule = GlideComposeRule()

  @Test
  fun glideImage_withContentScaleNone_noTransformation_doesNotApplyTransformation() = runTest {
    val resourceId = android.R.drawable.star_big_on
    val expectedDrawable = loadExpectedDrawable(resourceId)

    glideComposeRule.setContent {
      ContentScaleGlideImage(
        model = resourceId,
        contentScale = ContentScale.None,
      )
    }

    glideComposeRule.onNodeWithDefaultContentDescription().assertDisplays(expectedDrawable)
  }

  @Test
  fun glideImage_withContentScaleFit_noTransformation_appliesCenterInsideTransformation() =
    runTest {
      val resourceId = android.R.drawable.star_big_on
      val expectedDrawable = loadExpectedDrawable(resourceId) { it.centerInside() }

      glideComposeRule.setContent {
        ContentScaleGlideImage(
          model = resourceId,
          contentScale = ContentScale.Fit,
        )
      }

      glideComposeRule.onNodeWithDefaultContentDescription().assertDisplays(expectedDrawable)
    }

  @Test
  fun glideImage_withContentScaleFit_explicitTransformation_usesExplicitTransformation() = runTest {
    val resourceId = android.R.drawable.star_big_on
    val expectedDrawable = loadExpectedDrawable(resourceId) { it.centerCrop() }

    glideComposeRule.setContent {
      ContentScaleGlideImage(
        model = resourceId,
        contentScale = ContentScale.Fit,
      ) {
        it.centerCrop()
      }
    }

    glideComposeRule.onNodeWithDefaultContentDescription().assertDisplays(expectedDrawable)
  }

  @Test
  fun glideImage_withContentScaleInside_noTransformation_appliesCenterInsideTransformation() =
    runTest {
      val resourceId = android.R.drawable.star_big_on
      val expectedDrawable = loadExpectedDrawable(resourceId) { it.centerInside() }

      glideComposeRule.setContent {
        ContentScaleGlideImage(
          model = resourceId,
          contentScale = ContentScale.Inside,
        )
      }

      glideComposeRule.onNodeWithDefaultContentDescription().assertDisplays(expectedDrawable)
    }

  @Test
  fun glideImage_withContentScaleInside_explicitTransformation_usesExplicitTransformation() =
    runTest {
      val resourceId = android.R.drawable.star_big_on
      val expectedDrawable = loadExpectedDrawable(resourceId) { it.centerCrop() }

      glideComposeRule.setContent {
        ContentScaleGlideImage(
          model = resourceId,
          contentScale = ContentScale.Inside,
        ) {
          it.centerCrop()
        }
      }

      glideComposeRule.onNodeWithDefaultContentDescription().assertDisplays(expectedDrawable)
    }

  @Test
  fun glideImage_withContentScaleCrop_noTransformation_appliesCenterCropTransformation() = runTest {
    val resourceId = android.R.drawable.star_big_on
    val expectedDrawable = loadExpectedDrawable(resourceId) { it.centerCrop() }

    glideComposeRule.setContent {
      ContentScaleGlideImage(
        model = resourceId,
        contentScale = ContentScale.Crop,
      )
    }

    glideComposeRule.onNodeWithDefaultContentDescription().assertDisplays(expectedDrawable)
  }

  @Test
  fun glideImage_withContentScaleCrop_explicitTransformation_usesExplicitTransformation() =
    runTest {
      val resourceId = android.R.drawable.star_big_on
      val expectedDrawable = loadExpectedDrawable(resourceId) { it.centerInside() }

      glideComposeRule.setContent {
        ContentScaleGlideImage(
          model = resourceId,
          contentScale = ContentScale.Crop,
        ) {
          it.centerInside()
        }
      }

      glideComposeRule.onNodeWithDefaultContentDescription().assertDisplays(expectedDrawable)
    }

  private suspend fun RequestBuilder<Drawable>.loadRequiringSuccess() =
    (this.flow().first { it.status == Status.SUCCEEDED } as Resource<Drawable>).resource

  private suspend fun loadExpectedDrawable(
    @DrawableRes resourceId: Int,
    transformation: (RequestBuilder<Drawable>) -> RequestBuilder<Drawable> = { it -> it },
  ): Drawable =
    transformation(
      Glide.with(context).load(resourceId).override(WIDTH.dpToPixels(), HEIGHT.dpToPixels()))
      .loadRequiringSuccess()

  @Composable
  private fun ContentScaleGlideImage(
    model: Any?,
    contentScale: ContentScale,
    requestBuilderTransform: RequestBuilderTransform<Drawable> = { it -> it },
  ) =
    GlideImage(
      model = model,
      contentDescription = Constants.DEFAULT_DESCRIPTION,
      modifier = SIZE_MODIFIER,
      contentScale = contentScale,
      requestBuilderTransform = requestBuilderTransform,
    )

  companion object {
    const val WIDTH = 25
    // non-square
    const val HEIGHT = 30

    val SIZE_MODIFIER = Modifier.size(WIDTH.dp, HEIGHT.dp)
  }
}
