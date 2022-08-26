package com.bumptech.glide.integration.compose

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import com.bumptech.glide.Glide
import com.bumptech.glide.ListPreloader
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.RequestManager

/**
 * Preloads ahead of the users current scroll position for [LazyRow] and
 * [androidx.compose.foundation.lazy.LazyColumn], similar to [ListPreloader] and
 * [com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader].
 *
 * @param state The [LazyListState] provided to the `LazyRow` or `LazyColumn`
 * @param data The backing list of metadata that we're going to preload images for.
 * @param size The override size we'll pass to [RequestBuilder.override] .
 * @param numberOfItemsToPreload The number of items to preload ahead of the user's current
 * position. This should be tested for each application. If the total memory size of the preloaded
 * images exceeds the memory cache, preloading is effectively useless. However if you preload too
 * few things, the buffer may be small enough that images are not available when they could be, so
 * it's always a balancing act. The smaller the preloaded image, the more you can preload.
 * @param fixedVisibleItemCount The number of visible items. In some cases this can vary widely
 * in which case you can leave this value `null`. If the number of visible items is always one
 * or two, it might make sense to just set this to the larger of the two to reduce churn in the
 * preloader.
 * @param requestBuilderTransform See [ListPreloader.PreloadModelProvider.getPreloadRequestBuilder]
 */
// TODO(judds): Consider wrapping a LazyRow / LazyColumn and providing state instead of a separate
// function. Wrapping might also make it easier to pass through the size and request builder
// modifications so that it's easier to make sure the preload size matches a size on the
// GlideImage
@Composable
@ExperimentalGlideComposeApi
public fun <DataTypeT : Any> GlideLazyListPreloader(
  state: LazyListState,
  data: List<DataTypeT>,
  size: Size,
  numberOfItemsToPreload: Int,
  fixedVisibleItemCount: Int? = null,
  requestBuilderTransform: PreloadRequestBuilderTransform<DataTypeT>,
) {
  val preloader =
    rememberGlidePreloader(
      data = data,
      size = size,
      numberOfItemsToPreload = numberOfItemsToPreload,
      requestBuilderTransform = requestBuilderTransform,
    )
  doPreload(preloader = preloader, state = state, fixedVisibleItemCount = fixedVisibleItemCount)
}

@Composable
private fun <DataTypeT: Any> doPreload(
  preloader: ListPreloader<DataTypeT>, state: LazyListState, fixedVisibleItemCount: Int?
) =
  LaunchedEffect(preloader, state, fixedVisibleItemCount) {
    snapshotFlow { state.lazyListVisibleInfo(fixedVisibleItemCount) }
      .collect { lazyListVisibleInfo ->
        preloader.onScroll(
          null,
          lazyListVisibleInfo.firstVisibleItemIndex,
          lazyListVisibleInfo.visibleItemCount,
          lazyListVisibleInfo.totalItemCount,
        )
      }
  }

@Composable
private fun <DataTypeT: Any> rememberGlidePreloader(
  data: List<DataTypeT>,
  size: Size,
  numberOfItemsToPreload: Int,
  requestBuilderTransform: PreloadRequestBuilderTransform<DataTypeT>,
): ListPreloader<DataTypeT> {
  val context = LocalContext.current
  val requestManager = remember(context) { Glide.with(context) }

  val updatedData = rememberUpdatedState(data)
  val updatedSize = rememberUpdatedState(size)

  return remember(requestManager, requestBuilderTransform, numberOfItemsToPreload) {
    ListPreloader(
      requestManager,
      PreloadModelProvider(requestManager, requestBuilderTransform, updatedData),
      { _, _, _ ->
        intArrayOf(updatedSize.value.width.toInt(), updatedSize.value.height.toInt())
      },
      numberOfItemsToPreload,
    )
  }
}

private class PreloadModelProvider<DataTypeT : Any>(
  private val requestManager: RequestManager,
  private val requestBuilderTransform: PreloadRequestBuilderTransform<DataTypeT>,
  private val data : State<List<DataTypeT>>,
) : ListPreloader.PreloadModelProvider<DataTypeT> {
  override fun getPreloadItems(position: Int): MutableList<DataTypeT> {
    return mutableListOf(this.data.value[position])
  }

  override fun getPreloadRequestBuilder(item: DataTypeT): RequestBuilder<*> {
    return requestBuilderTransform(item, requestManager.asDrawable().load(item))
  }
}

private fun LazyListState.lazyListVisibleInfo(fixedVisibleItemCount: Int?) =
  LazyListVisibleInfo(
    firstVisibleItemIndex = firstVisibleItemIndex,
    visibleItemCount = fixedVisibleItemCount ?: layoutInfo.visibleItemsInfo.size,
    totalItemCount = layoutInfo.totalItemsCount
  )

@Immutable
private data class LazyListVisibleInfo(
  val firstVisibleItemIndex: Int, val visibleItemCount: Int, val totalItemCount: Int,
)

private typealias PreloadRequestBuilderTransform<DataTypeT> =
    (item: DataTypeT, requestBuilder: RequestBuilder<*>) -> RequestBuilder<*>
