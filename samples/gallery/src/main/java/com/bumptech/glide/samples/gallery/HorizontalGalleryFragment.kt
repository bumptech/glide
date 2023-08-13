package com.bumptech.glide.samples.gallery

import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.rememberGlidePreloadingData
import com.bumptech.glide.signature.MediaStoreSignature

/** Displays media store data in a recycler view. */
@OptIn(ExperimentalGlideComposeApi::class)
class HorizontalGalleryFragment : Fragment() {

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    val galleryViewModel: GalleryViewModel by viewModels()
    return ComposeView(requireContext()).apply {
      setContent { LoadableDeviceMedia(galleryViewModel) }
    }
  }

  @Composable
  fun LoadableDeviceMedia(viewModel: GalleryViewModel) {
    val mediaStoreData = viewModel.mediaStoreData.collectAsState()
    DeviceMedia(mediaStoreData.value)
  }

  @Composable
  fun DeviceMedia(mediaStoreData: List<MediaStoreData>) {
    val requestBuilderTransform =
      { item: MediaStoreData, requestBuilder: RequestBuilder<Drawable> ->
        requestBuilder.load(item.uri).signature(item.signature())
      }

    val preloadingData =
      rememberGlidePreloadingData(
        mediaStoreData,
        THUMBNAIL_SIZE,
        requestBuilderTransform = requestBuilderTransform,
      )

    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      items(preloadingData.size) { index ->
        val (mediaStoreItem, preloadRequestBuilder) = preloadingData[index]
        MediaStoreView(mediaStoreItem, preloadRequestBuilder, Modifier.fillParentMaxSize())
      }
    }
  }

  private fun MediaStoreData.signature() = MediaStoreSignature(mimeType, dateModified, orientation)

  @Composable
  fun MediaStoreView(
    item: MediaStoreData,
    preloadRequestBuilder: RequestBuilder<Drawable>,
    modifier: Modifier,
  ) =
    GlideImage(model = item.uri, contentDescription = item.displayName, modifier = modifier) {
      it.thumbnail(preloadRequestBuilder).signature(item.signature())
    }

  companion object {
    private const val THUMBNAIL_DIMENSION = 50
    private val THUMBNAIL_SIZE = Size(THUMBNAIL_DIMENSION.toFloat(), THUMBNAIL_DIMENSION.toFloat())
  }
}
