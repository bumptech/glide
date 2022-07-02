package com.bumptech.glide.samples.gallery

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader
import kotlinx.coroutines.launch

/** Displays media store data in a recycler view.  */
class HorizontalGalleryFragment : Fragment() {
  private lateinit var adapter: RecyclerAdapter
  private lateinit var recyclerView: RecyclerView
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val galleryViewModel: GalleryViewModel by viewModels()
    lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        galleryViewModel.mediaStoreData.collect { data ->
          adapter.setData(data)
        }
      }
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
  ): View? {
    val result = inflater.inflate(R.layout.recycler_view, container, false)
    recyclerView = result.findViewById<View>(R.id.recycler_view) as RecyclerView
    val layoutManager = GridLayoutManager(activity, 1)
    layoutManager.orientation = RecyclerView.HORIZONTAL
    recyclerView.layoutManager = layoutManager
    recyclerView.setHasFixedSize(true)

    val glideRequests = Glide.with(this)
    adapter = RecyclerAdapter(requireContext(), glideRequests)
    val preloader = RecyclerViewPreloader(glideRequests, adapter, adapter, 3)
    recyclerView.addOnScrollListener(preloader)
    recyclerView.adapter = adapter
    return result
  }
}