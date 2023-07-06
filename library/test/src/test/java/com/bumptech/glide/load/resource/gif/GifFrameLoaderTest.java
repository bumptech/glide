package com.bumptech.glide.load.resource.gif;

import static com.bumptech.glide.RobolectricConstants.ROBOLECTRIC_SDK;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.annotation.LooperMode.Mode.LEGACY;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
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
import com.bumptech.glide.tests.TearDownGlide;
import com.bumptech.glide.tests.Util.ReturnsSelfAnswer;
import com.bumptech.glide.util.Util;
import java.nio.ByteBuffer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

@LooperMode(LEGACY)
@RunWith(RobolectricTestRunner.class)
@Config(sdk = ROBOLECTRIC_SDK)
public class GifFrameLoaderTest {
  @Rule public TearDownGlide tearDownGlide = new TearDownGlide();

  @Mock private GifFrameLoader.FrameCallback callback;
  @Mock private GifDecoder gifDecoder;
  @Mock private Handler handler;
  @Mock private Transformation<Bitmap> transformation;
  @Mock private RequestManager requestManager;
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

  @NonNull
  private GifFrameLoader createGifFrameLoader(Handler handler) {
    Glide glide = getGlideSingleton();
    GifFrameLoader result =
        new GifFrameLoader(
            glide.getBitmapPool(),
            requestManager,
            gifDecoder,
            handler,
            requestBuilder,
            transformation,
            firstFrame);
    result.subscribe(callback);
    return result;
  }

