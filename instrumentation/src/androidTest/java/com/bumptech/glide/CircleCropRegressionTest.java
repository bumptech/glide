package com.bumptech.glide;

import static com.google.common.truth.Truth.assertThat;

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
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SplitByCpu
@SplitBySdk({26, 24, 23, 21, 18, 16})
@RegressionTest
public class CircleCropRegressionTest {
  @Rule public final TestName testName = new TestName();
  @Rule public final TearDownGlide tearDownGlide = new TearDownGlide();
  private BitmapRegressionTester bitmapRegressionTester;
  private Context context;
  private CanonicalBitmap canonical;

  @Before
  public void setUp() {
    context = ApplicationProvider.getApplicationContext();
    bitmapRegressionTester = new BitmapRegressionTester(getClass(), testName);
    canonical = new CanonicalBitmap();
  }

  @Test
  public void circleCrop_withSquareSmallerThanImage_returnsSquaredImage()
      throws ExecutionException, InterruptedException {
    Bitmap result =
        bitmapRegressionTester.test(
            GlideApp.with(context)
                .asBitmap()
                .load(canonical.getBitmap())
                .circleCrop()
                .override(50));

    assertThat(result.getWidth()).isEqualTo(50);
    assertThat(result.getHeight()).isEqualTo(50);
  }

  @Test
  public void circleCrop_withSquareLargerThanImage_returnsUpscaledFitImage()
      throws ExecutionException, InterruptedException {
    float multiplier = 1.1f;
    int multipliedWidth = (int) (canonical.getWidth() * multiplier);
    Bitmap result =
        bitmapRegressionTester.test(
            GlideApp.with(context)
                .asBitmap()
                .load(canonical.getBitmap())
                .circleCrop()
                .override(multipliedWidth));

    assertThat(result.getWidth()).isEqualTo(multipliedWidth);
    assertThat(result.getHeight()).isEqualTo(multipliedWidth);
  }

  @Test
  public void circleCrop_withNarrowRectangle_cropsWithin()
      throws ExecutionException, InterruptedException {
    Bitmap result =
        bitmapRegressionTester.test(
            GlideApp.with(context)
                .asBitmap()
                .load(canonical.getBitmap())
                .circleCrop()
                .override(canonical.getWidth() / 10, canonical.getHeight()));

    assertThat(result.getWidth()).isEqualTo(canonical.getWidth() / 10);
    assertThat(result.getHeight()).isEqualTo(canonical.getWidth() / 10);
  }

  @Test
  public void circleCrop_withShortRectangle_fitsWithinMaintainingAspectRatio()
      throws ExecutionException, InterruptedException {
    Bitmap result =
        bitmapRegressionTester.test(
            GlideApp.with(context)
                .asBitmap()
                .load(canonical.getBitmap())
                .circleCrop()
                .override(canonical.getWidth(), canonical.getHeight() / 2));

    assertThat(result.getWidth()).isEqualTo(canonical.getHeight() / 2);
    assertThat(result.getHeight()).isEqualTo(canonical.getHeight() / 2);
  }
}
