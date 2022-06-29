package com.bumptech.glide.samples.gallery

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

class GalleryViewModel(application: Application) : AndroidViewModel(application) {
  private val mediaStoreDataSource = MediaStoreDataSource(application)

  private val _uiState: MutableStateFlow<List<MediaStoreData>> = MutableStateFlow(emptyList())
  val mediaStoreData: StateFlow<List<MediaStoreData>> = _uiState

  init {
    viewModelScope.launch {
      mediaStoreDataSource.loadMediaStoreData().flowOn(Dispatchers.IO).collect {
        _uiState.value = it
      }
    }
  }
}