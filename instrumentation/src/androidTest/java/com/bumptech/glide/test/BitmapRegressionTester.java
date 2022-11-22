package com.bumptech.glide.test;

import static com.bumptech.glide.testutil.BitmapSubject.assertThat;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Environment;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;
import com.bumptech.glide.RequestBuilder;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import org.junit.rules.TestName;

/**
 * Checks for regressions for a given Glide load by comparing the result of a load to a previously
 * saved Bitmap.
 *
 * <p>Can be used to generate or re-generate expected {@link Bitmap}s by placing a file named
 * "regenerate" in /sdcard/DCIM/test_files. The apks containing this tester will need to have {@link
 * android.Manifest.permission#WRITE_EXTERNAL_STORAGE}. Resources can be split by apk by adding
 * {@link SplitBySdk} to test methods or classes. If {@link SplitBySdk} is added to both a test
 * class and a particular method, the values from the method will be used.
 *
 * <p>This class only handles exactly one Bitmap comparison per test method because the resource
 * names it expects and generates are based on the method name.
 */
public final class BitmapRegressionTester {
  private static final String RESOURCE_TYPE = "raw";
  private static final String EXTENSION = ".png";
  private static final String REGENERATE_SIGNAL_FILE_NAME = "regenerate";
  private static final String GENERATED_FILES_DIR = "test_files";
  private static final String SEPARATOR = "_";

  private final Class<?> testClass;
  private final TestName testName;
  private final Context context = ApplicationProvider.getApplicationContext();

  public BitmapRegressionTester(Class<?> testClass, TestName testName) {
    this.testClass = testClass;
    this.testName = testName;

    if (testClass.getAnnotation(RegressionTest.class) == null) {
      throw new IllegalArgumentException(
          testClass + " must be annotated with " + RegressionTest.class);
    }
  }

  public Bitmap test(RequestBuilder<Bitmap> request)
      throws ExecutionException, InterruptedException {
    Bitmap result = request.submit().get();
    if (writeNewExpected()) {
      writeBitmap(result);
    }
    Bitmap expected = decodeExpected();
    assertThat(result).sameAs(expected);
    return result;
  }

  private String getResourceName() {
    return getClassNameString()
        + SEPARATOR
        + testName.getMethodName().toLowerCase()
        + getSdkIntString()
        + getCpuString();
  }

  private String getClassNameString() {
    StringBuilder result = new StringBuilder();
    for (char c : testClass.getSimpleName().toCharArray()) {
      if (Character.isUpperCase(c)) {
        result.append(Character.toLowerCase(c));
      }
    }
    return result.toString();
  }

  @Nullable
  private SplitBySdk getSplitBySdkValues() {
    SplitBySdk result;
    try {
      Method method =
          testClass.getMethod(testName.getMethodName(), /* parameterTypes...= */ (Class[]) null);
      result = method.getAnnotation(SplitBySdk.class);
    } catch (NoSuchMethodException e) {
      throw new RuntimeException(e);
    }

    if (result == null) {
      result = testClass.getAnnotation(SplitBySdk.class);
    }
    return result;
  }

  private String getCpuString() {
    return splitByCpu() ? SEPARATOR + Build.CPU_ABI.replace("-", "_") : "";
  }

  private boolean splitByCpu() {
    return testClass.getAnnotation(SplitByCpu.class) != null;
  }

  private String getSdkIntString() {
    SplitBySdk splitBySdk = getSplitBySdkValues();
    if (splitBySdk == null) {
      return "";
    }
    int targetSdk = -1;
    int[] values = splitBySdk.value();
    Arrays.sort(values);
    for (int value : values) {
      if (value > Build.VERSION.SDK_INT) {
        break;
      }
      targetSdk = value;
    }

    if (targetSdk == -1) {
      return "";
    }

    return SEPARATOR + targetSdk;
  }

  private File getTestFilesDir() {
    File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
    return new File(dir, GENERATED_FILES_DIR);
  }

  private void writeBitmap(Bitmap bitmap) {
    File testFilesDir = getTestFilesDir();
    File subdirectory = new File(testFilesDir, RESOURCE_TYPE);
    if (!subdirectory.exists() && !subdirectory.mkdirs()) {
      throw new IllegalArgumentException("Failed to make directory: " + subdirectory);
    }

    File file = new File(subdirectory, getResourceName() + EXTENSION);
    if (file.exists() && !file.delete()) {
      throw new IllegalStateException("Failed to remove existing file: " + file);
    }

    OutputStream os = null;
    try {
      os = new BufferedOutputStream(new FileOutputStream(file));
      bitmap.compress(CompressFormat.PNG, /* quality= */ 100, os);
      os.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      if (os != null) {
        try {
          os.close();
        } catch (IOException e) {
          // Ignored.
        }
      }
    }
  }

  private boolean writeNewExpected() {
    File testFiles = getTestFilesDir();
    return new File(testFiles, REGENERATE_SIGNAL_FILE_NAME).exists();
  }

  private Bitmap decodeExpected() {
    int resourceId =
        context
            .getResources()
            .getIdentifier(getResourceName(), RESOURCE_TYPE, context.getPackageName());
    if (resourceId == 0) {
      throw new IllegalArgumentException(
          "Failed to find resource for: "
              + getResourceName()
              + " with type: "
              + RESOURCE_TYPE
              + " and package: "
              + context.getPackageName());
    }
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inScaled = false;
    return BitmapFactory.decodeResource(context.getResources(), resourceId, options);
  }
}
