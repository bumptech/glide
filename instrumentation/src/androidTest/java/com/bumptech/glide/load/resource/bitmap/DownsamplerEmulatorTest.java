package com.bumptech.glide.load.resource.bitmap;

import static android.graphics.Bitmap.CompressFormat.JPEG;
import static android.graphics.Bitmap.CompressFormat.PNG;
import static android.graphics.Bitmap.CompressFormat.WEBP;
import static android.os.Build.VERSION_CODES.KITKAT;
import static com.bumptech.glide.load.resource.bitmap.DownsamplerEmulatorTest.Api.apis;
import static com.bumptech.glide.load.resource.bitmap.DownsamplerEmulatorTest.Api.atAndAbove;
import static com.bumptech.glide.load.resource.bitmap.DownsamplerEmulatorTest.Api.below;
import static com.bumptech.glide.load.resource.bitmap.DownsamplerEmulatorTest.Api.onAllApisAndAllFormatsExpect;
import static com.bumptech.glide.load.resource.bitmap.DownsamplerEmulatorTest.Formats.Builder.allFormats;
import static com.bumptech.glide.load.resource.bitmap.DownsamplerEmulatorTest.Formats.Builder.formats;
import static org.junit.Assert.fail;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.util.DisplayMetrics;
import androidx.annotation.Nullable;
import androidx.exifinterface.media.ExifInterface;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.bumptech.glide.load.ImageHeaderParser;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.engine.bitmap_recycle.ArrayPool;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPoolAdapter;
import com.bumptech.glide.load.engine.bitmap_recycle.LruArrayPool;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.util.Preconditions;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Runs tests to make sure that DownsampleStrategy provides the output we expect.
 *
 * <p>WEBP at and above N rounds. Webp below N floors. PNG always floors. JPEG always rounds.
 */
@RunWith(AndroidJUnit4.class)
@SuppressWarnings("VisibleForTests")
public class DownsamplerEmulatorTest {

