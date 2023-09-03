package com.bumptech.glide.test

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.test.core.app.ApplicationProvider
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.lang.IllegalStateException

const val GENERATED_FILES_DIR = "compose_goldens"
const val EXTENSION = "png"
const val SEPARATOR = "_"

@Composable
fun Int.pxToDp() = with(LocalDensity.current) { toDp() }

fun ImageBitmap.compareToGolden(testName: String) {
  val bitmap = toBitmap()
  val existingGolden = readExistingGolden(testName)
  val reasonForWrite = if (existingGolden == null) {
    "Missing golden"
  } else if (!existingGolden.sameAs(bitmap)) {
    "Different golden"
  } else {
    null
  }
  if (reasonForWrite != null) {
    val filePath = writeBitmap(bitmap, testName)
    throw IllegalStateException("$reasonForWrite for $testName, wrote a new one. cd to androidTest/assets and run: adb pull $filePath")
  }
}

private fun ImageBitmap.toBitmap(): Bitmap {
  val pixels = toPixelMap()
  val bitmap = Bitmap.createBitmap(pixels.width, pixels.height, Bitmap.Config.ARGB_8888)
  bitmap.setPixels(
    pixels.buffer,
    pixels.bufferOffset,
    pixels.stride,
    0,
    0,
    pixels.width,
    pixels.height
  )
  return bitmap
}

private fun readExistingGolden(testName: String): Bitmap? {
  return try {
    ApplicationProvider.getApplicationContext<Context>()
      .assets
      .open(testFileName(testName)).use {
        val options = BitmapFactory.Options()
        options.inScaled = false
        BitmapFactory.decodeStream(it, null, options)
      }
  } catch (e: FileNotFoundException) {
    null
  }
}

private fun testFileName(testName: String) = "$testName$SEPARATOR${getDeviceString()}.$EXTENSION"
private fun getTestFilesDir(): File {
  val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
  return File(dir, GENERATED_FILES_DIR)
}

private fun getDeviceString() =
  "${ApplicationProvider.getApplicationContext<Context>()
    .resources
    .displayMetrics
    .density}$SEPARATOR${Build.VERSION.SDK_INT}"

private fun writeBitmap(bitmap: Bitmap, testName: String): String {
  val testFilesDir = getTestFilesDir()
  require(!(!testFilesDir.exists() && !testFilesDir.mkdirs())) { "Failed to make directory: $testFilesDir" }
  val file = File(testFilesDir, testFileName(testName))
  check(!(file.exists() && !file.delete())) { "Failed to remove existing file: $file" }
  var os: OutputStream? = null
  try {
    os = BufferedOutputStream(FileOutputStream(file))
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, os)
    os.close()
  } catch (e: IOException) {
    throw RuntimeException(e)
  } finally {
    if (os != null) {
      try {
        os.close()
      } catch (e: IOException) {
        // Ignored.
      }
    }
  }
  return file.absolutePath
}