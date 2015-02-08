package com.bumptech.glide.request;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.Encoder;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.Engine;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;
import com.bumptech.glide.provider.DataLoadProvider;
import com.bumptech.glide.provider.LoadProvider;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.animation.GlideAnimationFactory;
import com.bumptech.glide.request.target.SizeReadyCallback;
import com.bumptech.glide.request.target.Target;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, emulateSdk = 18)
public class GenericRequestTest {
    private RequestHarness harness;

    /**
     * {@link Number} and {@link List} are arbitrarily chosen types to test some type safety as well.
     * Both are in the middle of the hierarchy having multiple descendants and ancestors.
     */
    @SuppressWarnings("unchecked")
    private static class RequestHarness {
        ModelLoader<Number, Object> modelLoader;
        Engine engine = mock(Engine.class);
        Number model = 123456;
        Target<List> target = mock(Target.class);
        Context context = Robolectric.application;
        Resource<List> resource = mock(Resource.class);
        RequestCoordinator requestCoordinator = mock(RequestCoordinator.class);
        Priority priority = Priority.NORMAL;
        int placeholderResourceId = 0;
        Drawable placeholderDrawable = null;
        int errorResourceId = 0;
        Transformation transformation = mock(Transformation.class);
        Drawable errorDrawable = null;
        LoadProvider<Number, Object, Object, List> loadProvider = mock(LoadProvider.class);
        ResourceDecoder<File, Object> cacheDecoder = mock(ResourceDecoder.class);
        ResourceDecoder<Object, Object> sourceDecoder = mock(ResourceDecoder.class);
        ResourceEncoder<Object> encoder = mock(ResourceEncoder.class);
        ResourceTranscoder transcoder = mock(ResourceTranscoder.class);
        Encoder<Object> sourceEncoder = mock(Encoder.class);
        RequestListener<Number, List> requestListener = mock(RequestListener.class);
        boolean skipMemoryCache;
        GlideAnimationFactory<List> factory = mock(GlideAnimationFactory.class);
        int overrideWidth = -1;
        int overrideHeight = -1;
        List result = new ArrayList();
        DiskCacheStrategy diskCacheStrategy = DiskCacheStrategy.RESULT;
        Key signature = mock(Key.class);

        public RequestHarness() {
            modelLoader = mock(ModelLoader.class);
            when(modelLoader.getResourceFetcher(any(Number.class), anyInt(), anyInt()))
                    .thenReturn(mock(DataFetcher.class));
            when(loadProvider.getModelLoader()).thenReturn(modelLoader);
            when(loadProvider.getCacheDecoder()).thenReturn(cacheDecoder);
            when(loadProvider.getSourceDecoder()).thenReturn(sourceDecoder);
            when(loadProvider.getSourceEncoder()).thenReturn(sourceEncoder);
            when(loadProvider.getEncoder()).thenReturn(encoder);
            when(loadProvider.getTranscoder()).thenReturn(transcoder);
            when(requestCoordinator.canSetImage(any(Request.class))).thenReturn(true);
            when(requestCoordinator.canNotifyStatusChanged(any(Request.class))).thenReturn(true);

            when(resource.get()).thenReturn(result);
        }

        public GenericRequest<Number, Object, Object, List> getRequest() {
            return GenericRequest.obtain(loadProvider,
                    model,
                    signature,
                    context,
                    priority,
                    target,
                    1f,
                    placeholderDrawable,
                    placeholderResourceId,
                    errorDrawable,
                    errorResourceId,
                    requestListener,
                    requestCoordinator,
                    engine,
                    transformation,
                    List.class,
                    skipMemoryCache,
                    factory,
                    overrideWidth,
                    overrideHeight,
                    diskCacheStrategy);
        }
    }

    @Before
    public void setUp() {
        harness = new RequestHarness();
    }

    @Test(expected = NullPointerException.class)
    public void testThrowsWhenMissingCacheDecoderAndNeeded1() {
        harness.diskCacheStrategy = DiskCacheStrategy.SOURCE;
        when(harness.loadProvider.getCacheDecoder()).thenReturn(null);

        harness.getRequest();
    }

