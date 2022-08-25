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
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.engine.cache.MemoryCache
import com.bumptech.glide.load.engine.executor.GlideExecutor
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.common.truth.Correspondence
import com.google.common.truth.IterableSubject
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.util.concurrent.atomic.AtomicReference
import kotlin.reflect.KClass
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.toList
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
@OptIn(ExperimentalCoroutinesApi::class, ExperimentGlideFlows::class)
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
    val first =
      Glide.with(context)
        .load(temporaryFolder.newFile())
        .flow(100)
        .first()

    assertThat(first).isEqualTo(Placeholder<Drawable>(Status.RUNNING, placeholder = null))
  }

  @Test
  fun flow_failingNonNullModel_emitsRunningThenFailed() = runTest {
    val missingResourceId = 123
    val results =
      Glide.with(context)
        .load(missingResourceId)
        .flow(100)
        .firstLoad()
        .toList()

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
        .listener(
          onFailure(
            atMostOnce {
              restartAllRequestsOnNewThread(requestManager)
            }
          )
        )
        .flow(100)

    assertThat(flow.take(4).toList())
      .comparingStatus()
      .containsExactly(
        Status.RUNNING,
        Status.FAILED,
        Status.RUNNING,
        Status.FAILED
      )
  }

  @Test
  fun flow_successfulNonNullModel_emitsRunningThenSuccess() = runTest {
    val results =
      Glide.with(context)
        .load(newImageFile())
        .flow(100)
        .firstLoad()
        .toList()

    assertThat(results)
      .compareStatusAndType()
      .containsExactly(placeholder(Status.RUNNING), resource(Status.SUCCEEDED))
      .inOrder()
  }

  @Test
  fun flow_withNullModel_andFallbackDrawable_emitsFailureWithFallbackDrawable() = runTest {
    val fallbackDrawable = ColorDrawable(Color.BLUE)
    val first =
      Glide.with(context)
        .load(null as Uri?)
        .fallback(fallbackDrawable)
        .flow(100)
        .first()
    assertThat(first).isEqualTo(Placeholder<Drawable>(Status.FAILED, fallbackDrawable))
  }

  @Test
  fun flow_successfulNonNullModel_whenRestartedAfterSuccess_emitsSecondLoad() = runTest {
    val requestManager = Glide.with(context)

    val flow =
      requestManager
        .load(newImageFile())
        .listener(
          onSuccess(
            atMostOnce {
              restartAllRequestsOnNewThread(requestManager)
            }
          )
        )
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
  fun flow_successfulNonNullModel_oneSuccessfulThumbnail_emitsThumbnailAndMainResources() = runTest {
    makeGlideSingleThreadedToOrderThumbnailRequests()

    val output =
      Glide.with(context)
        .load(newImageFile())
        .thumbnail(
          Glide.with(context).load(newImageFile())
        )
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
        .thumbnail(
          Glide.with(context).load(missingResourceId)
        )
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
        .thumbnail(
          Glide.with(context).load(newImageFile())
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
  fun flow_failingNonNullModel_failingNonNullThumbnail_emitsRunningThenFailed() = runTest {
    makeGlideSingleThreadedToOrderThumbnailRequests()

    val missingResourceId = 123
    val output =
      Glide.with(context)
        .load(missingResourceId)
        .thumbnail(
          Glide.with(context).load(missingResourceId)
        )
        .flow(100)
        .firstLoad()
        .toList()

    assertThat(output)
      .compareStatusAndType()
      .containsExactly(
        placeholder(Status.RUNNING),
        placeholder(Status.FAILED)
      )
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
      .containsExactly(
        placeholder(Status.RUNNING),
        placeholder(Status.FAILED)
      )
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
            Glide.with(context).load(missingResourceId)
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
    Glide.init(context, GlideBuilder().setMemoryCache(object : MemoryCache {
      override fun getCurrentSize(): Long = 0
      override fun getMaxSize(): Long = 0
      override fun setSizeMultiplier(multiplier: Float) {}
      override fun remove(key: Key): com.bumptech.glide.load.engine.Resource<*>? {return null}
      override fun setResourceRemovedListener(listener: MemoryCache.ResourceRemovedListener) {}
      override fun clearMemory() {}
      override fun trimMemory(level: Int) {}

      override fun put(
        key: Key,
        resource: com.bumptech.glide.load.engine.Resource<*>?,
      ): com.bumptech.glide.load.engine.Resource<*>? {
        inCache.set(resource)
        return null
      }
    }))
    val data = Glide.with(context)
      .load(newImageFile())
      .flow(100, 100)
      .firstLoad()
      .toList()
    assertThat(data).isNotEmpty()
    assertThat(inCache.get()).isNotNull()
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
    file.outputStream().use {
      bitmap.compress(Bitmap.CompressFormat.JPEG, 75, it)
    }
    return file
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

private fun <ResourceT> simpleRequestListener(onSuccess: () -> Unit, onFailure: () -> Unit) :
  RequestListener<ResourceT> =
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
@OptIn(ExperimentGlideFlows::class)
private fun <ResourceT> Flow<GlideFlowInstant<ResourceT>>.firstLoad():
  Flow<GlideFlowInstant<ResourceT>>
{
  val originalFlow = this
  return flow {
    var completion: GlideFlowInstant<ResourceT>? = null
    originalFlow.takeWhile {
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

@OptIn(ExperimentGlideFlows::class)
private fun placeholder(status: Status) = StatusAndType(status, Placeholder::class)
@OptIn(ExperimentGlideFlows::class)
private fun resource(status: Status) = StatusAndType(status, Resource::class)

@OptIn(ExperimentGlideFlows::class)
private data class StatusAndType(
  val status: Status, val type: KClass<out GlideFlowInstant<*>>,
)

@OptIn(ExperimentGlideFlows::class)
private fun IterableSubject.compareStatusAndType()
  = comparingElementsUsing(statusAndType())

@OptIn(ExperimentGlideFlows::class)
private fun statusAndType() : Correspondence<GlideFlowInstant<*>, StatusAndType> =
  ktCorrespondenceFrom("statusAndType") { actual, expected -> actual?.statusAndType() == expected }

@OptIn(ExperimentGlideFlows::class)
private fun GlideFlowInstant<*>.statusAndType() =
  StatusAndType(
    status,
    when(this) {
      is Placeholder<*> -> Placeholder::class
      is Resource<*> -> Resource::class
    }
  )

@OptIn(ExperimentGlideFlows::class)
private fun IterableSubject.comparingStatus() =
  comparingElementsUsing(status())

@OptIn(ExperimentGlideFlows::class)
private fun status() : Correspondence<GlideFlowInstant<*>, Status> =
  ktCorrespondenceFrom("status") { actual, expected -> actual?.status == expected }

private fun <A, E> ktCorrespondenceFrom(
  description: String, predicate: Correspondence.BinaryPredicate<A, E>
) = Correspondence.from(predicate, description)
