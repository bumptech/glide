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
import com.bumptech.glide.test.TearDownGlide;
import java.util.concurrent.ExecutionException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@RegressionTest
@SplitByCpu
@SplitBySdk({24, 21, 16})
public class CenterCropRegressionTest {
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
  public void centerCrop_withSquareSmallerThanImage_returnsSquareImage()
      throws ExecutionException, InterruptedException {
    Bitmap result =
        bitmapRegressionTester.test(
            GlideApp.with(context)
                .asBitmap()
                .load(canonical.getBitmap())
                .centerCrop()
                .override(50));
    assertThat(result.getWidth()).isEqualTo(50);
    assertThat(result.getHeight()).isEqualTo(50);
  }

  @Test
  public void centerCrop_withRectangleSmallerThanImage_returnsRectangularImage()
      throws ExecutionException, InterruptedException {
    Bitmap result =
        bitmapRegressionTester.test(
            GlideApp.with(context)
                .asBitmap()
                .load(canonical.getBitmap())
                .centerCrop()
                .override(60, 70));
    assertThat(result.getWidth()).isEqualTo(60);
    assertThat(result.getHeight()).isEqualTo(70);
  }

  @Test
  public void centerCrop_withSquareLargerThanImage_returnsUpscaledRectangularImage()
      throws ExecutionException, InterruptedException {
    Bitmap result =
        bitmapRegressionTester.test(
            GlideApp.with(context)
                .asBitmap()
                .load(canonical.getBitmap())
                .centerCrop()
                .override(canonical.getWidth() * 2));
    assertThat(result.getWidth()).isEqualTo(canonical.getWidth() * 2);
    assertThat(result.getHeight()).isEqualTo(canonical.getWidth() * 2);
  }

  @Test
  public void centerCrop_withRectangleLargerThanImage_returnsUpscaledRectangularImage()
      throws ExecutionException, InterruptedException {
    Bitmap result =
        bitmapRegressionTester.test(
            GlideApp.with(context)
                .asBitmap()
                .load(canonical.getBitmap())
                .centerCrop()
                .override(canonical.getWidth() * 2, canonical.getHeight() * 2));
    assertThat(result.getWidth()).isEqualTo(canonical.getWidth() * 2);
    assertThat(result.getHeight()).isEqualTo(canonical.getHeight() * 2);
  }
}
