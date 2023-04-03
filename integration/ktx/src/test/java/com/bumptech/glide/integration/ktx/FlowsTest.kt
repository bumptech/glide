@file:OptIn(InternalGlideApi::class, ExperimentGlideFlows::class, ExperimentalCoroutinesApi::class)

package com.bumptech.glide.integration.ktx

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.Key
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.engine.cache.MemoryCache
import com.bumptech.glide.load.engine.executor.GlideExecutor
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.common.truth.Correspondence
import com.google.common.truth.IterableSubject
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.lang.RuntimeException
import java.util.concurrent.atomic.AtomicReference
import kotlin.reflect.KClass
import kotlin.test.assertFailsWith
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

// newFile throws IOException, which triggers this warning even though there's no reasonable
// alternative :/.
@Suppress("BlockingMethodInNonBlockingContext")
@RunWith(AndroidJUnit4::class)
class FlowsTest {
  private val context = ApplicationProvider.getApplicationContext<Context>()
  @get:Rule val temporaryFolder = TemporaryFolder()

  @After
  fun tearDown() {
    Glide.tearDown()
  }

  @Test
  fun flow_withPlaceholderDrawable_emitsPlaceholderDrawableFirst() = runTest {
    val placeholderDrawable = ColorDrawable(Color.RED)
    val first =
      Glide.with(context)
        .load(temporaryFolder.newFile())
        .placeholder(placeholderDrawable)
        .flow(100)
        .first()

    assertThat(first).isEqualTo(Placeholder<Drawable>(Status.RUNNING, placeholderDrawable))
  }

  @Test
  fun flow_withNoPlaceholderDrawable_emitsNullPlaceholderFirst() = runTest {
    val first = Glide.with(context).load(temporaryFolder.newFile()).flow(100).first()

    assertThat(first).isEqualTo(Placeholder<Drawable>(Status.RUNNING, placeholder = null))
  }

  @Test
  fun flow_failingNonNullModel_emitsRunningThenFailed() = runTest {
    val missingResourceId = 123
    val results = Glide.with(context).load(missingResourceId).flow(100).firstLoad().toList()

    assertThat(results)
      .containsExactly(
        Placeholder<Drawable>(Status.RUNNING, placeholder = null),
        Placeholder<Drawable>(Status.FAILED, placeholder = null)
      )
      .inOrder()
  }

  @Test
  fun flow_failingNonNullModel_whenRestartedAfterFailure_emitsSecondLoad() = runTest {
    val requestManager = Glide.with(context)
    val missingResourceId = 123

    val flow =
      requestManager
        .load(missingResourceId)
        .listener(onFailure(atMostOnce { restartAllRequestsOnNewThread(requestManager) }))
        .flow(100)

    assertThat(flow.take(4).toList())
      .comparingStatus()
      .containsExactly(Status.RUNNING, Status.FAILED, Status.RUNNING, Status.FAILED)
  }

  @Test
  fun flow_successfulNonNullModel_emitsRunningThenSuccess() = runTest {
    val results = Glide.with(context).load(newImageFile()).flow(100).firstLoad().toList()

    assertThat(results)
      .compareStatusAndType()
      .containsExactly(placeholder(Status.RUNNING), resource(Status.SUCCEEDED))
      .inOrder()
  }

  @Test
  fun flow_withNullModel_andFallbackDrawable_emitsFailureWithFallbackDrawable() = runTest {
    val fallbackDrawable = ColorDrawable(Color.BLUE)
    val first = Glide.with(context).load(null as Uri?).fallback(fallbackDrawable).flow(100).first()
    assertThat(first).isEqualTo(Placeholder<Drawable>(Status.FAILED, fallbackDrawable))
  }

  @Test
  fun flow_successfulNonNullModel_whenRestartedAfterSuccess_emitsSecondLoad() = runTest {
    val requestManager = Glide.with(context)

    val flow =
      requestManager
        .load(newImageFile())
        .listener(onSuccess(atMostOnce { restartAllRequestsOnNewThread(requestManager) }))
        .flow(100)

    assertThat(flow.take(4).toList())
      .comparingStatus()
      .containsExactly(
        Status.RUNNING,
        Status.SUCCEEDED,
        Status.CLEARED, // See the TODO in RequestTracker#pauseAllRequests
        Status.SUCCEEDED, // The request completes from in memory, so it never goes to RUNNING
      )
  }

