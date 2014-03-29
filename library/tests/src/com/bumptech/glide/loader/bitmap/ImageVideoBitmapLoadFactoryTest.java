package com.bumptech.glide.loader.bitmap;

import android.test.AndroidTestCase;
import com.bumptech.glide.loader.bitmap.model.ModelLoader;
import com.bumptech.glide.loader.bitmap.transformation.TransformationLoader;
import com.bumptech.glide.resize.load.BitmapDecoder;
import com.bumptech.glide.resize.load.Transformation;
import junit.framework.Assert;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
public class ImageVideoBitmapLoadFactoryTest extends AndroidTestCase {
    // Not magic, just a non 0 number.
    private static final int IMAGE_SIDE = 200;
    private static final String TRANSFORMATION_ID = "id";
    private TransformationLoader transformationLoader;

    @Override
    protected void setUp() throws Exception {
        transformationLoader = mock(TransformationLoader.class);
        Transformation transformation = mock(Transformation.class);
        when(transformation.getId()).thenReturn(TRANSFORMATION_ID);
        when(transformationLoader.getTransformation(any(Object.class))).thenReturn(transformation);
    }

    public void testIgnoresNullImageLoadFactory() {
        ImageVideoBitmapLoadFactory factory = new ImageVideoBitmapLoadFactory(null,
                mock(ResourceBitmapLoadFactory.class),
                transformationLoader);

        assertNotNull(factory.getLoadTask(new Object(), IMAGE_SIDE, IMAGE_SIDE));
    }

    public void testIgnoresNullVideoLoadFactory() {
        ImageVideoBitmapLoadFactory factory = new ImageVideoBitmapLoadFactory(mock(ResourceBitmapLoadFactory.class),
                null, transformationLoader);

        assertNotNull(factory.getLoadTask(new Object(), IMAGE_SIDE, IMAGE_SIDE));
    }

    public void testThrowsIfNullVideoAndNullImageLoaders() {
        try {
            new ImageVideoBitmapLoadFactory(null,
                    null, transformationLoader);
            Assert.fail("Expected IllegalArgumentException with null video and image loaders");
        } catch (IllegalArgumentException e) { }
    }

    public void testThrowsWithNullTransformationLoader() {
        try {
            new ImageVideoBitmapLoadFactory(mock(ResourceBitmapLoadFactory.class),
                    mock(ResourceBitmapLoadFactory.class), null);
            Assert.fail("Expected IllegalArgumentException with null transformation loader");
        } catch (IllegalArgumentException e) { }
    }
}
