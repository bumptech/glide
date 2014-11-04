package com.bumptech.glide.load;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bumptech.glide.load.engine.Resource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;

@RunWith(JUnit4.class)
public class MultiTransformationTest {
    @Test
    public void testReturnsConcatenatedTransformationIds() {
        String firstId = "firstId";
        Transformation first = mock(Transformation.class);
        when(first.getId()).thenReturn(firstId);
        String secondId = "secondId";
        Transformation second = mock(Transformation.class);
        when(second.getId()).thenReturn(secondId);
        String thirdId = "thirdId";
        Transformation third = mock(Transformation.class);
        when(third.getId()).thenReturn(thirdId);

        MultiTransformation transformation = new MultiTransformation(first, second, third);

        final String expected = firstId + secondId + thirdId;
        assertEquals(expected, transformation.getId());

        ArrayList<Transformation> transformations = new ArrayList<Transformation>();
        transformations.add(first);
        transformations.add(second);
        transformations.add(third);

        transformation = new MultiTransformation(transformations);

        assertEquals(expected, transformation.getId());
    }

    @Test
    public void testAppliesTransformationsInOrder() {
        final int width = 584;
        final int height = 768;

        Resource initial = mock(Resource.class);

        Resource firstTransformed = mock(Resource.class);
        Transformation first = mock(Transformation.class);
        when(first.transform(eq(initial), eq(width), eq(height))).thenReturn(firstTransformed);

        Resource secondTransformed = mock(Resource.class);
        Transformation second = mock(Transformation.class);
        when(second.transform(eq(firstTransformed), eq(width), eq(height))).thenReturn(secondTransformed);

        MultiTransformation transformation = new MultiTransformation(first, second);

        assertEquals(secondTransformed, transformation.transform(initial, width, height));
    }

    @Test
    public void testInitialResourceIsNotRecycled() {
        Resource initial = mock(Resource.class);

        Resource transformed = mock(Resource.class);
        Transformation first = mock(Transformation.class);
        when(first.transform(any(Resource.class), anyInt(), anyInt())).thenReturn(transformed);

        MultiTransformation transformation = new MultiTransformation(first);

        transformation.transform(initial, 123, 456);

        verify(initial, never()).recycle();
    }

    @Test
    public void testInitialResourceIsNotRecycledEvenIfReturnedByMultipleTransformations() {
        Resource initial = mock(Resource.class);
        Transformation first = mock(Transformation.class);
        when(first.transform(any(Resource.class), anyInt(), anyInt())).thenReturn(initial);
        Transformation second = mock(Transformation.class);
        when(second.transform(any(Resource.class), anyInt(), anyInt())).thenReturn(initial);

        MultiTransformation transformation = new MultiTransformation(first, second);
        transformation.transform(initial, 1111, 2222);

        verify(initial, never()).recycle();
    }

    @Test
    public void testInitialResourceIsNotRecycledIfReturnedByOneTransformationButNotByALaterTransformation() {
        Resource initial = mock(Resource.class);
        Transformation first = mock(Transformation.class);
        when(first.transform(any(Resource.class), anyInt(), anyInt())).thenReturn(initial);
        Transformation second = mock(Transformation.class);
        when(second.transform(any(Resource.class), anyInt(), anyInt())).thenReturn(mock(Resource.class));

        MultiTransformation transformation = new MultiTransformation(first, second);
        transformation.transform(initial, 1, 2);

        verify(initial, never()).recycle();
    }

    @Test
    public void testFinalResourceIsNotRecycled() {
        Resource transformed = mock(Resource.class);
        Transformation first = mock(Transformation.class);
        when(first.transform(any(Resource.class), anyInt(), anyInt())).thenReturn(transformed);

        MultiTransformation transformation = new MultiTransformation(first);

        transformation.transform(mock(Resource.class), 111, 222);

        verify(transformed, never()).recycle();
    }

    @Test
    public void testIntermediateResourcesAreRecycled() {
        Resource firstTransformed = mock(Resource.class);
        Transformation first = mock(Transformation.class);
        when(first.transform(any(Resource.class), anyInt(), anyInt())).thenReturn(firstTransformed);

        Resource secondTransformed = mock(Resource.class);
        Transformation second = mock(Transformation.class);
        when(second.transform(any(Resource.class), anyInt(), anyInt())).thenReturn(secondTransformed);

        MultiTransformation transformation = new MultiTransformation(first, second);

        transformation.transform(mock(Resource.class), 233, 454);

        verify(firstTransformed).recycle();
    }
}
