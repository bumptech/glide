package com.bumptech.glide.integration.compose

import android.graphics.drawable.Drawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import com.bumptech.glide.Glide
import com.bumptech.glide.ListPreloader
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.RequestManager

private const val DEFAULT_ITEMS_TO_PRELOAD = 10

/**
 * Preloads ahead of the data access position on the returned [GlidePreloadingData], similar to
 * [ListPreloader] and [com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader].
 *
 * The only time this API is useful is when your UI also loads an item with exactly the same
 * options, model and size. You can ensure you're doing so by using the [RequestBuilder] returned
 * by [GlidePreloadingData.get]
 *
 * Typical usage will look something like this:
 * ```
 * val glidePreloadingData =
 *   rememberGlidePreloadingData(myDataList, THUMBNAIL_SIZE) { myDataItem, requestBuilder ->
 *     // THUMBNAIL_SIZE is applied for you, but .load() is not because determining the model from
 *     // the underlying data isn't trivial. Don't forget to call .load()!
 *     requestBuilder.load(myDataItem.url)
 *   }
 *
 *  LazyRow(...) {
 *    item { Text(text = "Header") }
 *    items(glidePreloadingData.size) { index ->
 *      val (myDataItem, preloadRequest) = glidePreloadingData[index]
 *      GlideImage(model = item.url, contentDescription = item.description, ...) { primaryRequest ->
 *        primaryRequest.thumbnail(preloadRequest)
 *      }
 *    }
 *  }
 *  ```
 *
 *  Note that preloading will not occur until the first access of `glidePreloadingData`. If you
 *  have multiple disjoint data sets that you'd like to preload, or have some number of preceding
 *  header rows prior to your first image, you can optionally add a few manual calls to make
 *  preloading continue smoothly across data sets. One way you might do so is to call the next data
 *  set toward the end of the previous data  set, e.g.:
 *
 *  ```
 *  val itemsToPreload = 15
 *  items(firstDataSet.size) { index ->
 *    ... // Do something with first data set.
 *
 *    // Then as you get to the end of the first data set, start preloading the next data set
 *    manually
 *    if (index >= firstDataSet.size - itemsToPreload) {
 *      nextDataSet[itemsToPreload - (firstDataSet.size - index)]
 *    }
 *  }
 *  ```
 *
 * @param dataSize The total number of items to display and preload.
 * @param dataGetter A getter for the item at the given index (ie [List.get].
 * @param preloadImageSize The override size we'll pass to [RequestBuilder.override] .
 * @param numberOfItemsToPreload The number of items to preload ahead of the user's current
 * position. This should be tested for each application. If the total memory size of the preloaded
 * images exceeds the memory cache size, preloading for a lazy list is not effective. However if you
 * preload too few things, the buffer may be small enough that images are not available when they
 * could be, so it's always a balancing act. The smaller the preloaded image, the more you can
 * preload.
 * @param fixedVisibleItemCount The number of visible items. In some cases this can vary widely in
 * which case you can leave this value `null`. If the number of visible items is always one or two,
 * it might make sense to just set this to the larger of the two to reduce churn in the preloader.
 * @param requestBuilderTransform See [ListPreloader.PreloadModelProvider.getPreloadRequestBuilder].
 * You should call [RequestBuilder.load] on the given `item` so that any type specific options
 * applied the matching [RequestManager.load] method are applied identically to the preload request.
 * Remember that the request produced by this transform must exactly match the request made in your
 * non-preload request for preloading to be useful.
 */
@Composable
public fun <DataT : Any> rememberGlidePreloadingData(
  dataSize: Int,
  dataGetter: (Int) -> DataT,
  preloadImageSize: Size,
  numberOfItemsToPreload: Int = DEFAULT_ITEMS_TO_PRELOAD,
  fixedVisibleItemCount: Int? = null,
  requestBuilderTransform: PreloadRequestBuilderTransform<DataT>,
): GlidePreloadingData<DataT> {
  val requestManager = LocalContext.current.let { remember(it) { Glide.with(it) } }
  return remember(
    requestManager,
    dataSize,
    dataGetter,
    preloadImageSize,
    numberOfItemsToPreload,
    fixedVisibleItemCount,
    requestBuilderTransform,
  ) {
    val preloaderData =
      PreloaderData(dataSize, dataGetter, requestBuilderTransform, preloadImageSize)
    val preloader =
      ListPreloader<DataT>(
        requestManager,
        PreloadModelProvider(
          requestManager,
          preloaderData,
        ),
        PreloadDimensionsProvider(preloaderData),
        numberOfItemsToPreload,
      )
    PreloadDataImpl(
      dataSize,
      dataGetter,
      requestManager,
      preloadImageSize,
      fixedVisibleItemCount,
      preloader,
      requestBuilderTransform,
    )
  }
}

