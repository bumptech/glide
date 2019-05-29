package com.bumptech.glide.load.engine.bitmap_recycle;

import android.graphics.Bitmap;
import androidx.annotation.Nullable;

interface LruPoolStrategy {
  void put(Bitmap bitmap);

  @Nullable
  Bitmap get(int width, int height, Bitmap.Config config);

  @Nullable
  Bitmap removeLast();

  String logBitmap(Bitmap bitmap);

  String logBitmap(int width, int height, Bitmap.Config config);

  int getSize(Bitmap bitmap);
}