  @Test
  fun flow_successfulNonNullModel_oneSuccessfulThumbnail_emitsThumbnailAndMainResources() =
    runTest {
      makeGlideSingleThreadedToOrderThumbnailRequests()

      val output =
        Glide.with(context)
          .load(newImageFile())
          .thumbnail(Glide.with(context).load(newImageFile()))
          .flow(100)
          .firstLoad()
          .toList()
      assertThat(output)
        .compareStatusAndType()
        .containsExactly(
          placeholder(Status.RUNNING),
          resource(Status.RUNNING),
          resource(Status.SUCCEEDED),
        )
    }

  @Test
  fun flow_successfulNonNullModel_oneFailingThumbnail_emitMainResourceOnly() = runTest {
    makeGlideSingleThreadedToOrderThumbnailRequests()

    val missingResourceId = 123
    val output =
      Glide.with(context)
        .load(newImageFile())
        .thumbnail(Glide.with(context).load(missingResourceId))
        .flow(100)
        .firstLoad()
        .toList()
    assertThat(output)
      .compareStatusAndType()
      .containsExactly(
        placeholder(Status.RUNNING),
        resource(Status.SUCCEEDED),
      )
  }

  @Test
  fun flow_failingNonNullModel_successfulThumbnail_emitsThumbnailWithFailedStatus() = runTest {
    makeGlideSingleThreadedToOrderThumbnailRequests()

    val missingResourceId = 123
    val output =
      Glide.with(context)
        .load(missingResourceId)
        .thumbnail(Glide.with(context).load(newImageFile()))
        .flow(100)
        .firstLoad()
        .toList()
    assertThat(output)
      .compareStatusAndType()
      .containsExactly(
        placeholder(Status.RUNNING),
        resource(Status.RUNNING),
        resource(Status.FAILED),
      )
  }

  @Test
  fun flow_failingNonNullModel_failingNonNullThumbnail_emitsRunningThenFailed() = runTest {
    makeGlideSingleThreadedToOrderThumbnailRequests()

    val missingResourceId = 123
    val output =
      Glide.with(context)
        .load(missingResourceId)
        .thumbnail(Glide.with(context).load(missingResourceId))
        .flow(100)
        .firstLoad()
        .toList()

    assertThat(output)
      .compareStatusAndType()
      .containsExactly(placeholder(Status.RUNNING), placeholder(Status.FAILED))
  }

  @Test
  fun flow_failingNonNullModel_succeedingNonNullError_emitsRunningThenSuccess() = runTest {
    makeGlideSingleThreadedToOrderThumbnailRequests()

    val missingResourceId = 123
    val output =
      Glide.with(context)
        .load(missingResourceId)
        .error(Glide.with(context).load(newImageFile()))
        .flow(100)
        .firstLoad()
        .toList()

    assertThat(output)
      .compareStatusAndType()
      .containsExactly(
        placeholder(Status.RUNNING),
        // TODO(judds): This is probably another case where resource(Status.FAILURE) is more
        //  appropriate. TO do so, we'd need to avoid passing TargetListener in RequestBuilder into
        // thumbnails (and probably error request builders). That's a larger change
        resource(Status.SUCCEEDED)
      )
  }

  @Test
  fun flow_failingNonNullModel_failingNonNullError_emitsRunningThenFailure() = runTest {
    makeGlideSingleThreadedToOrderThumbnailRequests()

    val missingResourceId = 123
    val output =
      Glide.with(context)
        .load(missingResourceId)
        .error(Glide.with(context).load(missingResourceId))
        .flow(100)
        .firstLoad()
        .toList()

    assertThat(output)
      .compareStatusAndType()
      .containsExactly(placeholder(Status.RUNNING), placeholder(Status.FAILED))
  }

  @Test
  fun flow_failingNonNullModel_failingNonNullError_succeedingErrorThumbnail_emitsRunningThenRunningWithResourceThenFailureWithResource() =
    runTest {
      makeGlideSingleThreadedToOrderThumbnailRequests()

      val missingResourceId = 123
      val output =
        Glide.with(context)
          .load(missingResourceId)
          .error(
            Glide.with(context)
              .load(missingResourceId)
              .thumbnail(Glide.with(context).load(newImageFile()))
          )
          .flow(100)
          .firstLoad()
          .toList()

      assertThat(output)
        .compareStatusAndType()
        .containsExactly(
          placeholder(Status.RUNNING),
          resource(Status.RUNNING),
          resource(Status.FAILED),
        )
    }

