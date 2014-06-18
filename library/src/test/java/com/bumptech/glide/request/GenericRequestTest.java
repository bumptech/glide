package com.bumptech.glide.request;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import com.bumptech.glide.Priority;
import com.bumptech.glide.Resource;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.engine.Engine;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;
import com.bumptech.glide.provider.LoadProvider;
import com.bumptech.glide.request.target.Target;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import java.io.IOException;
import java.io.InputStream;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class GenericRequestTest {
    private RequestHarness harness;

    @SuppressWarnings("unchecked")
    private static class RequestHarness {
        Engine engine = mock(Engine.class);
        Object model = new Object();
        Target<Object> target = mock(Target.class);
        Context context = Robolectric.application;
        Resource<Object> resource = mock(Resource.class);
        RequestCoordinator requestCoordinator = mock(RequestCoordinator.class);
        Priority priority = Priority.NORMAL;
        int placeholderResourceId = 0;
        Drawable placeholderDrawable = null;
        int errorResourceId = 0;
        Drawable errorDrawable = null;
        LoadProvider<Object, Object, Object, Object> loadProvider = mock(LoadProvider.class);
        ResourceDecoder<InputStream, Object> cacheDecoder = mock(ResourceDecoder.class);
        ResourceDecoder<Object, Object> sourceDecoder = mock(ResourceDecoder.class);
        ResourceEncoder<Object> encoder = mock(ResourceEncoder.class);
        ResourceTranscoder transcoder = mock(ResourceTranscoder.class);
        RequestListener<Object, Object> requestListener = mock(RequestListener.class);
        boolean skipMemoryCache;
        GlideAnimationFactory<Object> factory = mock(GlideAnimationFactory.class);

        public RequestHarness() {
            ModelLoader<Object, Object> modelLoader = mock(ModelLoader.class);
            when(loadProvider.getModelLoader()).thenReturn(modelLoader);
            when(loadProvider.getCacheDecoder()).thenReturn(cacheDecoder);
            when(loadProvider.getSourceDecoder()).thenReturn(sourceDecoder);
            when(loadProvider.getEncoder()).thenReturn(encoder);
            when(loadProvider.getTranscoder()).thenReturn(transcoder);

            when(requestCoordinator.canSetImage(any(Request.class))).thenReturn(true);
            when(requestCoordinator.canSetPlaceholder(any(Request.class))).thenReturn(true);

            when(resource.get()).thenReturn(new Object());
        }

        public GenericRequest<Object, Object, Object, Object> getRequest() {
            return new GenericRequest<Object, Object, Object, Object>(loadProvider, model, context, priority, target,
                    1f, placeholderDrawable, placeholderResourceId, errorDrawable, errorResourceId, requestListener,
                    requestCoordinator, engine, mock(Transformation.class), Object.class, skipMemoryCache, factory);
        }
    }

    @Before
    public void setUp() {
        harness = new RequestHarness();
    }

    @Test(expected = NullPointerException.class)
    public void testThrowsWhenMissingCacheDecoder() {
        when(harness.loadProvider.getCacheDecoder()).thenReturn(null);

        harness.getRequest();
    }

    @Test(expected = NullPointerException.class)
    public void testThrowsWhenMissingSourceDecoder() {
        when(harness.loadProvider.getSourceDecoder()).thenReturn(null);

        harness.getRequest();
    }

    @Test(expected =  NullPointerException.class)
    public void testThrowsWhenMissingEncoder() {
        when(harness.loadProvider.getEncoder()).thenReturn(null);

        harness.getRequest();
    }

    @Test(expected =  NullPointerException.class)
    public void testThrowsWhenMissingTranscoder() {
        when(harness.loadProvider.getTranscoder()).thenReturn(null);

        harness.getRequest();
    }

    @Test(expected = NullPointerException.class)
    public void testThrowsWhenMissingModelLoader() {
        when(harness.loadProvider.getModelLoader()).thenReturn(null);

        harness.getRequest();
    }

    @Test
    public void testIsNotCompleteBeforeReceivingResource() {
        GenericRequest request = harness.getRequest();

        assertFalse(request.isComplete());
    }

    @Test
    public void testCanHandleNullResources() {
        GenericRequest request = harness.getRequest();

        request.onResourceReady(null);

        assertTrue(request.isFailed());
    }

    @Test
    public void testIsCompleteAfterReceivingResource() {
        GenericRequest request = harness.getRequest();

        when(harness.resource.get()).thenReturn(new Object());
        request.onResourceReady(harness.resource);

        assertTrue(request.isComplete());
    }

    @Test
    public void testResourceIsNotCompleteWhenAskingCoordinatorIfCanSetImage() {
        RequestCoordinator requestCoordinator = mock(RequestCoordinator.class);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Request request = (Request) invocation.getArguments()[0];
                assertFalse(request.isComplete());
                return true;
            }
        }).when(requestCoordinator).canSetImage(any(Request.class));

        harness.requestCoordinator = requestCoordinator;
        GenericRequest request = harness.getRequest();

        when(harness.resource.get()).thenReturn(new Object());
        request.onResourceReady(harness.resource);

        verify(requestCoordinator).canSetImage(eq(request));
    }

    @Test
    public void testIsNotFailedWithoutException() {
        GenericRequest request = harness.getRequest();

        assertFalse(request.isFailed());
    }

    @Test
    public void testIsFailedAfterException() {
        GenericRequest request = harness.getRequest();

        request.onException(new Exception("test"));
        assertTrue(request.isFailed());
    }

    @Test
    public void testEngineLoadPassedCorrectPriority() {
        Priority expected = Priority.HIGH;
        harness.priority = expected;
        GenericRequest request = harness.getRequest();


        request.onSizeReady(100, 100);

        verify(harness.engine).load(anyString(), anyInt(), anyInt(), any(ResourceDecoder.class),
                any(DataFetcher.class), any(ResourceDecoder.class), any(Transformation.class),
                any(ResourceEncoder.class), any(ResourceTranscoder.class), eq(expected), anyBoolean(),
                any(ResourceCallback.class));
    }

    @Test
    public void testEngineLoadCancelledOnCancel() {
        Engine.LoadStatus loadStatus = mock(Engine.LoadStatus.class);
        when(harness.engine.load(anyString(), anyInt(), anyInt(), any(ResourceDecoder.class),
                any(DataFetcher.class), any(ResourceDecoder.class), any(Transformation.class),
                any(ResourceEncoder.class), any(ResourceTranscoder.class), any(Priority.class),
                anyBoolean(), any(ResourceCallback.class))).thenReturn(loadStatus);

        GenericRequest request = harness.getRequest();

        request.onSizeReady(100, 100);
        request.cancel();

        verify(loadStatus).cancel();
    }

    @Test
    public void testResourceIsRecycledOnClear() {
        GenericRequest request = harness.getRequest();

        when(harness.resource.get()).thenReturn(new Object());
        request.onResourceReady(harness.resource);
        request.clear();

        verify(harness.resource).release();
    }

    @Test
    public void testPlaceholderResourceIsSet() {
        final int expectedId = 12345;
        Drawable expected = new ColorDrawable(Color.RED);

        Context context = mockContextToReturn(expectedId, expected);
        MockTarget target = new MockTarget();

        harness.context = context;
        harness.placeholderResourceId = expectedId;
        harness.target = target;
        GenericRequest request = harness.getRequest();
        request.run();

        assertEquals(expected, target.currentPlaceholder);
    }

    @Test
    public void testPlaceholderDrawableIsSet() {
        Drawable expected = new ColorDrawable(Color.RED);

        MockTarget target = new MockTarget();

        harness.placeholderDrawable = expected;
        harness.target = target;
        GenericRequest request = harness.getRequest();
        request.run();

        assertEquals(expected, target.currentPlaceholder);
    }

    @Test
    public void testErrorResourceIsSetOnLoadFailed() {
        final int expectedId = 12345;
        Drawable expected = new ColorDrawable(Color.RED);

        Context context = mockContextToReturn(expectedId, expected);
        MockTarget target = new MockTarget();

        harness.context = context;
        harness.errorResourceId = expectedId;
        harness.target = target;
        GenericRequest request = harness.getRequest();

        request.onException(null);

        assertEquals(expected, target.currentPlaceholder);
    }

    @Test
    public void testErrorDrawableIsSetOnLoadFailed() {
        Drawable expected = new ColorDrawable(Color.RED);

        MockTarget target = new MockTarget();

        harness.errorDrawable = expected;
        harness.target = target;
        GenericRequest request = harness.getRequest();

        request.onException(null);

        assertEquals(expected, target.currentPlaceholder);
    }

    @Test
    public void setTestPlaceholderDrawableSetOnNullModel() {
        Drawable placeholder = new ColorDrawable(Color.RED);

        MockTarget target = new MockTarget();

        harness.placeholderDrawable = placeholder;
        harness.target = target;
        harness.model = null;
        GenericRequest request = harness.getRequest();

        request.run();

        assertEquals(placeholder, target.currentPlaceholder);
    }

    @Test
    public void testErrorDrawableSetOnNullModel() {
        Drawable placeholder = new ColorDrawable(Color.RED);
        Drawable errorPlaceholder = new ColorDrawable(Color.GREEN);

        MockTarget target = new MockTarget();

        harness.placeholderDrawable = placeholder;
        harness.errorDrawable = errorPlaceholder;
        harness.target = target;
        harness.model = null;
        GenericRequest request = harness.getRequest();

        request.run();

        assertEquals(errorPlaceholder, target.currentPlaceholder);
    }

    @Test
    public void testIsNotRunningBeforeRunCalled() {
        assertFalse(harness.getRequest().isRunning());
    }

    @Test
    public void testIsRunningAfterRunCalled() {
        Request request = harness.getRequest();
        request.run();
        assertTrue(request.isRunning());
    }

    @Test
    public void testIsNotRunningAfterComplete() {
        GenericRequest<Object, Object, Object, Object> request = harness.getRequest();
        request.run();
        request.onResourceReady(harness.resource);

        assertFalse(request.isRunning());
    }

    @Test
    public void testIsNotRunningAfterFailing() {
        GenericRequest<Object, Object, Object, Object> request = harness.getRequest();
        request.run();
        request.onException(new RuntimeException("Test"));

        assertFalse(request.isRunning());
    }

    @Test
    public void testIsNotRunningAfterClear() {
        GenericRequest<Object, Object, Object, Object> request = harness.getRequest();
        request.run();
        request.clear();

        assertFalse(request.isRunning());
    }

    @Test
    public void testCallsTargetOnResourceReadyIfNoRequestListener() {
        harness.requestListener = null;
        GenericRequest<Object, Object, Object, Object> request = harness.getRequest();
        request.onResourceReady(harness.resource);

        verify(harness.target).onResourceReady(eq(harness.resource.get()), any(GlideAnimation.class));
    }

    @Test
    public void testCallsTargetOnResourceReadyIfRequestListenerReturnsFalse() {
        GenericRequest<Object, Object, Object, Object> request = harness.getRequest();
        when(harness.requestListener.onResourceReady(anyObject(), anyObject(), eq(harness.target),
                anyBoolean(), anyBoolean())).thenReturn(false);
        request.onResourceReady(harness.resource);

        verify(harness.target).onResourceReady(eq(harness.resource.get()), any(GlideAnimation.class));
    }

    @Test
    public void testDoesNotCallTargetOnResourceReadyIfRequestListenerReturnsTrue() {
        GenericRequest<Object, Object, Object, Object> request = harness.getRequest();
        when(harness.requestListener.onResourceReady(anyObject(), anyObject(), eq(harness.target),
                anyBoolean(), anyBoolean())).thenReturn(true);
        request.onResourceReady(harness.resource);

        verify(harness.target, never()).onResourceReady(anyObject(), any(GlideAnimation.class));
    }

    @Test
    public void testCallsTargetOnExceptionIfNoRequestListener() {
        harness.requestListener = null;
        GenericRequest<Object, Object, Object, Object> request = harness.getRequest();
        request.onException(new IOException("test"));

        verify(harness.target).setPlaceholder(eq(harness.errorDrawable));
    }

    @Test
    public void testCallsTargetOnExceptionIfRequestListenerReturnsFalse() {
        GenericRequest<Object, Object, Object, Object> request = harness.getRequest();
        when(harness.requestListener.onException(any(Exception.class), anyObject(), eq(harness.target), anyBoolean()))
                .thenReturn(false);
        request.onException(new IOException("test"));

        verify(harness.target).setPlaceholder(eq(harness.errorDrawable));
    }

    @Test
    public void testDoesNotCallTargetOnExceptionIfRequestListenerReturnsTrue() {
        GenericRequest<Object, Object, Object, Object> request = harness.getRequest();
        when(harness.requestListener.onException(any(Exception.class), anyObject(), eq(harness.target), anyBoolean()))
                .thenReturn(true);
        request.onException(new IllegalArgumentException("test"));

        verify(harness.target, never()).setPlaceholder(any(Drawable.class));
    }

    @Test
    public void testRequestListenerIsCalledWithResourceResult() {
        GenericRequest<Object, Object, Object, Object> request = harness.getRequest();
        request.onResourceReady(harness.resource);

        verify(harness.requestListener).onResourceReady(eq(harness.resource.get()), anyObject(), any(Target.class),
                anyBoolean(), anyBoolean());
    }

    @Test
    public void testRequestListenerIsCalledWithModel() {
        GenericRequest<Object, Object, Object, Object> request = harness.getRequest();
        request.onResourceReady(harness.resource);

        verify(harness.requestListener).onResourceReady(anyObject(), eq(harness.model), any(Target.class),
                anyBoolean(), anyBoolean());
    }

    @Test
    public void testRequestListenerIsCalledWithTarget() {
        GenericRequest<Object, Object, Object, Object> request = harness.getRequest();
        request.onResourceReady(harness.resource);

        verify(harness.requestListener).onResourceReady(anyObject(), anyObject(), eq(harness.target), anyBoolean(),
                anyBoolean());
    }

    @Test
    public void testRequestListenerIsCalledWithLoadedFromMemoryIfLoadCompletesSynchronously() {
        final GenericRequest<Object, Object, Object, Object> request = harness.getRequest();
        when(harness.engine.load(anyString(), anyInt(), anyInt(), any(ResourceDecoder.class), any(DataFetcher.class),
                any(ResourceDecoder.class), any(Transformation.class), any(ResourceEncoder.class),
                any(ResourceTranscoder.class), any(Priority.class), anyBoolean(), any(ResourceCallback.class)))
                .thenAnswer(new Answer<Object>() {
                    @Override
                    public Object answer(InvocationOnMock invocation) throws Throwable {
                        request.onResourceReady(harness.resource);
                        return null;
                    }
                });
        request.onSizeReady(100, 100);
        verify(harness.requestListener).onResourceReady(anyObject(), anyObject(), any(Target.class), eq(true),
                anyBoolean());
    }

    @Test
    public void testRequestListenerIsCalledWithNotLoadedFromMemoryCacheIfLoadCompletesAsynchronously() {
        GenericRequest<Object, Object, Object, Object> request = harness.getRequest();
        request.onSizeReady(100, 100);
        request.onResourceReady(harness.resource);

        verify(harness.requestListener).onResourceReady(anyObject(), anyObject(), any(Target.class), eq(false),
                anyBoolean());
    }

    @Test
    public void testRequestListenerIsCalledWithIsFirstResourceIfNoRequestCoordinator() {
        harness.requestCoordinator = null;
        GenericRequest<Object, Object, Object, Object> request = harness.getRequest();
        request.onResourceReady(harness.resource);

        verify(harness.requestListener).onResourceReady(anyObject(), anyObject(), any(Target.class), anyBoolean(),
                eq(true));
    }

    @Test
    public void testRequestListenerIsCalledWithIsFirstImageIfRequestCoordinatorReturnsNoRequestComplete() {
        GenericRequest<Object, Object, Object, Object> request = harness.getRequest();
        when(harness.requestCoordinator.isAnyRequestComplete()).thenReturn(false);
        request.onResourceReady(harness.resource);

        verify(harness.requestListener).onResourceReady(anyObject(), anyObject(), any(Target.class), anyBoolean(),
                eq(true));
    }

    @Test
    public void testRequestListenerIsCalledWithNotIsFirstRequestIfRequestCoordinatorReturnsARequestComplete() {
        GenericRequest<Object, Object, Object, Object> request = harness.getRequest();
        when(harness.requestCoordinator.isAnyRequestComplete()).thenReturn(true);
        request.onResourceReady(harness.resource);

        verify(harness.requestListener).onResourceReady(anyObject(), anyObject(), any(Target.class), anyBoolean(),
                eq(false));
    }

    @Test
    public void testTargetIsCalledWithAnimationFromFactory() {
        GenericRequest<Object, Object, Object, Object> request = harness.getRequest();
        GlideAnimation<Object> glideAnimation = mock(GlideAnimation.class);
        when(harness.factory.build(anyBoolean(), anyBoolean())).thenReturn(glideAnimation);
        request.onResourceReady(harness.resource);

        verify(harness.target).onResourceReady(eq(harness.resource.get()), eq(glideAnimation));
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
        public void onResourceReady(Object resource, GlideAnimation glideAnimation) {
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
        public void setRequest(Request request) {
        }

        @Override
        public Request getRequest() {
            return null;
        }
    }
}
