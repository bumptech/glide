package com.bumptech.glide.load.resource.gif;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.testing.EqualsTester;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;

import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.gifdecoder.GifDecoder;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.DecodeOptions;
import com.bumptech.glide.request.Request;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.tests.Util.ReturnsSelfAnswer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.UUID;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, emulateSdk = 18)
public class GifFrameLoaderTest {

    @Mock GifFrameLoader.FrameCallback callback;
    @Mock GifDecoder gifDecoder;
    @Mock Handler handler;
    private GifFrameLoader loader;
    private RequestBuilder requestBuilder;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(handler.obtainMessage(anyInt(), anyObject())).thenReturn(mock(Message.class));

        requestBuilder = mock(RequestBuilder.class, new ReturnsSelfAnswer());

        loader = new GifFrameLoader(Robolectric.application, callback, gifDecoder, handler, requestBuilder);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSetFrameTransformationSetsTransformationOnRequestBuilder() {
        Transformation<Bitmap> transformation = mock(Transformation.class);
        loader.setFrameTransformation(transformation);

        verify(requestBuilder).decode(any(DecodeOptions.class));
    }

    @Test(expected = NullPointerException.class)
    public void testSetFrameTransformationThrowsIfGivenNullTransformation() {
        loader.setFrameTransformation(null);
    }

    @Test
    public void testStartGetsNextFrameIfNotStartedAndWithNoLoadPending() {
        loader.start();

        verify(requestBuilder).into(any(Target.class));
    }

    @Test
    public void testGetNextFrameIncrementsSignatureAndAdvancesDecoderBeforeStartingLoad() {
        loader.start();

        InOrder order = inOrder(gifDecoder, requestBuilder);
        order.verify(gifDecoder).advance();
        order.verify(requestBuilder).apply(any(RequestOptions.class));
        order.verify(requestBuilder).into(any(Target.class));
    }

    @Test
    public void testGetCurrentFrameReturnsNullWhenNoLoadHasCompleted() {
        assertNull(loader.getCurrentFrame());
    }

    @Test
    public void testGetCurrentFrameReturnsCurrentBitmapAfterLoadHasCompleted() {
        final Bitmap result = Bitmap.createBitmap(100, 200, Bitmap.Config.ARGB_8888);
        GifFrameLoader.DelayTarget target = mock(GifFrameLoader.DelayTarget.class);
        when(target.getResource()).thenReturn(result);
        loader.onFrameReady(target);

        assertEquals(result, loader.getCurrentFrame());
    }

    @Test
    public void testStartDoesNotStartIfAlreadyRunning() {
        loader.start();
        loader.start();

        verify(requestBuilder, times(1)).into(any(Target.class));
    }

    @Test
    public void testGetNextFrameDoesNotStartLoadIfLoaderIsNotRunning() {
        loader.onFrameReady(mock(GifFrameLoader.DelayTarget.class));

        verify(requestBuilder, never()).into(any(Target.class));
    }

    @Test
    public void testGetNextFrameDoesNotStartLoadIfLoadIsInProgress() {
        loader.start();
        loader.stop();
        loader.start();

        verify(requestBuilder, times(1)).into(any(Target.class));
    }

    @Test
    public void testGetNextFrameDoesStartLoadIfRestartedAndNoLoadIsInProgress() {
        loader.start();
        loader.stop();

        loader.onFrameReady(mock(GifFrameLoader.DelayTarget.class));
        loader.start();

        verify(requestBuilder, times(2)).into(any(Target.class));
    }

    @Test
    public void testGetNextFrameDoesStartLoadAfterLoadCompletesIfStarted() {
        loader.start();
        loader.onFrameReady(mock(GifFrameLoader.DelayTarget.class));

        verify(requestBuilder, times(2)).into(any(Target.class));
    }

    @Test
    public void testOnFrameReadyClearsPreviousFrame() {
        // Force the loader to create a real Handler.
        loader = new GifFrameLoader(Robolectric.application, callback, gifDecoder, null /*handler*/, requestBuilder);

        GifFrameLoader.DelayTarget previous = mock(GifFrameLoader.DelayTarget.class);
        Request previousRequest = mock(Request.class);
        when(previous.getRequest()).thenReturn(previousRequest);

        loader.onFrameReady(previous);
        loader.onFrameReady(mock(GifFrameLoader.DelayTarget.class));

        verify(previousRequest).clear();
    }

    @Test
    public void testDelayTargetSendsMessageWithHandlerDelayed() {
        long targetTime = 1234;
        GifFrameLoader.DelayTarget delayTarget = new GifFrameLoader.DelayTarget(handler, 1, targetTime);
        delayTarget.onResourceReady(Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888), null /*glideAnimation*/);
        verify(handler).sendMessageAtTime(any(Message.class), eq(targetTime));
    }

    @Test
    public void testDelayTargetSetsResourceOnResourceReady() {
        GifFrameLoader.DelayTarget delayTarget = new GifFrameLoader.DelayTarget(handler, 1, 1);
        Bitmap expected = Bitmap.createBitmap(100, 200, Bitmap.Config.RGB_565);
        delayTarget.onResourceReady(expected, null /*glideAnimation*/);

        assertEquals(expected, delayTarget.getResource());
    }

    @Test
    public void testClearsCompletedLoadOnFrameReadyIfCleared() {
        // Force the loader to create a real Handler.
        loader = new GifFrameLoader(Robolectric.application, callback, gifDecoder, null /*handler*/, requestBuilder);
        loader.clear();
        GifFrameLoader.DelayTarget delayTarget = mock(GifFrameLoader.DelayTarget.class);
        Request request = mock(Request.class);
        when(delayTarget.getRequest()).thenReturn(request);

        loader.onFrameReady(delayTarget);

        verify(request).clear();
    }

    @Test
    public void testDoesNotReturnResourceForCompletedFrameInGetCurrentFrameIfLoadCompletesWhileCleared() {
        loader.clear();
        GifFrameLoader.DelayTarget delayTarget = mock(GifFrameLoader.DelayTarget.class);
        Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        when(delayTarget.getResource()).thenReturn(bitmap);

        loader.onFrameReady(delayTarget);

        assertNull(loader.getCurrentFrame());
    }

    @Test
    public void testFrameSignatureEquality() {
        UUID first = UUID.randomUUID();
        new EqualsTester()
                .addEqualityGroup(new GifFrameLoader.FrameSignature(first), new GifFrameLoader.FrameSignature(first))
                .addEqualityGroup(new GifFrameLoader.FrameSignature())
                .testEquals();
    }
}