  @Test
  fun flow_onClose_clearsTarget() = runTest {
    val inCache = AtomicReference<com.bumptech.glide.load.engine.Resource<*>?>()
    Glide.init(
      context,
      GlideBuilder()
        .setMemoryCache(
          object : MemoryCache {
            override fun getCurrentSize(): Long = 0
            override fun getMaxSize(): Long = 0
            override fun setSizeMultiplier(multiplier: Float) {}
            override fun remove(key: Key): com.bumptech.glide.load.engine.Resource<*>? {
              return null
            }
            override fun setResourceRemovedListener(
              listener: MemoryCache.ResourceRemovedListener
            ) {}
            override fun clearMemory() {}
            override fun trimMemory(level: Int) {}

            override fun put(
              key: Key,
              resource: com.bumptech.glide.load.engine.Resource<*>?,
            ): com.bumptech.glide.load.engine.Resource<*>? {
              inCache.set(resource)
              return null
            }
          }
        )
    )
    val data = Glide.with(context).load(newImageFile()).flow(100, 100).firstLoad().toList()
    assertThat(data).isNotEmpty()
    assertThat(inCache.get()).isNotNull()
  }

  @Test
  fun flow_withOverrideSize_andProvidedSize_prefersOverrideSize() = runTest {
    val requestedSizeReference = registerSizeCapturingFakeModelLoader()

    Glide.with(context).load(FakeModel()).override(50, 60).flow(200, 100).firstLoad().toList()

    assertThat(requestedSizeReference.get()).isEqualTo(Size(50, 60))
  }

  @Test
  fun flow_withOnlyProvidedSize_usesProvidedSize() = runTest {
    val requestedSizeReference = registerSizeCapturingFakeModelLoader()

    Glide.with(context).load(FakeModel()).flow(100, 200).firstLoad().toList()

    assertThat(requestedSizeReference.get()).isEqualTo(Size(100, 200))
  }

  @Test
  fun flow_withOnlySingleDimension_usesProvidedSize() = runTest {
    val requestedSizeReference = registerSizeCapturingFakeModelLoader()

    Glide.with(context).load(FakeModel()).flow(150).firstLoad().toList()

    assertThat(requestedSizeReference.get()).isEqualTo(Size(150, 150))
  }

