package com.bumptech.glide.load.engine.executor

import androidx.compose.ui.test.IdlingResource
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.idling.concurrent.IdlingThreadPoolExecutor
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

object GlideIdlingResourceInit {

  fun initGlide(composeRule: ComposeTestRule) {
    val executor =
      IdlingThreadPoolExecutor(
        "glide_test_thread",
        /* corePoolSize = */ 1,
        /* maximumPoolSize = */ 1,
        /* keepAliveTime = */ 1,
        TimeUnit.SECONDS,
        LinkedBlockingQueue()
      ) {
        Thread(it)
      }
    composeRule.registerIdlingResource(
      object : IdlingResource {
        override val isIdleNow: Boolean
          get() = executor.isIdleNow
      }
    )
    val glideExecutor = GlideExecutor(executor)
    Glide.init(
      ApplicationProvider.getApplicationContext(),
      GlideBuilder()
        .setSourceExecutor(glideExecutor)
        .setAnimationExecutor(glideExecutor)
        .setDiskCacheExecutor(glideExecutor)
    )
  }
}
