package com.bumptech.glide.load.resource.gif;

import static com.bumptech.glide.RobolectricConstants.ROBOLECTRIC_SDK;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.view.View;
import androidx.test.core.app.ApplicationProvider;
import com.bumptech.glide.gifdecoder.GifDecoder;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.tests.TearDownGlide;
import com.bumptech.glide.util.Preconditions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowCanvas;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = ROBOLECTRIC_SDK)
public class GifDrawableTest {
  @Rule public final TearDownGlide tearDownGlide = new TearDownGlide();

  private GifDrawable drawable;
  private int frameHeight;
  private int frameWidth;
  private Bitmap firstFrame;

  @Mock private Drawable.Callback cb;
  @Mock private GifFrameLoader frameLoader;
  @Mock private Paint paint;
  @Mock private Transformation<Bitmap> transformation;
  private Application context;

  private static Paint isAPaint() {
    return isA(Paint.class);
  }

  private static Rect isARect() {
    return isA(Rect.class);
  }

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    context = ApplicationProvider.getApplicationContext();
    frameWidth = 120;
    frameHeight = 450;
    firstFrame = Bitmap.createBitmap(frameWidth, frameHeight, Bitmap.Config.RGB_565);
    drawable = new GifDrawable(frameLoader, paint);
    when(frameLoader.getWidth()).thenReturn(frameWidth);
    when(frameLoader.getHeight()).thenReturn(frameHeight);
    when(frameLoader.getCurrentFrame()).thenReturn(firstFrame);
    when(frameLoader.getCurrentIndex()).thenReturn(0);
    drawable.setCallback(cb);
  }

  @Test
  public void shouldDrawFirstFrameBeforeAnyFrameRead() {
    Canvas canvas = new Canvas();
    drawable.draw(canvas);

    ShadowCanvas shadowCanvas = Shadow.extract(canvas);
    assertThat(shadowCanvas.getDescription())
        .isEqualTo(
            "Bitmap ("
                + firstFrame.getWidth()
                + " x "
                + firstFrame.getHeight()
                + ") at (0,0) with height=0 and width=0");
  }

  @Test
  public void doesDrawCurrentFrameIfOneIsAvailable() {
    Canvas canvas = mock(Canvas.class);
    Bitmap currentFrame = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_4444);
    when(frameLoader.getCurrentFrame()).thenReturn(currentFrame);

    drawable.draw(canvas);
    verify(canvas).drawBitmap(eq(currentFrame), (Rect) isNull(), isARect(), isAPaint());
    verify(canvas, never()).drawBitmap(eq(firstFrame), (Rect) isNull(), isARect(), isAPaint());
  }

  @Test
  public void requestsNextFrameOnStart() {
    drawable.setVisible(true, true);
    drawable.start();

    verify(frameLoader).subscribe(eq(drawable));
  }

  @Test
  public void requestsNextFrameOnStartWithoutCallToSetVisible() {
    drawable.start();

    verify(frameLoader).subscribe(eq(drawable));
  }

  @Test
  public void doesNotRequestNextFrameOnStartIfGotCallToSetVisibleWithVisibleFalse() {
    drawable.setVisible(false, false);
    drawable.start();

    verify(frameLoader, never()).subscribe(eq(drawable));
  }

  @Test
  public void doesNotRequestNextFrameOnStartIfHasSingleFrame() {
    when(frameLoader.getFrameCount()).thenReturn(1);
    drawable.setVisible(true, false);
    drawable.start();

    verify(frameLoader, never()).subscribe(eq(drawable));
  }

  @Test
  public void invalidatesSelfOnStartIfHasSingleFrame() {
    when(frameLoader.getFrameCount()).thenReturn(1);
    drawable.setVisible(true, false);
    drawable.start();

    verify(cb).invalidateDrawable(eq(drawable));
  }

  @Test
  public void shouldInvalidateSelfOnRun() {
    drawable.setVisible(true, true);
    drawable.start();

    verify(cb).invalidateDrawable(eq(drawable));
  }

  @Test
  public void shouldNotScheduleItselfIfAlreadyRunning() {
    drawable.setVisible(true, true);
    drawable.start();
    drawable.start();

    verify(frameLoader, times(1)).subscribe(eq(drawable));
  }

  @Test
  public void returnsFalseFromIsRunningWhenNotRunning() {
    assertFalse(drawable.isRunning());
  }

  @Test
  public void returnsTrueFromIsRunningWhenRunning() {
    drawable.setVisible(true, true);
    drawable.start();

    assertTrue(drawable.isRunning());
  }

  @Test
  public void invalidatesSelfWhenFrameReady() {
    drawable.setIsRunning(true);
    drawable.onFrameReady();

    verify(cb).invalidateDrawable(eq(drawable));
  }

  @Test
  public void doesNotStartLoadingNextFrameWhenCurrentFinishesIfHasNoCallback() {
    drawable.setIsRunning(true);
    drawable.setCallback(null);
    drawable.onFrameReady();

    verify(frameLoader).unsubscribe(eq(drawable));
  }

  @Test
  public void stopsWhenCurrentFrameFinishesIfHasNoCallback() {
    drawable.setIsRunning(true);
    drawable.setCallback(null);
    drawable.onFrameReady();

    assertFalse(drawable.isRunning());
  }

  @Test
  public void unsubscribesWhenCurrentFinishesIfHasNoCallback() {
    drawable.setIsRunning(true);
    drawable.setCallback(null);
    drawable.onFrameReady();

    verify(frameLoader).unsubscribe(eq(drawable));
  }

  @Test
  public void setsIsRunningFalseOnStop() {
    drawable.start();
    drawable.stop();

    assertFalse(drawable.isRunning());
  }

  @Test
  public void stopsOnSetVisibleFalse() {
    drawable.start();

    drawable.setVisible(false, true);

    assertFalse(drawable.isRunning());
  }

  @Test
  public void startsOnSetVisibleTrueIfRunning() {
    drawable.start();
    drawable.setVisible(false, false);
    drawable.setVisible(true, true);

    assertTrue(drawable.isRunning());
  }

  @Test
  public void doesNotStartOnVisibleTrueIfNotRunning() {
    drawable.setVisible(true, true);

    assertFalse(drawable.isRunning());
  }

  @Test
  public void doesNotStartOnSetVisibleIfStartedAndStopped() {
    drawable.start();
    drawable.stop();
    drawable.setVisible(true, true);

    assertFalse(drawable.isRunning());
  }

  @Test
  public void doesNotImmediatelyRunIfStartedWhileNotVisible() {
    drawable.setVisible(false, false);
    drawable.start();

    assertFalse(drawable.isRunning());
  }

  @Test
  public void getOpacityReturnsTransparent() {
    assertEquals(PixelFormat.TRANSPARENT, drawable.getOpacity());
  }

  @Test
  public void returnsFrameCountFromDecoder() {
    int expected = 4;
    when(frameLoader.getFrameCount()).thenReturn(expected);

    assertEquals(expected, drawable.getFrameCount());
  }

  @Test
  public void returnsDefaultFrameIndex() {
    final int expected = -1;

    when(frameLoader.getCurrentIndex()).thenReturn(expected);

    assertEquals(expected, drawable.getFrameIndex());
  }

  @Test
  public void returnsNonDefaultFrameIndex() {
    final int expected = 100;

    when(frameLoader.getCurrentIndex()).thenReturn(expected);

    assertEquals(expected, drawable.getFrameIndex());
  }

  @Test
  public void recycleCallsClearOnFrameManager() {
    drawable.recycle();

    verify(frameLoader).clear();
  }

  @Test
  public void isNotRecycledIfNotRecycled() {
    assertFalse(drawable.isRecycled());
  }

  @Test
  public void isRecycledAfterRecycled() {
    drawable.recycle();

    assertTrue(drawable.isRecycled());
  }

  @Test
  public void returnsNonNullConstantState() {
    assertNotNull(drawable.getConstantState());
  }

  @Test
  public void returnsSizeFromFrameLoader() {
    int size = 1243;
    when(frameLoader.getSize()).thenReturn(size);

    assertThat(drawable.getSize()).isEqualTo(size);
  }

  @Test
  public void returnsNewDrawableFromConstantState() {
    Bitmap firstFrame = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
    drawable =
        new GifDrawable(
            ApplicationProvider.getApplicationContext(),
            mock(GifDecoder.class),
            transformation,
            100,
            100,
            firstFrame);

    assertNotNull(Preconditions.checkNotNull(drawable.getConstantState()).newDrawable());
    assertNotNull(
        drawable
            .getConstantState()
            .newDrawable(ApplicationProvider.getApplicationContext().getResources()));
  }

  @Test
  public void returnsFrameWidthAndHeightForIntrinsicDimensions() {
    assertEquals(frameWidth, drawable.getIntrinsicWidth());
    assertEquals(frameHeight, drawable.getIntrinsicHeight());
  }

  @Test
  public void loopsASingleTimeIfLoopCountIsSetToOne() {
    final int loopCount = 1;
    final int frameCount = 2;
    when(frameLoader.getFrameCount()).thenReturn(frameCount);
    drawable.setLoopCount(loopCount);
    drawable.setVisible(true, true);
    drawable.start();

    runLoops(loopCount, frameCount);

    verifyRanLoops(loopCount, frameCount);
    assertFalse("drawable should be stopped after loop is completed", drawable.isRunning());
  }

  @Test
  public void loopsForeverIfLoopCountIsSetToLoopForever() {
    final int loopCount = 40;
    final int frameCount = 2;

    when(frameLoader.getFrameCount()).thenReturn(frameCount);
    drawable.setLoopCount(GifDrawable.LOOP_FOREVER);
    drawable.setVisible(true, true);
    drawable.start();

    runLoops(loopCount, frameCount);

    verifyRanLoops(loopCount, frameCount);
    assertTrue("drawable should be still running", drawable.isRunning());
  }

  @Test
  public void loopsOnceIfLoopCountIsSetToOneWithThreeFrames() {
    final int loopCount = 1;
    final int frameCount = 3;

    when(frameLoader.getFrameCount()).thenReturn(frameCount);
    drawable.setLoopCount(loopCount);
    drawable.setVisible(true, true);
    drawable.start();

    runLoops(loopCount, frameCount);

    verifyRanLoops(loopCount, frameCount);
    assertFalse("drawable should be stopped after loop is completed", drawable.isRunning());
  }

  @Test
  public void loopsThreeTimesIfLoopCountIsSetToThree() {
    final int loopCount = 3;
    final int frameCount = 2;

    when(frameLoader.getFrameCount()).thenReturn(frameCount);
    drawable.setLoopCount(loopCount);
    drawable.setVisible(true, true);
    drawable.start();

    runLoops(loopCount, frameCount);

    verifyRanLoops(loopCount, frameCount);
    assertFalse("drawable should be stopped after loop is completed", drawable.isRunning());
  }

  @Test
  public void callingStartResetsLoopCounter() {
    when(frameLoader.getFrameCount()).thenReturn(2);
    drawable.setLoopCount(1);
    drawable.setVisible(true, true);
    drawable.start();

    drawable.onFrameReady();
    when(frameLoader.getCurrentIndex()).thenReturn(1);
    drawable.onFrameReady();
    assertFalse("drawable should be stopped after loop is completed", drawable.isRunning());

    drawable.start();

    when(frameLoader.getCurrentIndex()).thenReturn(0);
    drawable.onFrameReady();
    when(frameLoader.getCurrentIndex()).thenReturn(1);
    drawable.onFrameReady();

    // 4 onFrameReady(), 2 start()
    verify(cb, times(4 + 2)).invalidateDrawable(eq(drawable));
    assertFalse("drawable should be stopped after loop is completed", drawable.isRunning());
  }

  @Test
  public void changingTheLoopCountAfterHittingTheMaxLoopCount() {
    final int initialLoopCount = 1;
    final int frameCount = 2;

    when(frameLoader.getFrameCount()).thenReturn(frameCount);
    drawable.setLoopCount(initialLoopCount);
    drawable.setVisible(true, true);
    drawable.start();

    runLoops(initialLoopCount, frameCount);
    assertFalse("drawable should be stopped after loop is completed", drawable.isRunning());

    final int newLoopCount = 2;

    drawable.setLoopCount(newLoopCount);
    drawable.start();

    runLoops(newLoopCount, frameCount);

    int numStarts = 2;
    int expectedFrames = (initialLoopCount + newLoopCount) * frameCount + numStarts;
    verify(cb, times(expectedFrames)).invalidateDrawable(eq(drawable));
    assertFalse("drawable should be stopped after loop is completed", drawable.isRunning());
  }

  @Test(expected = IllegalArgumentException.class)
  public void throwsIfGivenLoopCountLessThanZeroAndNotInfinite() {
    drawable.setLoopCount(-2);
  }

  @Test
  public void usesDecoderTotalLoopCountIfLoopCountIsLoopIntrinsic() {
    final int frameCount = 3;
    final int loopCount = 2;
    when(frameLoader.getLoopCount()).thenReturn(loopCount);
    when(frameLoader.getFrameCount()).thenReturn(frameCount);
    drawable.setLoopCount(GifDrawable.LOOP_INTRINSIC);
    drawable.setVisible(true, true);
    drawable.start();

    runLoops(loopCount, frameCount);

    verifyRanLoops(loopCount, frameCount);
    assertFalse("drawable should be stopped after loop is completed", drawable.isRunning());
  }

  @Test
  public void loopsForeverIfLoopCountIsLoopIntrinsicAndTotalIterationCountIsForever() {
    final int frameCount = 3;
    final int loopCount = 40;
    when(frameLoader.getLoopCount()).thenReturn(GifDecoder.TOTAL_ITERATION_COUNT_FOREVER);
    when(frameLoader.getFrameCount()).thenReturn(frameCount);
    drawable.setLoopCount(GifDrawable.LOOP_INTRINSIC);
    drawable.setVisible(true, true);
    drawable.start();

    runLoops(loopCount, frameCount);

    verifyRanLoops(loopCount, frameCount);
    assertTrue("drawable should be still running", drawable.isRunning());
  }

  @Test
  public void doesNotDrawFrameAfterRecycle() {
    Bitmap bitmap = Bitmap.createBitmap(100, 112341, Bitmap.Config.RGB_565);
    drawable.setVisible(true, true);
    drawable.start();
    when(frameLoader.getCurrentFrame()).thenReturn(bitmap);
    drawable.onFrameReady();
    drawable.recycle();
    Canvas canvas = mock(Canvas.class);
    drawable.draw(canvas);
    verify(canvas, never()).drawBitmap(eq(bitmap), isARect(), isARect(), isAPaint());
  }

  @Test
  public void setsFrameTransformationOnFrameManager() {
    Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
    drawable.setFrameTransformation(transformation, bitmap);

    verify(frameLoader).setFrameTransformation(eq(transformation), eq(bitmap));
  }

  @Test(expected = NullPointerException.class)
  public void throwsIfConstructedWithNullFirstFrame() {
    new GifDrawable(
        ApplicationProvider.getApplicationContext(),
        mock(GifDecoder.class),
        transformation,
        100,
        100,
        null);
  }

  @Test
  public void appliesGravityOnDrawAfterBoundsChange() {
    Rect bounds = new Rect(0, 0, frameWidth * 2, frameHeight * 2);
    drawable.setBounds(bounds);

    Canvas canvas = mock(Canvas.class);
    drawable.draw(canvas);

    verify(canvas).drawBitmap(isA(Bitmap.class), (Rect) isNull(), eq(bounds), eq(paint));
  }

  @Test
  public void setAlphaSetsAlphaOnPaint() {
    int alpha = 100;
    drawable.setAlpha(alpha);
    verify(paint).setAlpha(eq(alpha));
  }

  @Test
  public void setColorFilterSetsColorFilterOnPaint() {
    ColorFilter colorFilter = new PorterDuffColorFilter(Color.RED, Mode.ADD);
    drawable.setColorFilter(colorFilter);

    // Use ArgumentCaptor instead of eq() due to b/73121412 where ShadowPorterDuffColorFilter.equals
    // uses a method that can't be found (PorterDuffColorFilter.getColor).
    ArgumentCaptor<ColorFilter> captor = ArgumentCaptor.forClass(ColorFilter.class);
    verify(paint).setColorFilter(captor.capture());
    assertThat(captor.getValue()).isSameInstanceAs(colorFilter);
  }

  @Test
  public void returnsCurrentTransformationInGetFrameTransformation() {
    @SuppressWarnings("unchecked")
    Transformation<Bitmap> newTransformation = mock(Transformation.class);
    Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
    drawable.setFrameTransformation(newTransformation, bitmap);

    verify(frameLoader).setFrameTransformation(eq(newTransformation), eq(bitmap));
  }

  @Test(expected = NullPointerException.class)
  public void throwsIfCreatedWithNullState() {
    new GifDrawable(null);
  }

  @Test
  public void onFrameReady_whenAttachedToDrawableCallbackButNotViewCallback_stops() {
    TransitionDrawable topLevel = new TransitionDrawable(new Drawable[] {drawable});
    drawable.setCallback(topLevel);
    topLevel.setCallback(null);

    drawable.start();
    drawable.onFrameReady();

    assertThat(drawable.isRunning()).isFalse();
  }

  @Test
  public void onFrameReady_whenAttachedtoDrawableCallbackWithViewCallbackParent_doesNotStop() {
    TransitionDrawable topLevel = new TransitionDrawable(new Drawable[] {drawable});
    drawable.setCallback(topLevel);
    topLevel.setCallback(new View(context));

    drawable.start();
    drawable.onFrameReady();

    assertThat(drawable.isRunning()).isTrue();
  }

  private void verifyRanLoops(int loopCount, int frameCount) {
    // 1 for invalidate in start().
    verify(cb, times(1 + loopCount * frameCount)).invalidateDrawable(eq(drawable));
  }

  private void runLoops(int loopCount, int frameCount) {
    for (int loop = 0; loop < loopCount; loop++) {
      for (int frame = 0; frame < frameCount; frame++) {
        when(frameLoader.getCurrentIndex()).thenReturn(frame);
        assertTrue(
            "drawable should be started before calling drawable.onFrameReady()",
            drawable.isRunning());
        drawable.onFrameReady();
      }
    }
  }
}
