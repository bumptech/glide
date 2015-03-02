package com.bumptech.glide.request;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
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

import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;

import com.bumptech.glide.load.engine.Engine;
import com.bumptech.glide.load.engine.RequestContext;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.request.target.SizeReadyCallback;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.bumptech.glide.request.transition.TransitionFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, emulateSdk = 18)
public class SingleRequestTest {
  private RequestHarness harness;

  /**
   * {@link Number} and {@link List} are arbitrarily chosen types to test some type safety as well.
   * Both are in the middle of the hierarchy having multiple descendants and ancestors.
   */
  @SuppressWarnings("unchecked")
  private static class RequestHarness {
    Engine engine = mock(Engine.class);
    Number model = 123456;
    Target<List> target = mock(Target.class);
    Resource<List> resource = mock(Resource.class);
    RequestCoordinator requestCoordinator = mock(RequestCoordinator.class);
    Drawable placeholderDrawable = null;
    Drawable errorDrawable = null;
    RequestListener<List> requestListener = mock(RequestListener.class);
    TransitionFactory<List> factory = mock(TransitionFactory.class);
    int overrideWidth = -1;
    int overrideHeight = -1;
    List result = new ArrayList();
    RequestContext<Number, List> requestContext = mock(RequestContext.class);

    public RequestHarness() {
      when(requestCoordinator.canSetImage(any(Request.class))).thenReturn(true);
      when(requestCoordinator.canNotifyStatusChanged(any(Request.class))).thenReturn(true);
      when(resource.get()).thenReturn(result);
    }

    public SingleRequest<List> getRequest() {
      when(requestContext.getModel()).thenReturn(model);
      when(requestContext.getTranscodeClass()).thenReturn(List.class);
      when(requestContext.getErrorDrawable()).thenReturn(errorDrawable);
      when(requestContext.getPlaceholderDrawable()).thenReturn(placeholderDrawable);
      when(requestContext.getOverrideWidth()).thenReturn(overrideWidth);
      when(requestContext.getOverrideHeight()).thenReturn(overrideHeight);
      when(requestContext.getSizeMultiplier()).thenReturn(1f);

      return SingleRequest
          .obtain(requestContext, target, requestListener, requestCoordinator, engine, factory);
    }
  }

  @Before
  public void setUp() {
    harness = new RequestHarness();
  }

  @Test
  public void testIsNotCompleteBeforeReceivingResource() {
    SingleRequest<List> request = harness.getRequest();

    assertFalse(request.isComplete());
  }

  @Test
  public void testCanHandleNullResources() {
    SingleRequest<List> request = harness.getRequest();

    request.onResourceReady(null);

    assertTrue(request.isFailed());
    verify(harness.requestListener)
        .onLoadFailed(any(Number.class), eq(harness.target), anyBoolean());
  }

  @Test
  public void testCanHandleEmptyResources() {
    SingleRequest<List> request = harness.getRequest();
    when(harness.resource.get()).thenReturn(null);

    request.onResourceReady(harness.resource);

    assertTrue(request.isFailed());
    verify(harness.engine).release(eq(harness.resource));
    verify(harness.requestListener)
        .onLoadFailed(any(Number.class), eq(harness.target), anyBoolean());
  }

  @Test
  public void testCanHandleNonConformingResources() {
    SingleRequest<List> request = harness.getRequest();
    when(((Resource) (harness.resource)).get())
        .thenReturn("Invalid mocked String, this should be a List");

    request.onResourceReady(harness.resource);

    assertTrue(request.isFailed());
    verify(harness.engine).release(eq(harness.resource));
    verify(harness.requestListener)
        .onLoadFailed(any(Number.class), eq(harness.target), anyBoolean());
  }

  @Test
  public void testIsNotFailedAfterClear() {
    SingleRequest<List> request = harness.getRequest();

    request.onResourceReady(null);
    request.clear();

    assertFalse(request.isFailed());
  }

  @Test
  public void testIsPausedAfterPause() {
    SingleRequest<List> request = harness.getRequest();
    request.pause();

    assertTrue(request.isPaused());
  }

  @Test
  public void testIsNotCancelledAfterPause() {
    SingleRequest<List> request = harness.getRequest();
    request.pause();

    assertFalse(request.isCancelled());
  }

  @Test
  public void testIsNotPausedAfterBeginningWhilePaused() {
    SingleRequest<List> request = harness.getRequest();
    request.pause();
    request.begin();

    assertFalse(request.isPaused());
    assertTrue(request.isRunning());
  }

  @Test
  public void testIsNotFailedAfterBegin() {
    SingleRequest<List> request = harness.getRequest();

    request.onResourceReady(null);
    request.begin();

    assertFalse(request.isFailed());
  }

