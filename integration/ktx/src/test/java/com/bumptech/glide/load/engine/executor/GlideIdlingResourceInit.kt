package com.bumptech.glide.load.engine.executor

import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.idling.concurrent.IdlingThreadPoolExecutor
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

object GlideIdlingResources {

    fun initGlide(builder: GlideBuilder? = null) {
        val registry = IdlingRegistry.getInstance()
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
        val glideExecutor = GlideExecutor(executor)
        Glide.init(
            ApplicationProvider.getApplicationContext(),
            (builder ?: GlideBuilder())
                .setSourceExecutor(glideExecutor)
                .setAnimationExecutor(glideExecutor)
                .setDiskCacheExecutor(glideExecutor)
        )
        registry.register(executor)
    }
}
