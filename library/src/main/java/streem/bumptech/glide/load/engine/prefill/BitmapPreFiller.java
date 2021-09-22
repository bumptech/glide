package com.bumptech.glide.load.engine.prefill;

import android.graphics.Bitmap;
import androidx.annotation.VisibleForTesting;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.engine.cache.MemoryCache;
import com.bumptech.glide.util.Util;
import java.util.HashMap;
import java.util.Map;

/**
 * A class for pre-filling {@link android.graphics.Bitmap Bitmaps} in a {@link
 * com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool}.
 */
public final class BitmapPreFiller {

  private final MemoryCache memoryCache;
  private final BitmapPool bitmapPool;
  private final DecodeFormat defaultFormat;

  private BitmapPreFillRunner current;

  public BitmapPreFiller(
      MemoryCache memoryCache, BitmapPool bitmapPool, DecodeFormat defaultFormat) {
    this.memoryCache = memoryCache;
    this.bitmapPool = bitmapPool;
    this.defaultFormat = defaultFormat;
  }

  @SuppressWarnings("deprecation")
  public void preFill(PreFillType.Builder... bitmapAttributeBuilders) {
    if (current != null) {
      current.cancel();
    }

    PreFillType[] bitmapAttributes = new PreFillType[bitmapAttributeBuilders.length];
    for (int i = 0; i < bitmapAttributeBuilders.length; i++) {
      PreFillType.Builder builder = bitmapAttributeBuilders[i];
      if (builder.getConfig() == null) {
        builder.setConfig(
            defaultFormat == DecodeFormat.PREFER_ARGB_8888
                ? Bitmap.Config.ARGB_8888
                : Bitmap.Config.RGB_565);
      }
      bitmapAttributes[i] = builder.build();
    }

    PreFillQueue allocationOrder = generateAllocationOrder(bitmapAttributes);
    current = new BitmapPreFillRunner(bitmapPool, memoryCache, allocationOrder);
    Util.postOnUiThread(current);
  }

  @VisibleForTesting
  PreFillQueue generateAllocationOrder(PreFillType... preFillSizes) {
    final long maxSize =
        memoryCache.getMaxSize() - memoryCache.getCurrentSize() + bitmapPool.getMaxSize();

    int totalWeight = 0;
    for (PreFillType size : preFillSizes) {
      totalWeight += size.getWeight();
    }

    final float bytesPerWeight = maxSize / (float) totalWeight;

    Map<PreFillType, Integer> attributeToCount = new HashMap<>();
    for (PreFillType size : preFillSizes) {
      int bytesForSize = Math.round(bytesPerWeight * size.getWeight());
      int bytesPerBitmap = getSizeInBytes(size);
      int bitmapsForSize = bytesForSize / bytesPerBitmap;
      attributeToCount.put(size, bitmapsForSize);
    }

    return new PreFillQueue(attributeToCount);
  }

  private static int getSizeInBytes(PreFillType size) {
    return Util.getBitmapByteSize(size.getWidth(), size.getHeight(), size.getConfig());
  }
}
