package com.bumptech.glide.load.engine;

import android.graphics.Bitmap;
import android.os.Build;
import androidx.annotation.Nullable;

/** Metadata of a Bitmap, captured safely. */
public final class BitmapInfo {
  public final int width;
  public final int height;
  @Nullable public final Bitmap.Config config;
  public final int byteCount;
  public final boolean hasMipMap;
  @Nullable public final String colorSpace;

  public BitmapInfo(Bitmap bitmap) {
    this.width = bitmap.getWidth();
    this.height = bitmap.getHeight();
    this.config = bitmap.getConfig();
    this.byteCount = bitmap.getAllocationByteCount();
    this.hasMipMap = Build.VERSION.SDK_INT >= 17 && bitmap.hasMipMap();
    this.colorSpace =
        Build.VERSION.SDK_INT >= 26 && bitmap.getColorSpace() != null
            ? bitmap.getColorSpace().getName()
            : null;
  }
}
