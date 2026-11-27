package com.bumptech.glide.integration.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bumptech.glide.integration.ktx.InternalGlideApi
import com.bumptech.glide.request.target.Target
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(InternalGlideApi::class)
@RunWith(AndroidJUnit4::class)
class SizeObserverTest {

    @get:Rule val composeRule = createComposeRule()

    @Test
    fun intrinsicMeasurementDoesNotCompleteSizeObserverWithOriginalSize() {
        val sizeObserver = SizeObserver()

        composeRule.setContent {
            Column(modifier = Modifier.width(IntrinsicSize.Max)) {
                Box(modifier = Modifier.sizeObservingModifier(sizeObserver).size(50.dp))
            }
        }
        composeRule.waitForIdle()

        val observed =
            runBlocking { withTimeout(2000) { sizeObserver.getSize() } }
        assertTrue(
            "Intrinsic measurement completed SizeObserver with SIZE_ORIGINAL: $observed",
            observed.width != Target.SIZE_ORIGINAL && observed.height != Target.SIZE_ORIGINAL,
        )
    }
}
