package com.bumptech.glide.load.engine;

import android.graphics.Bitmap;
import android.graphics.Gainmap;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
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
    this.hasGainMap = VERSION.SDK_INT >= VERSION_CODES.UPSIDE_DOWN_CAKE && bitmap.hasGainmap();
    if (this.hasGainMap) {
      Gainmap gainmap = bitmap.getGainmap();
      int gainmapByteCount =
          (gainmap != null && gainmap.getGainmapContents() != null)
              ? gainmap.getGainmapContents().getAllocationByteCount()
              : 0;
      this.byteCount = bitmap.getAllocationByteCount() + gainmapByteCount;
    } else {
      this.byteCount = bitmap.getAllocationByteCount();
    }
  }
}
