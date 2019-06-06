package com.bumptech.glide.load.resource.bitmap;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import androidx.annotation.Nullable;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPoolAdapter;
import com.bumptech.glide.request.target.Target;
import java.util.concurrent.locks.Lock;

final class DrawableToBitmapConverter {
  private static final String TAG = "DrawableToBitmap";
  private static final BitmapPool NO_RECYCLE_BITMAP_POOL =
      new BitmapPoolAdapter() {
        @Override
        public void put(Bitmap bitmap) {
          // Avoid calling super to avoid recycling the given Bitmap.
        }
      };

  private DrawableToBitmapConverter() {
    // Utility class.
  }

  @Nullable
  static Resource<Bitmap> convert(BitmapPool bitmapPool, Drawable drawable, int width, int height) {
    // Handle DrawableContainer or StateListDrawables that may contain one or more BitmapDrawables.
    drawable = drawable.getCurrent();
    Bitmap result = null;
    boolean isRecycleable = false;
    if (drawable instanceof BitmapDrawable) {
      result = ((BitmapDrawable) drawable).getBitmap();
    } else if (!(drawable instanceof Animatable)) {
      result = drawToBitmap(bitmapPool, drawable, width, height);
      // We created and drew to the Bitmap, so it's safe for us to recycle or re-use.
      isRecycleable = true;
    }

    BitmapPool toUse = isRecycleable ? bitmapPool : NO_RECYCLE_BITMAP_POOL;
    return BitmapResource.obtain(result, toUse);
  }

  @Nullable
  private static Bitmap drawToBitmap(
      BitmapPool bitmapPool, Drawable drawable, int width, int height) {
    if (width == Target.SIZE_ORIGINAL && drawable.getIntrinsicWidth() <= 0) {
      if (Log.isLoggable(TAG, Log.WARN)) {
        Log.w(
            TAG,
            "Unable to draw "
                + drawable
                + " to Bitmap with Target.SIZE_ORIGINAL because the"
                + " Drawable has no intrinsic width");
      }
      return null;
    }
    if (height == Target.SIZE_ORIGINAL && drawable.getIntrinsicHeight() <= 0) {
      if (Log.isLoggable(TAG, Log.WARN)) {
        Log.w(
            TAG,
            "Unable to draw "
                + drawable
                + " to Bitmap with Target.SIZE_ORIGINAL because the"
                + " Drawable has no intrinsic height");
      }
      return null;
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
