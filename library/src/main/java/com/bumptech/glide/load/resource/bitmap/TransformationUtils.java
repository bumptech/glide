package com.bumptech.glide.load.resource.bitmap;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Shader;
import android.media.ExifInterface;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.util.Preconditions;
import com.bumptech.glide.util.Synthetic;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/** A class with methods to efficiently resize Bitmaps. */
// Legacy Public APIs.
@SuppressWarnings("WeakerAccess")
public final class TransformationUtils {
  private static final String TAG = "TransformationUtils";
  public static final int PAINT_FLAGS = Paint.DITHER_FLAG | Paint.FILTER_BITMAP_FLAG;
  private static final Paint DEFAULT_PAINT = new Paint(PAINT_FLAGS);
  private static final int CIRCLE_CROP_PAINT_FLAGS = PAINT_FLAGS | Paint.ANTI_ALIAS_FLAG;
  private static final Paint CIRCLE_CROP_SHAPE_PAINT = new Paint(CIRCLE_CROP_PAINT_FLAGS);
  private static final Paint CIRCLE_CROP_BITMAP_PAINT;

  // See #738.
  private static final Set<String> MODELS_REQUIRING_BITMAP_LOCK =
      new HashSet<>(
          Arrays.asList(
              // Moto X gen 2
              "XT1085",
              "XT1092",
              "XT1093",
              "XT1094",
              "XT1095",
              "XT1096",
              "XT1097",
              "XT1098",
              // Moto G gen 1
              "XT1031",
              "XT1028",
              "XT937C",
              "XT1032",
              "XT1008",
              "XT1033",
              "XT1035",
              "XT1034",
              "XT939G",
              "XT1039",
              "XT1040",
              "XT1042",
              "XT1045",
              // Moto G gen 2
              "XT1063",
              "XT1064",
              "XT1068",
              "XT1069",
              "XT1072",
              "XT1077",
              "XT1078",
              "XT1079"));

  /**
   * https://github.com/bumptech/glide/issues/738 On some devices, bitmap drawing is not thread
   * safe. This lock only locks for these specific devices. For other types of devices the lock is
   * always available and therefore does not impact performance
   */
  private static final Lock BITMAP_DRAWABLE_LOCK =
      MODELS_REQUIRING_BITMAP_LOCK.contains(Build.MODEL) ? new ReentrantLock() : new NoLock();

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
   * @param pool The BitmapPool to obtain a bitmap from.
   * @param inBitmap The Bitmap to resize.
   * @param width The width in pixels of the final Bitmap.
   * @param height The height in pixels of the final Bitmap.
   * @return The resized Bitmap (will be recycled if recycled is not null).
   */
  public static Bitmap centerCrop(
      @NonNull BitmapPool pool, @NonNull Bitmap inBitmap, int width, int height) {
    if (inBitmap.getWidth() == width && inBitmap.getHeight() == height) {
      return inBitmap;
    }
    // From ImageView/Bitmap.createScaledBitmap.
    final float scale;
    final float dx;
    final float dy;
    Matrix m = new Matrix();
    if (inBitmap.getWidth() * height > width * inBitmap.getHeight()) {
      scale = (float) height / (float) inBitmap.getHeight();
      dx = (width - inBitmap.getWidth() * scale) * 0.5f;
      dy = 0;
    } else {
      scale = (float) width / (float) inBitmap.getWidth();
      dx = 0;
      dy = (height - inBitmap.getHeight() * scale) * 0.5f;
    }

    m.setScale(scale, scale);
    m.postTranslate((int) (dx + 0.5f), (int) (dy + 0.5f));

    Bitmap result = pool.get(width, height, getNonNullConfig(inBitmap));
    // We don't add or remove alpha, so keep the alpha setting of the Bitmap we were given.
    TransformationUtils.setAlpha(inBitmap, result);

    applyMatrix(inBitmap, result, m);
    return result;
  }

