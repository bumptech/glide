package com.bumptech.glide.load.resource.bitmap;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 21)
public class DownsampleStrategyTest {

  @Test
  public void testAtMost_withSourceSmallerInOneDimensions_returnsScaleFactorForLargestDimension() {
    assertThat(DownsampleStrategy.AT_MOST.getScaleFactor(100, 200, 200, 200)).isEqualTo(1f);
    assertThat(DownsampleStrategy.AT_MOST.getScaleFactor(200, 100, 200, 200)).isEqualTo(1f);
    assertThat(DownsampleStrategy.AT_MOST.getScaleFactor(270, 480, 724, 440)).isEqualTo(1 / 2f);
    assertThat(DownsampleStrategy.AT_MOST.getScaleFactor(400, 200, 200, 200)).isEqualTo(1 / 2f);
    assertThat(DownsampleStrategy.AT_MOST.getScaleFactor(800, 200, 200, 200)).isEqualTo(1 / 4f);
  }

  @Test
  public void testAtMost_withSourceExactlyEqualToRequested_returnsScaleFactorOfOne() {
    assertThat(DownsampleStrategy.AT_MOST.getScaleFactor(100, 100, 100, 100)).isEqualTo(1f);
    assertThat(DownsampleStrategy.AT_MOST.getScaleFactor(1234, 452, 1234, 452)).isEqualTo(1f);
    assertThat(DownsampleStrategy.AT_MOST.getScaleFactor(341, 122, 341, 122)).isEqualTo(1f);
  }

  @Test
  public void testAtMost_withSourceLessThanTwiceRequestedSize_returnsScaleFactorOfTwo() {
    assertThat(DownsampleStrategy.AT_MOST.getScaleFactor(150, 150, 100, 100)).isEqualTo(1 / 2f);
    assertThat(DownsampleStrategy.AT_MOST.getScaleFactor(101, 101, 100, 100)).isEqualTo(1 / 2f);
    assertThat(DownsampleStrategy.AT_MOST.getScaleFactor(199, 199, 100, 100)).isEqualTo(1 / 2f);
  }

  @Test
  public void testAtMost_withSourceGreaterThanRequestedSize_returnsPowerOfTwoScaleFactor() {
    assertThat(DownsampleStrategy.AT_MOST.getScaleFactor(200, 200, 100, 100)).isEqualTo(1 / 2f);
    assertThat(DownsampleStrategy.AT_MOST.getScaleFactor(300, 300, 100, 100)).isEqualTo(1 / 4f);
    assertThat(DownsampleStrategy.AT_MOST.getScaleFactor(400, 400, 100, 100)).isEqualTo(1 / 4f);
    assertThat(DownsampleStrategy.AT_MOST.getScaleFactor(1000, 200, 100, 100)).isEqualTo(1 / 16f);
    assertThat(DownsampleStrategy.AT_MOST.getScaleFactor(1000, 1000, 100, 100)).isEqualTo(1 / 16f);
  }

  @Test
  public void testAtMost_withSourceGreaterInOneDimension_returnsScaleFactorOfLargestDimension() {
    assertThat(DownsampleStrategy.AT_MOST.getScaleFactor(101, 200, 100, 100)).isEqualTo(1 / 2f);
    assertThat(DownsampleStrategy.AT_MOST.getScaleFactor(199, 200, 100, 100)).isEqualTo(1 / 2f);
    assertThat(DownsampleStrategy.AT_MOST.getScaleFactor(400, 200, 100, 100)).isEqualTo(1 / 4f);
    assertThat(DownsampleStrategy.AT_MOST.getScaleFactor(1000, 400, 100, 100)).isEqualTo(1 / 16f);
  }

  @Test
  public void testAtLeast_withSourceSmallerInOneDimension_returnsScaleFactorOfOne() {
    assertThat(DownsampleStrategy.AT_LEAST.getScaleFactor(100, 200, 200, 200)).isEqualTo(1f);
    assertThat(DownsampleStrategy.AT_LEAST.getScaleFactor(200, 100, 200, 200)).isEqualTo(1f);
    assertThat(DownsampleStrategy.AT_LEAST.getScaleFactor(270, 480, 724, 440)).isEqualTo(1f);
  }