  @Test
  public void calculateScaling_withAtMost() throws IOException {
    new Tester(DownsampleStrategy.AT_MOST)
        // See #3673
        .setTargetDimensions(1977, 2636)
        .givenImageWithDimensionsOf(3024, 4032, onAllApisAndAllFormatsExpect(1512, 2016))
        .setTargetDimensions(100, 100)
        .givenSquareImageWithDimensionOf(100, onAllApisAndAllFormatsExpect(100, 100))
        .givenSquareImageWithDimensionOf(200, onAllApisAndAllFormatsExpect(100, 100))
        .givenSquareImageWithDimensionOf(400, onAllApisAndAllFormatsExpect(100, 100))
        .givenSquareImageWithDimensionOf(300, onAllApisAndAllFormatsExpect(75, 75))
        .givenImageWithDimensionsOf(
            799,
            100,
            atAndAbove(VERSION_CODES.N)
                .with(formats(JPEG, WEBP).expect(100, 13), formats(PNG).expect(99, 12)),
            below(VERSION_CODES.N)
                .with(formats(JPEG).expect(100, 13), formats(PNG, WEBP).expect(99, 12)))
        .givenImageWithDimensionsOf(
            800,
            100,
            atAndAbove(VERSION_CODES.N)
                .with(formats(JPEG, WEBP).expect(100, 13), formats(PNG).expect(100, 12)),
            below(VERSION_CODES.N)
                .with(formats(JPEG).expect(100, 13), formats(PNG, WEBP).expect(100, 12)))
        .givenImageWithDimensionsOf(801, 100, onAllApisAndAllFormatsExpect(50, 6))
        .givenImageWithDimensionsOf(
            100,
            800,
            atAndAbove(VERSION_CODES.N)
                .with(formats(JPEG, WEBP).expect(13, 100), formats(PNG).expect(12, 100)),
            below(VERSION_CODES.N)
                .with(formats(JPEG).expect(13, 100), formats(PNG, WEBP).expect(12, 100)))
        .givenImageWithDimensionsOf(
            801,
            100,
            below(KITKAT)
                .with(
                    // JPEG is correct because CENTER_INSIDE wants to give a subsequent
                    // transformation an image that is greater in size than the requested size. On
                    // Api > VERSION_CODES.KITKAT, CENTER_INSIDE can do the transformation itself.
                    // On < VERSION_CODES.KITKAT, it has to assume a subsequent transformation will
                    // be called.
                    formats(JPEG).expect(50, 6), formats(PNG, WEBP).expect(50, 6)))
        .givenImageWithDimensionsOf(87, 78, onAllApisAndAllFormatsExpect(87, 78))
        // This set of examples demonstrate that webp uses round on N+ and floor < N.
        .setTargetDimensions(13, 13)
        .givenSquareImageWithDimensionOf(
            99,
            atAndAbove(KITKAT)
                .with(
                    // 99 / 8.0 = 12.375. ceil(12.375) = 13. round(12.375) = 12. floor(12.375) = 12.
                    formats(JPEG).expect(13, 13), formats(PNG, WEBP).expect(12, 12)),
            below(KITKAT).with(formats(JPEG).expect(13, 13), formats(PNG, WEBP).expect(12, 12)))
        .givenSquareImageWithDimensionOf(
            100,
            atAndAbove(VERSION_CODES.N)
                .with(
                    // 100 / 8.0 = 12.5. ceil(12.5) = 13. round(12.5) = 13. floor(12.5) = 12.
                    formats(JPEG, WEBP).expect(13, 13), formats(PNG).expect(12, 12)),
            below(VERSION_CODES.N)
                .with(formats(JPEG).expect(13, 13), formats(PNG, WEBP).expect(12, 12)))
        // Upscaling
        .setTargetDimensions(500, 500)
        .givenSquareImageWithDimensionOf(200, onAllApisAndAllFormatsExpect(200, 200))
        .givenSquareImageWithDimensionOf(450, onAllApisAndAllFormatsExpect(450, 450))
        .givenImageWithDimensionsOf(200, 450, onAllApisAndAllFormatsExpect(200, 450))
        // Original scaling
        .setTargetDimensions(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
        .givenImageWithDimensionsOf(1821, 2634, onAllApisAndAllFormatsExpect(1821, 2634))
        .run();
  }

  @Test
  public void calculateScaling_withAtLeast() throws IOException {
    new Tester(DownsampleStrategy.AT_LEAST)
        // See #3673
        .setTargetDimensions(1977, 2636)
        .givenImageWithDimensionsOf(3024, 4032, onAllApisAndAllFormatsExpect(3024, 4032))
        .setTargetDimensions(100, 100)
        .givenSquareImageWithDimensionOf(100, onAllApisAndAllFormatsExpect(100, 100))
        .givenSquareImageWithDimensionOf(200, onAllApisAndAllFormatsExpect(100, 100))
        .givenSquareImageWithDimensionOf(300, onAllApisAndAllFormatsExpect(150, 150))
        .givenImageWithDimensionsOf(799, 100, onAllApisAndAllFormatsExpect(799, 100))
        .givenImageWithDimensionsOf(800, 100, onAllApisAndAllFormatsExpect(800, 100))
        .givenImageWithDimensionsOf(801, 100, onAllApisAndAllFormatsExpect(801, 100))
        .givenImageWithDimensionsOf(100, 800, onAllApisAndAllFormatsExpect(100, 800))
        .givenImageWithDimensionsOf(87, 78, onAllApisAndAllFormatsExpect(87, 78))
        // Upscaling
        .setTargetDimensions(500, 500)
        .givenSquareImageWithDimensionOf(200, onAllApisAndAllFormatsExpect(200, 200))
        .givenSquareImageWithDimensionOf(450, onAllApisAndAllFormatsExpect(450, 450))
        .givenImageWithDimensionsOf(200, 450, onAllApisAndAllFormatsExpect(200, 450))
        // Original scaling
        .setTargetDimensions(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
        .givenImageWithDimensionsOf(1821, 2634, onAllApisAndAllFormatsExpect(1821, 2634))
        .run();
  }

  @Test
  public void calculateScaling_withCenterInside() throws IOException {
    new Tester(DownsampleStrategy.CENTER_INSIDE)
        // See #3673
        .setTargetDimensions(1977, 2636)
        .givenImageWithDimensionsOf(
            3024,
            4032,
            atAndAbove(KITKAT).with(allFormats().expect(1977, 2636)),
            below(KITKAT).with(allFormats().expect(3024, 4032)))
        .setTargetDimensions(100, 100)
        .givenSquareImageWithDimensionOf(100, onAllApisAndAllFormatsExpect(100, 100))
        .givenSquareImageWithDimensionOf(400, onAllApisAndAllFormatsExpect(100, 100))
        .givenImageWithDimensionsOf(
            300,
            300,
            atAndAbove(KITKAT).with(allFormats().expect(100, 100)),
            below(KITKAT).with(allFormats().expect(150, 150)))
        .givenImageWithDimensionsOf(
            799,
            100,
            atAndAbove(KITKAT).with(allFormats().expect(100, 13)),
            below(KITKAT).with(formats(JPEG).expect(200, 25), formats(PNG, WEBP).expect(199, 25)))
        .givenImageWithDimensionsOf(
            800,
            100,
            atAndAbove(KITKAT).with(allFormats().expect(100, 13)),
            below(KITKAT).with(formats(JPEG).expect(100, 13), formats(PNG, WEBP).expect(100, 12)))
        .givenImageWithDimensionsOf(
            801,
            100,
            atAndAbove(VERSION_CODES.N)
                .with(formats(JPEG, WEBP).expect(100, 13), formats(PNG).expect(100, 12)),
            apis(KITKAT, VERSION_CODES.M)
                .with(formats(JPEG).expect(100, 13), formats(PNG, WEBP).expect(100, 12)),
            below(KITKAT)
                .with(
                    // JPEG is correct because CENTER_INSIDE wants to give a subsequent
                    // transformation an image that is greater in size than the requested size. On
                    // Api > VERSION_CODES.KITKAT, CENTER_INSIDE can do the transformation itself.
                    // On < VERSION_CODES.KITKAT, it has to assume a subsequent transformation will
                    // be called.
                    formats(JPEG).expect(101, 13), formats(PNG, WEBP).expect(100, 12)))
        .givenImageWithDimensionsOf(
            100,
            800,
            atAndAbove(KITKAT).with(allFormats().expect(13, 100)),
            below(KITKAT).with(formats(JPEG).expect(13, 100), formats(PNG, WEBP).expect(12, 100)))
        .givenImageWithDimensionsOf(87, 78, onAllApisAndAllFormatsExpect(87, 78))
        .setTargetDimensions(897, 897)
        .givenImageWithDimensionsOf(
            2208,
            1520,
            atAndAbove(KITKAT).with(allFormats().expect(897, 618)),
            below(KITKAT).with(allFormats().expect(1104, 760)))
        // Upscaling
        .setTargetDimensions(500, 500)
        .givenSquareImageWithDimensionOf(200, onAllApisAndAllFormatsExpect(200, 200))
        .givenSquareImageWithDimensionOf(450, onAllApisAndAllFormatsExpect(450, 450))
        .givenImageWithDimensionsOf(200, 450, onAllApisAndAllFormatsExpect(200, 450))
        // Original scaling
        .setTargetDimensions(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
        .givenImageWithDimensionsOf(1821, 2634, onAllApisAndAllFormatsExpect(1821, 2634))
        .run();
  }

  @Test
  public void calculateScaling_withCenterOutside() throws IOException {
    new Tester(DownsampleStrategy.CENTER_OUTSIDE)
        // See #3673
        .setTargetDimensions(1977, 2636)
        .givenImageWithDimensionsOf(
            3024,
            4032,
            atAndAbove(KITKAT).with(allFormats().expect(1977, 2636)),
            below(KITKAT).with(allFormats().expect(3024, 4032)))
        .setTargetDimensions(100, 100)
        .givenSquareImageWithDimensionOf(100, onAllApisAndAllFormatsExpect(100, 100))
        .givenSquareImageWithDimensionOf(200, onAllApisAndAllFormatsExpect(100, 100))
        .givenSquareImageWithDimensionOf(400, onAllApisAndAllFormatsExpect(100, 100))
        .givenImageWithDimensionsOf(
            300,
            300,
            atAndAbove(KITKAT).with(allFormats().expect(100, 100)),
            below(KITKAT).with(allFormats().expect(150, 150)))
        .givenImageWithDimensionsOf(799, 100, onAllApisAndAllFormatsExpect(799, 100))
        .givenImageWithDimensionsOf(800, 100, onAllApisAndAllFormatsExpect(800, 100))
        .givenImageWithDimensionsOf(801, 100, onAllApisAndAllFormatsExpect(801, 100))
        .givenImageWithDimensionsOf(100, 800, onAllApisAndAllFormatsExpect(100, 800))
        .givenImageWithDimensionsOf(
            87,
            78,
            atAndAbove(KITKAT).with(allFormats().expect(112, 100)),
            below(KITKAT).with(allFormats().expect(87, 78)))
        // Upscaling
        .setTargetDimensions(500, 500)
        .givenSquareImageWithDimensionOf(
            200,
            atAndAbove(KITKAT).with(allFormats().expect(500, 500)),
            below(KITKAT).with(allFormats().expect(200, 200)))
        .givenSquareImageWithDimensionOf(
            450,
            atAndAbove(KITKAT).with(allFormats().expect(500, 500)),
            below(KITKAT).with(allFormats().expect(450, 450)))
        .givenImageWithDimensionsOf(
            200,
            450,
            atAndAbove(KITKAT).with(allFormats().expect(500, 1125)),
            below(KITKAT).with(allFormats().expect(200, 450)))
        // Original scaling
        .setTargetDimensions(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
        .givenImageWithDimensionsOf(1821, 2634, onAllApisAndAllFormatsExpect(1821, 2634))
        .run();
  }

  @Test
  public void calculateScaling_withNone() throws IOException {
    new Tester(DownsampleStrategy.NONE)
        // See #3673
        .setTargetDimensions(1977, 2636)
        .givenImageWithDimensionsOf(3024, 4032, onAllApisAndAllFormatsExpect(3024, 4032))
        .setTargetDimensions(100, 100)
        .givenSquareImageWithDimensionOf(100, onAllApisAndAllFormatsExpect(100, 100))
        .givenSquareImageWithDimensionOf(200, onAllApisAndAllFormatsExpect(200, 200))
        .givenSquareImageWithDimensionOf(400, onAllApisAndAllFormatsExpect(400, 400))
        .givenSquareImageWithDimensionOf(300, onAllApisAndAllFormatsExpect(300, 300))
        .givenImageWithDimensionsOf(799, 100, onAllApisAndAllFormatsExpect(799, 100))
        .givenImageWithDimensionsOf(800, 100, onAllApisAndAllFormatsExpect(800, 100))
        .givenImageWithDimensionsOf(801, 100, onAllApisAndAllFormatsExpect(801, 100))
        .givenImageWithDimensionsOf(100, 800, onAllApisAndAllFormatsExpect(100, 800))
        .givenImageWithDimensionsOf(87, 78, onAllApisAndAllFormatsExpect(87, 78))
        .setTargetDimensions(500, 500)
        .givenSquareImageWithDimensionOf(200, onAllApisAndAllFormatsExpect(200, 200))
        .givenSquareImageWithDimensionOf(450, onAllApisAndAllFormatsExpect(450, 450))
        .givenImageWithDimensionsOf(200, 450, onAllApisAndAllFormatsExpect(200, 450))
        // Original scaling
        .setTargetDimensions(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
        .givenImageWithDimensionsOf(1821, 2634, onAllApisAndAllFormatsExpect(1821, 2634))
        .run();
  }

  @Test
  public void calculateScaling_withFitCenter() throws IOException {
    new Tester(DownsampleStrategy.FIT_CENTER)
        // See #3673
        .setTargetDimensions(1977, 2636)
        .givenImageWithDimensionsOf(
            3024,
            4032,
            atAndAbove(KITKAT).with(allFormats().expect(1977, 2636)),
            below(KITKAT).with(allFormats().expect(3024, 4032)))
        .setTargetDimensions(100, 100)
        .givenSquareImageWithDimensionOf(100, onAllApisAndAllFormatsExpect(100, 100))
        .givenSquareImageWithDimensionOf(200, onAllApisAndAllFormatsExpect(100, 100))
        .givenSquareImageWithDimensionOf(400, onAllApisAndAllFormatsExpect(100, 100))
        .givenImageWithDimensionsOf(
            300,
            300,
            atAndAbove(KITKAT).with(allFormats().expect(100, 100)),
            below(KITKAT).with(allFormats().expect(150, 150)))
        .givenImageWithDimensionsOf(
            799,
            100,
            atAndAbove(KITKAT).with(allFormats().expect(100, 13)),
            below(KITKAT).with(formats(JPEG).expect(200, 25), formats(PNG, WEBP).expect(199, 25)))
        .givenImageWithDimensionsOf(
            800,
            100,
            atAndAbove(KITKAT).with(allFormats().expect(100, 13)),
            below(KITKAT).with(formats(JPEG).expect(100, 13), formats(PNG, WEBP).expect(100, 12)))
        .givenImageWithDimensionsOf(
            801,
            100,
            atAndAbove(VERSION_CODES.N)
                .with(formats(JPEG, WEBP).expect(100, 13), formats(PNG).expect(100, 12)),
            apis(KITKAT, VERSION_CODES.M)
                .with(formats(JPEG).expect(100, 13), formats(PNG, WEBP).expect(100, 12)),
            below(KITKAT)
                .with(
                    // JPEG is correct because FIT_CENTER wants to give a subsequent transformation
                    // an image that is greater in size than the requested size. On
                    // Api > VERSION_CODES.KITKAT, FIT_CENTER can do the transformation itself.
                    // On < VERSION_CODES.KITKAT, it has to assume a transformation will be run
                    // after it that will fix the rounding error.
                    formats(JPEG).expect(101, 13), formats(PNG, WEBP).expect(100, 12)))
        .givenImageWithDimensionsOf(
            100,
            800,
            atAndAbove(KITKAT).with(allFormats().expect(13, 100)),
            below(KITKAT).with(formats(JPEG).expect(13, 100), formats(PNG, WEBP).expect(12, 100)))
        .givenImageWithDimensionsOf(
            87,
            78,
            atAndAbove(KITKAT).with(allFormats().expect(100, 90)),
            below(KITKAT).with(allFormats().expect(87, 78)))
        .setTargetDimensions(897, 897)
        .givenImageWithDimensionsOf(
            2208,
            1520,
            atAndAbove(KITKAT).with(allFormats().expect(897, 618)),
            below(KITKAT).with(allFormats().expect(1104, 760)))
        .setTargetDimensions(270, 270)
        // This set of larger image examples exercises sample sizes > 8. Android' scaling logic
        // varies for jpegs.
        .givenImageWithDimensionsOf(
            9014,
            1638,
            // 15 and 16 will OOM so don't run them.
            atAndAbove(KITKAT).with(allFormats().expect(270, 49)),
            apis(VERSION_CODES.JELLY_BEAN_MR1, VERSION_CODES.JELLY_BEAN_MR2)
                .with(allFormats().expect(281, 51)))
        .givenImageWithDimensionsOf(
            1638,
            9014,
            // 15 and 16 will OOM so don't run them.
            atAndAbove(KITKAT).with(allFormats().expect(49, 270)),
            apis(VERSION_CODES.JELLY_BEAN_MR1, VERSION_CODES.JELLY_BEAN_MR2)
                .with(allFormats().expect(51, 281)))
        .givenImageWithDimensionsOf(
            1638,
            1638,
            atAndAbove(KITKAT).with(allFormats().expect(270, 270)),
            below(KITKAT).with(formats(JPEG).expect(410, 410), formats(PNG, WEBP).expect(409, 409)))
        .givenImageWithDimensionsOf(
            4507,
            819,
            atAndAbove(KITKAT).with(allFormats().expect(270, 49)),
            below(KITKAT).with(formats(JPEG).expect(282, 51), formats(PNG, WEBP).expect(281, 51)))
        .givenImageWithDimensionsOf(
            4503,
            819,
            atAndAbove(KITKAT).with(allFormats().expect(270, 49)),
            below(KITKAT).with(allFormats().expect(281, 51)))
        // Upscaling
        .setTargetDimensions(500, 500)
        .givenSquareImageWithDimensionOf(
            200,
            atAndAbove(KITKAT).with(allFormats().expect(500, 500)),
            below(KITKAT).with(allFormats().expect(200, 200)))
        .givenSquareImageWithDimensionOf(
            450,
            atAndAbove(KITKAT).with(allFormats().expect(500, 500)),
            below(KITKAT).with(allFormats().expect(450, 450)))
        .givenImageWithDimensionsOf(
            200,
            450,
            atAndAbove(KITKAT).with(allFormats().expect(222, 500)),
            below(KITKAT).with(allFormats().expect(200, 450)))
        // Original scaling
        .setTargetDimensions(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
        .givenImageWithDimensionsOf(1821, 2634, onAllApisAndAllFormatsExpect(1821, 2634))
        .run();
  }

  /** Returns an error string if the test failed, and {@code null} otherwise. */
  @Nullable
  private static String runScaleTest(
      CompressFormat format,
      int initialWidth,
      int initialHeight,
      int targetWidth,
      int targetHeight,
      int exifOrientation,
      DownsampleStrategy strategy,
      int expectedWidth,
      int expectedHeight)
      throws IOException {
    Downsampler downsampler = buildDownsampler();

    InputStream is = openBitmapStream(format, initialWidth, initialHeight, exifOrientation);
    Options options = new Options().set(DownsampleStrategy.OPTION, strategy);
    Bitmap bitmap;
    try {
      bitmap = downsampler.decode(is, targetWidth, targetHeight, options).get();
    } catch (OutOfMemoryError e) {
      return "API: "
          + Build.VERSION.SDK_INT
          + ", os: "
          + Build.VERSION.RELEASE
          + ", format: "
          + format
          + ", strategy: "
          + strategy
          + ", orientation: "
          + exifOrientation
          + " -"
          + " Initial "
          + readableDimens(initialWidth, initialHeight)
          + " Target "
          + readableDimens(targetWidth, targetHeight)
          + " Expected "
          + readableDimens(expectedWidth, expectedHeight)
          + " but threw OutOfMemoryError";
    }
    try {
      if (bitmap.getWidth() != expectedWidth || bitmap.getHeight() != expectedHeight) {
        return "API: "
            + Build.VERSION.SDK_INT
            + ", os: "
            + Build.VERSION.RELEASE
            + ", format: "
            + format
            + ", strategy: "
            + strategy
            + ", orientation: "
            + exifOrientation
            + " -"
            + " Initial "
            + readableDimens(initialWidth, initialHeight)
            + " Target "
            + readableDimens(targetWidth, targetHeight)
            + " Expected "
            + readableDimens(expectedWidth, expectedHeight)
            + ", but Received "
            + readableDimens(bitmap.getWidth(), bitmap.getHeight());
      }
    } finally {
      bitmap.recycle();
    }
    return null;
  }

  private static String readableDimens(int width, int height) {
    return "[" + width + "x" + height + "]";
  }

  private static Downsampler buildDownsampler() {
    List<ImageHeaderParser> parsers =
        Collections.<ImageHeaderParser>singletonList(new DefaultImageHeaderParser());
    DisplayMetrics displayMetrics = new DisplayMetrics();
    // XHDPI.
    displayMetrics.densityDpi = 320;
    BitmapPool bitmapPool = new BitmapPoolAdapter();
    ArrayPool arrayPool = new LruArrayPool();
    return new Downsampler(parsers, displayMetrics, bitmapPool, arrayPool);
  }

  private static InputStream openBitmapStream(
      CompressFormat format, int width, int height, int exifOrientation) {
    Preconditions.checkArgument(
        format == CompressFormat.JPEG || exifOrientation == ExifInterface.ORIENTATION_UNDEFINED,
        "Can only orient JPEGs, but asked for orientation: "
            + exifOrientation
            + " with format: "
            + format);

    // TODO: support orientations for formats other than JPEG.
    if (format == CompressFormat.JPEG) {
      return openFileStream(width, height, exifOrientation);
    } else {
      return openInMemoryStream(format, width, height);
    }
  }

  private static InputStream openFileStream(int width, int height, int exifOrientation) {
    int rotationDegrees = TransformationUtils.getExifOrientationDegrees(exifOrientation);
    if (rotationDegrees == 270 || rotationDegrees == 90) {
      int temp = width;
      width = height;
      height = temp;
    }

    Bitmap bitmap = Bitmap.createBitmap(width, height, Config.ARGB_8888);

    OutputStream os = null;
    try {
      File tempFile =
          File.createTempFile(
              "ds-" + width + "-" + height + "-" + exifOrientation,
              ".jpeg",
              ApplicationProvider.getApplicationContext().getCacheDir());
      os = new BufferedOutputStream(new FileOutputStream(tempFile));
      bitmap.compress(CompressFormat.JPEG, /*quality=*/ 100, os);
      bitmap.recycle();
      os.close();

      ExifInterface exifInterface = new ExifInterface(tempFile.getAbsolutePath());
      exifInterface.setAttribute(ExifInterface.TAG_ORIENTATION, String.valueOf(exifOrientation));
      exifInterface.saveAttributes();

      InputStream result = new BufferedInputStream(new FileInputStream(tempFile));
      if (!tempFile.delete()) {
        throw new IllegalStateException("Failed to delete: " + tempFile);
      }
      return result;
    } catch (IOException e) {
      throw new IllegalStateException(e);
    } finally {
      if (os != null) {
        try {
          os.close();
        } catch (IOException e) {
        }
      }
    }
  }

  private static InputStream openInMemoryStream(CompressFormat format, int width, int height) {
    Bitmap bitmap = Bitmap.createBitmap(width, height, Config.ARGB_8888);
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    bitmap.compress(format, 100 /*quality*/, os);
    bitmap.recycle();
    byte[] data = os.toByteArray();
    return new ByteArrayInputStream(data);
  }

  static final class Tester {
    private final DownsampleStrategy strategy;
    private final List<TestCase> testCases = new ArrayList<>();

    private int targetWidth;
    private int targetHeight;

    Tester(DownsampleStrategy strategy) {
      this.strategy = strategy;
    }

    Tester setTargetDimensions(int targetWidth, int targetHeight) {
      this.targetWidth = targetWidth;
      this.targetHeight = targetHeight;
      return this;
    }

    Tester givenSquareImageWithDimensionOf(int dimension, Api... apis) {
      return givenImageWithDimensionsOf(dimension, dimension, apis);
    }

    Tester givenImageWithDimensionsOf(int sourceWidth, int sourceHeight, Api... apis) {
      testCases.add(new TestCase(sourceWidth, sourceHeight, targetWidth, targetHeight, apis));
      return this;
    }

    void run() throws IOException {
      List<String> results = new ArrayList<>();
      for (TestCase testCase : testCases) {
        results.addAll(testCase.test(strategy));
      }

      if (results.isEmpty()) {
        return;
      }

      StringBuilder failure = new StringBuilder("Failing Tests:\n");
      for (String result : results) {
        failure.append(result).append("\n");
      }
      fail(failure.substring(0, failure.length() - 1));
    }

    private static final class TestCase {
      private final int sourceWidth;
      private final int sourceHeight;
      private final int targetWidth;
      private final int targetHeight;
      private final Api[] apis;

      TestCase(int sourceWidth, int sourceHeight, int targetWidth, int targetHeight, Api... apis) {
        this.sourceWidth = sourceWidth;
        this.sourceHeight = sourceHeight;
        this.targetWidth = targetWidth;
        this.targetHeight = targetHeight;
        this.apis = apis;
      }

      List<String> test(DownsampleStrategy strategy) throws IOException {
        List<String> results = new ArrayList<>();
        for (Api api : apis) {
          results.addAll(api.test(sourceWidth, sourceHeight, targetWidth, targetHeight, strategy));
        }
        return results;
      }
    }
  }

  static final class Api {
    private final int startVersion;
    private final int stopVersion;
    private final Formats[] formats;

    static Builder apis(int min, int max) {
      return new Builder().min(min).max(max);
    }

    static Builder atAndAbove(int min) {
      return new Builder().min(min);
    }

    static Builder below(int max) {
      // max is inclusive.
      return new Builder().max(max - 1);
    }

    static Builder allApis() {
      return new Builder();
    }

    static Api onAllApisAndAllFormatsExpect(int width, int height) {
      return allApis().with(allFormats().expect(width, height));
    }

    static final class Builder {
      private int maxVersion = Integer.MAX_VALUE;
      private int minVersion = Integer.MIN_VALUE;

      Builder min(int version) {
        minVersion = version;
        return this;
      }

      Builder max(int version) {
        this.maxVersion = version;
        return this;
      }

      Api with(Formats... formats) {
        return new Api(minVersion, maxVersion, formats);
      }
    }

    Api(int startVersion, int stopVersion, Formats... formats) {
      this.startVersion = startVersion;
      this.stopVersion = stopVersion;
      this.formats = formats;
    }

    List<String> test(
        int sourceWidth,
        int sourceHeight,
        int targetWidth,
        int targetHeight,
        DownsampleStrategy strategy)
        throws IOException {
      if (Build.VERSION.SDK_INT < startVersion || Build.VERSION.SDK_INT > stopVersion) {
        return Collections.emptyList();
      }

      List<String> results = new ArrayList<>();
      for (Formats format : formats) {
        results.addAll(
            format.runTest(sourceWidth, sourceHeight, targetWidth, targetHeight, strategy));
      }
      return results;
    }
  }

  static final class Formats {
    private final int expectedWidth;
    private final int expectedHeight;
    private final CompressFormat[] formats;
    private static final int[] ALL_EXIF_ORIENTATIONS =
        new int[] {
          ExifInterface.ORIENTATION_UNDEFINED,
          ExifInterface.ORIENTATION_NORMAL,
          ExifInterface.ORIENTATION_FLIP_HORIZONTAL,
          ExifInterface.ORIENTATION_ROTATE_180,
          ExifInterface.ORIENTATION_FLIP_VERTICAL,
          ExifInterface.ORIENTATION_TRANSPOSE,
          ExifInterface.ORIENTATION_ROTATE_90,
          ExifInterface.ORIENTATION_TRANSVERSE,
          ExifInterface.ORIENTATION_ROTATE_270
        };
    private static final int[] UNDEFINED_EXIF_ORIENTATIONS =
        new int[] {ExifInterface.ORIENTATION_UNDEFINED};

    static final class Builder {
      private final CompressFormat[] formats;

      static Builder allFormats() {
        return formats(CompressFormat.values());
      }

      static Builder formats(CompressFormat... formats) {
        return new Builder(formats);
      }

      Builder(CompressFormat... formats) {
        this.formats = formats;
      }

      Formats expect(int width, int height) {
        return new Formats(formats, width, height);
      }
    }

    Formats(CompressFormat[] formats, int expectedWidth, int expectedHeight) {
      this.formats = formats;
      this.expectedWidth = expectedWidth;
      this.expectedHeight = expectedHeight;
    }

    List<String> runTest(
        int sourceWidth,
        int sourceHeight,
        int targetWidth,
        int targetHeight,
        DownsampleStrategy strategy)
        throws IOException {
      List<String> result = new ArrayList<>();
      for (CompressFormat format : formats) {
        int[] exifOrientations =
            format == CompressFormat.JPEG ? ALL_EXIF_ORIENTATIONS : UNDEFINED_EXIF_ORIENTATIONS;
        for (int exifOrientation : exifOrientations) {
          String testResult =
              runScaleTest(
                  format,
                  sourceWidth,
                  sourceHeight,
                  targetWidth,
                  targetHeight,
                  exifOrientation,
                  strategy,
                  expectedWidth,
                  expectedHeight);
          if (testResult != null) {
            result.add(testResult);
          }
        }
      }
      return result;
    }
  }
}
