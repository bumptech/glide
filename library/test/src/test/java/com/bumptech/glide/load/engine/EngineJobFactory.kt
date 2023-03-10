package com.bumptech.glide.load.engine

import androidx.core.util.Pools
import com.bumptech.glide.load.engine.EngineJob.EngineResourceFactory
import com.bumptech.glide.load.engine.EngineResource.ResourceListener
import com.bumptech.glide.load.engine.executor.GlideExecutor
import kotlinx.coroutines.asCoroutineDispatcher

/**
 * A Kotlin adapter that lets us convert GlideExecutors into CoroutineDispatchers without exposing
 * package private classes in [Engine].
 */
internal object EngineJobFactory {
  @JvmStatic
  fun <R> newFakeEngineJob(
    diskCacheExecutor: GlideExecutor,
    sourceExecutor: GlideExecutor,
    sourceUnlimitedExecutor: GlideExecutor,
    animationExecutor: GlideExecutor,
    engineJobListener: EngineJobListener,
    resourceListener: ResourceListener,
    pool: Pools.Pool<EngineJob<*>>,
    factory: EngineResourceFactory
  ) : EngineJob<R> =
    EngineJob(
      diskCacheExecutor,
      sourceExecutor,
      sourceUnlimitedExecutor,
      animationExecutor,
      diskCacheExecutor.asCoroutineDispatcher(),
      sourceExecutor.asCoroutineDispatcher(),
      sourceUnlimitedExecutor.asCoroutineDispatcher(),
      animationExecutor.asCoroutineDispatcher(),
      engineJobListener,
      resourceListener,
      pool,
      factory
    )
}