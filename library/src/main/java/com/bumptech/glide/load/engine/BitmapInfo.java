package com.bumptech.glide.load.engine;

import android.graphics.Bitmap;
import android.graphics.Gainmap;
import android.os.Build.VERSION;
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
    int byteCount = bitmap.getAllocationByteCount();
    if (VERSION.SDK_INT >= 34 && bitmap.hasGainmap()) {
      this.hasGainMap = true;
      Gainmap gainmap = bitmap.getGainmap();
      byteCount +=
          (gainmap != null && gainmap.getGainmapContents() != null)
              ? gainmap.getGainmapContents().getAllocationByteCount()
              : 0;
    } else {
      this.hasGainMap = false;
    }
    this.byteCount = byteCount;
  }
}