  @Test
  public void testAtLeast_withSourceExactlyEqualToRequested_returnsScaleFactorOfOne() {
    assertThat(DownsampleStrategy.AT_LEAST.getScaleFactor(100, 100, 100, 100)).isEqualTo(1f);
    assertThat(DownsampleStrategy.AT_LEAST.getScaleFactor(1234, 452, 1234, 452)).isEqualTo(1f);
    assertThat(DownsampleStrategy.AT_LEAST.getScaleFactor(341, 122, 341, 122)).isEqualTo(1f);
  }

  @Test
  public void testAtLeast_withSourceLessThanTwiceRequestedSize_returnsScaleFactorOfOne() {
    assertThat(DownsampleStrategy.AT_LEAST.getScaleFactor(150, 150, 100, 100)).isEqualTo(1f);
    assertThat(DownsampleStrategy.AT_LEAST.getScaleFactor(101, 101, 100, 100)).isEqualTo(1f);
    assertThat(DownsampleStrategy.AT_LEAST.getScaleFactor(199, 199, 100, 100)).isEqualTo(1f);
  }

  @Test
  public void testAtLeast_withSourceGreaterThanRequestedSize_returnsPowerOfTwoScaleFactor() {
    assertThat(DownsampleStrategy.AT_LEAST.getScaleFactor(200, 200, 100, 100)).isEqualTo(1 / 2f);
    assertThat(DownsampleStrategy.AT_LEAST.getScaleFactor(300, 300, 100, 100)).isEqualTo(1 / 2f);
    assertThat(DownsampleStrategy.AT_LEAST.getScaleFactor(400, 400, 100, 100)).isEqualTo(1 / 4f);
    assertThat(DownsampleStrategy.AT_LEAST.getScaleFactor(1000, 200, 100, 100)).isEqualTo(1 / 2f);
    assertThat(DownsampleStrategy.AT_LEAST.getScaleFactor(1000, 1000, 100, 100)).isEqualTo(1 / 8f);
  }

  @Test
  public void testAtLeast_withSourceGreaterInOneDimension_returnsScaleFactorOfSmallestDimension() {
    assertThat(DownsampleStrategy.AT_LEAST.getScaleFactor(101, 200, 100, 100)).isEqualTo(1f);
    assertThat(DownsampleStrategy.AT_LEAST.getScaleFactor(199, 200, 100, 100)).isEqualTo(1f);
    assertThat(DownsampleStrategy.AT_LEAST.getScaleFactor(400, 200, 100, 100)).isEqualTo(1 / 2f);
    assertThat(DownsampleStrategy.AT_LEAST.getScaleFactor(1000, 400, 100, 100)).isEqualTo(1 / 4f);
  }

  @Test
  public void testCenterInside_scalesImageToFitWithinRequestedBounds() {
    assertThat(DownsampleStrategy.FIT_CENTER.getScaleFactor(100, 200, 300, 300))
        .isEqualTo(300 / 200f);
    assertThat(DownsampleStrategy.FIT_CENTER.getScaleFactor(270, 480, 724, 440))
        .isEqualTo(440 / 480f);
    assertThat(DownsampleStrategy.FIT_CENTER.getScaleFactor(100, 100, 100, 100)).isEqualTo(1f);
  }

  @Test
  public void testCenterOutside_scalesImageToFitAroundRequestedBounds() {
    assertThat(DownsampleStrategy.CENTER_OUTSIDE.getScaleFactor(100, 200, 300, 300))
        .isEqualTo(300 / 100f);
    assertThat(DownsampleStrategy.CENTER_OUTSIDE.getScaleFactor(270, 480, 724, 440))
        .isEqualTo(724 / 270f);
    assertThat(DownsampleStrategy.CENTER_OUTSIDE.getScaleFactor(100, 100, 100, 100)).isEqualTo(1f);
  }

  @Test
  public void testNone_alwaysReturnsOne() {
    assertThat(DownsampleStrategy.NONE.getScaleFactor(100, 100, 100, 100)).isEqualTo(1f);
    assertThat(DownsampleStrategy.NONE.getScaleFactor(200, 200, 100, 100)).isEqualTo(1f);
    assertThat(DownsampleStrategy.NONE.getScaleFactor(100, 100, 200, 200)).isEqualTo(1f);
  }
}
