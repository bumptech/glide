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
  public final boolean hasGainMap;

  public BitmapInfo(Bitmap bitmap) {
    this.width = bitmap.getWidth();
    this.height = bitmap.getHeight();
    this.config = bitmap.getConfig();
    this.hasGainMap = Build.VERSION.SDK_INT >= 34 && bitmap.hasGainMap();
    this.byteCount =
        bitmap.getAllocationByteCount()
            + (hasGainMap ? bitmap.getGainMap().getContents().getAllocationByteCount() : 0);
  }
}
