package com.bumptech.glide.integration.ktx

import android.graphics.drawable.Drawable
import androidx.annotation.GuardedBy
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.intoDirect
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.Request
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.SizeReadyCallback
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import com.bumptech.glide.requestManager
import com.bumptech.glide.util.Util
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import java.lang.UnsupportedOperationException

@RequiresOptIn(
  level = RequiresOptIn.Level.ERROR,
  message =
    "Glide's flow integration is very experimental and subject to breaking API or behavior changes"
)
@Retention(AnnotationRetention.BINARY)
@kotlin.annotation.Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
public annotation class ExperimentGlideFlows

/**
 * The current status of a flow
 *
 * There is no well established graph that defines the valid Status transitions. Depending on
 * various factors like the parameters of the request, whether or not the resource is in the memory
 * cache, or even various calls to Glide's APIs, these may be emitted in different orders. As an
 * example, [RUNNING] is skipped if a request can be immediately completed from the memory cache.
 *
 * See [flow] for more details.
 */
@ExperimentGlideFlows
public enum class Status {
  /** The load is not started or has been cleared. */
  CLEARED,
  /** At least the primary load is still in progress. */
  RUNNING,
  /**
   * The primary load or the error load ([RequestBuilder.error]) associated with the primary have
   * finished successfully.
   */
  SUCCEEDED,
  /** The primary load has failed. One or more thumbnails may have succeeded. */
  FAILED,
}

/**
 * Identical to [flow] with [Target.SIZE_ORIGINAL] as the dimensions
 *
 * This isn't generally a good idea, [Target.SIZE_ORIGINAL] is often much larger than you need.
 * Using it unnecessarily will waste memory and cache space. It will also slow down future loads
 * from the disk cache.
 *
 * Use this method only if you you expect the request and all of the subrequests (
 * [RequestBuilder.override] and [RequestBuilder.error] to have specific sizes set). Validation is
 * only performed on the top level request because we cannot reliably verify all possible
 * subrequests.
 */
@ExperimentGlideFlows
public fun <ResourceT : Any> RequestBuilder<ResourceT>.flow(): Flow<GlideFlowInstant<ResourceT>> {
  require(isValidOverride) {
    "At least your primary request is missing override dimensions. If you want to use" +
      " Target.SIZE_ORIGINAL, do so explicitly"
  }
  return flow(Target.SIZE_ORIGINAL)
}

/** Identical to `flow(dimension, dimension)` */
@ExperimentGlideFlows
public fun <ResourceT : Any> RequestBuilder<ResourceT>.flow(
  dimension: Int
): Flow<GlideFlowInstant<ResourceT>> = flow(dimension, dimension)

/**
 * Identical to [flow] with dimensions, except that the size is resolved asynchronously using
 * [size].
 *
 * If an override size has been set using [RequestBuilder.override], that size will be used instead
 * and [size] may never be called.
 *
 * [Placeholder] values may be emitted prior to [size] returning. Similarly if
 * [RequestBuilder.thumbnail] requests are present and have overridden sizes, [Resource] values for
 * those thumbnails may also be emitted. [size] will only be used for requests where no
 * [RequestBuilder.override] size is available.
 *
 * If [size] never has [AsyncGlideSize.setSize] called, this flow may never return values other than
 * placeholders.
 *
 * This function is internal only, intended primarily for Compose. The Target API provides similar
 * functionality for traditional Views. We could consider expanding the visibility if there are use
 * cases for asynchronous size resolution outside of Glide's Compose integration.
 */
@OptIn(InternalGlideApi::class)
@ExperimentGlideFlows
public fun <ResourceT : Any> RequestBuilder<ResourceT>.flow(
  size: AsyncGlideSize,
): Flow<GlideFlowInstant<ResourceT>> = flowResolvable(size)

/**
 * Convert a load in Glide into a flow that emits placeholders and resources in the order they'd be
 * seen by a [Target].
 *
 * Just like a [Target] there is no well defined end to a Glide request. Barring cancellation, the
 * flow should eventually reach [Status.SUCCEEDED] or [Status.FAILED] at least once. However
 * connectivity changes, calls to [com.bumptech.glide.RequestManager.pauseAllRequests] or
 * [com.bumptech.glide.RequestManager.resumeRequests], or the lifecycle associated with this request
 * may cause the request to be started multiple times. As long as the flow is active, callers will
 * receive emissions from every run.
 *
 * This flow will clear the associated Glide request when it's cancelled. This means that callers
 * must keep the flow active while any resource emitted by the flow is still in use. For UI
 * contexts, collecting the flow in the appropriate fragment or view model coroutine context is
 * sufficient as long as you avoid truncating methods like [kotlinx.coroutines.flow.take],
 * [kotlinx.coroutines.flow.takeWhile], etc. If you do use these methods, you must be sure that
 * you're no longer using or displaying the associated resource once the flow is no longer active
 * (ie [kotlinx.coroutines.flow.collect] finishes). One way to do this would be to mimic the UI by
 * creating and keeping active a coroutine context that collects from the flow while the resource is
 * in use. If this restriction is limiting for you, please file an issue on Github so we can think
 * of alternative options.
 *
 * If there have been any previous calls to this [RequestBuilder]'s
 * [com.bumptech.glide.request.RequestOptions.override] method, the size specified in that method
 * will be used instead of the size provided here. This includes calls where override sizes may have
 * been copied from other option sets via [RequestBuilder.apply].
 */