  @Test
  public void testIsCompleteAfterReceivingResource() {
    SingleRequest<List> request = harness.getRequest();

    request.onResourceReady(harness.resource);

    assertTrue(request.isComplete());
  }

  @Test
  public void testIsNotCompleteAfterClear() {
    SingleRequest<List> request = harness.getRequest();
    request.onResourceReady(harness.resource);
    request.clear();

    assertFalse(request.isComplete());
  }

  @Test
  public void testIsCancelledAfterClear() {
    SingleRequest<List> request = harness.getRequest();
    request.clear();

    assertTrue(request.isCancelled());
  }

  @Test
  public void testDoesNotNotifyTargetTwiceIfClearedTwiceInARow() {
    SingleRequest<List> request = harness.getRequest();
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
    SingleRequest<List> request = harness.getRequest();

    request.onResourceReady(harness.resource);

    verify(requestCoordinator).canSetImage(eq(request));
  }

  @Test
  public void testIsNotFailedWithoutException() {
    SingleRequest<List> request = harness.getRequest();

    assertFalse(request.isFailed());
  }

  @Test
  public void testIsFailedAfterException() {
    SingleRequest<List> request = harness.getRequest();

    request.onLoadFailed();
    assertTrue(request.isFailed());
  }

  @Test
  public void testIgnoresOnSizeReadyIfNotWaitingForSize() {
    SingleRequest<List> request = harness.getRequest();
    request.begin();
    request.onSizeReady(100, 100);
    request.onSizeReady(100, 100);

    verify(harness.engine, times(1))
        .load(eq(harness.requestContext), eq(100), eq(100), any(ResourceCallback.class));
  }

  @Test
  public void testIsFailedAfterNoResultAndNullException() {
    SingleRequest<List> request = harness.getRequest();

    request.onLoadFailed();
    assertTrue(request.isFailed());
  }

  @Test
  public void testEngineLoadCancelledOnCancel() {
    Engine.LoadStatus loadStatus = mock(Engine.LoadStatus.class);

    when(harness.engine
        .load(any(RequestContext.class), anyInt(), anyInt(), any(ResourceCallback.class)))
        .thenReturn(loadStatus);

    SingleRequest<List> request = harness.getRequest();
    request.begin();

    request.onSizeReady(100, 100);
    request.cancel();

    verify(loadStatus).cancel();
  }

  @Test
  public void testResourceIsRecycledOnClear() {
    SingleRequest<List> request = harness.getRequest();

    request.onResourceReady(harness.resource);
    request.clear();

    verify(harness.engine).release(eq(harness.resource));
  }

  @Test
  public void testPlaceholderDrawableIsSet() {
    Drawable expected = new ColorDrawable(Color.RED);

    MockTarget target = new MockTarget();

    harness.placeholderDrawable = expected;
    harness.target = target;
    SingleRequest<List> request = harness.getRequest();
    request.begin();

    assertEquals(expected, target.currentPlaceholder);
  }

  @Test
  public void testErrorDrawableIsSetOnLoadFailed() {
    Drawable expected = new ColorDrawable(Color.RED);

    MockTarget target = new MockTarget();

    harness.errorDrawable = expected;
    harness.target = target;
    SingleRequest<List> request = harness.getRequest();

    request.onLoadFailed();

    assertEquals(expected, target.currentPlaceholder);
  }

  @Test
  public void setTestPlaceholderDrawableSetOnNullModel() {
    Drawable placeholder = new ColorDrawable(Color.RED);

    MockTarget target = new MockTarget();

    harness.placeholderDrawable = placeholder;
    harness.target = target;
    harness.model = null;
    SingleRequest<List> request = harness.getRequest();

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
    SingleRequest<List> request = harness.getRequest();

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
    SingleRequest<List> request = harness.getRequest();
    request.begin();
    request.onResourceReady(harness.resource);

    assertFalse(request.isRunning());
  }

  @Test
  public void testIsNotRunningAfterFailing() {
    SingleRequest<List> request = harness.getRequest();
    request.begin();
    request.onLoadFailed();

    assertFalse(request.isRunning());
  }

  @Test
  public void testIsNotRunningAfterClear() {
    SingleRequest<List> request = harness.getRequest();
    request.begin();
    request.clear();

    assertFalse(request.isRunning());
  }

  @Test
  public void testCallsTargetOnResourceReadyIfNoRequestListener() {
    harness.requestListener = null;
    SingleRequest<List> request = harness.getRequest();
    request.onResourceReady(harness.resource);

    verify(harness.target).onResourceReady(eq(harness.result), any(Transition.class));
  }

  @Test
  public void testCallsTargetOnResourceReadyIfRequestListenerReturnsFalse() {
    SingleRequest<List> request = harness.getRequest();
    when(harness.requestListener
        .onResourceReady(any(List.class), any(Number.class), eq(harness.target), anyBoolean(),
            anyBoolean())).thenReturn(false);
    request.onResourceReady(harness.resource);

    verify(harness.target).onResourceReady(eq(harness.result), any(Transition.class));
  }

