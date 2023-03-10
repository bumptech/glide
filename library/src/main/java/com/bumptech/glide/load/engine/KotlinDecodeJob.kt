package com.bumptech.glide.load.engine

import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import com.bumptech.glide.GlideContext
import com.bumptech.glide.Priority
import com.bumptech.glide.Registry.NoResultEncoderAvailableException
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.EncodeStrategy
import com.bumptech.glide.load.Key
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.ResourceEncoder
import com.bumptech.glide.load.Transformation
import com.bumptech.glide.load.data.DataFetcher
import com.bumptech.glide.load.data.DataFetcher.DataCallback
import com.bumptech.glide.load.data.DataRewinder
import com.bumptech.glide.load.engine.DecodeJob.DeferredEncodeManager
import com.bumptech.glide.load.engine.DecodeJob.DiskCacheProvider
import com.bumptech.glide.load.engine.executor.GlideExecutor
import com.bumptech.glide.load.engine.executor.GlideExecutor.UncaughtThrowableStrategy
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.resource.UnitTransformation
import com.bumptech.glide.load.resource.bitmap.Downsampler
import com.bumptech.glide.util.LogTime
import com.bumptech.glide.util.Util
import java.io.File
import java.io.IOException
import java.util.concurrent.RejectedExecutionException
import kotlin.coroutines.AbstractCoroutineContextKey
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