  private static Glide getGlideSingleton() {
    return Glide.get(ApplicationProvider.getApplicationContext());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testSetFrameTransformationSetsTransformationOnRequestBuilder() {
    verify(requestBuilder, times(2)).apply(isA(RequestOptions.class));
    Transformation<Bitmap> transformation = mock(Transformation.class);
    loader.setFrameTransformation(transformation, firstFrame);

    verify(requestBuilder, times(3)).apply(isA(RequestOptions.class));
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
    verify(requestBuilder).into(aTarget());
  }

  @Test
  public void testGetNextFrameIncrementsSignatureAndAdvancesDecoderBeforeStartingLoad() {
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
    loader.subscribe(mock(FrameCallback.class));

    verify(requestBuilder, times(1)).into(aTarget());
  }

  @Test
  public void testGetNextFrameDoesNotStartLoadIfLoaderIsNotRunning() {
    verify(requestBuilder, times(1)).into(aTarget());
    loader.unsubscribe(callback);
    loader.onFrameReady(mock(DelayTarget.class));

    verify(requestBuilder, times(1)).into(aTarget());
  }

  @Test
  public void testGetNextFrameDoesNotStartLoadIfLoadIsInProgress() {
    loader.unsubscribe(callback);
    loader.subscribe(callback);

    verify(requestBuilder, times(1)).into(aTarget());
  }

  @Test
  public void testGetNextFrameDoesStartLoadIfRestartedAndNoLoadIsInProgress() {
    loader.unsubscribe(callback);

    loader.onFrameReady(mock(DelayTarget.class));
    loader.subscribe(callback);

    verify(requestBuilder, times(2)).into(aTarget());
  }

  @Test
  public void testGetNextFrameDoesStartLoadAfterLoadCompletesIfStarted() {
    loader.onFrameReady(mock(DelayTarget.class));

    verify(requestBuilder, times(2)).into(aTarget());
  }

  @Test
  public void testOnFrameReadyClearsPreviousFrame() {
    // Force the loader to create a real Handler.
    loader = createGifFrameLoader(null);

    DelayTarget previous = newDelayTarget();
    Request previousRequest = mock(Request.class);
    previous.setRequest(previousRequest);
    previous.onResourceReady(
        Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888), /* transition= */ null);

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

    DelayTarget previous = newDelayTarget();
    Request previousRequest = mock(Request.class);
    previous.setRequest(previousRequest);
    previous.onResourceReady(Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888), null);

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
    delayTarget.onResourceReady(
        Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888), null
        /*glideAnimation*/ );
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
    DelayTarget delayTarget = newDelayTarget();
    Request request = mock(Request.class);
    delayTarget.setRequest(request);

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
  public void onFrameReady_whenNotRunning_doesNotClearPreviouslyLoadedImage() {
    loader = createGifFrameLoader(/* handler= */ null);
    DelayTarget loaded = mock(DelayTarget.class);
    when(loaded.getResource()).thenReturn(Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888));
    loader.onFrameReady(loaded);
    loader.unsubscribe(callback);

    DelayTarget nextFrame = mock(DelayTarget.class);
    when(nextFrame.getResource())
        .thenReturn(Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888));
    loader.onFrameReady(nextFrame);
    verify(requestManager, never()).clear(loaded);
  }

  @Test
  public void onFrameReady_whenNotRunning_clearsPendingFrameOnClear() {
    loader = createGifFrameLoader(/* handler= */ null);
    DelayTarget loaded = mock(DelayTarget.class);
    when(loaded.getResource()).thenReturn(Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888));
    loader.onFrameReady(loaded);
    loader.unsubscribe(callback);

    DelayTarget nextFrame = mock(DelayTarget.class);
    when(nextFrame.getResource())
        .thenReturn(Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888));
    loader.onFrameReady(nextFrame);

    loader.clear();
    verify(requestManager).clear(loaded);
    verify(requestManager).clear(nextFrame);
  }

  @Test
  public void onFrameReady_whenNotRunning_clearsOldFrameOnStart() {
    loader = createGifFrameLoader(/* handler= */ null);
    DelayTarget loaded = mock(DelayTarget.class);
    when(loaded.getResource()).thenReturn(Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888));
    loader.onFrameReady(loaded);
    loader.unsubscribe(callback);

    DelayTarget nextFrame = mock(DelayTarget.class);
    when(nextFrame.getResource())
        .thenReturn(Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888));
    loader.onFrameReady(nextFrame);

    loader.subscribe(callback);
    verify(requestManager).clear(loaded);
  }

  @Test
  public void onFrameReady_whenNotRunning_callsFrameReadyWithNewFrameOnStart() {
    loader = createGifFrameLoader(/* handler= */ null);
    DelayTarget loaded = mock(DelayTarget.class);
    when(loaded.getResource()).thenReturn(Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888));
    loader.onFrameReady(loaded);
    loader.unsubscribe(callback);

    DelayTarget nextFrame = mock(DelayTarget.class);
    Bitmap expected = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888);
    when(nextFrame.getResource()).thenReturn(expected);
    loader.onFrameReady(nextFrame);

    verify(callback, times(1)).onFrameReady();
    loader.subscribe(callback);
    verify(callback, times(2)).onFrameReady();
    assertThat(loader.getCurrentFrame()).isEqualTo(expected);
  }

  @Test
  public void onFrameReady_whenInvisible_setVisibleLater() {
    loader = createGifFrameLoader(/* handler= */ null);
    // The target is invisible at this point.
    loader.unsubscribe(callback);
    loader.setNextStartFromFirstFrame();
    DelayTarget loaded = mock(DelayTarget.class);
    when(loaded.getResource()).thenReturn(Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888));
    loader.onFrameReady(loaded);
    loader.subscribe(callback);
  }

  @Test
  public void startFromFirstFrame_withPendingFrame_clearsPendingFrame() {
    loader = createGifFrameLoader(/* handler= */ null);
    DelayTarget loaded = mock(DelayTarget.class);
    when(loaded.getResource()).thenReturn(Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888));
    loader.onFrameReady(loaded);
    loader.unsubscribe(callback);

    DelayTarget nextFrame = mock(DelayTarget.class);
    Bitmap expected = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888);
    when(nextFrame.getResource()).thenReturn(expected);
    loader.onFrameReady(nextFrame);

    loader.setNextStartFromFirstFrame();
    verify(requestManager).clear(nextFrame);

    loader.subscribe(callback);
    verify(callback, times(1)).onFrameReady();
  }

  private DelayTarget newDelayTarget() {
    return new DelayTarget(handler, /* index= */ 0, /* targetTime= */ 0);
  }

  @SuppressWarnings("unchecked")
  private static Target<Bitmap> aTarget() {
    return isA(Target.class);
  }
}