@ExperimentGlideFlows
@OptIn(InternalGlideApi::class)
public fun <ResourceT : Any> RequestBuilder<ResourceT>.flow(
  width: Int,
  height: Int
): Flow<GlideFlowInstant<ResourceT>> {
  require(Util.isValidDimensions(width, height))
  return flow(Size(width = width, height = height))
}

// We're not asserting on size here because it might come from RequestBuilder.override. Assertions
// for provided sizes belong in those methods, assertions for overrides belong in the override
// method.
@InternalGlideApi
@ExperimentGlideFlows
private fun <ResourceT : Any> RequestBuilder<ResourceT>.flow(
  size: Size
): Flow<GlideFlowInstant<ResourceT>> = flowResolvable(ImmediateGlideSize(size))

@OptIn(ExperimentGlideFlows::class)
@InternalGlideApi
public fun <ResourceT : Any> RequestBuilder<ResourceT>.flowResolvable(
  size: ResolvableGlideSize
): Flow<GlideFlowInstant<ResourceT>> = flow(size)

/**
 * A [Status] and value pair, where the value is either a [Placeholder] or a [Resource] depending on
 * how far the Glide load has progressed and/or how successful it's been.
 */
@ExperimentGlideFlows
public sealed class GlideFlowInstant<ResourceT> {
  public abstract val status: Status
}

/**
 * Wraps a [Status] and a placeholder [Drawable] (from [RequestBuilder.placeholder],
 * [RequestBuilder.fallback], [RequestBuilder.error] etc).
 */
@ExperimentGlideFlows
public data class Placeholder<ResourceT>(
  public override val status: Status,
  public val placeholder: Drawable?,
) : GlideFlowInstant<ResourceT>() {
  init {
    require(
      when (status) {
        Status.SUCCEEDED -> false
        Status.CLEARED -> true
        // Placeholder will be present prior to the first thumbnail succeeding
        Status.RUNNING -> true
        Status.FAILED -> true
      }
    )
  }
}

/**
 * Wraps a [Status] and a resource loaded from the primary request, a [RequestBuilder.thumbnail]
 * request, or a [RequestBuilder.error] request.
 *
 * **Status.FAILED** is a perfectly valid status with this class. If the primary request fails, but
 * at least one thumbnail succeeds, the flow will emit `Resource(FAILED, resource)` to indicate both
 * that we have some value but also that the primary request has failed.
 */
@ExperimentGlideFlows
public data class Resource<ResourceT>(
  public override val status: Status,
  public val resource: ResourceT,
  public val isFirstResource: Boolean,
  public val dataSource: DataSource,
) : GlideFlowInstant<ResourceT>() {
  init {
    require(
      when (status) {
        Status.SUCCEEDED -> true
        // A load with thumbnail(s) where the thumbnail(s) have finished but not the main request
        Status.RUNNING -> true
        // The primary request of the load failed, but at least one thumbnail was successful.
        Status.FAILED -> true
        // Once the load is cleared, it can only show a placeholder
        Status.CLEARED -> false
      }
    )
  }

  public fun asFailure():Resource<ResourceT> =
    Resource(Status.FAILED, resource, isFirstResource, dataSource)
}

@InternalGlideApi
@ExperimentGlideFlows
private fun <ResourceT : Any> RequestBuilder<ResourceT>.flow(
  size: ResolvableGlideSize,
): Flow<GlideFlowInstant<ResourceT>> {
  val requestBuilder = this
  val requestManager = requestBuilder.requestManager()
  return callbackFlow {
    val target = FlowTarget(this, size)
    requestBuilder.intoDirect(target)
    awaitClose { requestManager.clear(target) }
  }
}

/**
 * Observes a glide request using [Target] and [RequestListener] and tries to emit something
 * resembling a coherent set of placeholders and resources for it.
 *
 * Threading in this class is a bit complicated. As a general rule, the callback methods are ordered
 * by callers. So we have to handle being called from multiple threads, but we don't need to try to
 * handle callbacks being called in parallel.
 *
 * The primary area of concern around thread is that [resolvedSize] and [sizeReadyCallbacks] must be
 * updated atomically, but can be modified on different threads.
 *
 * [currentRequest] would normally be a concern because [Target]s can be cancelled on threads other
 * than where they were started. However in our case, [currentRequest] is set once when our request
 * is started (by us) and is only cancelled when the request finishes. So we just have to avoid NPEs
 * and make sure the state is reasonably up to date.
 *
 * [lastResource] is an unfortunate hack that tries to make sure that we emit [Status.FAILED] if a
 * thumbnail request succeeds, but then the primary request fails. In that case, we'd normally
 * already have emitted [Resource] with [Status.RUNNING] and the thumbnail value and then we'd emit
 * nothing else. That's not very satisfying for callers who expect some resolution. So instead we
 * track the last resource produced by thumbnails and emit that along with [Status.FAILED] when we
 * see that the primary request has failed. As a result we're not concerned with ordering with
 * regards to [lastResource], but it is possible the callbacks will be called on different threads,
 * so the value may be updated from different threads even if it's not concurrent.
 */