internal class KotlinDecodeJob<TranscodeT>(
  private val diskCacheProvider: DiskCacheProvider,
  private val glideContext: GlideContext,
  private val model: Any,
  private val signature: Key,
  private val width: Int,
  private val height: Int,
  private val resourceClass: Class<*>,
  private val transcodeClass: Class<TranscodeT>,
  internal val priority: Priority,
  private val diskCacheStrategy: DiskCacheStrategy,
  private val transformations: Map<Class<out Any>, Transformation<out Any>>,
  private val isTransformationRequired: Boolean,
  private val isScaleOnlyOrNoTransform: Boolean,
  private val onlyRetrieveFromCache: Boolean,
  private val options: Options,
  private val callback: DecodeJob.Callback<TranscodeT>,
  internal val order: Int,
) {
  private var isEncoding: Boolean = false
  private val deferredEncodeManager: DeferredEncodeManager<Any> = DeferredEncodeManager<Any>()
  private val throwables: MutableList<Throwable> by lazy { mutableListOf() }
  private val loadDataList: List<ModelLoader.LoadData<out Any>> by lazy {
    val modelLoaders = glideContext.registry.getModelLoaders(model)
    buildList {
      for (modelLoader in modelLoaders) {
        val current = modelLoader.buildLoadData(model, width, height, options) ?: continue
        add(current)
      }
    }
  }
  private val cacheKeys: List<Key> by lazy {
    buildList {
      for (loadData in loadDataList) {
        if (!contains(loadData.sourceKey)) {
          add(loadData.sourceKey)
        }
        for (alternateKey in loadData.alternateKeys) {
          if (!contains(alternateKey)) {
            add(alternateKey)
          }
        }
      }
    }
  }

  fun willDecodeFromCache(): Boolean =
    diskCacheStrategy.decodeCachedData() || diskCacheStrategy.decodeCachedResource()

  suspend fun run() {
    try {
      runWrapped()
    } catch (e: CallbackException) {
      throw e
    } catch (t: Throwable) {
      // TODO: CallbackException?
      if (t is CancellationException) {
        return
      }
      if (Log.isLoggable(TAG, Log.WARN)) {
        Log.w(TAG, "Load for $model threw unexpectedly", t)
      }
      // If we're encoding, we've already notified the callback once and should not do so a second
      // time
      if (!isEncoding) {
        throwables.add(t)
        notifyFailed()
      }
      throw t
    }
  }

  private fun notifyFailed() {
    val glideException = GlideException("Failed to load resource", throwables.toList())
    callback.onLoadFailed(glideException)
  }

  private suspend fun runWrapped() {
    val startTime = LogTime.getLogTime()
    val resourceCacheResourceKeyAndDataSource = decodeFromResourceCache()
    if (resourceCacheResourceKeyAndDataSource != null) {
      if (Log.isLoggable(TAG, Log.VERBOSE)) {
        Log.v(
          TAG,
          "loaded $model with size [$width, $height] from resource cache in ${
            LogTime.getElapsedMillis(startTime)
          }"
        )
      }
      notifyEncodeAndRelease(resourceCacheResourceKeyAndDataSource)
      return
    }

    if (!coroutineContext.isActive) {
      notifyFailed()
      return
    }

    withContext(callback.sourceExecutor) {
      val dataCacheResourceKeyAndDataSource = decodeFromSourceCache()
      if (dataCacheResourceKeyAndDataSource != null) {
        notifyEncodeAndRelease(dataCacheResourceKeyAndDataSource)
        return@withContext
      }

      if (!coroutineContext.isActive) {
        notifyFailed()
        return@withContext
      }

      val sourceResourceKeyAndDataSource = decodeFromSource()
      if (sourceResourceKeyAndDataSource != null) {
        notifyEncodeAndRelease(sourceResourceKeyAndDataSource)
        return@withContext
      }

      notifyFailed()
    }
  }

  private fun notifyEncodeAndRelease(
    resourceCacheKeyAndDataSource: ResourceCacheKeyAndDataSource<TranscodeT>,
  ) {
    if (resourceCacheKeyAndDataSource.resource is Initializable) {
      resourceCacheKeyAndDataSource.resource.initialize()
    }

    if (!deferredEncodeManager.hasResourceToEncode()) {
      callResourceReadyCallback(resourceCacheKeyAndDataSource)
      return
    }

    val lockedResource = LockedResource.obtain(resourceCacheKeyAndDataSource.resource)
    callResourceReadyCallback(
      overrideResource = lockedResource,
      resourceCacheKeyAndDataSource = resourceCacheKeyAndDataSource,
    )

    isEncoding = true;

    try {
      deferredEncodeManager.encode(diskCacheProvider, options)
    } finally {
      lockedResource.unlock()
    }
  }

  private fun callResourceReadyCallback(
    resourceCacheKeyAndDataSource: ResourceCacheKeyAndDataSource<TranscodeT>,
    overrideResource: Resource<TranscodeT> = resourceCacheKeyAndDataSource.resource,
  ) =
    callback.onResourceReady(
      overrideResource,
      resourceCacheKeyAndDataSource.dataSource,
      resourceCacheKeyAndDataSource.isLoadedFromAlternateCacheKey)

  private suspend fun decodeFromSource(): ResourceCacheKeyAndDataSource<TranscodeT>? {
    if (onlyRetrieveFromCache) {
      return null
    }

    for (loadData in loadDataList) {
      if (!coroutineContext.isActive) {
        return null
      }
      if (!diskCacheStrategy.isDataCacheable(loadData.fetcher.dataSource)
        && !hasLoadPath(loadData.fetcher.dataClass)) {
        continue
      }

      try {
        val resource: Resource<TranscodeT>? =
          when (val dataOrFailure = loadData.fetcher.loadDataSuspended(loadData.sourceKey)) {
            is DataOrFailure.Data ->
              writeToAndDecodeFromCacheOrDecodeFromSource(
                loadData.sourceKey, dataOrFailure.data, loadData.fetcher.dataSource
              )
            is DataOrFailure.Failure -> {
              handleDataFailure(
                dataOrFailure, loadData.fetcher.dataSource, loadData.fetcher.dataClass
              )
              null
            }
        }
        if (resource != null) {
          // TODO: Is alternate cache key correct here?
          return ResourceCacheKeyAndDataSource(
            resource, loadData.sourceKey.isAlternateCacheKey, loadData.fetcher.dataSource)
        }
      } finally {
        loadData.fetcher.cleanup()
      }
    }
    return null
  }

  private suspend fun writeToAndDecodeFromCacheOrDecodeFromSource(
    sourceKey: Key, data: Any?, dataSource: DataSource,
  ): Resource<TranscodeT>? {
    if (data == null) {
      return null
    }
    return if (diskCacheStrategy.isDataCacheable(dataSource)) {
      val rewinder = glideContext.registry.getRewinder(data)
      val rewoundData = rewinder.rewindAndGet()
      val newKey = writeDataToCache(sourceKey, rewoundData)
      val cacheFile = newKey?.let { diskCacheProvider.diskCache.get(it) }
      if (cacheFile != null) {
        tryLoadFromCacheFile(sourceKey, cacheFile, dataSource, newKey)
      } else {
        decodeFromData(sourceKey, rewinder.rewindAndGet(), dataSource)
      }
    } else {
      decodeFromData(sourceKey, data, dataSource)
    }
  }

  private fun writeDataToCache(sourceKey: Key, data: Any): Key? {
    return try {
      writeDataToCacheChecked(sourceKey, data)
    } catch (e: IOException) {
      // An IOException means we weren't able to write data to cache or we weren't able to rewind
      // it after a disk cache write failed. In either case we can just move on and try the next
      // fetch below.
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, "Failed to properly rewind or write data to cache", e)
      }
      null
    }
  }

  private fun writeDataToCacheChecked(sourceKey: Key, data: Any): Key {
    val encoder = glideContext.registry.getSourceEncoder(data)
    val writer = DataCacheWriter(encoder, data, options)
    val newOriginalKey = DataCacheKey(sourceKey, signature)
    val diskCache = diskCacheProvider.diskCache
    diskCache.put(newOriginalKey, writer)
    return newOriginalKey
  }

  private suspend fun decodeFromSourceCache(): ResourceCacheKeyAndDataSource<TranscodeT>? {
    if (!diskCacheStrategy.decodeCachedData() || cacheKeys.isEmpty()) {
      return null
    }
    for (cacheKey in cacheKeys) {

      if (!coroutineContext.isActive) {
        return null
      }

      val dataCacheKey = DataCacheKey(cacheKey, signature)
      val cacheFile: File = diskCacheProvider.diskCache.get(dataCacheKey) ?: continue
      val resource: Resource<TranscodeT>? =
        tryLoadFromCacheFile(
          sourceKey = cacheKey,
          cacheFile = cacheFile,
          dataSource = DataSource.DATA_DISK_CACHE,
          currentAttemptingKey = dataCacheKey
        )
      if (resource != null) {
        return ResourceCacheKeyAndDataSource(
          resource, cacheKey.isAlternateCacheKey, DataSource.DATA_DISK_CACHE)
      }
    }
    return null
  }

  private val Key.isAlternateCacheKey: Boolean
    get() = this != cacheKeys[0]

  private fun sortedResourceClasses(): List<Class<*>> {
    val resourceClasses: List<Class<*>> =
      glideContext.registry.getRegisteredResourceClasses(
        model.javaClass, resourceClass, transcodeClass
      )
    if (resourceClasses.isEmpty()) {
      return resourceClasses
    }

    val idealResourceClass =
      if (transcodeClass != Drawable::class.java) transcodeClass else BitmapDrawable::class.java
    return resourceClasses.sortedBy { it != idealResourceClass }
  }

  private suspend fun decodeFromResourceCache(): ResourceCacheKeyAndDataSource<TranscodeT>? {
    if (!diskCacheStrategy.decodeCachedResource() || cacheKeys.isEmpty()) {
      return null
    }
    val resourceClasses: List<Class<*>> = sortedResourceClasses()
    if (resourceClasses.isEmpty()) {
      if (File::class.java == transcodeClass) {
        return null
      }
      throw IllegalStateException(
        "Failed to find any load path from ${model::class.java} to $transcodeClass"
      )
    }

    for (cacheKey in cacheKeys) {
      for (resourceClass in resourceClasses) {
        if (!coroutineContext.isActive) {
          return null
        }

        val transformation = getTransformation(resourceClass)
        val resourceCacheKey =
          ResourceCacheKey(
            glideContext.arrayPool,
            cacheKey,
            signature,
            width,
            height,
            transformation,
            resourceClass,
            options)

        val cacheFile: File = diskCacheProvider.diskCache.get(resourceCacheKey) ?: continue
        val resource: Resource<TranscodeT>? =
          tryLoadFromCacheFile(
            sourceKey = cacheKey,
            cacheFile = cacheFile,
            dataSource = DataSource.RESOURCE_DISK_CACHE,
            currentAttemptingKey = cacheKey
          )
        if (resource != null) {
          return ResourceCacheKeyAndDataSource(
            resource, cacheKey.isAlternateCacheKey, DataSource.RESOURCE_DISK_CACHE
          )
        }
      }
    }
    return null
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  private suspend fun DataFetcher<out Any>.loadDataSuspended(currentAttemptingKey: Key) =
    suspendCancellableCoroutine { cont ->
      loadData(priority, object : DataCallback<Any> {
        override fun onDataReady(data: Any?) {
          cont.resume(DataOrFailure.Data(data)) {
            cleanup()
          }
        }

        override fun onLoadFailed(e: Exception) {
          cont.resume(DataOrFailure.Failure(currentAttemptingKey, e)) {
            cleanup()
          }
        }
      })
    }

  private suspend fun tryLoadFromCacheFile(
    sourceKey: Key, cacheFile: File, dataSource: DataSource, currentAttemptingKey: Key,
  ): Resource<TranscodeT>? {
    val modelLoaders: List<ModelLoader<File, *>> =
      glideContext.registry.getModelLoaders(cacheFile)

    for (modelLoader in modelLoaders) {
      if (!coroutineContext.isActive) {
        return null
      }

      val loadData = modelLoader.buildLoadData(cacheFile, width, height, options) ?: continue
      if (!hasLoadPath(loadData.fetcher.dataClass)) {
        continue
      }

      try {
        val resource: Resource<TranscodeT>? =
          when (val dataOrFailure = loadData.fetcher.loadDataSuspended(currentAttemptingKey)) {
            is DataOrFailure.Data ->
              decodeFromData(sourceKey, dataOrFailure.data, dataSource)
            is DataOrFailure.Failure -> {
              handleDataFailure(dataOrFailure, dataSource, loadData.fetcher.dataClass)
              null
            }
          }
        if (resource != null) {
          return resource
        }
      } finally {
        loadData.fetcher.cleanup()
      }
    }

    return null
  }

  private fun handleDataFailure(
    failure: DataOrFailure.Failure, dataSource: DataSource, dataClass: Class<out Any>,
  ) {
    val glideException = GlideException("Fetching data failed", failure.throwable)
    glideException.setLoggingDetails(
      failure.currentAttemptingKey, dataSource, dataClass
    )
    throwables.add(glideException)
  }

  private fun <DataT> decodeFromData(
    sourceKey: Key, data: DataT?, dataSource: DataSource,
  ): Resource<TranscodeT>? {
    if (data == null) {
      return null
    }
    val loadPath: LoadPath<DataT & Any, Any, TranscodeT> =
      getLoadPath(data.javaClass) ?: return null
    val optionsWithHardwareConfig = getOptionsWithHardwareConfig(dataSource)
    val rewinder: DataRewinder<DataT> = glideContext.registry.getRewinder(data)
    return try {
      loadPath.load(rewinder, optionsWithHardwareConfig, width, height) { resource ->
        onResourceDecoded(sourceKey, dataSource, resource)
      }
    } catch (e: GlideException) {
      throwables.add(e)
      null
    } finally {
      rewinder.cleanup()
    }
  }

  private fun Key.isSourceKey() =
    loadDataList.map { it.sourceKey }.any { this == it }

  private fun <Z> onResourceDecoded(
    cacheKey: Key, dataSource: DataSource, decoded: Resource<Z>,
  ): Resource<Z> {
    val resourceSubClass = decoded.get().javaClass
    var appliedTransformation: Transformation<Z>? = null
    var transformed = decoded
    if (dataSource != DataSource.RESOURCE_DISK_CACHE) {
      appliedTransformation = getTransformation(resourceSubClass)
      transformed = appliedTransformation.transform(glideContext, decoded, width, height)
    }
    // TODO: Make this the responsibility of the Transformation.
    if (decoded != transformed) {
      decoded.recycle()
    }
    val encodeStrategy: EncodeStrategy
    val encoder: ResourceEncoder<Z>?
    if (glideContext.registry.isResourceEncoderAvailable(transformed)) {
      encoder = glideContext.registry.getResultEncoder(transformed)
      encodeStrategy = encoder.getEncodeStrategy(options)
    } else {
      encoder = null
      encodeStrategy = EncodeStrategy.NONE
    }
    var result: Resource<Z> = transformed
    val isFromAlternateCacheKey: Boolean = !cacheKey.isSourceKey()
    if (diskCacheStrategy.isResourceCacheable(
        isFromAlternateCacheKey, dataSource, encodeStrategy
      )
    ) {
      if (encoder == null) {
        throw NoResultEncoderAvailableException(transformed.get().javaClass)
      }
      val key: Key =
        when (encodeStrategy) {
          EncodeStrategy.SOURCE -> DataCacheKey(cacheKey, signature)
          EncodeStrategy.TRANSFORMED ->
            ResourceCacheKey(
              glideContext.arrayPool,
              cacheKey,
              signature,
              width,
              height,
              appliedTransformation,
              resourceSubClass,
              options
            )
          else -> throw IllegalArgumentException("Unknown strategy: $encodeStrategy")
      }
      val lockedResult = LockedResource.obtain(transformed)
      deferredEncodeManager.init(key, encoder, lockedResult)
      result = lockedResult
    }
    return result
  }

  private class ResourceCacheKeyAndDataSource<TranscodeT>(
    val resource: Resource<TranscodeT>,
    val isLoadedFromAlternateCacheKey: Boolean,
    val dataSource: DataSource,
  )

  private sealed class DataOrFailure {
    class Data(val data: Any?) : DataOrFailure()
    class Failure(val currentAttemptingKey: Key, val throwable: Throwable?) : DataOrFailure()
  }

  private fun hasLoadPath(dataClass: Class<out Any>) = getLoadPath(dataClass) != null

  @Suppress("UNCHECKED_CAST")
  private fun <DataT> getLoadPath(dataClass: Class<DataT>): LoadPath<DataT, Any, TranscodeT>? =
    glideContext.registry.getLoadPath(dataClass, resourceClass, transcodeClass)
      as LoadPath<DataT, Any, TranscodeT>?

  @Suppress("UNCHECKED_CAST")
  private fun <ResourceT> getTransformation(
    resourceClass: Class<out ResourceT>,
  ): Transformation<ResourceT> {
    val result: Transformation<ResourceT>? =
      transformations[resourceClass] as? Transformation<ResourceT>?
    if (result == null) {
      for ((key, value) in transformations) {
        if (key.isAssignableFrom(resourceClass)) {
          return value as Transformation<ResourceT>
        }
      }
    }

    if (transformations.isEmpty() && isTransformationRequired) {
      throw IllegalArgumentException(
        "Missing transformation for $resourceClass. If you wish to ignore unknown resource types," +
          " use the optional transformation methods."
      )
    } else {
      return UnitTransformation.get()
    }
  }

  private fun getOptionsWithHardwareConfig(dataSource: DataSource): Options {
    var options = options
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
      return options
    }
    val isHardwareConfigSafe =
      dataSource == DataSource.RESOURCE_DISK_CACHE || isScaleOnlyOrNoTransform
    val isHardwareConfigAllowed = options.get(Downsampler.ALLOW_HARDWARE_CONFIG)

    // If allow hardware config is defined, we can use it if it's set to false or if it's safe to
    // use the hardware config for the request.
    if (isHardwareConfigAllowed != null && (!isHardwareConfigAllowed || isHardwareConfigSafe)) {
      return options
    }

    // If allow hardware config is undefined or is set to true but it's unsafe for us to use the
    // hardware config for this request, we need to override the config.
    options = Options()
    options.putAll(this.options)
    options.set(Downsampler.ALLOW_HARDWARE_CONFIG, isHardwareConfigSafe)
    return options
  }

  companion object {
    const val TAG = "KotlinDecodeJob"
  }
}

