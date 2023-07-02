@file:OptIn(ExperimentalGlideComposeApi::class)

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
import com.google.common.truth.ComparableSubject
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test

class GlidePainterTest {
    val context: Context = ApplicationProvider.getApplicationContext()

    @get:Rule
    val glideComposeRule = GlideComposeRule()

    @Test
    fun rememberGlidePainter_withoutSize_startsWithStateLoading() {
        var painter: GlidePainter? = null
        glideComposeRule.setContent {
            painter = rememberGlidePainter(model = android.R.drawable.star_big_on)
        }
        assertThat(painter!!.state).isEqualTo(GlidePainter.State.Loading)
    }

    @Test
    fun rememberGlidePainter_withOverrideSize_loadsImage() {
        var painter: GlidePainter? = null
        glideComposeRule.setContent {
            painter = rememberGlidePainter(model = android.R.drawable.star_big_on) {
                it.override(50)
            }
        }
        glideComposeRule.waitForIdle()
        assertThat(painter!!.state).isInstanceOf(GlidePainter.State.Success::class.java)
    }

    @Test
    fun rememberGlidePainter_whenDrawnWithSize_loadsImage() {
        var painter: GlidePainter? = null
        glideComposeRule.setContent {
            painter = rememberGlidePainter(model = android.R.drawable.star_big_on)
            Image(
                painter = painter!!,
                contentDescription = "",
            )
        }
        glideComposeRule.waitForIdle()
        assertThat(painter!!.state).isInstanceOf(GlidePainter.State.Success::class.java)
    }

    @Test
    fun rememberGlidePainter_withProvidedSize_andOverrideSize_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            glideComposeRule.setContent {
                val (size, _) = rememberSizeAndModifier(Modifier.size(10.dp))
                rememberGlidePainter(model = android.R.drawable.star_big_on, size) {
                    it.override(50)
                }
            }
        }
    }

    @Test
    fun rememberGlidePainter_withLayoutSize_startsWithStateLoading() {
        var painter: GlidePainter? = null
        glideComposeRule.setContent {
            val (size, _) = rememberSizeAndModifier(Modifier.size(10.dp))
            painter = rememberGlidePainter(model = android.R.drawable.star_big_on, size)
        }
        assertThat(painter!!.state).isEqualTo(GlidePainter.State.Loading)
    }

    @Test
    fun rememberGlidePainter_withLayoutSize_appliedToBox_loadsImage() {
        var painter: GlidePainter? = null
        glideComposeRule.setContent {
            val (size, modifier) = rememberSizeAndModifier(Modifier.size(10.dp))
            painter = rememberGlidePainter(model = android.R.drawable.star_big_on, size)
            Box(modifier)
        }
        glideComposeRule.waitForIdle()
        assertThat(painter!!.state).isInstanceOf(GlidePainter.State.Success::class.java)
    }

    @Test
    fun rememberGlidePainter_withOverrideSize_andInvalidImage_setsStateToFailed() {
        var painter: GlidePainter? = null
        glideComposeRule.setContent {
            painter = rememberGlidePainter(model = 1234) {
                it.override(50)
            }
        }
        glideComposeRule.waitForIdle()
        assertThat(painter!!.state).isEqualTo(GlidePainter.State.Failure)
    }

    @Test
    fun rememberGlidePainter_withLayoutSize_andInvalidImage_setsStateToFailed() {
        var painter: GlidePainter? = null
        glideComposeRule.setContent {
            val (size, modifier) = rememberSizeAndModifier(Modifier.size(10.dp))
            painter = rememberGlidePainter(model = 1234, size)
            Box(modifier)
        }
        glideComposeRule.waitForIdle()
        assertThat(painter!!.state).isEqualTo(GlidePainter.State.Failure)
    }

    @Test
    fun rememberGlidePainter_onLoadFromSource_setsDataSourceToSource() {
        var painter: GlidePainter? = null
        glideComposeRule.setContent {
            painter = rememberGlidePainter(model = android.R.drawable.star_big_on) {
                it.override(50)
            }
        }
        glideComposeRule.waitForIdle()
        assertThatDataSource(painter).isEqualTo(DataSource.LOCAL)
    }

    @Test
    fun rememberGlidePainter_onLoadFromMemory_setsDataSourceToMemory() {
        var painter: GlidePainter? = null
        val resourceId = android.R.drawable.star_big_on
        val overrideSize = 50
        // TODO: Compose always uses the generic paths to load models, so it skips options that are
        // set by default by Glide's various class specific .load() method overrides.
        val future = Glide.with(context).load(resourceId as Any).override(overrideSize).submit()
        glideComposeRule.waitForIdle()
        future.get()
        glideComposeRule.setContent {
            painter = rememberGlidePainter(model = resourceId) {
                it.override(overrideSize)
            }
        }

        glideComposeRule.waitForIdle()
        assertThatDataSource(painter).isEqualTo(DataSource.MEMORY_CACHE)
    }

    private fun assertThatDataSource(painter: GlidePainter?): ComparableSubject<DataSource> =
        assertThat((painter!!.state as GlidePainter.State.Success).dataSource)
}

