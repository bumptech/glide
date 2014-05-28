package com.bumptech.glide.resize.request;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.animation.Animation;
import com.bumptech.glide.loader.bitmap.model.ModelLoader;
import com.bumptech.glide.loader.bitmap.resource.ResourceFetcher;
import com.bumptech.glide.resize.Engine;
import com.bumptech.glide.resize.Metadata;
import com.bumptech.glide.resize.Priority;
import com.bumptech.glide.resize.RequestContext;
import com.bumptech.glide.resize.Resource;
import com.bumptech.glide.resize.ResourceCallback;
import com.bumptech.glide.resize.ResourceDecoder;
import com.bumptech.glide.resize.ResourceEncoder;
import com.bumptech.glide.resize.load.DecodeFormat;
import com.bumptech.glide.resize.target.Target;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class BitmapRequestTest {
    private RequestHarness harness;

    private static class RequestHarness {
        Engine engine = mock(Engine.class);
        RequestContext requestContext = mock(RequestContext.class);
        Object model = new Object();
        Target target = mock(Target.class);
        Context context = Robolectric.application;


        public RequestHarness() {
            when(requestContext.getModelLoader(eq(Object.class), eq(Object.class)))
                    .thenReturn(mock(ModelLoader.class));
        }

        public BitmapRequestBuilder<Object, Object> getBuilder() {
            return new BitmapRequestBuilder<Object, Object>(Object.class)
                    .setContext(context)
                    .setRequestContext(requestContext)
                    .setEngine(engine)
                    .setTarget(target)
                    .setPriority(Metadata.DEFAULT.getPriority())
                    .setDecodeFormat(Metadata.DEFAULT.getDecodeFormat())
                    .setModel(model);
        }
    }

    @Before
    public void setUp() {
        harness = new RequestHarness();
    }

    @Test
    public void testIsNotCompleteBeforeReceiveingResource() {
        BitmapRequest request = harness.getBuilder().build();

        assertFalse(request.isComplete());
    }

    @Test
    public void testIsCompleteAfterReceivingResource() {
        BitmapRequest request = harness.getBuilder().build();

        request.onResourceReady(mock(Resource.class));

        assertTrue(request.isComplete());
    }

    @Test
    public void testIsNotFailedWithoutException() {
        BitmapRequest request = harness.getBuilder().build();

        assertFalse(request.isFailed());
    }

    @Test
    public void testIsFailedAfterException() {
        BitmapRequest request = harness.getBuilder().build();

        request.onException(new Exception("test"));
        assertTrue(request.isFailed());
    }

    @Test
    public void testEngineLoadPassedCorrectMetadata() {
        Metadata expected = new Metadata(Priority.IMMEDIATE, DecodeFormat.ALWAYS_ARGB_8888);
        BitmapRequest request = harness.getBuilder()
                .setPriority(expected.getPriority())
                .setDecodeFormat(expected.getDecodeFormat())
                .build();


        request.onSizeReady(100, 100);

        verify(harness.engine).load(anyString(), anyInt(), anyInt(), any(ResourceDecoder.class),
                any(ResourceFetcher.class), any(ResourceDecoder.class), any(ResourceEncoder.class), eq(expected),
                any(ResourceCallback.class));
    }

    @Test
    public void testEngineLoadCancelledOnCancel() {
        Engine.LoadStatus loadStatus = mock(Engine.LoadStatus.class);
        when(harness.engine.load(anyString(), anyInt(), anyInt(), any(ResourceDecoder.class), any(ResourceFetcher.class),
                any(ResourceDecoder.class), any(ResourceEncoder.class), any(Metadata.class),
                any(ResourceCallback.class))).thenReturn(loadStatus);

        BitmapRequest request = harness.getBuilder().build();

        request.onSizeReady(100, 100);
        request.cancel();

        verify(loadStatus).cancel();
    }

    @Test
    public void testResourceIsRecycledOnClear() {
        Resource<Bitmap> resource = mock(Resource.class);

        BitmapRequest request = harness.getBuilder().build();

        request.onResourceReady(resource);
        request.clear();

        verify(harness.engine).recycle(eq(resource));
    }

    @Test
    public void testPlaceholderResourceIsSet() {
        final int expectedId = 12345;
        Drawable expected = new ColorDrawable(Color.RED);

        Context context = mockContextToReturn(expectedId, expected);
        MockTarget target = new MockTarget();

        BitmapRequest request = harness.getBuilder()
                .setContext(context)
                .setPlaceholderResource(expectedId)
                .setTarget(target)
                .build();
        request.run();

        assertEquals(expected, target.currentPlaceholder);
    }

    @Test
    public void testPlaceholderDrawableIsSet() {
        Drawable expected = new ColorDrawable(Color.RED);

        MockTarget target = new MockTarget();

        BitmapRequest request = harness.getBuilder()
                .setPlaceholderDrawable(expected)
                .setTarget(target)
                .build();
        request.run();

        assertEquals(expected, target.currentPlaceholder);
    }

    @Test
    public void testErrorResourceIsSetOnLoadFailed() {
        final int expectedId = 12345;
        Drawable expected = new ColorDrawable(Color.RED);

        Context context = mockContextToReturn(expectedId, expected);
        MockTarget target = new MockTarget();

        BitmapRequest request = harness.getBuilder()
                .setContext(context)
                .setErrorResource(expectedId)
                .setTarget(target)
                .build();

        request.onException(null);

        assertEquals(expected, target.currentPlaceholder);
    }

    @Test
    public void testErrorDrawableIsSetOnLoadFailed() {
        Drawable expected = new ColorDrawable(Color.RED);

        MockTarget target = new MockTarget();

        BitmapRequest request = harness.getBuilder()
                .setErrorDrawable(expected)
                .setTarget(target)
                .build();

        request.onException(null);

        assertEquals(expected, target.currentPlaceholder);
    }

    @Test
    public void setTestPlaceholderDrawableSetOnNullModel() {
        Drawable placeholder = new ColorDrawable(Color.RED);


        MockTarget target = new MockTarget();

        BitmapRequest request = harness.getBuilder()
                .setPlaceholderDrawable(placeholder)
                .setTarget(target)
                .setModel(null)
                .build();

        request.run();

        assertEquals(placeholder, target.currentPlaceholder);
    }

    @Test
    public void testErrorDrawableSetOnNullModel() {
        Drawable placeholder = new ColorDrawable(Color.RED);
        Drawable errorPlaceholder = new ColorDrawable(Color.GREEN);

        MockTarget target = new MockTarget();

        BitmapRequest request = harness.getBuilder()
                .setPlaceholderDrawable(placeholder)
                .setErrorDrawable(errorPlaceholder)
                .setTarget(target)
                .setModel(null)
                .build();

        request.run();

        assertEquals(errorPlaceholder, target.currentPlaceholder);
    }

    private Context mockContextToReturn(int resourceId, Drawable drawable) {
        Resources resources = mock(Resources.class);
        Context context = mock(Context.class);

        when(context.getApplicationContext()).thenReturn(context);
        when(context.getResources()).thenReturn(resources);
        when(resources.getDrawable(eq(resourceId))).thenReturn(drawable);

        return context;
    }

    private static class MockTarget implements Target {
        private Drawable currentPlaceholder;

        @Override
        public void onImageReady(Bitmap bitmap) {
            currentPlaceholder = null;
        }

        @Override
        public void setPlaceholder(Drawable placeholder) {
            currentPlaceholder = placeholder;
        }

        @Override
        public void getSize(SizeReadyCallback cb) {
        }

        @Override
        public void startAnimation(Animation animation) {
        }

        @Override
        public void setRequest(Request request) {
        }

        @Override
        public Request getRequest() {
            return null;
        }
    }
}