    @Test(expected = NullPointerException.class)
    public void testThrowsWhenMissingCacheDecoderAndNeeded2() {
        harness.diskCacheStrategy = DiskCacheStrategy.ALL;
        when(harness.loadProvider.getCacheDecoder()).thenReturn(null);

        harness.getRequest();
    }

    @Test(expected = NullPointerException.class)
    public void testReturnsWhenMissingCacheDecoderAndNeeded3() {
        harness.diskCacheStrategy = DiskCacheStrategy.RESULT;
        when(harness.loadProvider.getCacheDecoder()).thenReturn(null);

        harness.getRequest();
    }

    @Test
    public void testReturnsWhenMissingCacheDecoderAndNotNeeded2() {
        harness.diskCacheStrategy = DiskCacheStrategy.NONE;
        when(harness.loadProvider.getCacheDecoder()).thenReturn(null);

        assertNotNull(harness.getRequest());
    }

    @Test(expected = NullPointerException.class)
    public void testThrowsWhenMissingSourceDecoderAndNeeded1() {
        harness.diskCacheStrategy = DiskCacheStrategy.RESULT;
        when(harness.loadProvider.getSourceDecoder()).thenReturn(null);

        harness.getRequest();
    }

    @Test(expected = NullPointerException.class)
    public void testThrowsWhenMissingSourceDecoderAndNeeded2() {
        harness.diskCacheStrategy = DiskCacheStrategy.NONE;
        when(harness.loadProvider.getSourceDecoder()).thenReturn(null);

        harness.getRequest();
    }

    @Test
    public void testReturnsWhenMissingSourceDecoderAndNotNeeded1() {
        harness.diskCacheStrategy = DiskCacheStrategy.SOURCE;
        when(harness.loadProvider.getSourceDecoder()).thenReturn(null);

        assertNotNull(harness.getRequest());
    }

    @Test
    public void testReturnsWhenMissingSourceDecoderAndNotNeeded2() {
        harness.diskCacheStrategy = DiskCacheStrategy.ALL;
        when(harness.loadProvider.getSourceDecoder()).thenReturn(null);

        assertNotNull(harness.getRequest());
    }

    @Test(expected =  NullPointerException.class)
    public void testThrowsWhenMissingEncoderWhenNeeded1() {
        harness.diskCacheStrategy = DiskCacheStrategy.RESULT;
        when(harness.loadProvider.getEncoder()).thenReturn(null);

        harness.getRequest();
    }

    @Test(expected =  NullPointerException.class)
    public void testThrowsWhenMissingEncoderWhenNeeded2() {
        harness.diskCacheStrategy = DiskCacheStrategy.ALL;
        when(harness.loadProvider.getEncoder()).thenReturn(null);

        harness.getRequest();
    }

    @Test
    public void testReturnsWhenMissingEncoderWhenNotNeeded1() {
        harness.diskCacheStrategy = DiskCacheStrategy.SOURCE;
        when(harness.loadProvider.getEncoder()).thenReturn(null);

        assertNotNull(harness.getRequest());
    }

    @Test
    public void testReturnsWhenMissingEncoderWhenNotNeeded2() {
        harness.diskCacheStrategy = DiskCacheStrategy.NONE;
        when(harness.loadProvider.getEncoder()).thenReturn(null);

        assertNotNull(harness.getRequest());
    }

    @Test
    public void testThrowsWhenMissingTranscoder() {
        for (DiskCacheStrategy strategy : DiskCacheStrategy.values()) {
            harness = new RequestHarness();
            harness.diskCacheStrategy = strategy;
            when(harness.loadProvider.getTranscoder()).thenReturn(null);

            try {
                harness.getRequest();
                fail(NullPointerException.class.getSimpleName() + " expected for " + strategy);
            } catch (NullPointerException ex) {
                // expected
            }
        }
    }

    @Test
    public void testThrowsWhenMissingModelLoader() {
        for (DiskCacheStrategy strategy : DiskCacheStrategy.values()) {
            harness = new RequestHarness();
            harness.diskCacheStrategy = strategy;
            when(harness.loadProvider.getModelLoader()).thenReturn(null);

            try {
                harness.getRequest();
                fail(NullPointerException.class.getSimpleName() + " expected for " + strategy);
            } catch (NullPointerException ex) {
                // expected
            }
        }
    }