internal class CoroutineScopeHolder(
  private val supervisor: CompletableJob = SupervisorJob(),
  internal val coroutineScope: CoroutineScope = CoroutineScope(supervisor)) {
  fun cancelAll() {
    supervisor.cancel()
  }
}

internal class KotlinResourceLoadTask<TranscodeT>(
  private val kotlinDecodeJob: KotlinDecodeJob<TranscodeT>,
  private val scopeHolder: CoroutineScopeHolder,
) : EngineJob.ResourceLoadTask {
  private var job: Job? = null

  private val KotlinDecodeJob<*>.priorityInfo: PriorityDispatcher.PriorityInfo
    get() = PriorityDispatcher.PriorityInfo(priority, order)

  override fun execute(executor: GlideExecutor, dispatcher: CoroutineDispatcher) {
    check(job == null)
    job =
      scopeHolder.coroutineScope.launch {
        withContext(dispatcher + kotlinDecodeJob.priorityInfo) {
          try {
            kotlinDecodeJob.run()
          } catch (t: Throwable) {
            val glideExecutor =
              (dispatcher as ExecutorCoroutineDispatcher).executor as GlideExecutor
            glideExecutor.uncaughtThrowableStrategy().handle(t)
          }
        }
      }
  }
  override fun cancel() {
    job?.cancel()
  }
  override fun release(isRemovedFromQueue: Boolean) {}
  override fun willDecodeFromCache() = kotlinDecodeJob.willDecodeFromCache()
}