/**
 * A helper for [rememberGlidePreloadingData] that accepts a [List]. See the more general equivalent
 * for details.
 */
@Composable
public fun <DataT : Any> rememberGlidePreloadingData(
  data: List<DataT>,
  preloadImageSize: Size,
  numberOfItemsToPreload: Int = DEFAULT_ITEMS_TO_PRELOAD,
  fixedVisibleItemCount: Int? = null,
  requestBuilderTransform: PreloadRequestBuilderTransform<DataT>,
): GlidePreloadingData<DataT> {
  return rememberGlidePreloadingData(
    dataSize = data.size,
    dataGetter = data::get,
    preloadImageSize = preloadImageSize,
    numberOfItemsToPreload = numberOfItemsToPreload,
    fixedVisibleItemCount = fixedVisibleItemCount,
    requestBuilderTransform = requestBuilderTransform,
  )
}

private data class PreloaderData<DataT>(
  val dataSize: Int,
  val dataAccessor: (Int) -> DataT,
  val requestBuilderTransform: PreloadRequestBuilderTransform<DataT>,
  val size: Size,
)  {
  fun preloadRequests(
    requestManager: RequestManager,
    item: DataT,
  ): RequestBuilder<Drawable> {
    return requestBuilderTransform(item, requestManager.asDrawable())
  }
}

/**
 * Wraps a set of data, triggers image preloads based on the positions provided to [get] and exposes
 * the data and the preload [RequestBuilder].
 */
public interface GlidePreloadingData<DataT> {
  /** The total number of items in the data set. */
  public val size: Int

  /**
   * Returns the [DataT] at a given index in the data and a [RequestBuilder] that will trigger a
   * request that exactly matches the preload request for this index.
   *
   * The returned [RequestBuilder] should always be used to display the item at the given index.
   * Otherwise the preload request triggered by this call is likely useless work. The
   * [RequestBuilder] can either be used as the primary request, or more likely, passed as the
   * [RequestBuilder.thumbnail] to a higher resolution request.
   *
   * This method has side affects! Calling it will trigger preloads based on the given [index].
   * Preloading assumes sequential access in a manner that matches what the user will see. If you
   * need to look up data at indices for other reasons, use the underlying data source directly so
   * that you do not confuse the preloader. Only use this method when obtaining data to display to
   * the user.
   */
  @Composable
  public operator fun get(index: Int): Pair<DataT, RequestBuilder<Drawable>>
}

private class PreloadDataImpl<DataT : Any>(
  override val size: Int,
  private val indexToData: (Int) -> DataT,
  private val requestManager: RequestManager,
  private val preloadImageSize: Size,
  private val fixedVisibleItemCount: Int?,
  private val preloader: ListPreloader<DataT>,
  private val requestBuilderTransform: PreloadRequestBuilderTransform<DataT>,
) : GlidePreloadingData<DataT> {

  @Composable
  override fun get(index: Int): Pair<DataT, RequestBuilder<Drawable>> {
    val item = indexToData(index)
    val requestBuilder =
      requestBuilderTransform(
        item,
        requestManager.asDrawable()
          .override(preloadImageSize.width.toInt(), preloadImageSize.height.toInt()),
      )

    LaunchedEffect(preloader, preloadImageSize, requestBuilderTransform, indexToData, index) {
      preloader.onScroll(
        /* absListView = */ null,
        index,
        fixedVisibleItemCount ?: 1,
        size,
      )
    }
    return item to requestBuilder
  }
}

private class PreloadDimensionsProvider<DataT : Any>(
  private val updatedData: PreloaderData<DataT>,
) : ListPreloader.PreloadSizeProvider<DataT> {
  override fun getPreloadSize(item: DataT, adapterPosition: Int, perItemPosition: Int): IntArray =
    updatedData.size.toIntArray()
}

private fun Size.toIntArray() = intArrayOf(width.toInt(), height.toInt())

private class PreloadModelProvider<DataT : Any>(
  private val requestManager: RequestManager,
  private val data: PreloaderData<DataT>,
) : ListPreloader.PreloadModelProvider<DataT> {

  override fun getPreloadItems(position: Int): MutableList<DataT> {
    return mutableListOf(data.dataAccessor(position))
  }

  override fun getPreloadRequestBuilder(item: DataT): RequestBuilder<*> {
    return data.preloadRequests(requestManager, item)
  }
}

/**
 * Provides the data to load and a [RequestBuilder] to load it with.
 *
 * You must at least call [RequestBuilder.load] with the appropriate model extracted from `item` on
 * the given `requestBuilder`. You can also optionally call any other methods available on
 * `requestBuilder` to customize your load.
 */
public typealias PreloadRequestBuilderTransform<DataTypeT> =
  (item: DataTypeT, requestBuilder: RequestBuilder<Drawable>) -> RequestBuilder<Drawable>