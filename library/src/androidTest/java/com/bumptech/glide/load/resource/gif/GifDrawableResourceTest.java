package com.bumptech.glide.load.resource.gif;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.graphics.Bitmap;

import com.bumptech.glide.util.Util;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, emulateSdk = 18)
public class GifDrawableResourceTest {
    private GifDrawable drawable;
    private GifDrawableResource resource;

    @Before
    public void setUp() {
        drawable = mock(GifDrawable.class);
        resource = new GifDrawableResource(drawable);
    }

    @Test
    public void testReturnsDrawableSizePlusFirstFrameSize() {
        final int size = 2134;
        Bitmap firstFrame = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        when(drawable.getFirstFrame()).thenReturn(firstFrame);
        when(drawable.getData()).thenReturn(new byte[size]);

        assertEquals(size + Util.getBitmapByteSize(firstFrame), resource.getSize());
    }

    @Test
    public void testStopsAndThenRecyclesDrawableWhenRecycled() {
        resource.recycle();

        InOrder inOrder = inOrder(drawable);
        inOrder.verify(drawable).stop();
        inOrder.verify(drawable).recycle();
    }

}
