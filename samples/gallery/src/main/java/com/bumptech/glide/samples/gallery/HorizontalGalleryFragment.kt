package com.bumptech.glide.samples.gallery

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.GlideLazyListPreloader
import com.bumptech.glide.signature.MediaStoreSignature

/** Displays media store data in a recycler view.  */
@OptIn(ExperimentalGlideComposeApi::class)
class HorizontalGalleryFragment : Fragment() {

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
  ): View {
    val galleryViewModel: GalleryViewModel by viewModels()
    return ComposeView(requireContext()).apply {
      setContent {
        LoadableDeviceMedia(galleryViewModel)
      }
    }
  }

  @Composable
  fun LoadableDeviceMedia(viewModel: GalleryViewModel) {
    val mediaStoreData = viewModel.mediaStoreData.collectAsState()
    DeviceMedia(mediaStoreData.value)
  }

  @Composable
  fun DeviceMedia(mediaStoreData: List<MediaStoreData>) {
    val state = rememberLazyListState()
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), state = state) {
      items(mediaStoreData) { mediaStoreItem ->
        MediaStoreView(mediaStoreItem, Modifier.fillParentMaxSize())
      }
    }

    GlideLazyListPreloader(
      state = state,
      data = mediaStoreData,
      size = THUMBNAIL_SIZE,
      numberOfItemsToPreload = 15,
      fixedVisibleItemCount = 2,
    ) { item, requestBuilder -> requestBuilder.load(item.uri).signature(item.signature()) }
  }

  private fun MediaStoreData.signature() =
    MediaStoreSignature(mimeType, dateModified, orientation)

  @Composable
  fun MediaStoreView(item: MediaStoreData, modifier: Modifier) {
    val requestManager = Glide.with(requireContext())
    val signature = item.signature()

    GlideImage(
      model = item.uri,
      contentDescription = item.displayName,
      modifier = modifier,
    ) {
      it.thumbnail(
        requestManager.asDrawable()
          .load(item.uri)
          .signature(signature)
          .override(THUMBNAIL_DIMENSION)
      )
        .signature(signature)
    }
  }

  companion object {
    private const val THUMBNAIL_DIMENSION = 50
    private val THUMBNAIL_SIZE = Size(THUMBNAIL_DIMENSION.toFloat(), THUMBNAIL_DIMENSION.toFloat())
  }
}
