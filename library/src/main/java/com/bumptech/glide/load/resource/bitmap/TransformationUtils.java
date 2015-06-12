package com.bumptech.glide.load.resource.bitmap;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.ExifInterface;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.Log;

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;

/**
 * A class with methods to efficiently resize Bitmaps.
 */
public final class TransformationUtils {
  private static final String TAG = "TransformationUtils";
  public static final int PAINT_FLAGS = Paint.DITHER_FLAG | Paint.FILTER_BITMAP_FLAG;
  private static final Paint DEFAULT_PAINT = new Paint(PAINT_FLAGS);
  private static final int CIRCLE_CROP_PAINT_FLAGS = PAINT_FLAGS | Paint.ANTI_ALIAS_FLAG;
  private static final Paint CIRCLE_CROP_SHAPE_PAINT = new Paint(CIRCLE_CROP_PAINT_FLAGS);
  private static final Paint CIRCLE_CROP_BITMAP_PAINT;
  static {
    CIRCLE_CROP_BITMAP_PAINT = new Paint(CIRCLE_CROP_PAINT_FLAGS);
    CIRCLE_CROP_BITMAP_PAINT.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
  }

  private TransformationUtils() {
    // Utility class.
  }

  /**
   * A potentially expensive operation to crop the given Bitmap so that it fills the given
   * dimensions. This operation is significantly less expensive in terms of memory if a mutable
   * Bitmap with the given dimensions is passed in as well.
   *
   * @param pool     The BitmapPool to obtain a bitmap from.
   * @param toCrop   The Bitmap to resize.
   * @param width    The width in pixels of the final Bitmap.
   * @param height   The height in pixels of the final Bitmap.
   * @return The resized Bitmap (will be recycled if recycled is not null).
   */
  public static Bitmap centerCrop(@NonNull BitmapPool pool, @NonNull Bitmap toCrop, int width,
      int height) {
    if (toCrop.getWidth() == width && toCrop.getHeight() == height) {
      return toCrop;
    }
    // From ImageView/Bitmap.createScaledBitmap.
    final float scale;
    float dx = 0, dy = 0;
    Matrix m = new Matrix();
    if (toCrop.getWidth() * height > width * toCrop.getHeight()) {
      scale = (float) height / (float) toCrop.getHeight();
      dx = (width - toCrop.getWidth() * scale) * 0.5f;
    } else {
      scale = (float) width / (float) toCrop.getWidth();
      dy = (height - toCrop.getHeight() * scale) * 0.5f;
    }

    m.setScale(scale, scale);
    m.postTranslate((int) (dx + 0.5f), (int) (dy + 0.5f));

    Bitmap result = pool.get(width, height, getSafeConfig(toCrop));
    // We don't add or remove alpha, so keep the alpha setting of the Bitmap we were given.
    TransformationUtils.setAlpha(toCrop, result);

    Canvas canvas = new Canvas(result);
    canvas.drawBitmap(toCrop, m, DEFAULT_PAINT);
    return result;
  }

