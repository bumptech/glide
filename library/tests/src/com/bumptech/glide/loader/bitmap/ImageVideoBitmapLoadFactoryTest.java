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

    public void testIgnoresNullImageLoader() {
        ImageVideoBitmapLoadFactory factory = new ImageVideoBitmapLoadFactory(null,
                mock(BitmapDecoder.class), mock(ModelLoader.class), mock(BitmapDecoder.class),
                transformationLoader);

        assertNotNull(factory.getLoadTask(new Object(), IMAGE_SIDE, IMAGE_SIDE));
    }

    public void testIgnoresNullImageDecoder() {
        ImageVideoBitmapLoadFactory factory = new ImageVideoBitmapLoadFactory(mock(ModelLoader.class), null,
                mock(ModelLoader.class), mock(BitmapDecoder.class), transformationLoader);

        assertNotNull(factory);
    }

    public void testIgnoresNullVideoLoader() {
        ImageVideoBitmapLoadFactory factory = new ImageVideoBitmapLoadFactory(mock(ModelLoader.class),
                mock(BitmapDecoder.class), null, mock(BitmapDecoder.class), transformationLoader);

        assertNotNull(factory.getLoadTask(new Object(), IMAGE_SIDE, IMAGE_SIDE));
    }

    public void testIgnoresNullVideoDecoder() {
        ImageVideoBitmapLoadFactory factory = new ImageVideoBitmapLoadFactory(mock(ModelLoader.class), mock(BitmapDecoder.class),
                mock(ModelLoader.class), null, transformationLoader);

        assertNotNull(factory.getLoadTask(new Object(), IMAGE_SIDE, IMAGE_SIDE));
    }

    public void testThrowsIfNullVideoAndNullImageLoaders() {
        try {
            new ImageVideoBitmapLoadFactory(null,
                    mock(BitmapDecoder.class), null, mock(BitmapDecoder.class), transformationLoader);
            Assert.fail("Expected IllegalArgumentException with null video and image loaders");
        } catch (IllegalArgumentException e) { }
    }

    public void testThrowsIfNullVideoAndNullImageDecoders() {
        try {
            new ImageVideoBitmapLoadFactory(mock(ModelLoader.class), null, mock(ModelLoader.class), null,
                    transformationLoader);
            Assert.fail("Expected IllegalArgumentException with null video and image loaders");
        } catch (IllegalArgumentException e) { }
    }

    public void testThrowsWithNullTransformationLoader() {
        try {
            new ImageVideoBitmapLoadFactory(mock(ModelLoader.class), mock(BitmapDecoder.class), mock(ModelLoader.class),
                    mock(BitmapDecoder.class), null);
            Assert.fail("Expected IllegalArgumentException with null transformation loader");
        } catch (IllegalArgumentException e) { }
    }
}
