package com.bumptech.glide.load.resource.gif;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.gifdecoder.GifDecoder;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.resource.gif.GifFrameLoader.DelayTarget;
import com.bumptech.glide.load.resource.gif.GifFrameLoader.FrameCallback;
import com.bumptech.glide.request.Request;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.tests.Util.ReturnsSelfAnswer;
import com.bumptech.glide.util.Util;
import com.google.common.testing.EqualsTester;
import java.nio.ByteBuffer;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 18)
public class GifFrameLoaderTest {

  @Mock GifFrameLoader.FrameCallback callback;
  @Mock GifDecoder gifDecoder;
  @Mock Handler handler;
  @Mock Transformation<Bitmap> transformation;
  @Mock RequestManager requestManager;
  private GifFrameLoader loader;
  private RequestBuilder<Bitmap> requestBuilder;
  private Bitmap firstFrame;

  @SuppressWarnings("unchecked")
  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    when(handler.obtainMessage(anyInt(), isA(DelayTarget.class))).thenReturn(mock(Message.class));

    firstFrame = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);

    ByteBuffer byteBuffer = ByteBuffer.allocate(10);
    when(gifDecoder.getData()).thenReturn(byteBuffer);

    requestBuilder = mock(RequestBuilder.class, new ReturnsSelfAnswer());

    loader = createGifFrameLoader(handler);
  }

  @After
  public void tearDown() {
    Glide.tearDown();
  }

  @NonNull
  private GifFrameLoader createGifFrameLoader(Handler handler) {
    Glide glide = getGlideSingleton();
    return new GifFrameLoader(
        glide.getBitmapPool(),
        requestManager,
        gifDecoder,
        handler,
        requestBuilder,
        transformation,
        firstFrame);
  }

  private static Glide getGlideSingleton() {
    return Glide.get(RuntimeEnvironment.application);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testSetFrameTransformationSetsTransformationOnRequestBuilder() {
    Transformation<Bitmap> transformation = mock(Transformation.class);
    loader.setFrameTransformation(transformation, firstFrame);

    verify(requestBuilder, times(2)).apply(isA(RequestOptions.class));
  }

  @Test(expected = NullPointerException.class)
  public void testSetFrameTransformationThrowsIfGivenNullTransformation() {
    loader.setFrameTransformation(null, null);
  }

  @Test
  public void testReturnsSizeFromGifDecoderAndCurrentFrame() {
    int decoderByteSize = 123456;
    when(gifDecoder.getByteSize()).thenReturn(decoderByteSize);
    assertThat(loader.getSize()).isEqualTo(decoderByteSize + Util.getBitmapByteSize(firstFrame));
  }

  @Test
  public void testStartGetsNextFrameIfNotStartedAndWithNoLoadPending() {
    loader.subscribe(callback);

    verify(requestBuilder).into(aTarget());
  }

  @Test
  public void testGetNextFrameIncrementsSignatureAndAdvancesDecoderBeforeStartingLoad() {
    loader.subscribe(callback);

    InOrder order = inOrder(gifDecoder, requestBuilder);
    order.verify(gifDecoder).advance();
    order.verify(requestBuilder).apply(isA(RequestOptions.class));
    order.verify(requestBuilder).into(aTarget());
  }

  @Test
  public void testGetCurrentFrameReturnsFirstFrameWHenNoLoadHasCompleted() {
    assertThat(loader.getCurrentFrame()).isEqualTo(firstFrame);
  }

  @Test
  public void testGetCurrentFrameReturnsCurrentBitmapAfterLoadHasCompleted() {
    final Bitmap result = Bitmap.createBitmap(100, 200, Bitmap.Config.ARGB_8888);
    DelayTarget target = mock(DelayTarget.class);
    when(target.getResource()).thenReturn(result);
    loader.onFrameReady(target);

    assertEquals(result, loader.getCurrentFrame());
  }

  @Test
  public void testStartDoesNotStartIfAlreadyRunning() {
    loader.subscribe(callback);
    loader.subscribe(mock(FrameCallback.class));

    verify(requestBuilder, times(1)).into(aTarget());
  }

  @Test
  public void testGetNextFrameDoesNotStartLoadIfLoaderIsNotRunning() {
    loader.onFrameReady(mock(DelayTarget.class));

    verify(requestBuilder, never()).into(aTarget());
  }

  @Test
  public void testGetNextFrameDoesNotStartLoadIfLoadIsInProgress() {
    loader.subscribe(callback);
    loader.unsubscribe(callback);
    loader.subscribe(callback);

    verify(requestBuilder, times(1)).into(aTarget());
  }

  @Test
  public void testGetNextFrameDoesStartLoadIfRestartedAndNoLoadIsInProgress() {
    loader.subscribe(callback);
    loader.unsubscribe(callback);

    loader.onFrameReady(mock(DelayTarget.class));
    loader.subscribe(callback);

    verify(requestBuilder, times(2)).into(aTarget());
  }

  @Test
  public void testGetNextFrameDoesStartLoadAfterLoadCompletesIfStarted() {
    loader.subscribe(callback);
    loader.onFrameReady(mock(DelayTarget.class));

    verify(requestBuilder, times(2)).into(aTarget());
  }

  @Test
  public void testOnFrameReadyClearsPreviousFrame() {
    // Force the loader to create a real Handler.
    loader = createGifFrameLoader(null);

    DelayTarget previous = mock(DelayTarget.class);
    Request previousRequest = mock(Request.class);
    when(previous.getRequest()).thenReturn(previousRequest);
    when(previous.getResource()).thenReturn(Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888));

    DelayTarget current = mock(DelayTarget.class);
    when(current.getResource()).thenReturn(Bitmap.createBitmap(100, 100, Bitmap.Config.RGB_565));
    loader.onFrameReady(previous);
    loader.onFrameReady(current);

    verify(requestManager).clear(eq(previous));
  }

  @Test
  public void testOnFrameReadyWithNullResourceDoesNotClearPreviousFrame() {
    // Force the loader to create a real Handler by passing null.
    loader = createGifFrameLoader(null);

    DelayTarget previous = mock(DelayTarget.class);
    Request previousRequest = mock(Request.class);
    when(previous.getRequest()).thenReturn(previousRequest);
    when(previous.getResource()).thenReturn(Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888));

    DelayTarget current = mock(DelayTarget.class);
    when(current.getResource()).thenReturn(null);
    loader.onFrameReady(previous);
    loader.onFrameReady(current);

    verify(previousRequest, never()).clear();
  }

  @Test
  public void testDelayTargetSendsMessageWithHandlerDelayed() {
    long targetTime = 1234;
    DelayTarget delayTarget = new DelayTarget(handler, 1, targetTime);
    delayTarget.onResourceReady(Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888), null
    /*glideAnimation*/);
    verify(handler).sendMessageAtTime(isA(Message.class), eq(targetTime));
  }

  @Test
  public void testDelayTargetSetsResourceOnResourceReady() {
    DelayTarget delayTarget = new DelayTarget(handler, 1, 1);
    Bitmap expected = Bitmap.createBitmap(100, 200, Bitmap.Config.RGB_565);
    delayTarget.onResourceReady(expected, null /*glideAnimation*/);

    assertEquals(expected, delayTarget.getResource());
  }

  @Test
  public void testClearsCompletedLoadOnFrameReadyIfCleared() {
    // Force the loader to create a real Handler by passing null;
    loader = createGifFrameLoader(null);
    loader.clear();
    DelayTarget delayTarget = mock(DelayTarget.class);
    Request request = mock(Request.class);
    when(delayTarget.getRequest()).thenReturn(request);

    loader.onFrameReady(delayTarget);

    verify(requestManager).clear(eq(delayTarget));
  }

  @Test
  public void
  testDoesNotReturnResourceForCompletedFrameInGetCurrentFrameIfLoadCompletesWhileCleared() {
    loader.clear();
    DelayTarget delayTarget = mock(DelayTarget.class);
    Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
    when(delayTarget.getResource()).thenReturn(bitmap);

    loader.onFrameReady(delayTarget);

    assertNull(loader.getCurrentFrame());
  }

  @Test
  public void testFrameSignatureEquality() {
    UUID first = UUID.randomUUID();
    new EqualsTester().addEqualityGroup(new GifFrameLoader.FrameSignature(first),
        new GifFrameLoader.FrameSignature(first))
        .addEqualityGroup(new GifFrameLoader.FrameSignature()).testEquals();
  }

  @SuppressWarnings("unchecked")
  private static Target<Bitmap> aTarget() {
    return isA(Target.class);
  }
}