  @Test
  public void testDoesNotCallTargetOnResourceReadyIfRequestListenerReturnsTrue() {
    SingleRequest<List> request = harness.getRequest();
    when(harness.requestListener
        .onResourceReady(any(List.class), any(Number.class), eq(harness.target), anyBoolean(),
            anyBoolean())).thenReturn(true);
    request.onResourceReady(harness.resource);

    verify(harness.target, never()).onResourceReady(any(List.class), any(Transition.class));
  }

  @Test
  public void testCallsTargetOnExceptionIfNoRequestListener() {
    harness.requestListener = null;
    SingleRequest<List> request = harness.getRequest();
    request.onLoadFailed();

    verify(harness.target).onLoadFailed(eq(harness.errorDrawable));
  }

  @Test
  public void testCallsTargetOnExceptionIfRequestListenerReturnsFalse() {
    SingleRequest<List> request = harness.getRequest();
    when(harness.requestListener
        .onLoadFailed(any(Number.class), eq(harness.target), anyBoolean()))
        .thenReturn(false);
    request.onLoadFailed();

    verify(harness.target).onLoadFailed(eq(harness.errorDrawable));
  }

  @Test
  public void testDoesNotCallTargetOnExceptionIfRequestListenerReturnsTrue() {
    SingleRequest<List> request = harness.getRequest();
    when(harness.requestListener
        .onLoadFailed(any(Number.class), eq(harness.target), anyBoolean()))
        .thenReturn(true);

    request.onLoadFailed();

    verify(harness.target, never()).onLoadFailed(any(Drawable.class));
  }

  @Test
  public void testRequestListenerIsCalledWithResourceResult() {
    SingleRequest<List> request = harness.getRequest();
    request.onResourceReady(harness.resource);

    verify(harness.requestListener)
        .onResourceReady(eq(harness.result), any(Number.class), any(Target.class), anyBoolean(),
            anyBoolean());
  }

  @Test
  public void testRequestListenerIsCalledWithModel() {
    SingleRequest<List> request = harness.getRequest();
    request.onResourceReady(harness.resource);

    verify(harness.requestListener)
        .onResourceReady(any(List.class), eq(harness.model), any(Target.class), anyBoolean(),
            anyBoolean());
  }

  @Test
  public void testRequestListenerIsCalledWithTarget() {
    SingleRequest<List> request = harness.getRequest();
    request.onResourceReady(harness.resource);

    verify(harness.requestListener)
        .onResourceReady(any(List.class), any(Number.class), eq(harness.target), anyBoolean(),
            anyBoolean());
  }

  @Test
  public void testRequestListenerIsCalledWithLoadedFromMemoryIfLoadCompletesSynchronously() {
    final SingleRequest<List> request = harness.getRequest();

    when(harness.engine
        .load(any(RequestContext.class), anyInt(), anyInt(), any(ResourceCallback.class)))
        .thenAnswer(new Answer<Object>() {
          @Override
          public Object answer(InvocationOnMock invocation) throws Throwable {
            request.onResourceReady(harness.resource);
            return null;
          }
        });

    request.begin();
    request.onSizeReady(100, 100);
    verify(harness.requestListener)
        .onResourceReady(eq(harness.result), any(Number.class), any(Target.class), eq(true),
            anyBoolean());
  }

  @Test
  public void
  testRequestListenerIsCalledWithNotLoadedFromMemoryCacheIfLoadCompletesAsynchronously() {
    SingleRequest<List> request = harness.getRequest();
    request.onSizeReady(100, 100);
    request.onResourceReady(harness.resource);

    verify(harness.requestListener)
        .onResourceReady(eq(harness.result), any(Number.class), any(Target.class), eq(false),
            anyBoolean());
  }

  @Test
  public void testRequestListenerIsCalledWithIsFirstResourceIfNoRequestCoordinator() {
    harness.requestCoordinator = null;
    SingleRequest<List> request = harness.getRequest();
    request.onResourceReady(harness.resource);

    verify(harness.requestListener)
        .onResourceReady(eq(harness.result), any(Number.class), any(Target.class), anyBoolean(),
            eq(true));
  }

  @Test
  public void testRequestListenerIsCalledWithFirstImageIfRequestCoordinatorReturnsNoResourceSet() {
    SingleRequest<List> request = harness.getRequest();
    when(harness.requestCoordinator.isAnyResourceSet()).thenReturn(false);
    request.onResourceReady(harness.resource);

    verify(harness.requestListener)
        .onResourceReady(eq(harness.result), any(Number.class), any(Target.class), anyBoolean(),
            eq(true));
  }