@ExperimentGlideFlows
@InternalGlideApi
private class FlowTarget<ResourceT : Any>(
  private val scope: ProducerScope<GlideFlowInstant<ResourceT>>,
  private val size: ResolvableGlideSize,
) : Target<ResourceT>, RequestListener<ResourceT> {
  @Volatile private var resolvedSize: Size? = null
  @Volatile private var currentRequest: Request? = null
  @Volatile private var lastResource: Resource<ResourceT>? = null

  @GuardedBy("this") private val sizeReadyCallbacks = mutableListOf<SizeReadyCallback>()

  init {
    when (size) {
      // If we have a size, skip the coroutine, we can continue immediately.
      is ImmediateGlideSize -> resolvedSize = size.size
      // Otherwise, we do not want to block the flow while waiting on a size because one or more
      // requests in the chain may have a fixed size, even if the primary request does not.
      // Starting the Glide request right away allows any subrequest that has a fixed size to
      // begin immediately, shaving off some small amount of time.
      is AsyncGlideSize ->
        scope.launch {
          val localResolvedSize = size.getSize()
          val callbacksToNotify: List<SizeReadyCallback>
          synchronized(this) {
            resolvedSize = localResolvedSize
            callbacksToNotify = ArrayList(sizeReadyCallbacks)
            sizeReadyCallbacks.clear()
          }
          callbacksToNotify.forEach {
            it.onSizeReady(localResolvedSize.width, localResolvedSize.height)
          }
        }
    }
  }

  override fun onStart() {}
  override fun onStop() {}
  override fun onDestroy() {}

  override fun onLoadStarted(placeholder: Drawable?) {
    lastResource = null
    scope.trySend(Placeholder(Status.RUNNING, placeholder))
  }

  override fun onLoadFailed(errorDrawable: Drawable?) {
    scope.trySend(Placeholder(Status.FAILED, errorDrawable))
  }

  override fun onResourceReady(resource: ResourceT, transition: Transition<in ResourceT>?) {
    throw UnsupportedOperationException()

  }

  override fun onLoadCleared(placeholder: Drawable?) {
    lastResource = null
    scope.trySend(Placeholder(Status.CLEARED, placeholder))
  }

  override fun getSize(cb: SizeReadyCallback) {
    val localResolvedSize = resolvedSize
    if (localResolvedSize != null) {
      cb.onSizeReady(localResolvedSize.width, localResolvedSize.height)
      return
    }

    synchronized(this@FlowTarget) {
      val lockedResolvedSize = resolvedSize
      if (lockedResolvedSize != null) {
        cb.onSizeReady(lockedResolvedSize.width, lockedResolvedSize.height)
      } else {
        sizeReadyCallbacks.add(cb)
      }
    }
  }

  override fun removeCallback(cb: SizeReadyCallback) {
    synchronized(this) { sizeReadyCallbacks.remove(cb) }
  }

  override fun setRequest(request: Request?) {
    currentRequest = request
  }

  override fun getRequest(): Request? {
    return currentRequest
  }

  override fun onLoadFailed(
    e: GlideException?,
    model: Any?,
    target: Target<ResourceT>,
    isFirstResource: Boolean,
  ): Boolean {
    val localLastResource = lastResource
    val localRequest = currentRequest
    if (localLastResource != null && localRequest?.isComplete == false && !localRequest.isRunning) {
      scope.channel.trySend(localLastResource.asFailure())
    }
    return false
  }

  override fun onResourceReady(
    resource: ResourceT,
    model: Any,
    target: Target<ResourceT>,
    dataSource: DataSource,
    isFirstResource: Boolean,
  ): Boolean {
    val result =
      Resource(
        // currentRequest is the entire request state, so we can use it to figure out if this
        // resource is from a thumbnail request (isComplete is false) or the primary request.
        if (currentRequest?.isComplete == true) Status.SUCCEEDED else Status.RUNNING,
        resource,
        isFirstResource,
        dataSource
      )
    lastResource = result
    scope.trySend(result)
    return true
  }
}

@InternalGlideApi
public data class Size(val width: Int, val height: Int) {
  init {
    require(width.isValidGlideDimension())
    require(height.isValidGlideDimension())
  }
}

@InternalGlideApi public sealed class ResolvableGlideSize

@InternalGlideApi public data class ImmediateGlideSize(val size: Size) : ResolvableGlideSize()

@OptIn(InternalGlideApi::class)
public class AsyncGlideSize : ResolvableGlideSize() {
  private val size = CompletableDeferred<Size>()

  public fun setSize(size: Size) {
    this.size.complete(size)
  }

  public suspend fun getSize(): Size {
    return size.await()
  }
}

@InternalGlideApi public fun Int.isValidGlideDimension(): Boolean = Util.isValidDimension(this)