  @Test
  fun flow_withSizeOriginal_usesSizeOriginal() = runTest {
    val requestedSizeReference = registerSizeCapturingFakeModelLoader()

    Glide.with(context)
      .load(FakeModel())
      .flow(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
      .firstLoad()
      .toList()

    assertThat(requestedSizeReference.get())
      .isEqualTo(Size(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL))
  }

  @Test
  fun flow_withSizeOriginalOverride_concreteProvidedSize_usesSizeOriginal() = runTest {
    val requestedSizeReference = registerSizeCapturingFakeModelLoader()

    Glide.with(context)
      .load(FakeModel())
      .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
      .flow(200, 300)
      .firstLoad()
      .toList()

    assertThat(requestedSizeReference.get())
      .isEqualTo(Size(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL))
  }

  @Test
  fun flow_withConcreteOverride_sizeOriginalProvidedSize_usesConcreteSize() = runTest {
    val requestedSizeReference = registerSizeCapturingFakeModelLoader()

    Glide.with(context)
      .load(FakeModel())
      .override(200, 300)
      .flow(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
      .firstLoad()
      .toList()

    assertThat(requestedSizeReference.get()).isEqualTo(Size(200, 300))
  }

  @Test
  fun flow_withThumbnailWithOverrideSize_usesOverrideSizeForThumbnail() = runTest {
    makeGlideSingleThreadedToOrderThumbnailRequests()

    val requestedSizeReference = registerSizeCapturingFakeModelLoader()

    Glide.with(context)
      .load(newImageFile())
      .thumbnail(Glide.with(context).load(FakeModel()).override(100, 200))
      .flow(Target.SIZE_ORIGINAL)
      .firstLoad()
      .toList()

    assertThat(requestedSizeReference.get()).isEqualTo(Size(100, 200))
  }

  @Test
  fun flow_withThumbnailWithoutOverrideSize_usesProvidedSizeForThumbnail() = runTest {
    makeGlideSingleThreadedToOrderThumbnailRequests()
    val requestedSizeReference = registerSizeCapturingFakeModelLoader()

    Glide.with(context)
      .load(newImageFile())
      .thumbnail(Glide.with(context).load(FakeModel()))
      .flow(300, 400)
      .firstLoad()
      .toList()

    assertThat(requestedSizeReference.get()).isEqualTo(Size(300, 400))
  }

  @Test
  fun flow_withInvalidProvidedWith_throws() = runTest {
    val missingResourceId = 123
    val requestBuilder = Glide.with(context).load(missingResourceId)

    assertFailsWith<IllegalArgumentException> { requestBuilder.flow(-100, 100) }
  }

  @Test
  fun flow_withInvalidProvidedHeight_throws() {
    val missingResourceId = 123
    val requestBuilder = Glide.with(context).load(missingResourceId)

    assertFailsWith<IllegalArgumentException> { requestBuilder.flow(100, -100) }
  }

  @Test
  fun flow_withAsyncSize_immediatelyEmitsPlaceholder() = runTest {
    val placeholder = ColorDrawable(Color.GREEN)

    val missingResourceId = 123
    val result =
      Glide.with(context)
        .load(missingResourceId)
        .placeholder(placeholder)
        .flow(delayForever)
        .first()

    assertThat(result).isEqualTo(Placeholder<Drawable>(Status.RUNNING, placeholder))
  }

  @Test
  fun flow_withAsyncSizeThatNeverCompletes_andOverrideSize_finishesSuccessfully() = runTest {
    val result =
      Glide.with(context)
        .load(newImageFile())
        .override(100, 100)
        .flow(delayForever)
        .firstLoad()
        .toList()

    assertThat(result).comparingStatus().containsExactly(Status.RUNNING, Status.SUCCEEDED).inOrder()
  }

  @Test
  fun flow_withAsyncSize_andOverrideSize_usesOverrideSize() = runTest {
    val requestedSizeReference = registerSizeCapturingFakeModelLoader()

    Glide.with(context)
      .load(FakeModel())
      .override(200, 100)
      .flow { Size(1, 2) }
      .firstLoad()
      .toList()

    assertThat(requestedSizeReference.get()).isEqualTo(Size(200, 100))
  }

  @Test
  fun flow_withAsyncSize_thumbnailWithConcreteSize_startsThumbnailWithoutWaitingForSize() =
    runTest {
      val result =
        Glide.with(context)
          .load(newImageFile())
          .thumbnail(Glide.with(context).load(newImageFile()).override(25, 50))
          .flow(delayForever)
          .take(2)
          .toList()

      assertThat(result)
        .compareStatusAndType()
        .containsExactly(placeholder(Status.RUNNING), resource(Status.RUNNING))
        .inOrder()
    }

  @Test
  fun flow_withAsyncSize_concreteSizeForThumbnail_startsMainRequestWhenAsyncSizeIsAvailable() =
    runTest {
      val waitForThumbnailToFinishChannel = Channel<Boolean>()
      val waitForThumbnailToFinishSize: suspend () -> Size = {
        waitForThumbnailToFinishChannel.receive()
        Size(100, 200)
      }

      val result =
        Glide.with(context)
          .load(newImageFile())
          .thumbnail(
            Glide.with(context)
              .load(newImageFile())
              .override(75, 50)
              .listener(onSuccess { launch { waitForThumbnailToFinishChannel.send(true) } })
          )
          .flow(waitForThumbnailToFinishSize)
          .firstLoad()
          .toList()

      assertThat(result)
        .compareStatusAndType()
        .containsExactly(
          placeholder(Status.RUNNING),
          resource(Status.RUNNING),
          resource(Status.SUCCEEDED),
        )
    }

  // TODO(judds): Consider adding a test for invalid async sizes. It doesn't seem like Glide
  // asserts on this in the existing framework, so it's probably not super important to do for
  // flows, but it might be nice.

  @Test
  fun flow_withNoProvidedSize_overrideSizePresent_usesOverrideSize() = runTest {
    val requestedSizeReference = registerSizeCapturingFakeModelLoader()

    Glide.with(context).load(FakeModel()).override(4, 5).flow().firstLoad().toList()

    assertThat(requestedSizeReference.get()).isEqualTo(Size(4, 5))
  }

  @Test
  fun flow_withNoProvidedSize_overrideSizeMissing_throws() = runTest {
    val requestBuilder = Glide.with(context).load(FakeModel())

    assertFailsWith<IllegalArgumentException> { requestBuilder.flow() }
  }

  private val delayForever: suspend () -> Size = {
    delay(kotlin.time.Duration.INFINITE)
    throw RuntimeException()
  }

  private fun registerSizeCapturingFakeModelLoader(): AtomicReference<Size> {
    val result = AtomicReference<Size>()
    Glide.get(context)
      .registry
      .append(
        FakeModel::class.java,
        File::class.java,
        SizeObservingFakeModelLoader.Factory(newImageFile(), result)
      )
    return result
  }

  // Avoid race conditions where the main request finishes first by making sure they execute
  // sequentially using a single threaded executor.
  private fun makeGlideSingleThreadedToOrderThumbnailRequests() {
    Glide.init(
      context,
      GlideBuilder().setSourceExecutor(GlideExecutor.newSourceBuilder().setThreadCount(1).build()),
    )
  }

  // Robolectric will produce a Bitmap from any File, but this is relatively easy and will work on
  // emulators as well as robolectric.
  private fun newImageFile(): File {
    val file = temporaryFolder.newFile()
    val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(Color.GREEN)
    file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 75, it) }
    return file
  }

