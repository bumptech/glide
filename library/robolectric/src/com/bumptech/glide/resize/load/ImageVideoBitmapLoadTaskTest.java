package com.bumptech.glide.resize.load;

import android.graphics.Bitmap;
import com.bumptech.glide.resize.bitmap_recycle.BitmapPool;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class ImageVideoBitmapLoadTaskTest {
    private static final int IMAGE_SIDE = 235;

    @Test
    public void testLoadsOnlyWithImageLoaderIfImageLoaderSucceeds() throws Exception {
        Bitmap expected = Bitmap.createBitmap(10, 10, Bitmap.Config.ALPHA_8);
        BitmapLoad imageLoad = mock(BitmapLoad.class);
        when(imageLoad.load(any(BitmapPool.class))).thenReturn(expected);

        Bitmap notExpected = Bitmap.createBitmap(11, 11, Bitmap.Config.ARGB_8888);
        BitmapLoad videoLoad = mock(BitmapLoad.class);
        when(videoLoad.load(any(BitmapPool.class))).thenReturn(notExpected);

        Transformation transformation = mock(Transformation.class);
        when(transformation.transform(any(Bitmap.class), any(BitmapPool.class), anyInt(), anyInt()))
                .thenAnswer(new Answer<Object>() {
                    @Override
                    public Object answer(InvocationOnMock invocation) throws Throwable {
                        return invocation.getArguments()[0];
                    }
                });

        ImageVideoBitmapLoad task = new ImageVideoBitmapLoad(imageLoad, videoLoad,
                IMAGE_SIDE, IMAGE_SIDE, transformation);

        Bitmap result = task.load(mock(BitmapPool.class));

        assertEquals(expected, result);
        verify(videoLoad, never()).load(any(BitmapPool.class));
    }

    @Test
    public void testLoadsWithImageLoaderIfVideoLoaderFails() throws Exception {
        Bitmap expected = Bitmap.createBitmap(10, 10, Bitmap.Config.ALPHA_8);
        ImageVideoBitmapLoad task = createBaseBitmapLoadTask(expected, null);

        Bitmap result = task.load(mock(BitmapPool.class));

        assertEquals(expected, result);
    }

    @Test
    public void testLoadsWithVideoLoaderIfImageLoadFails() throws Exception {
        Bitmap expected = Bitmap.createBitmap(10, 10, Bitmap.Config.ALPHA_8);
        ImageVideoBitmapLoad task = createBaseBitmapLoadTask(null, expected);

        Bitmap result = task.load(mock(BitmapPool.class));

        assertEquals(expected, result);
    }

    @Test
    public void testReturnsNullIfImageAndVideoLoadsFail() throws Exception {
        ImageVideoBitmapLoad task = createBaseBitmapLoadTask(null, null);

        Bitmap result = task.load(mock(BitmapPool.class));

        assertEquals(null, result);
    }

    @Test
    public void testTransformsImageResult() throws Exception {
        Bitmap fromImage = Bitmap.createBitmap(10, 10, Bitmap.Config.ALPHA_8);
        Bitmap transformed = Bitmap.createBitmap(9, 9, Bitmap.Config.ARGB_8888);

        Transformation transformation = mock(Transformation.class);
        when(transformation.transform(eq(fromImage), any(BitmapPool.class), anyInt(), anyInt()))
                .thenReturn(transformed);

        ImageVideoBitmapLoad task = createBaseBitmapLoadTask(fromImage, null, transformation);

        Bitmap result = task.load(mock(BitmapPool.class));

        assertEquals(transformed, result);
    }

    @Test
    public void testTransformsVideoResult() throws Exception {
        Bitmap fromVideo = Bitmap.createBitmap(10, 10, Bitmap.Config.ALPHA_8);
        Bitmap transformed = Bitmap.createBitmap(9, 9, Bitmap.Config.ARGB_8888);

        Transformation transformation = mock(Transformation.class);
        when(transformation.transform(eq(fromVideo), any(BitmapPool.class), anyInt(), anyInt()))
                .thenReturn(transformed);

        ImageVideoBitmapLoad task = createBaseBitmapLoadTask(null, fromVideo, transformation);

        Bitmap result = task.load(mock(BitmapPool.class));

        assertEquals(transformed, result);
    }

    @Test
    public void testTransformationIsGivenImageSize() throws Exception {
        Transformation transformation = mock(Transformation.class);

        int width = 123;
        int height = 456;
        ImageVideoBitmapLoad task = createBaseBitmapLoadTask(Bitmap.createBitmap(10, 10, Bitmap.Config.ALPHA_8),
                Bitmap.createBitmap(10, 10, Bitmap.Config.ALPHA_8), transformation, width, height);

        task.load(mock(BitmapPool.class));

        verify(transformation).transform(any(Bitmap.class), any(BitmapPool.class), eq(width), eq(height));
    }

    @Test
    public void testGeneratesIdWithAllArguments() {
        List<String> allIds = new ArrayList<String>();
        String initialId = getGeneratedIdWithMutatedArgumentIdAt(-1);
        for (int i = 0; i < 2; i++) {
            allIds.add(getGeneratedIdWithMutatedArgumentIdAt(i));
        }
        for (String id : allIds) {
            assertFalse(initialId.equals(id));
        }
    }

    private ImageVideoBitmapLoad createBaseBitmapLoadTask(Bitmap imageDecoderResult, Bitmap videoDecoderResult)
            throws Exception {
        return createBaseBitmapLoadTask(imageDecoderResult, videoDecoderResult, null);
    }

    private ImageVideoBitmapLoad createBaseBitmapLoadTask(Bitmap imageDecoderResult,
            Bitmap videoDecoderResult, Transformation transformation) throws Exception {
        return createBaseBitmapLoadTask(imageDecoderResult, videoDecoderResult, transformation, IMAGE_SIDE, IMAGE_SIDE);
    }

    private ImageVideoBitmapLoad createBaseBitmapLoadTask(Bitmap imageDecoderResult, Bitmap videoDecoderResult,
            Transformation transformation, int width, int height) throws Exception {

        BitmapLoad imageLoad = mock(BitmapLoad.class);
        when(imageLoad.load(any(BitmapPool.class))).thenReturn(imageDecoderResult);

        BitmapLoad videoLoad = mock(BitmapLoad.class);
        when(videoLoad.load(any(BitmapPool.class))).thenReturn(videoDecoderResult);

        if (transformation == null) {
            transformation = mock(Transformation.class);
            when(transformation.transform(any(Bitmap.class), any(BitmapPool.class), anyInt(), anyInt()))
                    .thenAnswer(new Answer<Object>() {
                        @Override
                        public Object answer(InvocationOnMock invocation) throws Throwable {
                            return invocation.getArguments()[0];
                        }
                    });
        }

        return new ImageVideoBitmapLoad(imageLoad, videoLoad, width, height, transformation);
    }

    private String getGeneratedIdWithMutatedArgumentIdAt(int argumentIndex) {
        String imageLoadId = "ImageLoadId" + (argumentIndex == 0 ? "1" : "");
        BitmapLoad imageLoad = mock(BitmapLoad.class);
        when(imageLoad.getId()).thenReturn(imageLoadId);

        String videoLoadId = "VideoLoadId" + (argumentIndex == 1 ? "1" : "");
        BitmapLoad videoLoad = mock(BitmapLoad.class);
        when(videoLoad.getId()).thenReturn(videoLoadId);

        String transformationId = "TransformationId" + (argumentIndex == 2 ? "1" : "");
        Transformation transformation = mock(Transformation.class);
        when(transformation.getId()).thenReturn(transformationId);

        ImageVideoBitmapLoad task = new ImageVideoBitmapLoad(imageLoad, videoLoad, IMAGE_SIDE, IMAGE_SIDE,
                transformation);
        return task.getId();
    }
}
