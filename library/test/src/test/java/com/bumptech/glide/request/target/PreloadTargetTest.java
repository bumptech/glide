package com.bumptech.glide.request.target;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.os.Looper;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.request.Request;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 18)
public class PreloadTargetTest {

  @Mock private RequestManager requestManager;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    shadowOf(Looper.getMainLooper()).pause();
  }

  @Test
  public void testCallsSizeReadyWithGivenDimensions() {
    int width = 1234;
    int height = 456;
    PreloadTarget<Object> target = PreloadTarget.obtain(requestManager, width, height);
    SizeReadyCallback cb = mock(SizeReadyCallback.class);
    target.getSize(cb);

    verify(cb).onSizeReady(eq(width), eq(height));
  }

  // This isn't really supposed to happen, but just to double check...
  @Test
  public void onResourceReady_withNullRequest_doesNotClearTarget() {
    PreloadTarget<Object> target = PreloadTarget.obtain(requestManager, 100, 100);
    target.setRequest(null);

    callOnResourceReadyAndRunUiRunnables(target);

    verify(requestManager, never()).clear(target);
  }

  @Test
  public void onResourceReady_withNotYetCompleteRequest_doesNotClearTarget() {
    Request request = mock(Request.class);
    when(request.isComplete()).thenReturn(false);

    PreloadTarget<Object> target = PreloadTarget.obtain(requestManager, 100, 100);
    target.setRequest(request);

    callOnResourceReadyAndRunUiRunnables(target);

    verify(requestManager, never()).clear(target);
  }

  @Test
  public void onResourceReady_withCompleteRequest_postsToClearTarget() {
    Request request = mock(Request.class);
    when(request.isComplete()).thenReturn(true);

    PreloadTarget<Object> target = PreloadTarget.obtain(requestManager, 100, 100);
    target.setRequest(request);

    callOnResourceReadyAndRunUiRunnables(target);

    verify(requestManager).clear(target);
  }

  @Test
  public void onResourceReady_withCompleteRequest_doesNotImmediatelyClearTarget() {
    Request request = mock(Request.class);
    when(request.isComplete()).thenReturn(true);

    PreloadTarget<Object> target = PreloadTarget.obtain(requestManager, 100, 100);
    target.setRequest(request);

    target.onResourceReady(new Object(), /* transition= */ null);

    verify(requestManager, never()).clear(target);
  }

  private void callOnResourceReadyAndRunUiRunnables(Target<Object> target) {
    target.onResourceReady(new Object(), /* transition= */ null);
    shadowOf(Looper.getMainLooper()).idle();
  }
}