  /**
   * An expensive operation to resize the given Bitmap down so that it fits within the given
   * dimensions maintain the original proportions.
   *
   * @param pool   The BitmapPool obtain a bitmap from.
   * @param toFit  The Bitmap to shrink.
   * @param width  The width in pixels the final image will fit within.
   * @param height The height in pixels the final image will fit within.
   * @return A new Bitmap shrunk to fit within the given dimensions, or toFit if toFit's width or
   * height matches the given dimensions and toFit fits within the given dimensions
   */
  public static Bitmap fitCenter(@NonNull BitmapPool pool, @NonNull Bitmap toFit, int width,
      int height) {
    if (toFit.getWidth() == width && toFit.getHeight() == height) {
      if (Log.isLoggable(TAG, Log.VERBOSE)) {
        Log.v(TAG, "requested target size matches input, returning input");
      }
      return toFit;
    }
    final float widthPercentage = width / (float) toFit.getWidth();
    final float heightPercentage = height / (float) toFit.getHeight();
    final float minPercentage = Math.min(widthPercentage, heightPercentage);

    // take the floor of the target width/height, not round. If the matrix
    // passed into drawBitmap rounds differently, we want to slightly
    // overdraw, not underdraw, to avoid artifacts from bitmap reuse.
    final int targetWidth = (int) (minPercentage * toFit.getWidth());
    final int targetHeight = (int) (minPercentage * toFit.getHeight());

    if (toFit.getWidth() == targetWidth && toFit.getHeight() == targetHeight) {
      if (Log.isLoggable(TAG, Log.VERBOSE)) {
        Log.v(TAG, "adjusted target size matches input, returning input");
      }
      return toFit;
    }

    Bitmap.Config config = getSafeConfig(toFit);
    Bitmap toReuse = pool.get(targetWidth, targetHeight, config);

    // We don't add or remove alpha, so keep the alpha setting of the Bitmap we were given.
    TransformationUtils.setAlpha(toFit, toReuse);

    if (Log.isLoggable(TAG, Log.VERBOSE)) {
      Log.v(TAG, "request: " + width + "x" + height);
      Log.v(TAG, "toFit:   " + toFit.getWidth() + "x" + toFit.getHeight());
      Log.v(TAG, "toReuse: " + toReuse.getWidth() + "x" + toReuse.getHeight());
      Log.v(TAG, "minPct:   " + minPercentage);
    }

    Canvas canvas = new Canvas(toReuse);
    Matrix matrix = new Matrix();
    matrix.setScale(minPercentage, minPercentage);
    canvas.drawBitmap(toFit, matrix, DEFAULT_PAINT);

    return toReuse;
  }

  /**
   * Sets the alpha of the Bitmap we're going to re-use to the alpha of the Bitmap we're going to
   * transform. This keeps {@link android.graphics.Bitmap#hasAlpha()}} consistent before and after
   * the transformation for transformations that don't add or remove transparent pixels.
   *
   * @param toTransform The {@link android.graphics.Bitmap} that will be transformed.
   * @param outBitmap   The {@link android.graphics.Bitmap} that will be returned from the
   *                    transformation.
   */
  public static void setAlpha(Bitmap toTransform, Bitmap outBitmap) {
    setAlphaIfAvailable(outBitmap, toTransform.hasAlpha());
  }

