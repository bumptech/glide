package com.bumptech.glide.load.resource.gif;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import com.bumptech.glide.gifdecoder.GifDecoder;
import com.bumptech.glide.gifdecoder.GifHeader;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.tests.GlideShadowLooper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = GlideShadowLooper.class)
public class GifDrawableTest {
    private GifDecoder gifDecoder;
    private GifDrawable drawable;
    private GifFrameManager frameManager = mock(GifFrameManager.class);
    private Drawable.Callback cb = mock(Drawable.Callback.class);
    private int frameHeight;
    private int frameWidth;
    private Bitmap firstFrame;
    private BitmapPool bitmapPool;

    @Before
    public void setUp() {
        frameWidth = 120;
        frameHeight = 450;
        gifDecoder = mock(GifDecoder.class);
        firstFrame = Bitmap.createBitmap(frameWidth, frameHeight, Bitmap.Config.RGB_565);
        bitmapPool = mock(BitmapPool.class);
        drawable = new GifDrawable(gifDecoder, frameManager, firstFrame, bitmapPool);
        drawable.setCallback(cb);
    }

    @Test
    public void testShouldDrawFirstFrameBeforeAnyFrameRead() {
        Canvas canvas = mock(Canvas.class);
        drawable.draw(canvas);

        verify(canvas).drawBitmap(eq(firstFrame), anyInt(), anyInt(), any(Paint.class));
    }

    @Test
    public void testShouldNotDrawNullBitmapFrame() {
        Canvas canvas = mock(Canvas.class);
        drawable = new GifDrawable(gifDecoder, frameManager, firstFrame, bitmapPool);
        drawable.onFrameRead(null, 0);
        drawable.draw(canvas);

        verify(canvas).drawBitmap(eq(firstFrame), anyInt(), anyInt(), any(Paint.class));
        verify(canvas, never()).drawBitmap((Bitmap) isNull(), anyInt(), anyInt(), any(Paint.class));
    }

    @Test
    public void testDoesNotDrawNullFirstFrame() {
        drawable = new GifDrawable(gifDecoder, frameManager, null, bitmapPool);
        Canvas canvas = mock(Canvas.class);

        verify(canvas, never()).drawBitmap(any(Bitmap.class), anyInt(), anyInt(), any(Paint.class));
    }

    @Test
    public void testRequestsNextFrameOnStart() {
        drawable.setVisible(true, true);
        drawable.start();

        verify(frameManager).getNextFrame(eq(drawable));
    }

    @Test
    public void testRequestsNextFrameOnStartWithoutCallToSetVisible() {
        drawable.start();

        verify(frameManager).getNextFrame(eq(drawable));
    }

    @Test
    public void testDoesNotRequestNextFrameOnStartIfGotCallToSetVisibleWithVisibleFalse() {
        drawable.setVisible(false, false);
        drawable.start();

        verify(frameManager, never()).getNextFrame(any(GifFrameManager.FrameCallback.class));
    }

    @Test
    public void testDoesNotRequestNextFrameOnStartIfHasSingleFrame() {
        when(gifDecoder.getFrameCount()).thenReturn(1);
        drawable.setVisible(true, false);
        drawable.start();

        verify(frameManager, never()).getNextFrame(any(GifFrameManager.FrameCallback.class));
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

        verify(frameManager, times(1)).getNextFrame(eq(drawable));
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
    public void testStartsLoadingNextFrameWhenCurrentFinishes() {
        drawable.setIsRunning(true);
        drawable.onFrameRead(Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888), 0);

        verify(frameManager).getNextFrame(eq(drawable));
    }

    @Test
    public void testInvalidatesSelfWhenFrameReady() {
        drawable.setIsRunning(true);
        drawable.onFrameRead(Bitmap.createBitmap(100, 100, Bitmap.Config.RGB_565), 0);

        verify(cb).invalidateDrawable(eq(drawable));
    }

    @Test
    public void testDoesNotStartLoadingNextFrameWhenCurrentFinishesIfNotRunning() {
        drawable.setIsRunning(false);
        drawable.onFrameRead(Bitmap.createBitmap(10, 100, Bitmap.Config.ARGB_8888), 0);

        verify(frameManager, never()).getNextFrame(eq(drawable));
    }

    @Test
    public void testDoesNotStartLoadingNextFrameWhenCurrentFinishesIfHasNoCallback() {
        drawable.setIsRunning(true);
        drawable.setCallback(null);
        drawable.onFrameRead(Bitmap.createBitmap(1, 2, Bitmap.Config.ARGB_8888), 0);

        verify(frameManager, never()).getNextFrame(eq(drawable));
    }

    @Test
    public void testStopsWhenCurrentFrameFinishesIfHasNoCallback() {
        drawable.setIsRunning(true);
        drawable.setCallback(null);
        drawable.onFrameRead(Bitmap.createBitmap(2, 1, Bitmap.Config.ARGB_8888), 0);

        assertFalse(drawable.isRunning());
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
    public void testGetOpacityReturnsTransparentfDecoderHasTransparency() {
        when(gifDecoder.isTransparent()).thenReturn(true);

        assertEquals(PixelFormat.TRANSPARENT, drawable.getOpacity());
    }

    @Test
    public void testGetOpacityReturnsOpaqueIfDecoderDoesNotHaveTransparency() {
        when(gifDecoder.isTransparent()).thenReturn(false);

        assertEquals(PixelFormat.OPAQUE, drawable.getOpacity());
    }

    @Test
    public void testRecycleCallsClearOnFrameManager() {
        drawable.recycle();

        verify(frameManager).clear();
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
        drawable = new GifDrawable(Robolectric.application, provider, bitmapPool, transformation, 100, 100, "fakeId",
                gifHeader, new byte[0], firstFrame);

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

        verify(frameManager, times(loopCount * frameCount)).getNextFrame(eq(drawable));
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

        verify(frameManager, times((loopCount * frameCount) + 1)).getNextFrame(eq(drawable));
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

        verify(frameManager, times(frameCount * loopCount)).getNextFrame(eq(drawable));
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

        verify(frameManager, times(loopCount * frameCount)).getNextFrame(eq(drawable));
        assertFalse(drawable.isRunning());
    }

    @Test
    public void testCallingStartResetsLoopCounter() {
        when(gifDecoder.getFrameCount()).thenReturn(2);
        drawable.setLoopCount(1);
        drawable.setVisible(true, true);
        drawable.start();

        drawable.onFrameRead(getBitmap(), 0);
        drawable.onFrameRead(getBitmap(), 1);

        drawable.start();

        drawable.onFrameRead(getBitmap(), 0);
        drawable.onFrameRead(getBitmap(), 1);

        verify(frameManager, times(4)).getNextFrame(eq(drawable));
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

        int expectedFrames = (initialLoopCount + newLoopCount) * frameCount;
        verify(frameManager, times(expectedFrames)).getNextFrame(eq(drawable));
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

        verify(frameManager, times(frameCount * loopCount)).getNextFrame(eq(drawable));
    }

    private void runLoops(int loopCount, int frameCount) {
        for (int loop = 0; loop < loopCount; loop++) {
            for (int frame = 0; frame < frameCount; frame++) {
                drawable.onFrameRead(getBitmap(), frame);
            }
        }
    }

    private static Bitmap getBitmap() {
        return Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
    }
}
