package com.bumptech.glide.integration.compose

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.core.app.ApplicationProvider
import com.bumptech.glide.integration.compose.test.expectDisplayedDrawable
import com.bumptech.glide.integration.compose.test.expectDisplayedResource
import com.bumptech.glide.integration.compose.test.expectNoDrawable
import com.bumptech.glide.testutil.TearDownGlide
import com.bumptech.glide.testutil.WaitModelLoaderRule
import org.junit.Rule
import org.junit.Test

/**
 * Avoids [com.bumptech.glide.load.engine.executor.GlideIdlingResourceInit] and
 * [com.bumptech.glide.integration.compose.test.GlideComposeRule] because we want to make assertions
 * about loads that have not yet completed.
 */
@Suppress("DEPRECATION") // Tests for a deprecated method
@OptIn(ExperimentalGlideComposeApi::class)
class GlideImagePlaceholderTest {
  private val context: Context = ApplicationProvider.getApplicationContext()
  @get:Rule(order = 1)
  val composeRule = createComposeRule()
  @get:Rule(order = 2)
  val waitModelLoaderRule = WaitModelLoaderRule()
  @get:Rule(order = 3)
  val tearDownGlide = TearDownGlide()

  @Test
  fun requestBuilderTransform_withPlaceholderResourceId_displaysPlaceholder() {
    val description = "test"
    val waitModel = waitModelLoaderRule.waitOn(android.R.drawable.star_big_on)
    val placeholderResourceId = android.R.drawable.star_big_off
    composeRule.setContent {
      GlideImage(model = waitModel, contentDescription = description) {
        it.placeholder(placeholderResourceId)
      }
    }

    composeRule
      .onNodeWithContentDescription(description)
      .assert(expectDisplayedResource(placeholderResourceId))
  }

  @Test
  fun requestBuilderTransform_withPlaceholderDrawable_displaysPlaceholder() {
    val description = "test"
    val waitModel = waitModelLoaderRule.waitOn(android.R.drawable.star_big_on)
    val placeholderDrawable = context.getDrawable(android.R.drawable.star_big_off)
    composeRule.setContent {
      GlideImage(model = waitModel, contentDescription = description) {
        it.placeholder(placeholderDrawable)
      }
    }

    composeRule
      .onNodeWithContentDescription(description)
      .assert(expectDisplayedDrawable(placeholderDrawable))
  }

  @Test
  fun loadingParameter_withResourceId_displaysResource() {
    val description = "test"
    val waitModel = waitModelLoaderRule.waitOn(android.R.drawable.star_big_on)
    val placeholderResourceId = android.R.drawable.star_big_off
    composeRule.setContent {
      GlideImage(
        model = waitModel,
        contentDescription = description,
        loading = placeholder(placeholderResourceId),
      )
    }

    composeRule
      .onNodeWithContentDescription(description)
      .assert(expectDisplayedResource(placeholderResourceId))
  }

  @Test
  fun loadingParameter_withDrawable_displaysResource() {
    val description = "test"
    val waitModel = waitModelLoaderRule.waitOn(android.R.drawable.star_big_on)
    val placeholderDrawable = context.getDrawable(android.R.drawable.star_big_off)
    composeRule.setContent {
      GlideImage(
        model = waitModel,
        contentDescription = description,
        loading = placeholder(placeholderDrawable),
      )
    }

    composeRule
      .onNodeWithContentDescription(description)
      .assert(expectDisplayedDrawable(placeholderDrawable))
  }

  @Test
  fun loadingParameter_withNullDrawable_displaysNothing() {
    val description = "test"
    val waitModel = waitModelLoaderRule.waitOn(android.R.drawable.star_big_on)
    composeRule.setContent {
      GlideImage(
        model = waitModel,
        contentDescription = description,
        loading = placeholder(null as Drawable?)
      )
    }

    composeRule.onNodeWithContentDescription(description).assert(expectNoDrawable())
  }

  @Test
  fun loadingParameter_withComposable_displaysComposable() {
    val waitModel = waitModelLoaderRule.waitOn(android.R.drawable.star_big_on)
    val placeholderResourceId = android.R.drawable.star_big_off
    val description = "test"
    composeRule.setContent {
      GlideImage(
        model = waitModel,
        contentDescription = "none",
        loading =
        placeholder {
          // Nesting GlideImage is not really a good idea, but it's convenient for this test
          // because
          // we can use our helpers to assert on its contents.
          GlideImage(
            model = waitModel,
            contentDescription = description,
            loading = placeholder(placeholderResourceId),
          )
        }
      )
    }

    composeRule
      .onNodeWithContentDescription(description)
      .assert(expectDisplayedResource(placeholderResourceId))
  }

  @Test
  fun loading_setViaLoadingParameterWithResourceId_andRequestBuilderTransform_prefersLoadingParameter() {
    val description = "test"
    val waitModel = waitModelLoaderRule.waitOn(android.R.drawable.star_big_on)
    val placeholderResourceId = android.R.drawable.star_big_off
    composeRule.setContent {
      GlideImage(
        model = waitModel,
        contentDescription = description,
        loading = placeholder(placeholderResourceId),
      ) {
        it.placeholder(android.R.drawable.btn_star)
      }
    }

    composeRule
      .onNodeWithContentDescription(description)
      .assert(expectDisplayedResource(placeholderResourceId))
  }

  @Test
  fun loading_setViaLoadingParameterWithDrawable_andRequestBuilderTransform_prefersLoadingParameter() {
    val description = "test"
    val waitModel = waitModelLoaderRule.waitOn(android.R.drawable.star_big_on)
    val placeholderDrawable = context.getDrawable(android.R.drawable.star_big_off)
    composeRule.setContent {
      GlideImage(
        model = waitModel,
        contentDescription = description,
        loading = placeholder(placeholderDrawable),
      ) {
        it.placeholder(android.R.drawable.btn_star)
      }
    }

    composeRule
      .onNodeWithContentDescription(description)
      .assert(expectDisplayedDrawable(placeholderDrawable))
  }

  @Test
  fun loading_setViaLoadingParameterWithNullDrawable_andRequestBuilderTransform_showsNoResource() {
    val description = "test"
    val waitModel = waitModelLoaderRule.waitOn(android.R.drawable.star_big_on)
    composeRule.setContent {
      GlideImage(
        model = waitModel,
        contentDescription = description,
        loading = placeholder(null as Drawable?),
      ) {
        it.placeholder(android.R.drawable.btn_star)
      }
    }

    composeRule.onNodeWithContentDescription(description).assert(expectNoDrawable())
  }

  @Test
  fun loading_setViaLoadingParameterWithComposable_andRequestBuilderTransform_showsComposable() {
    val description = "test"
    val waitModel = waitModelLoaderRule.waitOn(android.R.drawable.star_big_on)
    val placeholderResourceId = android.R.drawable.star_big_off
    composeRule.setContent {
      GlideImage(
        model = waitModel,
        contentDescription = "other",
        loading =
        placeholder {
          GlideImage(
            model = waitModel,
            contentDescription = description,
            loading = placeholder(placeholderResourceId),
          )
        },
      ) {
        it.placeholder(android.R.drawable.btn_star)
      }
    }

    composeRule
      .onNodeWithContentDescription(description)
      .assert(expectDisplayedResource(placeholderResourceId))
  }
}