internal class PriorityDispatcher(
  override val executor: GlideExecutor,
): ExecutorCoroutineDispatcher() {
  override fun close() {}

  override fun equals(other: Any?): Boolean =
    other is PriorityDispatcher && other.executor == executor

  override fun hashCode(): Int = System.identityHashCode(executor)

  override fun dispatch(context: CoroutineContext, block: Runnable) {
    try {
      executor.execute(wrapTask(block, context.priorityInfo))
    } catch (e: RejectedExecutionException) {
      cancelJob(context, e)
      // Let the coroutine finish and be cleaned up. See the reasoning in in kotlinx's Executors
      // class
      Dispatchers.IO.dispatch(context, block)
    }
  }

  private fun cancelJob(context: CoroutineContext, e: RejectedExecutionException) =
    context.cancel(CancellationException("Executor rejected task", e))

  private fun wrapTask(block: Runnable, priorityInfo: PriorityInfo): Runnable =
    ComparableRunnable(block, priorityInfo)

  private class ComparableRunnable(
    private val delegate: Runnable,
    private val priorityInfo: PriorityInfo,
  ) : Runnable, Comparable<ComparableRunnable> {
    override fun run() = delegate.run()
    override fun compareTo(other: ComparableRunnable): Int =
      priorityInfo.compareTo(other.priorityInfo)
  }

  internal class PriorityInfo(
    private val priority: Priority, private val order: Int,
  ) : CoroutineContext.Element, Comparable<PriorityInfo> {
    companion object Key : CoroutineContext.Key<PriorityInfo>
    override val key = Key
    override fun compareTo(other: PriorityInfo): Int {
      val result = priority.ordinal - other.priority.ordinal
      return if (result != 0) result else order - other.order
    }
  }

  private val CoroutineContext.priorityInfo: PriorityInfo
    get() = this[PriorityInfo.Key] ?: throw RuntimeException("Missing priority info")
}

