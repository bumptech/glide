package com.bumptech.glide.samples.gallery

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import android.provider.MediaStore
import android.provider.MediaStore.Files.FileColumns
import android.provider.MediaStore.MediaColumns
import com.bumptech.glide.util.Preconditions
import com.bumptech.glide.util.Util
import java.util.ArrayList
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

/** Loads metadata from the media store for images and videos. */
class MediaStoreDataSource
internal constructor(
  private val context: Context,
) {

  fun loadMediaStoreData(): Flow<List<MediaStoreData>> = callbackFlow {
    val contentObserver =
      object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
          super.onChange(selfChange)
          launch { trySend(query()) }
        }
      }

    context.contentResolver.registerContentObserver(
      MEDIA_STORE_FILE_URI,
      /* notifyForDescendants=*/ true,
      contentObserver
    )

    trySend(query())

    awaitClose { context.contentResolver.unregisterContentObserver(contentObserver) }
  }

  private fun query(): MutableList<MediaStoreData> {
    Preconditions.checkArgument(
      Util.isOnBackgroundThread(),
      "Can only query from a background thread"
    )
    val data: MutableList<MediaStoreData> = ArrayList()
    val cursor =
      context.contentResolver.query(
        MEDIA_STORE_FILE_URI,
        PROJECTION,
        FileColumns.MEDIA_TYPE +
          " = " +
          FileColumns.MEDIA_TYPE_IMAGE +
          " OR " +
          FileColumns.MEDIA_TYPE +
          " = " +
          FileColumns.MEDIA_TYPE_VIDEO,
        /* selectionArgs= */ null,
        "${MediaColumns.DATE_TAKEN} DESC"
      )
        ?: return data

    @Suppress("NAME_SHADOWING") // Might as well, it's the same object?
    cursor.use { cursor ->
      val idColNum = cursor.getColumnIndexOrThrow(MediaColumns._ID)
      val dateTakenColNum = cursor.getColumnIndexOrThrow(MediaColumns.DATE_TAKEN)
      val dateModifiedColNum = cursor.getColumnIndexOrThrow(MediaColumns.DATE_MODIFIED)
      val mimeTypeColNum = cursor.getColumnIndexOrThrow(MediaColumns.MIME_TYPE)
      val orientationColNum = cursor.getColumnIndexOrThrow(MediaColumns.ORIENTATION)
      val mediaTypeColumnIndex = cursor.getColumnIndexOrThrow(FileColumns.MEDIA_TYPE)
      val displayNameIndex = cursor.getColumnIndexOrThrow(FileColumns.DISPLAY_NAME)

      while (cursor.moveToNext()) {
        val id = cursor.getLong(idColNum)
        val dateTaken = cursor.getLong(dateTakenColNum)
        val mimeType = cursor.getString(mimeTypeColNum)
        val dateModified = cursor.getLong(dateModifiedColNum)
        val orientation = cursor.getInt(orientationColNum)
        val displayName = cursor.getString(displayNameIndex)
        val type =
          if (cursor.getInt(mediaTypeColumnIndex) == FileColumns.MEDIA_TYPE_IMAGE) Type.IMAGE
          else Type.VIDEO
        data.add(
          MediaStoreData(
            type = type,
            rowId = id,
            uri = Uri.withAppendedPath(MEDIA_STORE_FILE_URI, id.toString()),
            mimeType = mimeType,
            dateModified = dateModified,
            orientation = orientation,
            dateTaken = dateTaken,
            displayName = displayName,
          )
        )
      }
    }
    return data
  }

  companion object {
    private val MEDIA_STORE_FILE_URI = MediaStore.Files.getContentUri("external")
    private val PROJECTION =
      arrayOf(
        MediaColumns._ID,
        MediaColumns.DATE_TAKEN,
        MediaColumns.DATE_MODIFIED,
        MediaColumns.MIME_TYPE,
        MediaColumns.ORIENTATION,
        MediaColumns.DISPLAY_NAME,
        FileColumns.MEDIA_TYPE
      )
  }
}

/** A data model containing data for a single media item. */
@Parcelize
data class MediaStoreData(
  private val type: Type,
  val rowId: Long,
  val uri: Uri,
  val mimeType: String?,
  val dateModified: Long,
  val orientation: Int,
  val dateTaken: Long,
  val displayName: String?
) : Parcelable

/** The type of data. */
enum class Type {
  VIDEO,
  IMAGE
}
