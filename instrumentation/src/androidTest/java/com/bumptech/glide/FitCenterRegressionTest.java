package com.bumptech.glide;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import android.content.Context;
import android.graphics.Bitmap;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.bumptech.glide.test.BitmapRegressionTester;
import com.bumptech.glide.test.CanonicalBitmap;
import com.bumptech.glide.test.GlideApp;
import com.bumptech.glide.test.RegressionTest;
import com.bumptech.glide.test.SplitByCpu;
import com.bumptech.glide.test.SplitBySdk;
import com.bumptech.glide.testutil.TearDownGlide;
import java.util.concurrent.ExecutionException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SplitByCpu
@SplitBySdk({24, 23, 21, 19, 18, 16})
@RegressionTest
public class FitCenterRegressionTest {
  @Rule public final TestName testName = new TestName();
  @Rule public final TearDownGlide tearDownGlide = new TearDownGlide();
  private BitmapRegressionTester bitmapRegressionTester;
  private Context context;
  private CanonicalBitmap canonical;

  @Before
  public void setUp() {
    context = ApplicationProvider.getApplicationContext();
    bitmapRegressionTester =
        BitmapRegressionTester.newInstance(getClass(), testName).assumeShouldRun();
    canonical = new CanonicalBitmap();
  }

  @Test
  public void fitCenter_withSquareSmallerThanImage_returnsImageFitWithinSquare()
      throws ExecutionException, InterruptedException {

    Bitmap result =
        bitmapRegressionTester.test(
            GlideApp.with(context).asBitmap().load(canonical.getBitmap()).fitCenter().override(50));

    assertThat(result.getWidth()).isEqualTo(50);
    assertThat(result.getHeight()).isEqualTo(37);
  }

  @Test
  public void fitCenter_withSquareLargerThanImage_returnsUpscaledSquare()
      throws ExecutionException, InterruptedException {
    float multiplier = 1.1f;
    int multipliedWidth = (int) (canonical.getWidth() * multiplier);
    int multipliedHeight = (int) (canonical.getHeight() * multiplier);
    Bitmap result =
        bitmapRegressionTester.test(
            GlideApp.with(context)
                .asBitmap()
                .load(canonical.getBitmap())
                .fitCenter()
                .override(multipliedWidth));

    assertThat(result.getWidth()).isEqualTo(multipliedWidth);
    assertThat(result.getHeight()).isEqualTo(multipliedHeight);
  }

  @Test
  public void fitCenter_withNarrowRectangle_fitsWithinMaintainingAspectRatio()
      throws ExecutionException, InterruptedException {
    Bitmap result =
        bitmapRegressionTester.test(
            GlideApp.with(context)
                .asBitmap()
                .load(canonical.getBitmap())
                .fitCenter()
                .override(canonical.getWidth() / 10, canonical.getHeight()));

    assertThat(result.getWidth()).isEqualTo(canonical.getWidth() / 10);
    assertThat(result.getHeight()).isEqualTo(canonical.getHeight() / 10);
  }

  @Test
  public void fitCenter_withShortRectangle_fitsWithinMaintainingAspectRatio()
      throws ExecutionException, InterruptedException {
    Bitmap result =
        bitmapRegressionTester.test(
            GlideApp.with(context)
                .asBitmap()
                .load(canonical.getBitmap())
                .fitCenter()
                .override(canonical.getWidth(), canonical.getHeight() / 2));

    assertThat(result.getWidth()).isEqualTo(canonical.getWidth() / 2);
    assertThat(result.getHeight()).isEqualTo(canonical.getHeight() / 2);
  }

  @Test
  public void fitCenter_withHugeRectangle_throwsOOM()
      throws ExecutionException, InterruptedException {
    float multiplier = Integer.MAX_VALUE / (canonical.getWidth() * canonical.getHeight() * 2);
    final int overrideWidth = (int) multiplier * canonical.getWidth();
    final int overrideHeight = (int) multiplier * canonical.getHeight();

    assertThrows(
        ExecutionException.class,
        new ThrowingRunnable() {
          @Override
          public void run() throws Throwable {
            GlideApp.with(context)
                .asBitmap()
                .load(canonical.getBitmap())
                .fitCenter()
                .override(overrideWidth, overrideHeight)
                .submit()
                .get();
          }
        });
  }
}
