package com.bumptech.glide.loader.bitmap;

import com.bumptech.glide.resize.load.Transformation;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static junit.framework.TestCase.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class ImageVideoBitmapLoadFactoryTest {
    // Not magic, just a non 0 number.
    private static final int IMAGE_SIDE = 200;
    private static final String TRANSFORMATION_ID = "id";
    private Transformation transformation;

    @Before
    public void setUp() throws Exception {
        transformation = mock(Transformation.class);
        when(transformation.getId()).thenReturn(TRANSFORMATION_ID);
    }

    @Test
    public void testIgnoresNullImageLoadFactory() {
        ImageVideoBitmapLoadFactory factory = new ImageVideoBitmapLoadFactory(null,
                mock(ResourceBitmapLoadFactory.class),
                transformation);

        assertNotNull(factory.getLoadTask(new Object(), IMAGE_SIDE, IMAGE_SIDE));
    }

    @Test
    public void testIgnoresNullVideoLoadFactory() {
        ImageVideoBitmapLoadFactory factory = new ImageVideoBitmapLoadFactory(mock(ResourceBitmapLoadFactory.class),
                null, transformation);

        assertNotNull(factory.getLoadTask(new Object(), IMAGE_SIDE, IMAGE_SIDE));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowsIfNullVideoAndNullImageLoaders() {
        new ImageVideoBitmapLoadFactory(null, null, transformation);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowsWithNullTransformationLoader() {
        new ImageVideoBitmapLoadFactory(mock(ResourceBitmapLoadFactory.class),
                mock(ResourceBitmapLoadFactory.class), null);
    }
}
