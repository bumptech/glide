@file:OptIn(ExperimentalGlideComposeApi::class)

package com.bumptech.glide.integration.compose

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.core.app.ApplicationProvider
import com.bumptech.glide.integration.compose.test.expectDisplayedDrawable
import com.bumptech.glide.integration.compose.test.expectDisplayedResource
import com.bumptech.glide.integration.compose.test.expectNoDrawable
import com.bumptech.glide.load.engine.executor.GlideIdlingResourceInit
import com.bumptech.glide.testutil.TearDownGlide
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Avoids [com.bumptech.glide.load.engine.executor.GlideIdlingResourceInit] because we want to make
 * assertions about loads that have not yet completed.
 */
class GlideImageErrorTest {
  private val context: Context = ApplicationProvider.getApplicationContext()
  @get:Rule(order = 1) val composeRule = createComposeRule()
  @get:Rule(order = 2) val tearDownGlide = TearDownGlide()

  @Before
  fun before() {
    GlideIdlingResourceInit.initGlide(composeRule)
  }

  @Test
  fun requestBuilderTransform_withErrorResourceId_displaysError() {
    val description = "test"
    val errorResourceId = android.R.drawable.star_big_off
    composeRule.setContent {
      GlideImage(model = null, contentDescription = description) {
        it.error(errorResourceId)
      }
    }

    composeRule
      .onNodeWithContentDescription(description)
      .assert(expectDisplayedResource(errorResourceId))
  }

  @Test
  fun requestBuilderTransform_withErrorDrawable_displaysError() {
    val description = "test"
    val errorDrawable = context.getDrawable(android.R.drawable.star_big_off)
    composeRule.setContent {
      GlideImage(model = null, contentDescription = description) { it.error(errorDrawable) }
    }

    composeRule
      .onNodeWithContentDescription(description)
      .assert(expectDisplayedDrawable(errorDrawable))
  }

  @Test
  fun failureParameter_withErrorResourceId_displaysError() {
    val description = "test"
    val failureResourceId = android.R.drawable.star_big_off
    composeRule.setContent {
      GlideImage(
        model = null,
        contentDescription = description,
        failure = placeholder(failureResourceId),
      )
    }

    composeRule
      .onNodeWithContentDescription(description)
      .assert(expectDisplayedResource(failureResourceId))
  }

  @Test
  fun failureParameter_withDrawable_displaysDrawable() {
    val description = "test"
    val failureDrawable = context.getDrawable(android.R.drawable.star_big_off)
    composeRule.setContent {
      GlideImage(
        model = null,
        contentDescription = description,
        failure = placeholder(failureDrawable),
      )
    }

    composeRule
      .onNodeWithContentDescription(description)
      .assert(expectDisplayedDrawable(failureDrawable))
  }

  @Test
  fun failureParameter_withNullDrawable_displaysNothing() {
    val description = "test"
    composeRule.setContent {
      GlideImage(
        model = null,
        contentDescription = description,
        failure = placeholder(null as Drawable?)
      )
    }

    composeRule.onNodeWithContentDescription(description).assert(expectNoDrawable())
  }

  @Test
  fun failureParameter_withComposable_displaysComposable() {
    val failureResourceId = android.R.drawable.star_big_off
    val description = "test"
    composeRule.setContent {
      GlideImage(
        model = null,
        contentDescription = "none",
        failure =
          placeholder {
            // Nesting GlideImage is not really a good idea, but it's convenient for this test
            // because
            // we can use our helpers to assert on its contents.
            GlideImage(
              model = null,
              contentDescription = description,
              failure = placeholder(failureResourceId),
            )
          }
      )
    }

    composeRule
      .onNodeWithContentDescription(description)
      .assert(expectDisplayedResource(failureResourceId))
  }

  @Test
  fun failure_setViaFailureParameterWithResourceId_andRequestBuilderTransform_prefersFailureParameter() {
    val description = "test"
    val failureResourceId = android.R.drawable.star_big_off
    composeRule.setContent {
      GlideImage(
        model = null,
        contentDescription = description,
        failure = placeholder(failureResourceId),
      ) {
        it.error(android.R.drawable.btn_star)
      }
    }

    composeRule
      .onNodeWithContentDescription(description)
      .assert(expectDisplayedResource(failureResourceId))
  }

  @Test
  fun failure_setViaFailureParameterWithDrawable_andRequestBuilderTransform_prefersFailureParameter() {
    val description = "test"
    val failureDrawable = context.getDrawable(android.R.drawable.star_big_off)
    composeRule.setContent {
      GlideImage(
        model = null,
        contentDescription = description,
        failure = placeholder(failureDrawable),
      ) {
        it.error(android.R.drawable.btn_star)
      }
    }

    composeRule
      .onNodeWithContentDescription(description)
      .assert(expectDisplayedDrawable(failureDrawable))
  }

  @Test
  fun failure_setViaFailureParameterWithNullDrawable_andRequestBuilderTransformWithNonNullDrawable_showsNoPlaceholder() {
    val description = "test"
    composeRule.setContent {
      GlideImage(
        model = null,
        contentDescription = description,
        failure = placeholder(null as Drawable?),
      ) {
        it.error(android.R.drawable.btn_star)
      }
    }

    composeRule.onNodeWithContentDescription(description).assert(expectNoDrawable())
  }

  @Test
  fun failure_setViaFailureParameterWithComposable_andRequestBuilderTransform_showsComposable() {
    val description = "test"
    val failureResourceId = android.R.drawable.star_big_off
    composeRule.setContent {
      GlideImage(
        model = null,
        contentDescription = "other",
        failure =
          placeholder {
            GlideImage(
              model = null,
              contentDescription = description,
              failure = placeholder(failureResourceId),
            )
          },
      ) {
        it.error(android.R.drawable.btn_star)
      }
    }

    composeRule
      .onNodeWithContentDescription(description)
      .assert(expectDisplayedResource(failureResourceId))
  }
}