    @Test(expected = NullPointerException.class)
    public void testThrowsWhenMissingSourceEncoderAndNeeded1() {
        harness.diskCacheStrategy = DiskCacheStrategy.SOURCE;
        when(harness.loadProvider.getSourceEncoder()).thenReturn(null);

        harness.getRequest();
    }

    @Test(expected = NullPointerException.class)
    public void testThrowsWhenMissingSourceEncoderAndNeeded2() {
        harness.diskCacheStrategy = DiskCacheStrategy.ALL;
        when(harness.loadProvider.getSourceEncoder()).thenReturn(null);

        harness.getRequest();
    }

    @Test
    public void testReturnsWhenMissingSourceEncoderAndNotNeeded1() {
        harness.diskCacheStrategy = DiskCacheStrategy.RESULT;
        when(harness.loadProvider.getSourceEncoder()).thenReturn(null);

        assertNotNull(harness.getRequest());
    }

    @Test
    public void testReturnsWhenMissingSourceEncoderAndNotNeeded2() {
        harness.diskCacheStrategy = DiskCacheStrategy.NONE;
        when(harness.loadProvider.getSourceEncoder()).thenReturn(null);

        assertNotNull(harness.getRequest());
    }

    @Test
    public void testThrowsWhenTransformationIsNull() {
        for (DiskCacheStrategy strategy : DiskCacheStrategy.values()) {
            harness = new RequestHarness();
            harness.diskCacheStrategy = strategy;
            harness.transformation = null;

            try {
                harness.getRequest();
                fail(NullPointerException.class.getSimpleName() + " expected for " + strategy);
            } catch (NullPointerException ex) {
                // expected
            }
        }
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
        verify(harness.requestListener)
                .onException(any(Exception.class), any(Number.class), eq(harness.target), anyBoolean());
    }

    @Test
    public void testCanHandleEmptyResources() {
        GenericRequest request = harness.getRequest();
        when(harness.resource.get()).thenReturn(null);

        request.onResourceReady(harness.resource);

        assertTrue(request.isFailed());
        verify(harness.engine).release(eq(harness.resource));
        verify(harness.requestListener)
                .onException(any(Exception.class), any(Number.class), eq(harness.target), anyBoolean());
    }

    @Test
    public void testCanHandleNonConformingResources() {
        GenericRequest request = harness.getRequest();
        when(((Resource) (harness.resource)).get()).thenReturn("Invalid mocked String, this should be a List");

        request.onResourceReady(harness.resource);

        assertTrue(request.isFailed());
        verify(harness.engine).release(eq(harness.resource));
        verify(harness.requestListener)
                .onException(any(Exception.class), any(Number.class), eq(harness.target), anyBoolean());
    }

    @Test
    public void testIsNotFailedAfterClear() {
        GenericRequest request = harness.getRequest();

        request.onResourceReady(null);
        request.clear();

        assertFalse(request.isFailed());
    }

    @Test
    public void testIsPausedAfterPause() {
        GenericRequest request = harness.getRequest();
        request.pause();

        assertTrue(request.isPaused());
    }

    @Test
    public void testIsNotCancelledAfterPause() {
        GenericRequest request = harness.getRequest();
        request.pause();

        assertFalse(request.isCancelled());
    }

    @Test
    public void testIsNotPausedAfterBeginningWhilePaused() {
        GenericRequest request = harness.getRequest();
        request.pause();
        request.begin();

        assertFalse(request.isPaused());
        assertTrue(request.isRunning());
    }

    @Test
    public void testIsNotFailedAfterBegin() {
        GenericRequest request = harness.getRequest();

        request.onResourceReady(null);
        request.begin();

        assertFalse(request.isFailed());
    }

    @Test
    public void testIsCompleteAfterReceivingResource() {
        GenericRequest request = harness.getRequest();

        request.onResourceReady(harness.resource);

        assertTrue(request.isComplete());
    }

    @Test
    public void testIsNotCompleteAfterClear() {
        GenericRequest request = harness.getRequest();
        request.onResourceReady(harness.resource);
        request.clear();

        assertFalse(request.isComplete());
    }

