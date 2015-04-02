package com.bumptech.glide.load.resource.gif;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;

import com.bumptech.glide.gifdecoder.GifDecoder;
import com.bumptech.glide.gifdecoder.GifHeader;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.tests.GlideShadowLooper;
import com.bumptech.glide.tests.Util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.nio.ByteBuffer;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, emulateSdk = 18, shadows = GlideShadowLooper.class)
public class GifDrawableTest {
  private GifDrawable drawable;
  private int frameHeight;
  private int frameWidth;
  private Bitmap firstFrame;
  private int initialSdkVersion;

  @Mock private Drawable.Callback cb;
  @Mock private BitmapPool bitmapPool;
  @Mock private GifFrameLoader frameLoader;
  @Mock private Paint paint;
  @Mock private Transformation<Bitmap> transformation;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    frameWidth = 120;
    frameHeight = 450;
    firstFrame = Bitmap.createBitmap(frameWidth, frameHeight, Bitmap.Config.RGB_565);
    drawable = new GifDrawable(RuntimeEnvironment.application, frameLoader, bitmapPool, paint);
    when(frameLoader.getWidth()).thenReturn(frameWidth);
    when(frameLoader.getHeight()).thenReturn(frameHeight);
    when(frameLoader.getCurrentFrame()).thenReturn(firstFrame);
    drawable.setCallback(cb);
    initialSdkVersion = Build.VERSION.SDK_INT;
  }

  @After
  public void tearDown() {
    Util.setSdkVersionInt(initialSdkVersion);
  }

  @Test
  public void testShouldDrawFirstFrameBeforeAnyFrameRead() {
    Canvas canvas = mock(Canvas.class);
    drawable.draw(canvas);

    verify(canvas).drawBitmap(eq(firstFrame), anyRect(), anyRect(), anyPaint());
  }

  @Test
  public void testDoesNotDrawNullFirstFrame() {
    drawable = new GifDrawable(RuntimeEnvironment.application, frameLoader, bitmapPool, paint);
    Canvas canvas = mock(Canvas.class);

    verify(canvas, never()).drawBitmap(any(Bitmap.class), anyRect(), anyRect(), anyPaint());
  }

  @Test
  public void testDoesDrawCurrentFrameIfOneIsAvailable() {
    Canvas canvas = mock(Canvas.class);
    Bitmap currentFrame = Bitmap.createBitmap(100123, 123141, Bitmap.Config.ARGB_4444);
    when(frameLoader.getCurrentFrame()).thenReturn(currentFrame);

    drawable.draw(canvas);
    verify(canvas).drawBitmap(eq(currentFrame), anyRect(), anyRect(), anyPaint());
    verify(canvas, never()).drawBitmap(eq(firstFrame), anyRect(), anyRect(), anyPaint());
  }

  @Test
  public void testRequestsNextFrameOnStart() {
    drawable.setVisible(true, true);
    drawable.start();

    verify(frameLoader).subscribe(eq(drawable));
  }

  @Test
  public void testRequestsNextFrameOnStartWithoutCallToSetVisible() {
    drawable.start();

    verify(frameLoader).subscribe(eq(drawable));
  }

  @Test
  public void testDoesNotRequestNextFrameOnStartIfGotCallToSetVisibleWithVisibleFalse() {
    drawable.setVisible(false, false);
    drawable.start();

    verify(frameLoader, never()).subscribe(eq(drawable));
  }

  @Test
  public void testDoesNotRequestNextFrameOnStartIfHasSingleFrame() {
    when(frameLoader.getFrameCount()).thenReturn(1);
    drawable.setVisible(true, false);
    drawable.start();

    verify(frameLoader, never()).subscribe(eq(drawable));
  }

  @Test
  public void testInvalidatesSelfOnStartIfHasSingleFrame() {
    when(frameLoader.getFrameCount()).thenReturn(1);
    drawable.setVisible(true, false);
    drawable.start();

    verify(cb).invalidateDrawable(eq(drawable));
  }

  @Test
  public void testShouldInvalidateSelfOnRun() {
    drawable.setVisible(true, true);
    drawable.start();

    verify(cb).invalidateDrawable(eq(drawable));
  }

  @Test
  public void testShouldNotScheduleItselfIfAlreadyRunning() {
    drawable.setVisible(true, true);
    drawable.start();
    drawable.start();

    verify(frameLoader, times(1)).subscribe(eq(drawable));
  }

  @Test
  public void testReturnsFalseFromIsRunningWhenNotRunning() {
    assertFalse(drawable.isRunning());
  }

  @Test
  public void testReturnsTrueFromIsRunningWhenRunning() {
    drawable.setVisible(true, true);
    drawable.start();

    assertTrue(drawable.isRunning());
  }

  @Test
  public void testInvalidatesSelfWhenFrameReady() {
    drawable.setIsRunning(true);
    drawable.onFrameReady(0);

    verify(cb).invalidateDrawable(eq(drawable));
  }

  @Test
  public void testDoesNotStartLoadingNextFrameWhenCurrentFinishesIfHasNoCallback() {
    drawable.setIsRunning(true);
    drawable.setCallback(null);
    drawable.onFrameReady(0);

    verify(frameLoader).unsubscribe(eq(drawable));
  }

  @Test
  public void testStopsWhenCurrentFrameFinishesIfHasNoCallbackAndIsAtLeastAtHoneycomb() {
    drawable.setIsRunning(true);
    drawable.setCallback(null);
    drawable.onFrameReady(0);

    assertFalse(drawable.isRunning());
  }

  @Test
  public void testDoesNotStopWhenCurrentFrameFinishesIfHasNoCallbackAndIsPreHoneycomb() {
    Util.setSdkVersionInt(10);

    drawable.setIsRunning(true);
    drawable.setCallback(null);
    drawable.onFrameReady(0);

    assertTrue(drawable.isRunning());
  }

  @Test
  public void testUnsubscribesWhenCurrentFinishesIfHasNoCallbackAndIsAtLeastAtHoneycomb() {
    drawable.setIsRunning(true);
    drawable.setCallback(null);
    drawable.onFrameReady(0);

    verify(frameLoader).unsubscribe(eq(drawable));
  }

  @Test
  public void testSetsIsRunningFalseOnStop() {
    drawable.start();
    drawable.stop();

    assertFalse(drawable.isRunning());
  }

  @Test
  public void testStopsOnSetVisibleFalse() {
    drawable.start();

    drawable.setVisible(false, true);

    assertFalse(drawable.isRunning());
  }

  @Test
  public void testStartsOnSetVisibleTrueIfRunning() {
    drawable.start();
    drawable.setVisible(false, false);
    drawable.setVisible(true, true);

    assertTrue(drawable.isRunning());
  }

  @Test
  public void testDoesNotStartOnVisibleTrueIfNotRunning() {
    drawable.setVisible(true, true);

    assertFalse(drawable.isRunning());
  }

  @Test
  public void testDoesNotStartOnSetVisibleIfStartedAndStopped() {
    drawable.start();
    drawable.stop();
    drawable.setVisible(true, true);

    assertFalse(drawable.isRunning());
  }

  @Test
  public void testDoesNotImmediatelyRunIfStartedWhileNotVisible() {
    drawable.setVisible(false, false);
    drawable.start();

    assertFalse(drawable.isRunning());
  }

  @Test
  public void testGetOpacityReturnsTransparent() {
    assertEquals(PixelFormat.TRANSPARENT, drawable.getOpacity());
  }

  @Test
  public void testReturnsFrameCountFromDecoder() {
    int expected = 4;
    when(frameLoader.getFrameCount()).thenReturn(expected);

    assertEquals(expected, drawable.getFrameCount());
  }

  @Test
  public void testRecycleCallsClearOnFrameManager() {
    drawable.recycle();

    verify(frameLoader).clear();
  }

  @Test
  public void testIsNotRecycledIfNotRecycled() {
    assertFalse(drawable.isRecycled());
  }

  @Test
  public void testIsRecycledAfterRecycled() {
    drawable.recycle();

    assertTrue(drawable.isRecycled());
  }

  @Test
  public void testReturnsNonNullConstantState() {
    assertNotNull(drawable.getConstantState());
  }

  @Test
  public void testReturnsSizeFromFrameLoader() {
    int size = 1243;
    when(frameLoader.getSize()).thenReturn(size);

    assertThat(drawable.getSize()).isEqualTo(size);
  }

  @Test
  public void testReturnsNewDrawableFromConstantState() {
    GifHeader gifHeader = new GifHeader();
    GifDecoder.BitmapProvider provider = mock(GifDecoder.BitmapProvider.class);
    Bitmap firstFrame = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
    drawable =
        new GifDrawable(RuntimeEnvironment.application, provider, bitmapPool,
            ByteBuffer.allocate(10), transformation, 100, 100, gifHeader, firstFrame);

    assertNotNull(drawable.getConstantState().newDrawable());
    assertNotNull(
        drawable.getConstantState().newDrawable(RuntimeEnvironment.application.getResources()));
  }

  @Test
  public void testReturnsFrameWidthAndHeightForIntrinsicDimensions() {
    assertEquals(frameWidth, drawable.getIntrinsicWidth());
    assertEquals(frameHeight, drawable.getIntrinsicHeight());
  }

  @Test
  public void testLoopsASingleTimeIfLoopCountIsSetToOne() {
    final int loopCount = 1;
    final int frameCount = 2;
    when(frameLoader.getFrameCount()).thenReturn(frameCount);
    drawable.setLoopCount(loopCount);
    drawable.setVisible(true, true);
    drawable.start();

    runLoops(loopCount, frameCount);

    verifyRanLoops(loopCount, frameCount);
    assertFalse(drawable.isRunning());
  }

  @Test
  public void testLoopsForeverIfLoopCountIsSetToLoopForever() {
    final int loopCount = 40;
    final int frameCount = 2;

    when(frameLoader.getFrameCount()).thenReturn(frameCount);
    drawable.setLoopCount(GifDrawable.LOOP_FOREVER);
    drawable.setVisible(true, true);
    drawable.start();

    runLoops(loopCount, frameCount);

    verifyRanLoops(loopCount, frameCount);
  }

  @Test
  public void testLoopsOnceIfLoopCountIsSetToOneWithThreeFrames() {
    final int loopCount = 1;
    final int frameCount = 3;

    when(frameLoader.getFrameCount()).thenReturn(frameCount);
    drawable.setLoopCount(loopCount);
    drawable.setVisible(true, true);
    drawable.start();

    runLoops(loopCount, frameCount);

    verifyRanLoops(loopCount, frameCount);
    assertFalse(drawable.isRunning());
  }

  @Test
  public void testLoopsThreeTimesIfLoopCountIsSetToThree() {
    final int loopCount = 3;
    final int frameCount = 2;

    when(frameLoader.getFrameCount()).thenReturn(frameCount);
    drawable.setLoopCount(loopCount);
    drawable.setVisible(true, true);
    drawable.start();

    runLoops(loopCount, frameCount);

    verifyRanLoops(loopCount, frameCount);
    assertFalse(drawable.isRunning());
  }

  @Test
  public void testCallingStartResetsLoopCounter() {
    when(frameLoader.getFrameCount()).thenReturn(2);
    drawable.setLoopCount(1);
    drawable.setVisible(true, true);
    drawable.start();

    drawable.onFrameReady(0);
    drawable.onFrameReady(1);

    drawable.start();

    drawable.onFrameReady(0);
    drawable.onFrameReady(1);

    // 4 onFrameReady(), 2 start()
    verify(cb, times(4 + 2)).invalidateDrawable(eq(drawable));
    assertFalse(drawable.isRunning());
  }

  @Test
  public void testChangingTheLoopCountAfterHittingTheMaxLoopCount() {
    final int initialLoopCount = 1;
    final int frameCount = 2;

    when(frameLoader.getFrameCount()).thenReturn(frameCount);
    drawable.setLoopCount(initialLoopCount);
    drawable.setVisible(true, true);
    drawable.start();

    runLoops(initialLoopCount, frameCount);

    final int newLoopCount = 2;

    drawable.setLoopCount(newLoopCount);
    drawable.start();

    runLoops(newLoopCount, frameCount);

    int numStarts = 2;
    int expectedFrames = (initialLoopCount + newLoopCount) * frameCount + numStarts;
    verify(cb, times(expectedFrames)).invalidateDrawable(eq(drawable));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testThrowsIfGivenLoopCountLessThanZeroAndNotInfinite() {
    drawable.setLoopCount(-2);
  }

  @Test
  public void testUsesDecoderNetscapeLoopCountIfLoopCountIsLoopIntrinsic() {
    final int frameCount = 3;
    final int loopCount = 2;
    when(frameLoader.getLoopCount()).thenReturn(loopCount);
    when(frameLoader.getFrameCount()).thenReturn(frameCount);
    drawable.setLoopCount(GifDrawable.LOOP_INTRINSIC);
    drawable.setVisible(true, true);
    drawable.start();

    runLoops(loopCount, frameCount);

    verifyRanLoops(loopCount, frameCount);
  }

  @Test
  public void testDoesNotDrawFrameAfterRecycle() {
    Bitmap bitmap = Bitmap.createBitmap(100, 112341, Bitmap.Config.RGB_565);
    drawable.setVisible(true, true);
    drawable.start();
    when(frameLoader.getCurrentFrame()).thenReturn(bitmap);
    drawable.onFrameReady(1);
    drawable.recycle();
    Canvas canvas = mock(Canvas.class);
    drawable.draw(canvas);
    verify(canvas, never()).drawBitmap(any(Bitmap.class), anyRect(), anyRect(), anyPaint());
  }

  @Test
  public void testSetsFrameTransformationOnFrameManager() {
    Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
    drawable.setFrameTransformation(transformation, bitmap);

    verify(frameLoader).setFrameTransformation(eq(transformation), eq(bitmap));
  }

  @Test(expected = NullPointerException.class)
  public void testThrowsIfConstructedWithNullFirstFrame() {
    new GifDrawable(RuntimeEnvironment.application, mock(GifDecoder.BitmapProvider.class),
        bitmapPool, ByteBuffer.allocate(10), transformation, 100, 100,
        mock(GifHeader.class), null);
  }

  @Test
  public void testAppliesGravityOnDrawAfterBoundsChange() {
    Rect bounds = new Rect(0, 0, frameWidth * 2, frameHeight * 2);
    drawable.setBounds(bounds);

    Canvas canvas = mock(Canvas.class);
    drawable.draw(canvas);

    verify(canvas).drawBitmap(any(Bitmap.class), (Rect) isNull(), eq(bounds), eq(paint));
  }

  @Test
  public void testSetAlphaSetsAlphaOnPaint() {
    int alpha = 100;
    drawable.setAlpha(alpha);
    verify(paint).setAlpha(eq(alpha));
  }

  @Test
  public void testSetColorFilterSetsColorFilterOnPaint() {
    ColorFilter colorFilter = new ColorFilter();
    drawable.setColorFilter(colorFilter);
    verify(paint).setColorFilter(eq(colorFilter));
  }


  @Test
  public void testReturnsCurrentTransformationInGetFrameTransformation() {
    Transformation<Bitmap> newTransformation = mock(Transformation.class);
    Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
    drawable.setFrameTransformation(newTransformation, bitmap);

    verify(frameLoader).setFrameTransformation(eq(newTransformation), eq(bitmap));
  }

  @Test(expected = NullPointerException.class)
  public void testThrowsIfCreatedWithNullState() {
    new GifDrawable(null);
  }

  private void verifyRanLoops(int loopCount, int frameCount) {
    // 1 for invalidate in start().
    verify(cb, times(1 + loopCount * frameCount)).invalidateDrawable(eq(drawable));
  }

  private static Paint anyPaint() {
    return any(Paint.class);
  }

  private static Rect anyRect() {
    return any(Rect.class);
  }

  private void runLoops(int loopCount, int frameCount) {
    for (int loop = 0; loop < loopCount; loop++) {
      for (int frame = 0; frame < frameCount; frame++) {
        drawable.onFrameReady(frame);
      }
    }
  }
}
