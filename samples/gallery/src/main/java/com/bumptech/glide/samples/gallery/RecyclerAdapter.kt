package com.bumptech.glide.samples.gallery

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Point
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.samples.gallery.RecyclerAdapter.ListViewHolder
import com.bumptech.glide.ListPreloader.PreloadSizeProvider
import com.bumptech.glide.ListPreloader.PreloadModelProvider
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.Key
import com.bumptech.glide.signature.MediaStoreSignature
import com.bumptech.glide.util.Preconditions

/** Displays [com.bumptech.glide.samples.gallery.MediaStoreData] in a recycler view.  */
internal class RecyclerAdapter(
  context: Context,
  glideRequests: RequestManager,
) : RecyclerView.Adapter<ListViewHolder?>(), PreloadSizeProvider<MediaStoreData?>,
    PreloadModelProvider<MediaStoreData?> {
  private var data: List<MediaStoreData> = emptyList()
  private val screenWidth: Int
  private val requestBuilder: RequestBuilder<Drawable>
  private var actualDimensions: IntArray? = null

  override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ListViewHolder {
    val inflater = LayoutInflater.from(viewGroup.context)
    val view = inflater.inflate(R.layout.recycler_item, viewGroup, false)
    view.layoutParams.width = screenWidth
    if (actualDimensions == null) {
      view.viewTreeObserver
        .addOnPreDrawListener(
          object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
              if (actualDimensions == null) {
                actualDimensions = intArrayOf(view.width, view.height)
              }
              view.viewTreeObserver.removeOnPreDrawListener(this)
              return true
            }
          })
    }
    return ListViewHolder(view)
  }

  override fun onBindViewHolder(viewHolder: ListViewHolder, position: Int) {
    val current = data[position]
    val signature: Key =
      MediaStoreSignature(current.mimeType, current.dateModified, current.orientation)
    requestBuilder.clone().signature(signature).load(current.uri).into(viewHolder.image)
  }

  @SuppressLint("NotifyDataSetChanged")
  fun setData(mediaStoreData: List<MediaStoreData>) {
    data = mediaStoreData
    notifyDataSetChanged()
  }

  override fun getItemId(position: Int): Long {
    return data[position].rowId
  }

  override fun getItemCount(): Int {
    return data.size
  }

  override fun getItemViewType(position: Int): Int {
    return 0
  }

  override fun getPreloadItems(position: Int): List<MediaStoreData> {
    return if (data.isEmpty()) emptyList() else listOf(data[position])
  }

  override fun getPreloadRequestBuilder(item: MediaStoreData): RequestBuilder<Drawable>? {
    val signature = MediaStoreSignature(item.mimeType, item.dateModified, item.orientation)
    return requestBuilder.clone().signature(signature).load(item.uri)
  }

  override fun getPreloadSize(
    item: MediaStoreData, adapterPosition: Int, perItemPosition: Int,
  ): IntArray? {
    return actualDimensions
  }

  /**
   * ViewHolder containing views to display individual [ ].
   */
  internal class ListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val image: ImageView

    init {
      image = itemView.findViewById(R.id.image)
    }
  }

  companion object {
    // Display#getSize(Point)
    private fun getScreenWidth(context: Context): Int {
      val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
      val display = Preconditions.checkNotNull(wm).defaultDisplay
      val size = Point()
      display.getSize(size)
      return size.x
    }
  }

  init {
    requestBuilder = glideRequests.asDrawable().fitCenter()
    setHasStableIds(true)
    screenWidth = getScreenWidth(context)
  }
}