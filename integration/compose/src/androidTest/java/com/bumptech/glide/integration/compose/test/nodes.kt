package com.bumptech.glide.integration.compose.test

import android.app.Application
import android.content.Context
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.core.app.ApplicationProvider

object Constants {
  const val DEFAULT_DESCRIPTION = "test"
}

fun ComposeContentTestRule.onNodeWithDefaultContentDescription() =
  onNodeWithContentDescription(Constants.DEFAULT_DESCRIPTION)

fun SemanticsNodeInteraction.assertDisplays(@DrawableRes resourceId: Int) =
  assertDisplays(ApplicationProvider.getApplicationContext<Application>().getDrawable(resourceId))


fun SemanticsNodeInteraction.assertDisplays(drawable: Drawable?) =
  assert(expectDisplayedDrawable(drawable))

fun SemanticsNodeInteraction.assertDisplaysInstance(drawable: Drawable) =
  assert(expectSameInstance(drawable))
