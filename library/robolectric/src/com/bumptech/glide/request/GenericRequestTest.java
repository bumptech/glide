package com.bumptech.glide.request;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.animation.Animation;
import com.bumptech.glide.Priority;
import com.bumptech.glide.Resource;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.Engine;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.resource.ResourceFetcher;
import com.bumptech.glide.provider.LoadProvider;
import com.bumptech.glide.request.bitmap.GenericRequest;
import com.bumptech.glide.request.target.Target;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class GenericRequestTest {
    private RequestHarness harness;

    @SuppressWarnings("unchecked")
    private static class RequestHarness {
        Engine engine = mock(Engine.class);
        Object model = new Object();
        Target target = mock(Target.class);
        Context context = Robolectric.application;
        Resource<Object> resource = mock(Resource.class);
        RequestCoordinator requestCoordinator = null;
        Priority priority = Priority.NORMAL;
        int placeholderResourceId = 0;
        Drawable placeholderDrawable = null;
        int errorResourceId = 0;
        Drawable errorDrawable = null;
        LoadProvider<Object, Object, Object> loadProvider = mock(LoadProvider.class);

        public RequestHarness() {
            ModelLoader<Object, Object> modelLoader = mock(ModelLoader.class);
            when(loadProvider.getModelLoader()).thenReturn(modelLoader);
        }

        public GenericRequest<Object, Object, Object> getRequest() {
            return new GenericRequest<Object, Object, Object>(loadProvider, model, context, priority, target, 1f,
                    placeholderDrawable, placeholderResourceId, errorDrawable, errorResourceId, null, 0, null,
                    requestCoordinator, engine, mock(Transformation.class), Object.class);
        }
    }

    @Before
    public void setUp() {
        harness = new RequestHarness();
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
    public void testEngineLoadPassedCorrectMetadata() {
        Priority expected = Priority.HIGH;
        harness.priority = expected;
        GenericRequest request = harness.getRequest();


        request.onSizeReady(100, 100);

        verify(harness.engine).load(anyString(), anyInt(), anyInt(), any(ResourceDecoder.class),
                any(ResourceFetcher.class), any(ResourceDecoder.class), any(Transformation.class),
                any(ResourceEncoder.class), eq(expected), any(ResourceCallback.class));
    }

    @Test
    public void testEngineLoadCancelledOnCancel() {
        Engine.LoadStatus loadStatus = mock(Engine.LoadStatus.class);
        when(harness.engine.load(anyString(), anyInt(), anyInt(), any(ResourceDecoder.class),
                any(ResourceFetcher.class), any(ResourceDecoder.class), any(Transformation.class),
                any(ResourceEncoder.class), any(Priority.class), any(ResourceCallback.class))).thenReturn(loadStatus);

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
        public void onResourceReady(Object resource) {
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
