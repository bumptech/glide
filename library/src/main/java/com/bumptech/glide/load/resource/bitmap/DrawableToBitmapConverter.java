package com.bumptech.glide.load.resource.bitmap;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.request.target.Target;
import java.util.concurrent.locks.Lock;

final class DrawableToBitmapConverter {
  private DrawableToBitmapConverter() {
    // Utility class.
  }

  static boolean willDraw(Drawable drawable) {
    return !(drawable.getCurrent() instanceof BitmapDrawable);
  }

  @Nullable
  static Bitmap convert(BitmapPool bitmapPool, Drawable drawable, int width, int height) {
    // Handle DrawableContainer or StateListDrawables that may contain one or more BitmapDrawables.
    drawable = drawable.getCurrent();
    Bitmap result = null;
    if (drawable instanceof BitmapDrawable) {
      result = ((BitmapDrawable) drawable).getBitmap();
    } else if (!(drawable instanceof Animatable)) {
      result = drawToBitmap(bitmapPool, drawable, width, height);
    }

    if (result == null) {
      return null;
    }

    return result;
  }

  private static Bitmap drawToBitmap(
      BitmapPool bitmapPool, Drawable drawable, int width, int height) {
    if (width == Target.SIZE_ORIGINAL && drawable.getIntrinsicWidth() <= 0) {
      throw new IllegalArgumentException("Unable to draw " + drawable + " to Bitmap with "
          + "Target.SIZE_ORIGINAL because the Drawable has no intrinsic width");
    }
    if (height == Target.SIZE_ORIGINAL && drawable.getIntrinsicHeight() <= 0) {
      throw new IllegalArgumentException("Unable to draw " + drawable + " to Bitmap with "
          + "Target.SIZE_ORIGINAL because the Drawable has no intrinsic height");
    }
    int targetWidth = drawable.getIntrinsicWidth() > 0 ? drawable.getIntrinsicWidth() : width;
    int targetHeight = drawable.getIntrinsicHeight() > 0 ? drawable.getIntrinsicHeight() : height;

    Lock lock = TransformationUtils.getBitmapDrawableLock();
    lock.lock();
    Bitmap result = bitmapPool.get(targetWidth, targetHeight, Bitmap.Config.ARGB_8888);
    try {
      Canvas canvas = new Canvas(result);
      drawable.setBounds(0, 0, targetWidth, targetHeight);
      drawable.draw(canvas);
      canvas.setBitmap(null);
    } finally {
      lock.unlock();
    }
    return result;
  }
}
