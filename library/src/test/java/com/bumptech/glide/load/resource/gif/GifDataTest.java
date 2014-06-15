package com.bumptech.glide.load.resource.gif;

import android.graphics.Bitmap;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.gif.decoder.GifHeader;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotSame;
import static junit.framework.Assert.assertTrue;
import static junit.framework.TestCase.assertNotNull;
import static org.mockito.Mockito.mock;

@RunWith(RobolectricTestRunner.class)
public class GifDataTest {
    private GifData data;
    private byte[] bytes;

    @Before
    public void setUp() {
        BitmapPool bitmapPool = mock(BitmapPool.class);
        GifHeader header = mock(GifHeader.class);
        bytes = new byte[] { 'G', 'I', 'F' };
        data = new GifData(Robolectric.application, bitmapPool, "gifId", header, bytes, 123, 456);
    }

    @Test
    public void testReturnsDecoderByteSize() {
        assertEquals(bytes.length, data.getByteSize());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testReturnsSetTransformation(){
        Transformation<Bitmap> transformation = mock(Transformation.class);

        data.setFrameTransformation(transformation);
        assertEquals(transformation, data.getFrameTransformation());
    }

    @Test
    public void testReturnsDifferentDrawables() {
        GifDrawable first = data.getDrawable();
        GifDrawable second = data.getDrawable();

        assertNotSame(first, second);
    }

    @Test
    public void testCallsRecycleOnAllReturnedDrawablesWhenRecycled() {
        List<GifDrawable> drawables = new ArrayList<GifDrawable>();
        for (int i = 0; i < 10; i++) {
            drawables.add(data.getDrawable());
        }
        data.recycle();
        for (GifDrawable drawable : drawables) {
            assertTrue(drawable.isRecycled());
        }
    }

    @Test
    public void testReturnsNonNullDrawable() {
        assertNotNull(data.getDrawable());
    }
}
