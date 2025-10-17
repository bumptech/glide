package com.bumptech.glide.test;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;
import com.bumptech.glide.util.Preconditions;

public final class CanonicalBitmap {
  @Nullable private Bitmap bitmap;
  @Nullable private Float scaleFactor;

  @NonNull
  public synchronized Bitmap getBitmap() {
    if (bitmap == null) {
      bitmap = decodeBitmap();
    }
    return bitmap;
  }

  public CanonicalBitmap scale(float scaleFactor) {
    Preconditions.checkArgument(bitmap == null, "Can't set scale factor after decoding image");
    this.scaleFactor = scaleFactor;
    return this;
  }

  public int getWidth() {
    return getBitmap().getWidth();
  }

  public int getHeight() {
    return getBitmap().getHeight();
  }

  private Bitmap decodeBitmap() {
    Context context = ApplicationProvider.getApplicationContext();
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inScaled = false;
    int resourceId = ResourceIds.raw.canonical;
    Bitmap result = BitmapFactory.decodeResource(context.getResources(), resourceId, options);
    if (scaleFactor != null) {
      result =
          Bitmap.createScaledBitmap(
              result,
              (int) (result.getWidth() * scaleFactor),
              (int) (result.getHeight() * scaleFactor),
              /* filter= */ false);
    }
    // Make sure the Bitmap is immutable.
    return result;
  }
}
