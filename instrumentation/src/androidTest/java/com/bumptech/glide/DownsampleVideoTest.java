package com.bumptech.glide;

import static com.bumptech.glide.testutil.BitmapSubject.assertThat;
import static org.junit.Assume.assumeTrue;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.test.GlideApp;
import com.bumptech.glide.test.ResourceIds;
import com.bumptech.glide.testutil.ConcurrencyHelper;
import com.bumptech.glide.testutil.TearDownGlide;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class DownsampleVideoTest {
  // The dimensions of the test video.
  private static final int WIDTH = 1080;
  private static final int HEIGHT = 1920;

  @Rule public final TearDownGlide tearDownGlide = new TearDownGlide();
  private final ConcurrencyHelper concurrency = new ConcurrencyHelper();
  private final Context context = ApplicationProvider.getApplicationContext();

  @Before
  public void setUp() {
    assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1);
  }

  @Test
  public void loadVideo_downsampleStrategyNone_returnsOriginalVideoDimensions() {
    Bitmap bitmap =
        concurrency.get(
            GlideApp.with(context)
                .asBitmap()
                .load(ResourceIds.raw.video)
                .downsample(DownsampleStrategy.NONE)
                .submit(10, 10));

    assertThat(bitmap).hasDimensions(WIDTH, HEIGHT);
  }

  @Test
  public void loadVideo_downsampleStrategyNone_doesNotUpscale() {
    Bitmap bitmap =
        concurrency.get(
            GlideApp.with(context)
                .asBitmap()
                .load(ResourceIds.raw.video)
                .downsample(DownsampleStrategy.NONE)
                .submit(WIDTH * 2, HEIGHT * 2));

    assertThat(bitmap).hasDimensions(WIDTH, HEIGHT);
  }

  @Test
  public void loadVideo_downsampleDefault_downsamplesVideo() {
    Bitmap bitmap =
        concurrency.get(
            GlideApp.with(context).asBitmap().load(ResourceIds.raw.video).submit(10, 10));

    assertThat(bitmap).hasDimensions(10, 18);
  }

  @Test
  public void loadVideo_downsampleAtMost_downsamplesToSmallerSize() {
    Bitmap bitmap =
        concurrency.get(
            GlideApp.with(context)
                .asBitmap()
                .downsample(DownsampleStrategy.AT_MOST)
                .load(ResourceIds.raw.video)
                .submit(540, 959));
    assertThat(bitmap).hasDimensions(270, 480);
  }

  @Test
  public void loadVideo_downsampleAtMost_doesNotUpscale() {
    Bitmap bitmap =
        concurrency.get(
            GlideApp.with(context)
                .asBitmap()
                .downsample(DownsampleStrategy.AT_MOST)
                .load(ResourceIds.raw.video)
                .submit(WIDTH * 2, HEIGHT * 2));
    assertThat(bitmap).hasDimensions(WIDTH, HEIGHT);
  }

  @Test
  public void loadVideo_downsampleAtLeast_downsamplesToLargerSize() {
    Bitmap bitmap =
        concurrency.get(
            GlideApp.with(context)
                .asBitmap()
                .downsample(DownsampleStrategy.AT_LEAST)
                .load(ResourceIds.raw.video)
                .submit(270, 481));
    assertThat(bitmap).hasDimensions(540, 960);
  }

  @Test
  public void loadVideo_downsampleAtLeast_doesNotUpscale() {
    Bitmap bitmap =
        concurrency.get(
            GlideApp.with(context)
                .asBitmap()
                .downsample(DownsampleStrategy.AT_LEAST)
                .load(ResourceIds.raw.video)
                .submit(WIDTH * 2, HEIGHT * 2));
    assertThat(bitmap).hasDimensions(WIDTH, HEIGHT);
  }

  @Test
  public void loadVideo_downsampleCenterInside_downsamplesWithinBox() {
    Bitmap bitmap =
        concurrency.get(
            GlideApp.with(context)
                .asBitmap()
                .downsample(DownsampleStrategy.CENTER_INSIDE)
                .load(ResourceIds.raw.video)
                .submit(270, 481));
    assertThat(bitmap).hasDimensions(270, 480);
  }

  @Test
  public void loadVideo_downsampleCenterInside_doesNotUpscale() {
    Bitmap bitmap =
        concurrency.get(
            GlideApp.with(context)
                .asBitmap()
                .downsample(DownsampleStrategy.CENTER_INSIDE)
                .load(ResourceIds.raw.video)
                .submit(WIDTH * 2, HEIGHT * 2));
    assertThat(bitmap).hasDimensions(WIDTH, HEIGHT);
  }

  @Test
  public void loadVideo_downsampleCenterOutside_downsamplesOutsideBox() {
    Bitmap bitmap =
        concurrency.get(
            GlideApp.with(context)
                .asBitmap()
                .downsample(DownsampleStrategy.CENTER_OUTSIDE)
                .load(ResourceIds.raw.video)
                .submit(270, 481));
    assertThat(bitmap).hasDimensions(271, 481);
  }

  @Test
  public void loadVideo_downsampleCenterOutside_upsacles() {
    Bitmap bitmap =
        concurrency.get(
            GlideApp.with(context)
                .asBitmap()
                .downsample(DownsampleStrategy.CENTER_OUTSIDE)
                .load(ResourceIds.raw.video)
                .submit(WIDTH * 2, HEIGHT * 2));
    assertThat(bitmap).hasDimensions(WIDTH * 2, HEIGHT * 2);
  }

  @Test
  public void loadVideo_downsampleFitCenter_downsamplesInsideBox() {
    Bitmap bitmap =
        concurrency.get(
            GlideApp.with(context)
                .asBitmap()
                .downsample(DownsampleStrategy.FIT_CENTER)
                .load(ResourceIds.raw.video)
                .submit(270, 481));
    assertThat(bitmap).hasDimensions(270, 480);
  }

  @Test
  public void loadVideo_downsampleFitCenter_upscales() {
    Bitmap bitmap =
        concurrency.get(
            GlideApp.with(context)
                .asBitmap()
                .downsample(DownsampleStrategy.FIT_CENTER)
                .load(ResourceIds.raw.video)
                .submit(WIDTH * 2, HEIGHT * 2));
    assertThat(bitmap).hasDimensions(WIDTH * 2, HEIGHT * 2);
  }

  @Test
  public void loadVideo_withSizeOriginal_ignoresDownsampleStrategy() {
    Bitmap bitmap =
        concurrency.get(
            GlideApp.with(context)
                .asBitmap()
                .downsample(DownsampleStrategy.AT_MOST)
                .load(ResourceIds.raw.video)
                .submit(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL));

    assertThat(bitmap).hasDimensions(WIDTH, HEIGHT);
  }
}
