package com.bumptech.glide.integration.compose

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalGlideComposeApi::class)
@RunWith(AndroidJUnit4::class)
class GlideImageTest {

  @get:Rule(order = 1) val composeRule = createComposeRule()

  @Test
  fun glideImage_zeroWidthFillBounds_doesNotCrash() {
    composeRule.setContent {
      GlideImage(
        model = null,
        contentDescription = null,
        modifier = Modifier.width(0.dp).heightIn(0.dp, 100.dp),
        contentScale = ContentScale.FillBounds,
        loading = placeholder(android.R.drawable.star_on),
      )
    }
  }
}