    @Test
    public void testIsCancelledAfterClear() {
        GenericRequest request = harness.getRequest();
        request.clear();

        assertTrue(request.isCancelled());
    }

    @Test
    public void testDoesNotNotifyTargetTwiceIfClearedTwiceInARow() {
        GenericRequest request = harness.getRequest();
        request.clear();
        request.clear();

        verify(harness.target, times(1)).onLoadCleared(any(Drawable.class));
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
    public void testIgnoresOnSizeReadyIfNotWaitingForSize() {
        GenericRequest request = harness.getRequest();
        request.begin();
        request.onSizeReady(100, 100);
        request.onSizeReady(100, 100);

        verify(harness.engine, times(1)).load(eq(harness.signature), eq(100), eq(100), any(DataFetcher.class),
                any(DataLoadProvider.class), any(Transformation.class), any(ResourceTranscoder.class),
                any(Priority.class), anyBoolean(), any(DiskCacheStrategy.class), any(ResourceCallback.class));
    }

    @Test
    public void testIsFailedAfterNoResultAndNullException() {
        GenericRequest request = harness.getRequest();

        request.onException(null);
        assertTrue(request.isFailed());
    }

    @Test
    public void testEngineLoadPassedCorrectPriority() {
        Priority expected = Priority.HIGH;
        harness.priority = expected;
        GenericRequest request = harness.getRequest();
        request.begin();

        request.onSizeReady(100, 100);

        verify(harness.engine).load(any(Key.class), anyInt(), anyInt(), any(DataFetcher.class),
                any(DataLoadProvider.class), any(Transformation.class), any(ResourceTranscoder.class),
                eq(expected), anyBoolean(), any(DiskCacheStrategy.class), any(ResourceCallback.class));
    }

    @Test
    public void testEngineLoadCancelledOnCancel() {
        Engine.LoadStatus loadStatus = mock(Engine.LoadStatus.class);
        when(harness.engine.load(any(Key.class), anyInt(), anyInt(), any(DataFetcher.class),
                any(DataLoadProvider.class), any(Transformation.class), any(ResourceTranscoder.class),
                any(Priority.class), anyBoolean(), any(DiskCacheStrategy.class), any(ResourceCallback.class)))
                .thenReturn(loadStatus);

        GenericRequest request = harness.getRequest();
        request.begin();

        request.onSizeReady(100, 100);
        request.cancel();

        verify(loadStatus).cancel();
    }

    @Test
    public void testResourceIsRecycledOnClear() {
        GenericRequest request = harness.getRequest();

        request.onResourceReady(harness.resource);
        request.clear();

        verify(harness.engine).release(eq(harness.resource));
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
        request.begin();

        assertEquals(expected, target.currentPlaceholder);
    }

    @Test
    public void testPlaceholderDrawableIsSet() {
        Drawable expected = new ColorDrawable(Color.RED);

        MockTarget target = new MockTarget();

        harness.placeholderDrawable = expected;
        harness.target = target;
        GenericRequest request = harness.getRequest();
        request.begin();

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

        request.begin();

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

        request.begin();

        assertEquals(errorPlaceholder, target.currentPlaceholder);
    }

    @Test
    public void testIsNotRunningBeforeRunCalled() {
        assertFalse(harness.getRequest().isRunning());
    }

    @Test
    public void testIsRunningAfterRunCalled() {
        Request request = harness.getRequest();
        request.begin();
        assertTrue(request.isRunning());
    }

    @Test
    public void testIsNotRunningAfterComplete() {
        GenericRequest request = harness.getRequest();
        request.begin();
        request.onResourceReady(harness.resource);

        assertFalse(request.isRunning());
    }

    @Test
    public void testIsNotRunningAfterFailing() {
        GenericRequest request = harness.getRequest();
        request.begin();
        request.onException(new RuntimeException("Test"));

        assertFalse(request.isRunning());
    }

    @Test
    public void testIsNotRunningAfterClear() {
        GenericRequest request = harness.getRequest();
        request.begin();
        request.clear();

        assertFalse(request.isRunning());
    }

    @Test
    public void testCallsTargetOnResourceReadyIfNoRequestListener() {
        harness.requestListener = null;
        GenericRequest request = harness.getRequest();
        request.onResourceReady(harness.resource);

        verify(harness.target).onResourceReady(eq(harness.result), any(GlideAnimation.class));
    }

    @Test
    public void testCallsTargetOnResourceReadyIfRequestListenerReturnsFalse() {
        GenericRequest request = harness.getRequest();
        when(harness.requestListener.onResourceReady(any(List.class), any(Number.class), eq(harness.target),
                anyBoolean(), anyBoolean())).thenReturn(false);
        request.onResourceReady(harness.resource);

        verify(harness.target).onResourceReady(eq(harness.result), any(GlideAnimation.class));
    }

    @Test
    public void testDoesNotCallTargetOnResourceReadyIfRequestListenerReturnsTrue() {
        GenericRequest request = harness.getRequest();
        when(harness.requestListener.onResourceReady(any(List.class), any(Number.class), eq(harness.target),
                anyBoolean(), anyBoolean())).thenReturn(true);
        request.onResourceReady(harness.resource);

        verify(harness.target, never()).onResourceReady(any(List.class), any(GlideAnimation.class));
    }

    @Test
    public void testCallsTargetOnExceptionIfNoRequestListener() {
        harness.requestListener = null;
        GenericRequest request = harness.getRequest();
        Exception exception = new IOException("test");
        request.onException(exception);

        verify(harness.target).onLoadFailed(eq(exception), eq(harness.errorDrawable));
    }

    @Test
    public void testCallsTargetOnExceptionIfRequestListenerReturnsFalse() {
        GenericRequest request = harness.getRequest();
        when(harness.requestListener
                .onException(any(Exception.class), any(Number.class), eq(harness.target), anyBoolean()))
                .thenReturn(false);
        Exception exception = new IOException("Test");
        request.onException(exception);

        verify(harness.target).onLoadFailed(eq(exception), eq(harness.errorDrawable));
    }

    @Test
    public void testDoesNotCallTargetOnExceptionIfRequestListenerReturnsTrue() {
        GenericRequest request = harness.getRequest();
        when(harness.requestListener
                .onException(any(Exception.class), any(Number.class), eq(harness.target), anyBoolean()))
                .thenReturn(true);

        request.onException(new IllegalArgumentException("test"));

        verify(harness.target, never()).onLoadFailed(any(Exception.class), any(Drawable.class));
    }

    @Test
    public void testRequestListenerIsCalledWithResourceResult() {
        GenericRequest request = harness.getRequest();
        request.onResourceReady(harness.resource);

        verify(harness.requestListener).onResourceReady(eq(harness.result), any(Number.class), any(Target.class),
                anyBoolean(), anyBoolean());
    }

    @Test
    public void testRequestListenerIsCalledWithModel() {
        GenericRequest request = harness.getRequest();
        request.onResourceReady(harness.resource);

        verify(harness.requestListener).onResourceReady(any(List.class), eq(harness.model), any(Target.class),
                anyBoolean(), anyBoolean());
    }

    @Test
    public void testRequestListenerIsCalledWithTarget() {
        GenericRequest request = harness.getRequest();
        request.onResourceReady(harness.resource);

        verify(harness.requestListener).onResourceReady(any(List.class), any(Number.class), eq(harness.target),
                anyBoolean(), anyBoolean());
    }

    @Test
    public void testRequestListenerIsCalledWithLoadedFromMemoryIfLoadCompletesSynchronously() {
        final GenericRequest request = harness.getRequest();
        when(harness.engine.load(any(Key.class), anyInt(), anyInt(), any(DataFetcher.class),
                any(DataLoadProvider.class), any(Transformation.class), any(ResourceTranscoder.class),
                any(Priority.class), anyBoolean(),  any(DiskCacheStrategy.class),
                any(ResourceCallback.class))).thenAnswer(new Answer<Object>() {
                    @Override
                    public Object answer(InvocationOnMock invocation) throws Throwable {
                        request.onResourceReady(harness.resource);
                        return null;
                    }
                });

        request.begin();
        request.onSizeReady(100, 100);
        verify(harness.requestListener).onResourceReady(eq(harness.result), any(Number.class), any(Target.class),
                eq(true), anyBoolean());
    }

    @Test
    public void testRequestListenerIsCalledWithNotLoadedFromMemoryCacheIfLoadCompletesAsynchronously() {
        GenericRequest request = harness.getRequest();
        request.onSizeReady(100, 100);
        request.onResourceReady(harness.resource);

        verify(harness.requestListener).onResourceReady(eq(harness.result), any(Number.class), any(Target.class),
                eq(false), anyBoolean());
    }

    @Test
    public void testRequestListenerIsCalledWithIsFirstResourceIfNoRequestCoordinator() {
        harness.requestCoordinator = null;
        GenericRequest request = harness.getRequest();
        request.onResourceReady(harness.resource);

        verify(harness.requestListener).onResourceReady(eq(harness.result), any(Number.class), any(Target.class),
                anyBoolean(), eq(true));
    }

    @Test
    public void testRequestListenerIsCalledWithIsFirstImageIfRequestCoordinatorReturnsNoResourceSet() {
        GenericRequest request = harness.getRequest();
        when(harness.requestCoordinator.isAnyResourceSet()).thenReturn(false);
        request.onResourceReady(harness.resource);

        verify(harness.requestListener).onResourceReady(eq(harness.result), any(Number.class), any(Target.class),
                anyBoolean(), eq(true));
    }

    @Test
    public void testRequestListenerIsCalledWithNotIsFirstRequestIfRequestCoordinatorReturnsResourceSet() {
        GenericRequest request = harness.getRequest();
        when(harness.requestCoordinator.isAnyResourceSet()).thenReturn(true);
        request.onResourceReady(harness.resource);

        verify(harness.requestListener).onResourceReady(eq(harness.result), any(Number.class), any(Target.class),
                anyBoolean(), eq(false));
    }

    @Test
    public void testTargetIsCalledWithAnimationFromFactory() {
        GenericRequest request = harness.getRequest();
        GlideAnimation<List> glideAnimation = mock(GlideAnimation.class);
        when(harness.factory.build(anyBoolean(), anyBoolean())).thenReturn(glideAnimation);
        request.onResourceReady(harness.resource);

        verify(harness.target).onResourceReady(eq(harness.result), eq(glideAnimation));
    }

    @Test
    public void testCallsGetSizeIfOverrideWidthIsLessThanZero() {
        harness.overrideWidth = -1;
        harness.overrideHeight = 100;
        GenericRequest request = harness.getRequest();
        request.begin();

        verify(harness.target).getSize(any(SizeReadyCallback.class));
    }

    @Test
    public void testCallsGetSizeIfOverrideHeightIsLessThanZero() {
        harness.overrideHeight = -1;
        harness.overrideWidth = 100;
        GenericRequest request = harness.getRequest();
        request.begin();

        verify(harness.target).getSize(any(SizeReadyCallback.class));
    }

    @Test
    public void testDoesNotCallGetSizeIfOverrideWidthAndHeightAreSet() {
        harness.overrideWidth = 100;
        harness.overrideHeight = 100;
        GenericRequest request = harness.getRequest();
        request.begin();

        verify(harness.target, never()).getSize(any(SizeReadyCallback.class));
    }

    @Test
    public void testCallsEngineWithOverrideWidthAndHeightIfSet() {
        harness.overrideWidth = 1;
        harness.overrideHeight = 2;

        GenericRequest request = harness.getRequest();
        request.begin();

        verify(harness.engine).load(any(Key.class), eq(harness.overrideWidth), eq(harness.overrideHeight),
                any(DataFetcher.class), any(DataLoadProvider.class), any(Transformation.class),
                any(ResourceTranscoder.class), any(Priority.class), anyBoolean(), any(DiskCacheStrategy.class),
                any(ResourceCallback.class));
    }

    @Test
    public void testDoesNotSetErrorDrawableIfRequestCoordinatorDoesntAllowIt() {
        harness.errorDrawable = new ColorDrawable(Color.RED);
        GenericRequest request = harness.getRequest();
        when(harness.requestCoordinator.canNotifyStatusChanged(any(Request.class))).thenReturn(false);
        request.onException(new IOException("Test"));

        verify(harness.target, never()).onLoadFailed(any(Exception.class), any(Drawable.class));
    }

    @Test
    public void testCanReRunCancelledRequests() {
        doAnswer(new CallSizeReady(100, 100)).when(harness.target)
                .getSize(any(SizeReadyCallback.class));
        when(harness.engine.load(any(Key.class), anyInt(), anyInt(), any(DataFetcher.class),
                any(DataLoadProvider.class), any(Transformation.class), any(ResourceTranscoder.class),
                any(Priority.class), anyBoolean(), any(DiskCacheStrategy.class), any(ResourceCallback.class)))
                .thenAnswer(new CallResourceCallback(harness.resource));
        GenericRequest request = harness.getRequest();

        request.begin();
        request.cancel();
        request.begin();

        verify(harness.target, times(2)).onResourceReady(eq(harness.result), any(GlideAnimation.class));
    }

    @Test
    public void testResourceOnlyReceivesOneGetOnResourceReady() {
        GenericRequest request = harness.getRequest();
        request.onResourceReady(harness.resource);

        verify(harness.resource, times(1)).get();
    }

    @Test
    public void testOnSizeReadyWithNullDataFetcherCallsOnException() {
        GenericRequest request = harness.getRequest();
        when(harness.modelLoader.getResourceFetcher(any(Number.class), anyInt(), anyInt())).thenReturn(null);
        request.begin();
        request.onSizeReady(100, 100);

        verify(harness.requestListener).onException(any(Exception.class), any(Number.class), any(Target.class),
                anyBoolean());
    }

    @Test
    public void testDoesNotStartALoadIfOnSizeReadyIsCalledAfterCancel() {
        GenericRequest request = harness.getRequest();
        request.cancel();
        request.onSizeReady(100, 100);

        verify(harness.engine, never()).load(any(Key.class), anyInt(), anyInt(), any(DataFetcher.class),
                any(DataLoadProvider.class), any(Transformation.class), any(ResourceTranscoder.class),
                any(Priority.class), anyBoolean(), any(DiskCacheStrategy.class), any(ResourceCallback.class));
    }

    @Test
    public void testStartsLoadImmediatelyWhenGivenOverrideWithSizeOriginal() {
        harness.overrideWidth = Target.SIZE_ORIGINAL;
        harness.overrideHeight = Target.SIZE_ORIGINAL;
        GenericRequest<Number, Object, Object, List> request = harness.getRequest();
        request.begin();

        verify(harness.engine).load(any(Key.class), eq(Target.SIZE_ORIGINAL), eq(Target.SIZE_ORIGINAL),
                any(DataFetcher.class), any(DataLoadProvider.class), any(Transformation.class),
                any(ResourceTranscoder.class), any(Priority.class), anyBoolean(), any(DiskCacheStrategy.class),
                any(ResourceCallback.class));
    }

    private static class CallResourceCallback implements Answer {

        private Resource resource;

        public CallResourceCallback(Resource resource) {
            this.resource = resource;
        }

        @Override
        public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
            ResourceCallback cb = (ResourceCallback)
                    invocationOnMock.getArguments()[invocationOnMock.getArguments().length - 1];
            cb.onResourceReady(resource);
            return null;
        }
    }

    private static class CallSizeReady implements Answer {

        private int width;
        private int height;

        public CallSizeReady(int width, int height) {
            this.width = width;
            this.height = height;
        }

        @Override
        public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
            SizeReadyCallback cb =
                    (SizeReadyCallback) invocationOnMock.getArguments()[0];
            cb.onSizeReady(width, height);
            return null;
        }
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
        public void onLoadCleared(Drawable placeholder) {
            currentPlaceholder = placeholder;
        }

        @Override
        public void onLoadStarted(Drawable placeholder) {
            currentPlaceholder = placeholder;

        }

        @Override
        public void onLoadFailed(Exception e, Drawable errorDrawable) {
            currentPlaceholder = errorDrawable;

        }

        @Override
        public void onResourceReady(Object resource, GlideAnimation glideAnimation) {
            currentPlaceholder = null;
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

        @Override
        public void onStart() {
        }

        @Override
        public void onStop() {

        }

        @Override
        public void onDestroy() {

        }
    }
}
