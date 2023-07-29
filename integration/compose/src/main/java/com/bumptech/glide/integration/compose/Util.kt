package com.bumptech.glide.integration.compose

import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.ColorPainter
import com.google.accompanist.drawablepainter.DrawablePainter

fun Drawable.toPainter() =
    when (this) {
        is BitmapDrawable -> BitmapPainter(bitmap.asImageBitmap())
        is ColorDrawable -> ColorPainter(Color(color))
        else -> DrawablePainter(mutate())
    }