package com.bumptech.glide.load.resource.gif;

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
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.tests.GlideShadowLooper;
import com.bumptech.glide.tests.Util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, emulateSdk = 18, shadows = GlideShadowLooper.class)
public class GifDrawableTest {
    private GifDecoder gifDecoder;
    private GifDrawable drawable;
    private Drawable.Callback cb;
    private int frameHeight;
    private int frameWidth;
    private Bitmap firstFrame;
    private BitmapPool bitmapPool;
    private int initialSdkVersion;
    private GifFrameLoader frameLoader;
    private Paint paint;

    @Before
    public void setUp() {
        frameWidth = 120;
        frameHeight = 450;
        cb = mock(Drawable.Callback.class);
        gifDecoder = mock(GifDecoder.class);
        firstFrame = Bitmap.createBitmap(frameWidth, frameHeight, Bitmap.Config.RGB_565);
        bitmapPool = mock(BitmapPool.class);
        frameLoader = mock(GifFrameLoader.class);
        paint = mock(Paint.class);
        drawable = new GifDrawable(gifDecoder, frameLoader, firstFrame, bitmapPool, paint);
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
    public void testShouldNotDrawNullBitmapFrame() {
        Canvas canvas = mock(Canvas.class);
        drawable = new GifDrawable(gifDecoder, frameLoader, firstFrame, bitmapPool, paint);
        drawable.onFrameReady(0);
        when(frameLoader.getCurrentFrame()).thenReturn(null);
        drawable.draw(canvas);

        verify(canvas).drawBitmap(eq(firstFrame), anyRect(), anyRect(), anyPaint());
        verify(canvas, never()).drawBitmap((Bitmap) isNull(), anyRect(), anyRect(), anyPaint());
    }

    @Test
    public void testDoesNotDrawNullFirstFrame() {
        drawable = new GifDrawable(gifDecoder, frameLoader, null, bitmapPool, paint);
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

        verify(frameLoader).start();
    }

    @Test
    public void testRequestsNextFrameOnStartWithoutCallToSetVisible() {
        drawable.start();

        verify(frameLoader).start();
    }

    @Test
    public void testDoesNotRequestNextFrameOnStartIfGotCallToSetVisibleWithVisibleFalse() {
        drawable.setVisible(false, false);
        drawable.start();

        verify(frameLoader, never()).start();
    }

    @Test
    public void testDoesNotRequestNextFrameOnStartIfHasSingleFrame() {
        when(gifDecoder.getFrameCount()).thenReturn(1);
        drawable.setVisible(true, false);
        drawable.start();

        verify(frameLoader, never()).start();
    }

    @Test
    public void testInvalidatesSelfOnStartIfHasSingleFrame() {
        when(gifDecoder.getFrameCount()).thenReturn(1);
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

        verify(frameLoader, times(1)).start();
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

        verify(frameLoader).stop();
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
    public void testResetsFrameManagerWhenCurrentFinishesIfHasNoCallbackAndIsAtLeastAtHoneycomb() {
        drawable.setIsRunning(true);
        drawable.setCallback(null);
        drawable.onFrameReady(0);

        verify(frameLoader).clear();
    }

    @Test
    public void testDoesNotResetFrameManagerWhenCurrentFinishesIfHasNoCallbackPreHoneycomb() {
        Util.setSdkVersionInt(10);

        drawable.setIsRunning(true);
        drawable.setCallback(null);
        drawable.onFrameReady(0);

        verify(frameLoader, never()).clear();
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
    public void testDoesNotResetOnStopIfAtLeastAtHoneycomb() {
        drawable.start();
        drawable.stop();

        verify(frameLoader, never()).clear();
        // invalidate once from start.
        verify(cb, times(1)).invalidateDrawable(eq(drawable));
    }

    @Test
    public void testDoesResetOnStopIfPreHoneycomb() {
        Util.setSdkVersionInt(10);
        drawable.start();
        drawable.stop();

        verify(frameLoader).clear();
        verify(cb, times(2)).invalidateDrawable(eq(drawable));
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
        when(gifDecoder.getFrameCount()).thenReturn(expected);

        assertEquals(expected, drawable.getFrameCount());
    }

    @Test
    public void testRecycleCallsClearOnFrameManager() {
        drawable.recycle();

        verify(frameLoader).clear();
    }

    @Test
    public void testRecycleReturnsFirstFrameToPool() {
        drawable.recycle();

        verify(bitmapPool).put(eq(firstFrame));
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
    public void testReturnsNewDrawableFromConstantState() {
        GifHeader gifHeader = new GifHeader();
        Transformation<Bitmap> transformation = mock(Transformation.class);
        GifDecoder.BitmapProvider provider = mock(GifDecoder.BitmapProvider.class);
        Bitmap firstFrame = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        drawable = new GifDrawable(Robolectric.application, provider, bitmapPool, transformation, 100, 100,  gifHeader,
                new byte[0], firstFrame);

        assertNotNull(drawable.getConstantState().newDrawable());
        assertNotNull(drawable.getConstantState().newDrawable(Robolectric.application.getResources()));
    }

    @Test
    public void testReturnsFrameWidthAndHeightForIntrinsictDimensions() {
        assertEquals(frameWidth, drawable.getIntrinsicWidth());
        assertEquals(frameHeight, drawable.getIntrinsicHeight());
    }

    @Test
    public void testIsAnimated() {
        assertTrue(drawable.isAnimated());
    }

    @Test
    public void testLoopsASingleTimeIfLoopCountIsSetToOne() {
        final int loopCount = 1;
        final int frameCount = 2;
        when(gifDecoder.getFrameCount()).thenReturn(frameCount);
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

        when(gifDecoder.getFrameCount()).thenReturn(frameCount);
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

        when(gifDecoder.getFrameCount()).thenReturn(frameCount);
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

        when(gifDecoder.getFrameCount()).thenReturn(frameCount);
        drawable.setLoopCount(loopCount);
        drawable.setVisible(true, true);
        drawable.start();

        runLoops(loopCount, frameCount);

        verifyRanLoops(loopCount, frameCount);
        assertFalse(drawable.isRunning());
    }

    @Test
    public void testCallingStartResetsLoopCounter() {
        when(gifDecoder.getFrameCount()).thenReturn(2);
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

        when(gifDecoder.getFrameCount()).thenReturn(frameCount);
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
        when(gifDecoder.getLoopCount()).thenReturn(loopCount);
        when(gifDecoder.getFrameCount()).thenReturn(frameCount);
        drawable.setLoopCount(GlideDrawable.LOOP_INTRINSIC);
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
        Transformation<Bitmap> transformation = mock(Transformation.class);
        Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        drawable.setFrameTransformation(transformation, bitmap);

        verify(frameLoader).setFrameTransformation(eq(transformation));
    }

    @Test(expected = NullPointerException.class)
    public void testThrowsIfSetFrameTransformationIsCalledWithANullFrame() {
        drawable.setFrameTransformation(mock(Transformation.class), null);
    }

    @Test(expected = NullPointerException.class)
    public void testThrowsIfSetFrameTransformationIsCalledWithANullTransformation() {
        drawable.setFrameTransformation(null, Bitmap.createBitmap(100, 100, Bitmap.Config.RGB_565));
    }

    @Test(expected = NullPointerException.class)
    public void testThrowsIfConstructedWIthNullFirstFrame() {
        new GifDrawable(Robolectric.application, mock(GifDecoder.BitmapProvider.class), mock(BitmapPool.class),
                mock(Transformation.class), 100, 100, mock(GifHeader.class), new byte[0], null);
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
    public void testGetDecoderReturnsGivenDecoder() {
        assertEquals(gifDecoder, drawable.getDecoder());
    }

    @Test
    public void testReturnsCurrentTransformationInGetFrameTransformation() {
        Transformation<Bitmap> newTransformation = mock(Transformation.class);
        drawable.setFrameTransformation(newTransformation, Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888));

        assertEquals(newTransformation, drawable.getFrameTransformation());
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