  @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
  private static void setAlphaIfAvailable(Bitmap bitmap, boolean hasAlpha) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1 && bitmap != null) {
      bitmap.setHasAlpha(hasAlpha);
    }
  }

  /**
   * This is an expensive operation that copies the image in place with the pixels rotated. If
   * possible rather use getOrientationMatrix, and put that as the imageMatrix on an ImageView.
   *
   * @param imageToOrient   Image Bitmap to orient.
   * @param degreesToRotate number of degrees to rotate the image by. If zero the original image is
   *                        returned unmodified.
   * @return The oriented bitmap. May be the imageToOrient without modification, or a new Bitmap.
   */
  public static Bitmap rotateImage(@NonNull Bitmap imageToOrient, int degreesToRotate) {
    Bitmap result = imageToOrient;
    try {
      if (degreesToRotate != 0) {
        Matrix matrix = new Matrix();
        matrix.setRotate(degreesToRotate);
        result = Bitmap.createBitmap(imageToOrient, 0, 0, imageToOrient.getWidth(),
            imageToOrient.getHeight(), matrix, true /*filter*/);
      }
    } catch (Exception e) {
      if (Log.isLoggable(TAG, Log.ERROR)) {
        Log.e(TAG, "Exception when trying to orient image", e);
      }
    }
    return result;
  }

  /**
   * Get the # of degrees an image must be rotated to match the given exif orientation.
   *
   * @param exifOrientation The exif orientation [1-8]
   * @return the number of degrees to rotate
   */
  public static int getExifOrientationDegrees(int exifOrientation) {
    final int degreesToRotate;
    switch (exifOrientation) {
      case ExifInterface.ORIENTATION_TRANSPOSE:
      case ExifInterface.ORIENTATION_ROTATE_90:
        degreesToRotate = 90;
        break;
      case ExifInterface.ORIENTATION_ROTATE_180:
      case ExifInterface.ORIENTATION_FLIP_VERTICAL:
        degreesToRotate = 180;
        break;
      case ExifInterface.ORIENTATION_TRANSVERSE:
      case ExifInterface.ORIENTATION_ROTATE_270:
        degreesToRotate = 270;
        break;
      default:
        degreesToRotate = 0;
    }
    return degreesToRotate;
  }

  /**
   * Rotate and/or flip the image to match the given exif orientation.
   *
   * @param pool            A pool that may or may not contain an image of the necessary
   *                        dimensions.
   * @param toOrient        The bitmap to rotate/flip.
   * @param exifOrientation the exif orientation [1-8].
   * @return The rotated and/or flipped image or toOrient if no rotation or flip was necessary.
   */
  public static Bitmap rotateImageExif(@NonNull BitmapPool pool, @NonNull Bitmap toOrient,
      int exifOrientation) {
    final Matrix matrix = new Matrix();
    initializeMatrixForRotation(exifOrientation, matrix);
    if (matrix.isIdentity()) {
      return toOrient;
    }

    // From Bitmap.createBitmap.
    final RectF newRect = new RectF(0, 0, toOrient.getWidth(), toOrient.getHeight());
    matrix.mapRect(newRect);

    final int newWidth = Math.round(newRect.width());
    final int newHeight = Math.round(newRect.height());

    Bitmap.Config config = getSafeConfig(toOrient);
    Bitmap result = pool.get(newWidth, newHeight, config);

    matrix.postTranslate(-newRect.left, -newRect.top);

    final Canvas canvas = new Canvas(result);
    canvas.drawBitmap(toOrient, matrix, DEFAULT_PAINT);

    return result;
  }

  /**
   * Crop the image to a circle and resize to the specified width/height.  The circle crop will
   * have the same width and height equal to the min-edge of the result image.
   *
   * @param pool   The BitmapPool obtain a bitmap from.
   * @param toCrop   The Bitmap to resize.
   * @param destWidth    The width in pixels of the final Bitmap.
   * @param destHeight   The height in pixels of the final Bitmap.
   * @return The resized Bitmap (will be recycled if recycled is not null).
   */
  public static Bitmap circleCrop(@NonNull BitmapPool pool, @NonNull Bitmap toCrop, int destWidth,
      int destHeight) {
    int destMinEdge = Math.min(destWidth, destHeight);
    float radius = destMinEdge / 2f;
    Rect destRect = new Rect((destWidth - destMinEdge) / 2, (destHeight - destMinEdge) / 2,
        destMinEdge, destMinEdge);

    int srcWidth = toCrop.getWidth();
    int srcHeight = toCrop.getHeight();
    int srcMinEdge = Math.min(srcWidth, srcHeight);
    Rect srcRect = new Rect((srcWidth - srcMinEdge) / 2, (srcHeight - srcMinEdge) / 2,
        srcMinEdge, srcMinEdge);

    Bitmap result = pool.get(destWidth, destHeight, getSafeConfig(toCrop));
    setAlphaIfAvailable(result, true /*hasAlpha*/);
    Canvas canvas = new Canvas(result);

    // Draw a circle
    canvas.drawCircle(destRect.left + radius, destRect.top + radius, radius,
        CIRCLE_CROP_SHAPE_PAINT);

    // Draw the bitmap in the circle
    canvas.drawBitmap(toCrop, srcRect, destRect, CIRCLE_CROP_BITMAP_PAINT);

    return result;
  }

  private static Bitmap.Config getSafeConfig(Bitmap bitmap) {
    return bitmap.getConfig() != null ? bitmap.getConfig() : Bitmap.Config.ARGB_8888;
  }

  // Visible for testing.
  static void initializeMatrixForRotation(int exifOrientation, Matrix matrix) {
    switch (exifOrientation) {
      case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
        matrix.setScale(-1, 1);
        break;
      case ExifInterface.ORIENTATION_ROTATE_180:
        matrix.setRotate(180);
        break;
      case ExifInterface.ORIENTATION_FLIP_VERTICAL:
        matrix.setRotate(180);
        matrix.postScale(-1, 1);
        break;
      case ExifInterface.ORIENTATION_TRANSPOSE:
        matrix.setRotate(90);
        matrix.postScale(-1, 1);
        break;
      case ExifInterface.ORIENTATION_ROTATE_90:
        matrix.setRotate(90);
        break;
      case ExifInterface.ORIENTATION_TRANSVERSE:
        matrix.setRotate(-90);
        matrix.postScale(-1, 1);
        break;
      case ExifInterface.ORIENTATION_ROTATE_270:
        matrix.setRotate(-90);
        break;
      default:
        // Do nothing.
    }
  }
}
