package com.bumptech.glide.request.target;

import android.graphics.Bitmap;
import android.widget.ImageView;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import static junit.framework.Assert.assertNotNull;

@RunWith(RobolectricTestRunner.class)
public class ImageViewTargetFactoryTest {
    private ImageViewTargetFactory factory;
    private ImageView view;

    @Before
    public void setUp() {
        factory = new ImageViewTargetFactory();
        view = new ImageView(Robolectric.application);
    }

    @Test
    public void testReturnsTargetForBitmaps() {
        assertNotNull(factory.buildTarget(view, Bitmap.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowsForUnknownType() {
        factory.buildTarget(view, Object.class);
    }
}