  /**
   * An expensive operation to resize the given Bitmap down so that it fits within the given
   * dimensions maintain the original proportions.
   *
   * @param pool The BitmapPool obtain a bitmap from.
   * @param inBitmap The Bitmap to shrink.
   * @param width The width in pixels the final image will fit within.
   * @param height The height in pixels the final image will fit within.
   * @return A new Bitmap shrunk to fit within the given dimensions, or toFit if toFit's width or
   *     height matches the given dimensions and toFit fits within the given dimensions
   */
  public static Bitmap fitCenter(
      @NonNull BitmapPool pool, @NonNull Bitmap inBitmap, int width, int height) {
    if (inBitmap.getWidth() == width && inBitmap.getHeight() == height) {
      if (Log.isLoggable(TAG, Log.VERBOSE)) {
        Log.v(TAG, "requested target size matches input, returning input");
      }
      return inBitmap;
    }
    final float widthPercentage = width / (float) inBitmap.getWidth();
    final float heightPercentage = height / (float) inBitmap.getHeight();
    final float minPercentage = Math.min(widthPercentage, heightPercentage);

    // Round here in case we've decoded exactly the image we want, but take the floor below to
    // avoid a line of garbage or blank pixels in images.
    int targetWidth = Math.round(minPercentage * inBitmap.getWidth());
    int targetHeight = Math.round(minPercentage * inBitmap.getHeight());

    if (inBitmap.getWidth() == targetWidth && inBitmap.getHeight() == targetHeight) {
      if (Log.isLoggable(TAG, Log.VERBOSE)) {
        Log.v(TAG, "adjusted target size matches input, returning input");
      }
      return inBitmap;
    }

    // Take the floor of the target width/height, not round. If the matrix
    // passed into drawBitmap rounds differently, we want to slightly
    // overdraw, not underdraw, to avoid artifacts from bitmap reuse.
    targetWidth = (int) (minPercentage * inBitmap.getWidth());
    targetHeight = (int) (minPercentage * inBitmap.getHeight());

    Bitmap.Config config = getNonNullConfig(inBitmap);
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
   * @param pool The BitmapPool obtain a bitmap from.
   * @param inBitmap The Bitmap to center.
   * @param width The width in pixels of the target.
   * @param height The height in pixels of the target.
   * @return returns input Bitmap if smaller or equal to target, or toFit if the Bitmap's width or
   *     height is larger than the given dimensions
   */
  public static Bitmap centerInside(
      @NonNull BitmapPool pool, @NonNull Bitmap inBitmap, int width, int height) {
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
   * @param outBitmap The {@link android.graphics.Bitmap} that will be returned from the
   *     transformation.
   */
  public static void setAlpha(Bitmap inBitmap, Bitmap outBitmap) {
    outBitmap.setHasAlpha(inBitmap.hasAlpha());
  }

  /**
   * This is an expensive operation that copies the image in place with the pixels rotated. If
   * possible rather use getOrientationMatrix, and put that as the imageMatrix on an ImageView.
   *
   * @param imageToOrient Image Bitmap to orient.
   * @param degreesToRotate number of degrees to rotate the image by. If zero the original image is
   *     returned unmodified.
   * @return The oriented bitmap. May be the imageToOrient without modification, or a new Bitmap.
   */
  public static Bitmap rotateImage(@NonNull Bitmap imageToOrient, int degreesToRotate) {
    Bitmap result = imageToOrient;
    try {
      if (degreesToRotate != 0) {
        Matrix matrix = new Matrix();
        matrix.setRotate(degreesToRotate);
        result =
            Bitmap.createBitmap(
                imageToOrient,
                0,
                0,
                imageToOrient.getWidth(),
                imageToOrient.getHeight(),
                matrix,
                true /*filter*/);
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
   * @param pool A pool that may or may not contain an image of the necessary dimensions.
   * @param inBitmap The bitmap to rotate/flip.
   * @param exifOrientation the exif orientation [1-8].
   * @return The rotated and/or flipped image or toOrient if no rotation or flip was necessary.
   */
  public static Bitmap rotateImageExif(
      @NonNull BitmapPool pool, @NonNull Bitmap inBitmap, int exifOrientation) {
    if (!isExifOrientationRequired(exifOrientation)) {
      return inBitmap;
    }

    final Matrix matrix = new Matrix();
    initializeMatrixForRotation(exifOrientation, matrix);

    // From Bitmap.createBitmap.
    final RectF newRect = new RectF(0, 0, inBitmap.getWidth(), inBitmap.getHeight());
    matrix.mapRect(newRect);

    final int newWidth = Math.round(newRect.width());
    final int newHeight = Math.round(newRect.height());

    Bitmap.Config config = getNonNullConfig(inBitmap);
    Bitmap result = pool.get(newWidth, newHeight, config);

    matrix.postTranslate(-newRect.left, -newRect.top);

    result.setHasAlpha(inBitmap.hasAlpha());

    applyMatrix(inBitmap, result, matrix);
    return result;
  }

  /**
   * Returns {@code true} if the given exif orientation indicates that a transformation is necessary
   * and {@code false} otherwise.
   */
  public static boolean isExifOrientationRequired(int exifOrientation) {
    switch (exifOrientation) {
      case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
      case ExifInterface.ORIENTATION_ROTATE_180:
      case ExifInterface.ORIENTATION_FLIP_VERTICAL:
      case ExifInterface.ORIENTATION_TRANSPOSE:
      case ExifInterface.ORIENTATION_ROTATE_90:
      case ExifInterface.ORIENTATION_TRANSVERSE:
      case ExifInterface.ORIENTATION_ROTATE_270:
        return true;
      default:
        return false;
    }
  }

  /**
   * Crop the image to a circle and resize to the specified width/height. The circle crop will have
   * the same width and height equal to the min-edge of the result image.
   *
   * @param pool The BitmapPool obtain a bitmap from.
   * @param inBitmap The Bitmap to resize.
   * @param destWidth The width in pixels of the final Bitmap.
   * @param destHeight The height in pixels of the final Bitmap.
   * @return The resized Bitmap (will be recycled if recycled is not null).
   */
  public static Bitmap circleCrop(
      @NonNull BitmapPool pool, @NonNull Bitmap inBitmap, int destWidth, int destHeight) {
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

    Bitmap.Config outConfig = getAlphaSafeConfig(inBitmap);
    Bitmap result = pool.get(destMinEdge, destMinEdge, outConfig);
    result.setHasAlpha(true);

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

  private static Bitmap getAlphaSafeBitmap(
      @NonNull BitmapPool pool, @NonNull Bitmap maybeAlphaSafe) {
    Bitmap.Config safeConfig = getAlphaSafeConfig(maybeAlphaSafe);
    if (safeConfig.equals(maybeAlphaSafe.getConfig())) {
      return maybeAlphaSafe;
    }

    Bitmap argbBitmap = pool.get(maybeAlphaSafe.getWidth(), maybeAlphaSafe.getHeight(), safeConfig);
    new Canvas(argbBitmap).drawBitmap(maybeAlphaSafe, 0 /*left*/, 0 /*top*/, null /*paint*/);

    // We now own this Bitmap. It's our responsibility to replace it in the pool outside this method
    // when we're finished with it.
    return argbBitmap;
  }

  @NonNull
  private static Config getAlphaSafeConfig(@NonNull Bitmap inBitmap) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      // Avoid short circuiting the sdk check.
      if (Bitmap.Config.RGBA_F16.equals(inBitmap.getConfig())) { // NOPMD
        return Bitmap.Config.RGBA_F16;
      }
    }

    return Bitmap.Config.ARGB_8888;
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
   * @deprecated Width and height are unused and ignored. Use {@link #roundedCorners(BitmapPool,
   *     Bitmap, int)} instead.
   */
  @Deprecated
  public static Bitmap roundedCorners(
      @NonNull BitmapPool pool,
      @NonNull Bitmap inBitmap,
      @SuppressWarnings("unused") int width,
      @SuppressWarnings("unused") int height,
      int roundingRadius) {
    return roundedCorners(pool, inBitmap, roundingRadius);
  }

  /**
   * Creates a bitmap from a source bitmap and rounds the corners.
   *
   * <p>This method does <em>NOT</em> resize the given {@link Bitmap}, it only rounds it's corners.
   * To both resize and round the corners of an image, consider {@link
   * com.bumptech.glide.request.RequestOptions#transform(Transformation[])} and/or {@link
   * com.bumptech.glide.load.MultiTransformation}.
   *
   * @param inBitmap the source bitmap to use as a basis for the created bitmap.
   * @param roundingRadius the corner radius to be applied (in device-specific pixels).
   * @return a {@link Bitmap} similar to inBitmap but with rounded corners.
   * @throws IllegalArgumentException if roundingRadius, width or height is 0 or less.
   */
  public static Bitmap roundedCorners(
      @NonNull BitmapPool pool, @NonNull Bitmap inBitmap, final int roundingRadius) {
    Preconditions.checkArgument(roundingRadius > 0, "roundingRadius must be greater than 0.");

    return roundedCorners(
        pool,
        inBitmap,
        new DrawRoundedCornerFn() {
          @Override
          public void drawRoundedCorners(Canvas canvas, Paint paint, RectF rect) {
            canvas.drawRoundRect(rect, roundingRadius, roundingRadius, paint);
          }
        });
  }

  /**
   * Creates a bitmap from a source bitmap and rounds the corners, applying a potentially different
   * [X, Y] radius to each corner.
   *
   * <p>This method does <em>NOT</em> resize the given {@link Bitmap}, it only rounds it's corners.
   * To both resize and round the corners of an image, consider {@link
   * com.bumptech.glide.request.RequestOptions#transform(Transformation[])} and/or {@link
   * com.bumptech.glide.load.MultiTransformation}.
   *
   * @param inBitmap the source bitmap to use as a basis for the created bitmap.
   * @param topLeft top-left radius
   * @param topRight top-right radius
   * @param bottomRight bottom-right radius
   * @param bottomLeft bottom-left radius
   * @return a {@link Bitmap} similar to inBitmap but with rounded corners.
   */
  public static Bitmap roundedCorners(
      @NonNull BitmapPool pool,
      @NonNull Bitmap inBitmap,
      final float topLeft,
      final float topRight,
      final float bottomRight,
      final float bottomLeft) {
    return roundedCorners(
        pool,
        inBitmap,
        new DrawRoundedCornerFn() {
          @Override
          public void drawRoundedCorners(Canvas canvas, Paint paint, RectF rect) {
            Path path = new Path();
            path.addRoundRect(
                rect,
                new float[] {
                  topLeft,
                  topLeft,
                  topRight,
                  topRight,
                  bottomRight,
                  bottomRight,
                  bottomLeft,
                  bottomLeft
                },
                Path.Direction.CW);
            canvas.drawPath(path, paint);
          }
        });
  }

  private static Bitmap roundedCorners(
      @NonNull BitmapPool pool, @NonNull Bitmap inBitmap, DrawRoundedCornerFn drawRoundedCornerFn) {

    // Alpha is required for this transformation.
    Bitmap.Config safeConfig = getAlphaSafeConfig(inBitmap);
    Bitmap toTransform = getAlphaSafeBitmap(pool, inBitmap);
    Bitmap result = pool.get(toTransform.getWidth(), toTransform.getHeight(), safeConfig);

    result.setHasAlpha(true);

    BitmapShader shader =
        new BitmapShader(toTransform, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
    Paint paint = new Paint();
    paint.setAntiAlias(true);
    paint.setShader(shader);
    RectF rect = new RectF(0, 0, result.getWidth(), result.getHeight());
    BITMAP_DRAWABLE_LOCK.lock();
    try {
      Canvas canvas = new Canvas(result);
      canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
      drawRoundedCornerFn.drawRoundedCorners(canvas, paint, rect);
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

  @NonNull
  private static Bitmap.Config getNonNullConfig(@NonNull Bitmap bitmap) {
    return bitmap.getConfig() != null ? bitmap.getConfig() : Bitmap.Config.ARGB_8888;
  }

  private static void applyMatrix(
      @NonNull Bitmap inBitmap, @NonNull Bitmap targetBitmap, Matrix matrix) {
    BITMAP_DRAWABLE_LOCK.lock();
    try {
      Canvas canvas = new Canvas(targetBitmap);
      canvas.drawBitmap(inBitmap, matrix, DEFAULT_PAINT);
      clear(canvas);
    } finally {
      BITMAP_DRAWABLE_LOCK.unlock();
    }
  }

  @VisibleForTesting
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

  /** Convenience function for drawing a rounded bitmap. */
  private interface DrawRoundedCornerFn {

    void drawRoundedCorners(Canvas canvas, Paint paint, RectF rect);
  }

  private static final class NoLock implements Lock {

    @Synthetic
    NoLock() {}

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
