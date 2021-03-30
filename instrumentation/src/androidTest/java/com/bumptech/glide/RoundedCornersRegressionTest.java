package com.bumptech.glide;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
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
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

/**
 * Compares the output of RoundedCorners with canonical resource files for all SDKs Glide supports
 * and fails on deltas.
 */
@RunWith(AndroidJUnit4.class)
@SplitByCpu
@SplitBySdk({26, 24, 23, 21, 19, 18, 16})
@RegressionTest
public class RoundedCornersRegressionTest {
  @Rule public final TestRule tearDownGlide = new TearDownGlide();
  @Rule public final TestName testName = new TestName();

  private Context context;
  private BitmapRegressionTester bitmapRegressionTester;
  private CanonicalBitmap canonicalBitmap;

  @Before
  public void setUp() throws Exception {
    context = ApplicationProvider.getApplicationContext();
    bitmapRegressionTester = new BitmapRegressionTester(getClass(), testName);
    canonicalBitmap = new CanonicalBitmap();
  }

  @Test
  public void testRoundedCorners() throws ExecutionException, InterruptedException {
    bitmapRegressionTester.test(
        GlideApp.with(context)
            .asBitmap()
            .load(canonicalBitmap.getBitmap())
            .transform(new RoundedCorners(5)));
  }

  @Test
  public void testRoundedCorners_usePool() throws ExecutionException, InterruptedException {
    canonicalBitmap = canonicalBitmap.scale(0.1f);

    Bitmap redRect =
        createRect(
            Color.RED,
            canonicalBitmap.getWidth(),
            canonicalBitmap.getHeight(),
            Bitmap.Config.ARGB_8888);

    Glide.get(context).getBitmapPool().put(redRect);

    Bitmap roundedRect =
        bitmapRegressionTester.test(
            GlideApp.with(context)
                .asBitmap()
                .load(canonicalBitmap.getBitmap())
                .override(canonicalBitmap.getWidth(), canonicalBitmap.getHeight())
                .transform(new RoundedCorners(5)));

    assertThat(roundedRect).isEqualTo(redRect);
  }

  @Test
  public void testRoundedCorners_overRounded() throws ExecutionException, InterruptedException {
    bitmapRegressionTester.test(
        GlideApp.with(context)
            .asBitmap()
            .load(canonicalBitmap.getBitmap())
            .transform(new RoundedCorners(20)));
  }

  private Bitmap createRect(int color, int width, int height, Bitmap.Config config) {
    final Bitmap result = Bitmap.createBitmap(width, height, config);
    Canvas canvas = new Canvas(result);
    canvas.drawColor(color);
    return result;
  }
}
