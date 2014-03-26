package com.bumptech.glide.resize;

import android.graphics.Bitmap;
import android.test.AndroidTestCase;
import com.bumptech.glide.loader.bitmap.resource.ResourceFetcher;
import com.bumptech.glide.resize.bitmap_recycle.BitmapPool;
import com.bumptech.glide.resize.load.BitmapDecoder;
import com.bumptech.glide.resize.load.Transformation;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ImageVideoBitmapLoadTaskTest extends AndroidTestCase {
    private static final int IMAGE_SIDE = 235;

    public void testLoadsOnlyWithImageLoaderIfImageLoaderSucceeds() throws Exception {
        Object imageFetcherResult = new Object();
        ResourceFetcher imageFetcher = mock(ResourceFetcher.class);
        when(imageFetcher.loadResource()).thenReturn(imageFetcherResult);

        Bitmap expected = Bitmap.createBitmap(10, 10, Bitmap.Config.ALPHA_8);
        BitmapDecoder imageDecoder = mock(BitmapDecoder.class);
        when(imageDecoder.decode(eq(imageFetcherResult), any(BitmapPool.class), anyInt(), anyInt()))
                .thenReturn(expected);

        ResourceFetcher videoFetcher = mock(ResourceFetcher.class);
        BitmapDecoder videoDecoder = mock(BitmapDecoder.class);

        Transformation transformation = mock(Transformation.class);
        when(transformation.transform(eq(expected), any(BitmapPool.class), anyInt(), anyInt()))
                .thenReturn(expected);

        ImageVideoBitmapLoad task = new ImageVideoBitmapLoad(imageFetcher, imageDecoder, videoFetcher, videoDecoder,
                transformation, IMAGE_SIDE, IMAGE_SIDE);

        Bitmap result = task.load(mock(BitmapPool.class));
        assertEquals(expected, result);

        verify(videoFetcher, never()).loadResource();
        verify(videoDecoder, never()).decode(anyObject(), any(BitmapPool.class), anyInt(), anyInt());
    }

    public void testLoadsWithVideoLoaderIfImageLoaderFails() throws Exception {
        Bitmap expected = Bitmap.createBitmap(10, 10, Bitmap.Config.ALPHA_8);
        Bitmap notExpected = Bitmap.createBitmap(9, 9, Bitmap.Config.ARGB_8888);

        ImageVideoBitmapLoad task = createBaseBitmapLoadTask(null, notExpected, new Object(), expected);

        Bitmap result = task.load(mock(BitmapPool.class));

        assertEquals(expected, result);
    }

    public void testLoadsWithVideoLoaderIfImageDecoderFails() throws Exception {
        Bitmap expected = Bitmap.createBitmap(10, 10, Bitmap.Config.ALPHA_8);
        ImageVideoBitmapLoad task = createBaseBitmapLoadTask(new Object(), null, new Object(), expected);

        Bitmap result = task.load(mock(BitmapPool.class));

        assertEquals(expected, result);
    }

    public void testReturnsNullIfImageAndVideoLoadsFail() throws Exception {
        ImageVideoBitmapLoad task = createBaseBitmapLoadTask(new Object(), null, new Object(), null);

        Bitmap result = task.load(mock(BitmapPool.class));

        assertEquals(null, result);
    }

    public void testTransformsImageResult() throws Exception {
        Bitmap fromImage = Bitmap.createBitmap(10, 10, Bitmap.Config.ALPHA_8);
        Bitmap transformed = Bitmap.createBitmap(9, 9, Bitmap.Config.ARGB_8888);

        Transformation transformation = mock(Transformation.class);
        when(transformation.transform(eq(fromImage), any(BitmapPool.class), anyInt(), anyInt()))
                .thenReturn(transformed);

        ImageVideoBitmapLoad task = createBaseBitmapLoadTask(new Object(), fromImage, null, null, transformation);

        Bitmap result = task.load(mock(BitmapPool.class));

        assertEquals(transformed, result);
    }

    public void testTransformsVideoResult() throws Exception {
        Bitmap fromVideo = Bitmap.createBitmap(10, 10, Bitmap.Config.ALPHA_8);
        Bitmap transformed = Bitmap.createBitmap(9, 9, Bitmap.Config.ARGB_8888);

        Transformation transformation = mock(Transformation.class);
        when(transformation.transform(eq(fromVideo), any(BitmapPool.class), anyInt(), anyInt()))
                .thenReturn(transformed);

        ImageVideoBitmapLoad task = createBaseBitmapLoadTask(null, null, new Object(), fromVideo, transformation);

        Bitmap result = task.load(mock(BitmapPool.class));

        assertEquals(transformed, result);
    }

    public void testTransformationIsGivenImageSize() throws Exception {
        Transformation transformation = mock(Transformation.class);

        int width = 123;
        int height = 456;
        ImageVideoBitmapLoad task = createBaseBitmapLoadTask(new Object(),
                Bitmap.createBitmap(10, 10, Bitmap.Config.ALPHA_8), new Object(),
                Bitmap.createBitmap(10, 10, Bitmap.Config.ALPHA_8), transformation, width, height);

        task.load(mock(BitmapPool.class));

        verify(transformation).transform(any(Bitmap.class), any(BitmapPool.class), eq(width), eq(height));
    }

    public void testGeneratesIdWithAllArguments() {
        List<String> allIds = new ArrayList<String>();
        String initialId = getGeneratedIdWithMutatedArgumentIdAt(-1);
        for (int i = 0; i < 6; i++) {
            allIds.add(getGeneratedIdWithMutatedArgumentIdAt(i));
        }
        for (String id : allIds) {
            assertFalse(initialId.equals(id));
        }
    }

    private ImageVideoBitmapLoad createBaseBitmapLoadTask(Object imageFetcherResult, Bitmap imageDecoderResult,
            Object videoFetcherResult, Bitmap videoDecoderResult) throws Exception {
        return createBaseBitmapLoadTask(imageFetcherResult, imageDecoderResult, videoFetcherResult, videoDecoderResult,
                null);
    }

    private ImageVideoBitmapLoad createBaseBitmapLoadTask(Object imageFetcherResult, Bitmap imageDecoderResult,
            Object videoFetcherResult, Bitmap videoDecoderResult, Transformation transformation) throws Exception {
        return createBaseBitmapLoadTask(imageFetcherResult, imageDecoderResult, videoFetcherResult, videoDecoderResult,
                transformation, IMAGE_SIDE, IMAGE_SIDE);
    }

    private ImageVideoBitmapLoad createBaseBitmapLoadTask(Object imageFetcherResult, Bitmap imageDecoderResult,
            Object videoFetcherResult, Bitmap videoDecoderResult, Transformation transformation, int width, int height) throws Exception {
           ResourceFetcher imageFetcher = mock(ResourceFetcher.class);
        when(imageFetcher.loadResource()).thenReturn(imageFetcherResult);

        BitmapDecoder imageDecoder = mock(BitmapDecoder.class);
        when(imageDecoder.decode(anyObject(), any(BitmapPool.class), anyInt(), anyInt()))
                .thenReturn(imageDecoderResult);

        ResourceFetcher videoFetcher = mock(ResourceFetcher.class);
        when(videoFetcher.loadResource()).thenReturn(videoFetcherResult);

        BitmapDecoder videoDecoder = mock(BitmapDecoder.class);
        when(videoDecoder.decode(anyObject(), any(BitmapPool.class), anyInt(), anyInt()))
                .thenReturn(videoDecoderResult);

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

        return new ImageVideoBitmapLoad(imageFetcher, imageDecoder, videoFetcher, videoDecoder,
                transformation, width, height);
    }

    private String getGeneratedIdWithMutatedArgumentIdAt(int argumentIndex) {
        String imageLoaderId = "ImageLoaderId" + (argumentIndex == 0 ? "1" : "");
        ResourceFetcher imageLoader = mock(ResourceFetcher.class);
        when(imageLoader.getId()).thenReturn(imageLoaderId);

        String imageDecoderId = "ImageDecoderId" + (argumentIndex == 1 ? "1" : "");
        BitmapDecoder imageDecoder = mock(BitmapDecoder.class);
        when(imageDecoder.getId()).thenReturn(imageDecoderId);

        String videoLoaderId = "VideoLoaderId" + (argumentIndex == 2 ? "1" : "");
        ResourceFetcher videoLoader = mock(ResourceFetcher.class);
        when(videoLoader.getId()).thenReturn(videoLoaderId);

        String videoDecoderId = "VideoDecoderId" + (argumentIndex == 3 ? "1" : "");
        BitmapDecoder videoDecoder = mock(BitmapDecoder.class);
        when(videoDecoder.getId()).thenReturn(videoDecoderId);

        String transformationId = "TransformationId" + (argumentIndex == 4 ? "1" : "");
        Transformation transformation = mock(Transformation.class);
        when(transformation.getId()).thenReturn(transformationId);

        int width = 1234 + (argumentIndex == 5 ? 1 : 0);
        int height = 5678 + (argumentIndex == 6 ? 1 : 0);

        ImageVideoBitmapLoad task = new ImageVideoBitmapLoad(imageLoader, imageDecoder, videoLoader, videoDecoder,
                transformation, width, height);
        return task.getId();
    }
}
