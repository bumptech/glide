package com.bumptech.glide.load.resource.gif;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

import com.bumptech.glide.gifdecoder.GifDecoder;
import com.bumptech.glide.gifdecoder.GifHeader;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.tests.GlideShadowLooper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
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

    @Before
    public void setUp() {
        gifDecoder = mock(GifDecoder.class);
        drawable = new GifDrawable(gifDecoder, frameManager);
        drawable.setCallback(cb);
    }

    @Test
    public void testReturnsInvalidWidthBeforeFirstFrame() {
        assertEquals(-1, drawable.getIntrinsicWidth());
    }

    @Test
    public void testReturnsInvalidHeightBeforeFirstFrame() {
        assertEquals(-1, drawable.getIntrinsicHeight());
    }

    @Test
    public void testReturnsFrameWidthAfterFirstFrame() {
        int width = 123;
        Bitmap bitmap = Bitmap.createBitmap(width, 1231, Bitmap.Config.ARGB_8888);

        drawable.setIsRunning(true);
        drawable.onFrameRead(bitmap);

        assertEquals(width, drawable.getIntrinsicWidth());
    }

    @Test
    public void testReturnsFrameHeightAfterFirstFrame() {
        int height = 456;
        Bitmap bitmap = Bitmap.createBitmap(1, height, Bitmap.Config.RGB_565);

        drawable.setIsRunning(true);
        drawable.onFrameRead(bitmap);

        assertEquals(height, drawable.getIntrinsicHeight());
    }

    @Test
    public void testShouldNotDrawNullBitmap() {
        Canvas canvas = mock(Canvas.class);
        drawable.draw(canvas);

        verify(canvas, never()).drawBitmap((Bitmap) isNull(), anyInt(), anyInt(), any(Paint.class));
    }

    @Test
    public void testRequestsNextFrameOnStart() {
        drawable.start();

        verify(frameManager).getNextFrame(eq(drawable));
    }

    @Test
    public void testShouldInvalidateSelfOnRun() {
        drawable.start();

        verify(cb).invalidateDrawable(eq(drawable));
    }

    @Test
    public void testShouldNotScheduleItselfIfAlreadyRunning() {
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
        drawable.start();

        assertTrue(drawable.isRunning());
    }

    @Test
    public void testStartsLoadingNextFrameWhenCurrentFinishes() {
        drawable.setIsRunning(true);
        drawable.onFrameRead(Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888));

        verify(frameManager).getNextFrame(eq(drawable));
    }

    @Test
    public void testInvalidatesSelfWhenFrameReady() {
        drawable.setIsRunning(true);
        drawable.onFrameRead(Bitmap.createBitmap(100, 100, Bitmap.Config.RGB_565));

        verify(cb).invalidateDrawable(eq(drawable));
    }

    @Test
    public void testDoesNotStartLoadingNextFrameWhenCurrentFinishesIfNotRunning() {
        drawable.setIsRunning(false);
        drawable.onFrameRead(Bitmap.createBitmap(10, 100, Bitmap.Config.ARGB_8888));

        verify(frameManager, never()).getNextFrame(eq(drawable));
    }

    @Test
    public void testDoesNotStartLoadingNextFrameWhenCurrentFinishesIfHasNoCallback() {
        drawable.setIsRunning(true);
        drawable.setCallback(null);
        drawable.onFrameRead(Bitmap.createBitmap(1, 2, Bitmap.Config.ARGB_8888));

        verify(frameManager, never()).getNextFrame(eq(drawable));
    }

    @Test
    public void testStopsWhenCurrentFrameFinishesIfHasNoCallback() {
        drawable.setIsRunning(true);
        drawable.setCallback(null);
        drawable.onFrameRead(Bitmap.createBitmap(2, 1, Bitmap.Config.ARGB_8888));

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
    public void testStartsOnSetVisibleTrue() {
        drawable.setVisible(true, true);

        assertTrue(drawable.isRunning());
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
        GifHeader gifHeader = mock(GifHeader.class);
        Transformation<Bitmap> transformation = mock(Transformation.class);
        GifDecoder.BitmapProvider provider = mock(GifDecoder.BitmapProvider.class);
        drawable = new GifDrawable("fakeId", gifHeader, new byte[0], Robolectric.application, transformation, 100, 100,
                provider);

        assertNotNull(drawable.getConstantState().newDrawable());
        assertNotNull(drawable.getConstantState().newDrawable(Robolectric.application.getResources()));
    }
}
