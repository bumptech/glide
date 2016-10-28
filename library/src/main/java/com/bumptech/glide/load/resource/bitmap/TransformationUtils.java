package com.bumptech.glide.load.resource.bitmap;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Shader;
import android.media.ExifInterface;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.Log;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.util.Preconditions;
import com.bumptech.glide.util.Synthetic;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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
  /**
   * https://github.com/bumptech/glide/issues/738 On some devices (Moto X with android 5.1) bitmap
   * drawing is not thread safe.
   * This lock only locks for these specific devices. For other types of devices the lock is always
   * available and therefore does not impact performance
   */
  private static final Lock BITMAP_DRAWABLE_LOCK = "XT1097".equals(Build.MODEL)
      // TODO: Switch to Build.VERSION_CODES.LOLLIPOP_MR1 when apps have updated target API levels.
      && Build.VERSION.SDK_INT == 22
      ? new ReentrantLock()
      : new NoLock();

  static {
    CIRCLE_CROP_BITMAP_PAINT = new Paint(CIRCLE_CROP_PAINT_FLAGS);
    CIRCLE_CROP_BITMAP_PAINT.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
  }

  private TransformationUtils() {
    // Utility class.
  }


  public static Lock getBitmapDrawableLock() {
    return BITMAP_DRAWABLE_LOCK;
  }

  /**
   * A potentially expensive operation to crop the given Bitmap so that it fills the given
   * dimensions. This operation is significantly less expensive in terms of memory if a mutable
   * Bitmap with the given dimensions is passed in as well.
   *
   * @param pool     The BitmapPool to obtain a bitmap from.
   * @param inBitmap   The Bitmap to resize.
   * @param width    The width in pixels of the final Bitmap.
   * @param height   The height in pixels of the final Bitmap.
   * @return The resized Bitmap (will be recycled if recycled is not null).
   */
  public static Bitmap centerCrop(@NonNull BitmapPool pool, @NonNull Bitmap inBitmap, int width,
      int height) {
    if (inBitmap.getWidth() == width && inBitmap.getHeight() == height) {
      return inBitmap;
    }
    // From ImageView/Bitmap.createScaledBitmap.
    final float scale;
    float dx = 0, dy = 0;
    Matrix m = new Matrix();
    if (inBitmap.getWidth() * height > width * inBitmap.getHeight()) {
      scale = (float) height / (float) inBitmap.getHeight();
      dx = (width - inBitmap.getWidth() * scale) * 0.5f;
    } else {
      scale = (float) width / (float) inBitmap.getWidth();
      dy = (height - inBitmap.getHeight() * scale) * 0.5f;
    }

    m.setScale(scale, scale);
    m.postTranslate((int) (dx + 0.5f), (int) (dy + 0.5f));

    Bitmap result = pool.get(width, height, getSafeConfig(inBitmap));
    // We don't add or remove alpha, so keep the alpha setting of the Bitmap we were given.
    TransformationUtils.setAlpha(inBitmap, result);

    applyMatrix(inBitmap, result, m);
    return result;
  }

  /**
   * An expensive operation to resize the given Bitmap down so that it fits within the given
   * dimensions maintain the original proportions.
   *
   * @param pool   The BitmapPool obtain a bitmap from.
   * @param inBitmap  The Bitmap to shrink.
   * @param width  The width in pixels the final image will fit within.
   * @param height The height in pixels the final image will fit within.
   * @return A new Bitmap shrunk to fit within the given dimensions, or toFit if toFit's width or
   * height matches the given dimensions and toFit fits within the given dimensions
   */
  public static Bitmap fitCenter(@NonNull BitmapPool pool, @NonNull Bitmap inBitmap, int width,
      int height) {
    if (inBitmap.getWidth() == width && inBitmap.getHeight() == height) {
      if (Log.isLoggable(TAG, Log.VERBOSE)) {
        Log.v(TAG, "requested target size matches input, returning input");
      }
      return inBitmap;
    }
    final float widthPercentage = width / (float) inBitmap.getWidth();
    final float heightPercentage = height / (float) inBitmap.getHeight();
    final float minPercentage = Math.min(widthPercentage, heightPercentage);

    // take the floor of the target width/height, not round. If the matrix
    // passed into drawBitmap rounds differently, we want to slightly
    // overdraw, not underdraw, to avoid artifacts from bitmap reuse.
    final int targetWidth = (int) (minPercentage * inBitmap.getWidth());
    final int targetHeight = (int) (minPercentage * inBitmap.getHeight());

    if (inBitmap.getWidth() == targetWidth && inBitmap.getHeight() == targetHeight) {
      if (Log.isLoggable(TAG, Log.VERBOSE)) {
        Log.v(TAG, "adjusted target size matches input, returning input");
      }
      return inBitmap;
    }

    Bitmap.Config config = getSafeConfig(inBitmap);
    Bitmap toReuse = pool.get(targetWidth, targetHeight, config);

    // We don't add or remove alpha, so keep the alpha setting of the Bitmap we were given.
    TransformationUtils.setAlpha(inBitmap, toReuse);

    if (Log.isLoggable(TAG, Log.VERBOSE)) {
      Log.v(TAG, "request: " + width + "x" + height);
      Log.v(TAG, "toFit:   " + inBitmap.getWidth() + "x" + inBitmap.getHeight());
      Log.v(TAG, "toReuse: " + toReuse.getWidth() + "x" + toReuse.getHeight());
      Log.v(TAG, "minPct:   " + minPercentage);
    }

    Matrix matrix = new Matrix();
    matrix.setScale(minPercentage, minPercentage);
    applyMatrix(inBitmap, toReuse, matrix);

    return toReuse;
  }

  /**
   * If the Bitmap is smaller or equal to the Target it returns the original size, if not then
   * {@link #fitCenter(BitmapPool, Bitmap, int, int)} is called instead.
   *
   * @param pool   The BitmapPool obtain a bitmap from.
   * @param inBitmap  The Bitmap to center.
   * @param width  The width in pixels of the target.
   * @param height The height in pixels of the target.
   * @return returns input Bitmap if smaller or equal to target, or toFit if the Bitmap's width or
   * height is larger than the given dimensions
   */
  public static Bitmap centerInside(@NonNull BitmapPool pool, @NonNull Bitmap inBitmap, int width,
                                 int height) {
    if (inBitmap.getWidth() <= width && inBitmap.getHeight() <= height) {
      if (Log.isLoggable(TAG, Log.VERBOSE)) {
        Log.v(TAG, "requested target size larger or equal to input, returning input");
      }
      return inBitmap;
    } else {
      if (Log.isLoggable(TAG, Log.VERBOSE)) {
        Log.v(TAG, "requested target size too big for input, fit centering instead");
      }
      return fitCenter(pool, inBitmap, width, height);
    }
  }

  /**
   * Sets the alpha of the Bitmap we're going to re-use to the alpha of the Bitmap we're going to
   * transform. This keeps {@link android.graphics.Bitmap#hasAlpha()}} consistent before and after
   * the transformation for transformations that don't add or remove transparent pixels.
   *
   * @param inBitmap The {@link android.graphics.Bitmap} that will be transformed.
   * @param outBitmap   The {@link android.graphics.Bitmap} that will be returned from the
   *                    transformation.
   */
  public static void setAlpha(Bitmap inBitmap, Bitmap outBitmap) {
    setAlphaIfAvailable(outBitmap, inBitmap.hasAlpha());
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
        break;
    }
    return degreesToRotate;
  }

  /**
   * Rotate and/or flip the image to match the given exif orientation.
   *
   * @param pool            A pool that may or may not contain an image of the necessary
   *                        dimensions.
   * @param inBitmap        The bitmap to rotate/flip.
   * @param exifOrientation the exif orientation [1-8].
   * @return The rotated and/or flipped image or toOrient if no rotation or flip was necessary.
   */
  public static Bitmap rotateImageExif(@NonNull BitmapPool pool, @NonNull Bitmap inBitmap,
      int exifOrientation) {
    final Matrix matrix = new Matrix();
    initializeMatrixForRotation(exifOrientation, matrix);
    if (matrix.isIdentity()) {
      return inBitmap;
    }

    // From Bitmap.createBitmap.
    final RectF newRect = new RectF(0, 0, inBitmap.getWidth(), inBitmap.getHeight());
    matrix.mapRect(newRect);

    final int newWidth = Math.round(newRect.width());
    final int newHeight = Math.round(newRect.height());

    Bitmap.Config config = getSafeConfig(inBitmap);
    Bitmap result = pool.get(newWidth, newHeight, config);

    matrix.postTranslate(-newRect.left, -newRect.top);

    applyMatrix(inBitmap, result, matrix);
    return result;
  }

  /**
   * Crop the image to a circle and resize to the specified width/height.  The circle crop will
   * have the same width and height equal to the min-edge of the result image.
   *
   * @param pool   The BitmapPool obtain a bitmap from.
   * @param inBitmap   The Bitmap to resize.
   * @param destWidth    The width in pixels of the final Bitmap.
   * @param destHeight   The height in pixels of the final Bitmap.
   * @return The resized Bitmap (will be recycled if recycled is not null).
   */
  public static Bitmap circleCrop(@NonNull BitmapPool pool, @NonNull Bitmap inBitmap,
      int destWidth, int destHeight) {
    int destMinEdge = Math.min(destWidth, destHeight);
    float radius = destMinEdge / 2f;

    int srcWidth = inBitmap.getWidth();
    int srcHeight = inBitmap.getHeight();

    float scaleX = destMinEdge / (float) srcWidth;
    float scaleY = destMinEdge / (float) srcHeight;
    float maxScale = Math.max(scaleX, scaleY);

    float scaledWidth = maxScale * srcWidth;
    float scaledHeight = maxScale * srcHeight;
    float left = (destMinEdge - scaledWidth) / 2f;
    float top = (destMinEdge - scaledHeight) / 2f;

    RectF destRect = new RectF(left, top, left + scaledWidth, top + scaledHeight);

    // Alpha is required for this transformation.
    Bitmap toTransform = getAlphaSafeBitmap(pool, inBitmap);

    Bitmap result = pool.get(destMinEdge, destMinEdge, Bitmap.Config.ARGB_8888);
    setAlphaIfAvailable(result, true /*hasAlpha*/);

    BITMAP_DRAWABLE_LOCK.lock();
    try {
      Canvas canvas = new Canvas(result);
      // Draw a circle
      canvas.drawCircle(radius, radius, radius, CIRCLE_CROP_SHAPE_PAINT);
      // Draw the bitmap in the circle
      canvas.drawBitmap(toTransform, null, destRect, CIRCLE_CROP_BITMAP_PAINT);
      clear(canvas);
    } finally {
      BITMAP_DRAWABLE_LOCK.unlock();
    }

    if (!toTransform.equals(inBitmap)) {
      pool.put(toTransform);
    }

    return result;
  }

  private static Bitmap getAlphaSafeBitmap(@NonNull BitmapPool pool,
      @NonNull Bitmap maybeAlphaSafe) {
    if (Bitmap.Config.ARGB_8888.equals(maybeAlphaSafe.getConfig())) {
      return maybeAlphaSafe;
    }

    Bitmap argbBitmap = pool.get(maybeAlphaSafe.getWidth(), maybeAlphaSafe.getHeight(),
        Bitmap.Config.ARGB_8888);
    new Canvas(argbBitmap).drawBitmap(maybeAlphaSafe, 0 /*left*/, 0 /*top*/, null /*pain*/);

    // We now own this Bitmap. It's our responsibility to replace it in the pool outside this method
    // when we're finished with it.
    return argbBitmap;
  }

  /**
   * Creates a bitmap from a source bitmap and rounds the corners.
   *
   * @param inBitmap the source bitmap to use as a basis for the created bitmap.
   * @param width the width of the generated bitmap.
   * @param height the height of the generated bitmap.
   * @param roundingRadius the corner radius to be applied (in device-specific pixels).
   * @return a {@link Bitmap} similar to inBitmap but with rounded corners.
   * @throws IllegalArgumentException if roundingRadius, width or height is 0 or less.
   */
  public static Bitmap roundedCorners(@NonNull BitmapPool pool, @NonNull Bitmap inBitmap,
      int width, int height, int roundingRadius) {
    Preconditions.checkArgument(width > 0, "width must be greater than 0.");
    Preconditions.checkArgument(height > 0, "height must be greater than 0.");
    Preconditions.checkArgument(roundingRadius > 0, "roundingRadius must be greater than 0.");

    // Alpha is required for this transformation.
    Bitmap toTransform = getAlphaSafeBitmap(pool, inBitmap);
    Bitmap result = pool.get(width, height, Bitmap.Config.ARGB_8888);

    setAlphaIfAvailable(result, true /* hasAlpha */);

    BitmapShader shader = new BitmapShader(toTransform, Shader.TileMode.CLAMP,
        Shader.TileMode.CLAMP);
    Paint paint = new Paint();
    paint.setAntiAlias(true);
    paint.setShader(shader);
    RectF rect = new RectF(0, 0, result.getWidth(), result.getHeight());
    BITMAP_DRAWABLE_LOCK.lock();
    try {
      Canvas canvas = new Canvas(result);
      canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
      canvas.drawRoundRect(rect, roundingRadius, roundingRadius, paint);
      clear(canvas);
    } finally {
      BITMAP_DRAWABLE_LOCK.unlock();
    }

    if (!toTransform.equals(inBitmap)) {
      pool.put(toTransform);
    }

    return result;
  }

  // Avoids warnings in M+.
  private static void clear(Canvas canvas) {
    canvas.setBitmap(null);
  }

  private static Bitmap.Config getSafeConfig(Bitmap bitmap) {
    return bitmap.getConfig() != null ? bitmap.getConfig() : Bitmap.Config.ARGB_8888;
  }

  private static void applyMatrix(@NonNull Bitmap inBitmap, @NonNull Bitmap targetBitmap,
      Matrix matrix) {
    BITMAP_DRAWABLE_LOCK.lock();
    try {
      Canvas canvas = new Canvas(targetBitmap);
      canvas.drawBitmap(inBitmap, matrix, DEFAULT_PAINT);
      clear(canvas);
    } finally {
      BITMAP_DRAWABLE_LOCK.unlock();
    }
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

  private static final class NoLock implements Lock {

    @Synthetic
    NoLock() { }

    @Override
    public void lock() {
      // do nothing
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
      // do nothing
    }

    @Override
    public boolean tryLock() {
      return true;
    }

    @Override
    public boolean tryLock(long time, @NonNull TimeUnit unit) throws InterruptedException {
      return true;
    }

    @Override
    public void unlock() {
      // do nothing
    }

    @NonNull
    @Override
    public Condition newCondition() {
      throw new UnsupportedOperationException("Should not be called");
    }
  }
}
