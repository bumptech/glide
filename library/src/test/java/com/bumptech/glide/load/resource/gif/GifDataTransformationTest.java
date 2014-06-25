package com.bumptech.glide.load.resource.gif;

import android.graphics.Bitmap;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.Transformation;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class GifDataTransformationTest {
    Transformation<Bitmap> wrapped;
    GifDataTransformation transformation;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() {
        wrapped = mock(Transformation.class);
        transformation = new GifDataTransformation(wrapped);
    }

    @Test
    public void testReturnsWrappedTransformationId() {
        final String id = "testId";
        when(wrapped.getId()).thenReturn(id);

        assertEquals(id, transformation.getId());
    }

    @Test
    public void testSetsTransformationAsFrameTransformation() {
        Resource<GifData> resource = mock(Resource.class);
        GifData gifData = mock(GifData.class);
        when(gifData.getFrameTransformation()).thenReturn(Transformation.NONE);
        when(resource.get()).thenReturn(gifData);

        final Resource<Bitmap> toTransform = mock(Resource.class);

        final int width = 123;
        final int height = 456;
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Transformation<Bitmap> transformation = (Transformation<Bitmap>) invocation.getArguments()[0];
                transformation.transform(toTransform, width, height);
                return null;
            }
        }).when(gifData).setFrameTransformation(any(Transformation.class));

        transformation.transform(resource, width, height);

        verify(gifData).setFrameTransformation(any(Transformation.class));
        verify(wrapped).transform(eq(toTransform), eq(width), eq(height));
    }
}
