package com.bumptech.glide.integration.compose

import android.util.Log
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
 * The only time this API is useful is when your UI also loads an item with exactly the same
 * options, model and size. Be careful to make sure that your requests are identical in the
 * preloader and in the UI, or you might end up hurting performance instead of improving it.
 *
 * @param state The [LazyListState] provided to the `LazyRow` or `LazyColumn`
 * @param data The backing list of metadata that we're going to preload images for.
 * @param size The override size we'll pass to [RequestBuilder.override] .
 * @param numberOfItemsToPreload The number of items to preload ahead of the user's current
 * position. This should be tested for each application. If the total memory size of the preloaded
 * images exceeds the memory cache size, preloading for a lazy list is not effective. However if you
 * preload too few things, the buffer may be small enough that images are not available when they
 * could be, so it's always a balancing act. The smaller the preloaded image, the more you can
 * preload.
 * @param fixedVisibleItemCount The number of visible items. In some cases this can vary widely in
 * which case you can leave this value `null`. If the number of visible items is always one or two,
 * it might make sense to just set this to the larger of the two to reduce churn in the preloader.
 * @param viewToDataPosition A function that can be used to map a view position to a position in
 * [data]. If your `LazyRow` or `LazyColumn` contains only images so there's a 1:1 correspondence
 * between the position in the view and the position in [data], you do not need to provide this
 * function. Otherwise you need to provide a function that offsets the view position into [data] or
 * returns `null` if the position corresponds to a view that isn't showing an image from [data].
 * TODO(judds): like the TODO below, we could handle this automatically with some more wrapping.
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
  viewToDataPosition: (Int) -> Int? =
    viewToDataPosition@{
      if (it >= data.size) {
        if (Log.isLoggable(Constants.TAG, Log.WARN)) {
          Log.w(
            Constants.TAG,
            "Mismatch between view size and data size, provide a viewToDataPosition to" +
              " GlideLazyListPreloader",
          )
        }
        return@viewToDataPosition null
      }
      it
    },
  size: Size,
  numberOfItemsToPreload: Int,
  fixedVisibleItemCount: Int? = null,
  requestBuilderTransform: PreloadRequestBuilderTransform<DataTypeT>,
) {
  val preloader =
    rememberGlidePreloader(
      data = data,
      viewToDataPosition = viewToDataPosition,
      size = size,
      numberOfItemsToPreload = numberOfItemsToPreload,
      requestBuilderTransform = requestBuilderTransform,
    )
  LaunchPreload(preloader = preloader, state = state, fixedVisibleItemCount = fixedVisibleItemCount)
}

@Composable
private fun <DataTypeT : Any> LaunchPreload(
  preloader: ListPreloader<DataTypeT>,
  state: LazyListState,
  fixedVisibleItemCount: Int?
) =
  LaunchedEffect(preloader, state, fixedVisibleItemCount) {
    snapshotFlow { state.lazyListVisibleInfo(fixedVisibleItemCount) }
      .collect { lazyListVisibleInfo ->
        preloader.onScroll(
          /* absListView = */ null,
          lazyListVisibleInfo.firstVisibleItemIndex,
          lazyListVisibleInfo.visibleItemCount,
          lazyListVisibleInfo.totalItemCount,
        )
      }
  }

@Composable
private fun <DataTypeT : Any> rememberGlidePreloader(
  data: List<DataTypeT>,
  viewToDataPosition: (Int) -> Int?,
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
      PreloadModelProvider(
        requestManager,
        requestBuilderTransform,
        updatedData,
        viewToDataPosition,
      ),
      { _, _, _ -> intArrayOf(updatedSize.value.width.toInt(), updatedSize.value.height.toInt()) },
      numberOfItemsToPreload,
    )
  }
}


private class PreloadModelProvider<DataTypeT : Any>(
  private val requestManager: RequestManager,
  private val requestBuilderTransform: PreloadRequestBuilderTransform<DataTypeT>,
  private val data: State<List<DataTypeT>>,
  private val viewToDataPosition: (Int) -> Int?,
) : ListPreloader.PreloadModelProvider<DataTypeT> {
  override fun getPreloadItems(viewPosition: Int): List<DataTypeT> {
    val dataPosition = viewToDataPosition(viewPosition)
    return dataPosition?.let { listOf(this.data.value[it]) } ?: listOf()
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
  val firstVisibleItemIndex: Int,
  val visibleItemCount: Int,
  val totalItemCount: Int,
)

private typealias PreloadRequestBuilderTransform<DataTypeT> =
  (item: DataTypeT, requestBuilder: RequestBuilder<*>) -> RequestBuilder<*>

private object Constants {
  const val TAG = "GlidePreloader"
}