  class FakeModel

  class SizeObservingFakeModelLoader(
    private val fileLoader: ModelLoader<File, File>,
    private val fakeResult: File,
    private val sizeReference: AtomicReference<Size>,
  ) : ModelLoader<FakeModel, File> {

    override fun buildLoadData(
      model: FakeModel,
      width: Int,
      height: Int,
      options: Options,
    ): ModelLoader.LoadData<File>? {
      sizeReference.set(Size(width, height))
      return fileLoader.buildLoadData(fakeResult, width, height, options)
    }

    override fun handles(model: FakeModel): Boolean = true

    class Factory(private val fakeResult: File, private val sizeReference: AtomicReference<Size>) :
      ModelLoaderFactory<FakeModel, File> {
      override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<FakeModel, File> {
        return SizeObservingFakeModelLoader(
          multiFactory.build(File::class.java, File::class.java),
          fakeResult,
          sizeReference
        )
      }
      override fun teardown() {}
    }
  }
}

private fun atMostOnce(function: () -> Unit): () -> Unit {
  var isCalled = false
  return {
    if (!isCalled) {
      isCalled = true
      function()
    }
  }
}

private fun <ResourceT> onSuccess(onSuccess: () -> Unit) =
  simpleRequestListener<ResourceT>(onSuccess) {}

private fun <ResourceT> onFailure(onFailure: () -> Unit) =
  simpleRequestListener<ResourceT>({}, onFailure)

private fun <ResourceT> simpleRequestListener(
  onSuccess: () -> Unit,
  onFailure: () -> Unit
): RequestListener<ResourceT> =
  object : RequestListener<ResourceT> {
    override fun onResourceReady(
      resource: ResourceT?,
      model: Any?,
      target: Target<ResourceT>?,
      dataSource: DataSource?,
      isFirstResource: Boolean,
    ): Boolean {
      onSuccess()
      return false
    }

    override fun onLoadFailed(
      e: GlideException?,
      model: Any?,
      target: Target<ResourceT>?,
      isFirstResource: Boolean,
    ): Boolean {
      onFailure()
      return false
    }
  }

// TODO(judds): This function may be useful in production code as well, consider exposing it.
private fun <ResourceT> Flow<GlideFlowInstant<ResourceT>>.firstLoad():
  Flow<GlideFlowInstant<ResourceT>> {
  val originalFlow = this
  return flow {
    var completion: GlideFlowInstant<ResourceT>? = null
    originalFlow
      .takeWhile {
        if (it.status != Status.SUCCEEDED && it.status != Status.FAILED) {
          true
        } else {
          completion = it
          false
        }
      }
      .collect { emit(it) }

    emit(completion!!)
  }
}

@OptIn(DelicateCoroutinesApi::class)
private fun restartAllRequestsOnNewThread(requestManager: RequestManager) =
  newSingleThreadContext("restart").use {
    it.executor.execute {
      requestManager.pauseAllRequests()
      requestManager.resumeRequests()
    }
  }

private fun placeholder(status: Status) = StatusAndType(status, Placeholder::class)

private fun resource(status: Status) = StatusAndType(status, Resource::class)

private data class StatusAndType(
  val status: Status,
  val type: KClass<out GlideFlowInstant<*>>,
)

private fun IterableSubject.compareStatusAndType() = comparingElementsUsing(statusAndType())

private fun statusAndType(): Correspondence<GlideFlowInstant<*>, StatusAndType> =
  ktCorrespondenceFrom("statusAndType") { actual, expected -> actual?.statusAndType() == expected }

private fun GlideFlowInstant<*>.statusAndType() =
  StatusAndType(
    status,
    when (this) {
      is Placeholder<*> -> Placeholder::class
      is Resource<*> -> Resource::class
    }
  )

private fun IterableSubject.comparingStatus() = comparingElementsUsing(status())

private fun status(): Correspondence<GlideFlowInstant<*>, Status> =
  ktCorrespondenceFrom("status") { actual, expected -> actual?.status == expected }

private fun <A, E> ktCorrespondenceFrom(
  description: String,
  predicate: Correspondence.BinaryPredicate<A, E>
) = Correspondence.from(predicate, description)