  @Test
  public void
  testRequestListenerIsCalledWithNotIsFirstRequestIfRequestCoordinatorReturnsResourceSet() {
    SingleRequest<List> request = harness.getRequest();
    when(harness.requestCoordinator.isAnyResourceSet()).thenReturn(true);
    request.onResourceReady(harness.resource);

    verify(harness.requestListener)
        .onResourceReady(eq(harness.result), any(Number.class), any(Target.class), anyBoolean(),
            eq(false));
  }

  @Test
  public void testTargetIsCalledWithAnimationFromFactory() {
    SingleRequest<List> request = harness.getRequest();
    Transition<List> transition = mock(Transition.class);
    when(harness.factory.build(anyBoolean(), anyBoolean())).thenReturn(transition);
    request.onResourceReady(harness.resource);

    verify(harness.target).onResourceReady(eq(harness.result), eq(transition));
  }

  @Test
  public void testCallsGetSizeIfOverrideWidthIsLessThanZero() {
    harness.overrideWidth = -1;
    harness.overrideHeight = 100;
    SingleRequest<List> request = harness.getRequest();
    request.begin();

    verify(harness.target).getSize(any(SizeReadyCallback.class));
  }

  @Test
  public void testCallsGetSizeIfOverrideHeightIsLessThanZero() {
    harness.overrideHeight = -1;
    harness.overrideWidth = 100;
    SingleRequest<List> request = harness.getRequest();
    request.begin();

    verify(harness.target).getSize(any(SizeReadyCallback.class));
  }

  @Test
  public void testDoesNotCallGetSizeIfOverrideWidthAndHeightAreSet() {
    harness.overrideWidth = 100;
    harness.overrideHeight = 100;
    SingleRequest<List> request = harness.getRequest();
    request.begin();

    verify(harness.target, never()).getSize(any(SizeReadyCallback.class));
  }

  @Test
  public void testCallsEngineWithOverrideWidthAndHeightIfSet() {
    harness.overrideWidth = 1;
    harness.overrideHeight = 2;

    SingleRequest<List> request = harness.getRequest();
    request.begin();

    verify(harness.engine)
        .load(eq(harness.requestContext), eq(harness.overrideWidth), eq(harness.overrideHeight),
            any(ResourceCallback.class));
  }

  @Test
  public void testDoesNotSetErrorDrawableIfRequestCoordinatorDoesntAllowIt() {
    harness.errorDrawable = new ColorDrawable(Color.RED);
    SingleRequest<List> request = harness.getRequest();
    when(harness.requestCoordinator.canNotifyStatusChanged(any(Request.class))).thenReturn(false);
    request.onLoadFailed();

    verify(harness.target, never()).onLoadFailed(any(Drawable.class));
  }

  @Test
  public void testCanReRunCancelledRequests() {
    doAnswer(new CallSizeReady(100, 100)).when(harness.target)
        .getSize(any(SizeReadyCallback.class));

    when(harness.engine
        .load(eq(harness.requestContext), eq(100), eq(100), any(ResourceCallback.class)))
        .thenAnswer(new CallResourceCallback(harness.resource));
    SingleRequest<List> request = harness.getRequest();

    request.begin();
    request.cancel();
    request.begin();

    verify(harness.target, times(2)).onResourceReady(eq(harness.result), any(Transition.class));
  }

  @Test
  public void testResourceOnlyReceivesOneGetOnResourceReady() {
    SingleRequest<List> request = harness.getRequest();
    request.onResourceReady(harness.resource);

    verify(harness.resource, times(1)).get();
  }

  @Test
  public void testDoesNotStartALoadIfOnSizeReadyIsCalledAfterCancel() {
    SingleRequest<List> request = harness.getRequest();
    request.cancel();
    request.onSizeReady(100, 100);

    verify(harness.engine, never())
        .load(any(RequestContext.class), anyInt(), anyInt(), any(ResourceCallback.class));
  }

  private static class CallResourceCallback implements Answer {

    private Resource resource;

    public CallResourceCallback(Resource resource) {
      this.resource = resource;
    }

    @Override
    public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
      ResourceCallback cb =
          (ResourceCallback) invocationOnMock.getArguments()[invocationOnMock.getArguments().length
              - 1];
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
      SizeReadyCallback cb = (SizeReadyCallback) invocationOnMock.getArguments()[0];
      cb.onSizeReady(width, height);
      return null;
    }
  }

  private void mockContextToReturn(int resourceId, Drawable drawable) {
    Resources resources = mock(Resources.class);
    when(harness.requestContext.getResources()).thenReturn(resources);
    when(resources.getDrawable(eq(resourceId))).thenReturn(drawable);
  }

  private static class MockTarget implements Target<List> {
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
    public void onLoadFailed(Drawable errorDrawable) {
      currentPlaceholder = errorDrawable;

    }

    @Override
    public void onResourceReady(List resource, Transition<? super List> transition) {
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
