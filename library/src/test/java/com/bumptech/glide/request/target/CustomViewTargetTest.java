package com.bumptech.glide.request.target;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnAttachStateChangeListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.request.Request;
import com.bumptech.glide.request.transition.Transition;
import com.google.common.truth.Truth;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

/**
 * Test for {@link CustomViewTarget}.
 *
 * <p>TODO: This should really be in the tests subproject, but that causes errors because the R
 * class referenced in {@link CustomViewTarget} can't be found. This should be fixable with some
 * gradle changes, but I've so far failed to figure out the right set of commands.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = Config.OLDEST_SDK)
public class CustomViewTargetTest {
  private View view;
  private ViewGroup parent;
  private CustomViewTarget<View, Object> target;
  @Mock private SizeReadyCallback cb;
  @Mock private Request request;
  private AttachStateTarget attachStateTarget;

  public static final class TestActivity extends Activity {
    ViewGroup parent;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      parent = new FrameLayout(this);
      setContentView(parent);
    }
  }

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    ActivityController<TestActivity> activityController =
        Robolectric.buildActivity(TestActivity.class).setup();
    TestActivity activity = activityController.get();
    parent = activity.parent;
    view = new ImageView(activity);
    target = new TestViewTarget(view);
    attachStateTarget = new AttachStateTarget(view);
  }

  private void attachAndLayoutView() {
    if (view.getParent() == null) {
      parent.addView(view);
    }
    parent.requestLayout();
    ShadowLooper.idleMainLooper();
  }

  @After
  public void tearDown() {
    CustomViewTarget.SizeDeterminer.maxDisplayLength = null;
  }

  @Test
  public void testReturnsWrappedView() {
    assertEquals(view, target.getView());
  }

  @Test
  public void testReturnsNullFromGetRequestIfNoRequestSet() {
    assertNull(target.getRequest());
  }

  @Test
  public void testCanSetAndRetrieveRequest() {
    target.setRequest(request);

    assertEquals(request, target.getRequest());
  }

  @Test
  public void testRetrievesRequestFromPreviousTargetForView() {
    target.setRequest(request);

    CustomViewTarget<View, Object> second = new TestViewTarget(view);

    assertEquals(request, second.getRequest());
  }

  @Test
  public void testSizeCallbackIsCalledSynchronouslyIfViewSizeSet() {
    int dimens = 333;
    view.layout(0, 0, dimens, dimens);

    target.getSize(cb);

    verify(cb).onSizeReady(eq(dimens), eq(dimens));
  }

  @Test
  public void testSizeCallbackIsCalledSynchronouslyIfLayoutParamsConcreteSizeSet() {
    int dimens = 444;
    LayoutParams layoutParams = new FrameLayout.LayoutParams(dimens, dimens);
    view.setLayoutParams(layoutParams);
    view.requestLayout();

    target.getSize(cb);

    verify(cb).onSizeReady(eq(dimens), eq(dimens));
  }

  @Config(qualifiers = "w200dp-h300dp")
  @Test
  public void getSize_withBothWrapContent_usesDisplayDimens() {
    LayoutParams layoutParams =
        new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    view.setLayoutParams(layoutParams);

    attachAndLayoutView();

    target.getSize(cb);

    verify(cb).onSizeReady(300, 300);
  }

  @Config(qualifiers = "w100dp-h200dp")
  @Test
  public void getSize_withWrapContentWidthAndValidHeight_usesDisplayDimenAndValidHeight() {
    int height = 100;
    LayoutParams params = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, height);
    view.setLayoutParams(params);

    attachAndLayoutView();

    target.getSize(cb);

    verify(cb).onSizeReady(200, height);
  }

  @Config(qualifiers = "w200dp-h100dp")
  @Test
  public void getSize_withWrapContentHeightAndValidWidth_returnsWidthAndDisplayDimen() {
    int width = 100;
    LayoutParams params = new FrameLayout.LayoutParams(width, LayoutParams.WRAP_CONTENT);
    view.setLayoutParams(params);

    attachAndLayoutView();

    target.getSize(cb);

    verify(cb).onSizeReady(width, 200);
  }

  @Config(qualifiers = "w500dp-h600dp")
  @Test
  public void getSize_withWrapContentWidthAndMatchParentHeight_usesDisplayDimenWidthAndHeight() {
    LayoutParams params =
        new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
    view.setLayoutParams(params);

    target.getSize(cb);

    verify(cb, never()).onSizeReady(anyInt(), anyInt());

    int height = 32;
    parent.getLayoutParams().height = height;
    attachAndLayoutView();

    view.getViewTreeObserver().dispatchOnPreDraw();

    verify(cb).onSizeReady(600, height);
  }

  @Config(qualifiers = "w300dp-h400dp")
  @Test
  public void getSize_withMatchParentWidthAndWrapContentHeight_usesWidthAndDisplayDimenHeight() {
    LayoutParams params =
        new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
    view.setLayoutParams(params);

    target.getSize(cb);

    verify(cb, never()).onSizeReady(anyInt(), anyInt());

    int width = 32;
    parent.getLayoutParams().width = width;
    attachAndLayoutView();
    view.getViewTreeObserver().dispatchOnPreDraw();

    verify(cb).onSizeReady(width, 400);
  }

  @Test
  public void testMatchParentWidthAndHeight() {
    LayoutParams params =
        new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    view.setLayoutParams(params);

    target.getSize(cb);

    verify(cb, never()).onSizeReady(anyInt(), anyInt());

    attachAndLayoutView();
    view.getViewTreeObserver().dispatchOnPreDraw();

    verify(cb).onSizeReady(eq(parent.getWidth()), eq(parent.getHeight()));
  }

  @Test
  public void testSizeCallbackIsCalledPreDrawIfNoDimensAndNoLayoutParams() {
    target.getSize(cb);

    int width = 12;
    int height = 32;
    parent.getLayoutParams().width = width;
    parent.getLayoutParams().height = height;
    attachAndLayoutView();
    view.getViewTreeObserver().dispatchOnPreDraw();

    verify(cb).onSizeReady(eq(width), eq(height));
  }

  @Test
  public void testSizeCallbacksAreCalledInOrderPreDraw() {
    SizeReadyCallback[] cbs = new SizeReadyCallback[25];
    for (int i = 0; i < cbs.length; i++) {
      cbs[i] = mock(SizeReadyCallback.class);
      target.getSize(cbs[i]);
    }

    int width = 100;
    int height = 111;
    parent.getLayoutParams().width = width;
    parent.getLayoutParams().height = height;
    attachAndLayoutView();
    view.getViewTreeObserver().dispatchOnPreDraw();

    InOrder order = inOrder((Object[]) cbs);
    for (SizeReadyCallback cb : cbs) {
      order.verify(cb).onSizeReady(eq(width), eq(height));
    }
  }

  @Test
  public void testDoesNotNotifyCallbackTwiceIfAddedTwice() {
    target.getSize(cb);
    target.getSize(cb);

    view.setLayoutParams(new FrameLayout.LayoutParams(100, 100));
    attachAndLayoutView();
    view.getViewTreeObserver().dispatchOnPreDraw();

    verify(cb, times(1)).onSizeReady(anyInt(), anyInt());
  }

  @Test
  public void testDoesNotAddMultipleListenersIfMultipleCallbacksAreAdded() {
    SizeReadyCallback cb1 = mock(SizeReadyCallback.class);
    SizeReadyCallback cb2 = mock(SizeReadyCallback.class);
    target.getSize(cb1);
    target.getSize(cb2);
    view.getViewTreeObserver().dispatchOnPreDraw();
    // assertThat(shadowObserver.getPreDrawListeners()).hasSize(1);
  }

  @Test
  public void testDoesAddSecondListenerIfFirstListenerIsRemovedBeforeSecondRequest() {
    SizeReadyCallback cb1 = mock(SizeReadyCallback.class);
    target.getSize(cb1);

    view.setLayoutParams(new FrameLayout.LayoutParams(100, 100));
    attachAndLayoutView();
    view.getViewTreeObserver().dispatchOnPreDraw();

    SizeReadyCallback cb2 = mock(SizeReadyCallback.class);
    view.setLayoutParams(
        new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    target.getSize(cb2);

    view.setLayoutParams(new FrameLayout.LayoutParams(100, 100));
    view.getViewTreeObserver().dispatchOnPreDraw();

    verify(cb2).onSizeReady(anyInt(), anyInt());
  }

  @Test
  public void testSizeCallbackIsNotCalledPreDrawIfNoDimensSetOnPreDraw() {
    target.getSize(cb);
    view.getViewTreeObserver().dispatchOnPreDraw();

    verify(cb, never()).onSizeReady(anyInt(), anyInt());

    parent.getLayoutParams().width = 100;
    parent.getLayoutParams().height = 100;
    view.setLayoutParams(
        new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    attachAndLayoutView();
    view.getViewTreeObserver().dispatchOnPreDraw();
    verify(cb).onSizeReady(anyInt(), anyInt());
  }

  @Test
  public void testSizeCallbackIsCalledPreDrawIfNoDimensAndNoLayoutParamsButLayoutParamsSetLater() {
    target.getSize(cb);

    int width = 689;
    int height = 354;
    LayoutParams layoutParams = new FrameLayout.LayoutParams(width, height);
    view.setLayoutParams(layoutParams);
    view.requestLayout();
    view.getViewTreeObserver().dispatchOnPreDraw();

    verify(cb).onSizeReady(eq(width), eq(height));
  }

  @Test
  public void testCallbackIsNotCalledTwiceIfPreDrawFiresTwice() {
    attachAndLayoutView();
    target.getSize(cb);

    LayoutParams layoutParams = new FrameLayout.LayoutParams(1234, 4123);
    view.setLayoutParams(layoutParams);
    view.requestLayout();
    view.getViewTreeObserver().dispatchOnPreDraw();
    view.getViewTreeObserver().dispatchOnPreDraw();

    verify(cb, times(1)).onSizeReady(anyInt(), anyInt());
  }

  @Test
  public void testCallbacksFromMultipleRequestsAreNotifiedOnPreDraw() {
    SizeReadyCallback firstCb = mock(SizeReadyCallback.class);
    SizeReadyCallback secondCb = mock(SizeReadyCallback.class);
    target.getSize(firstCb);
    target.getSize(secondCb);

    int width = 68;
    int height = 875;
    LayoutParams layoutParams = new FrameLayout.LayoutParams(width, height);
    view.setLayoutParams(layoutParams);
    attachAndLayoutView();
    view.getViewTreeObserver().dispatchOnPreDraw();
    view.getViewTreeObserver().dispatchOnPreDraw();

    verify(firstCb, times(1)).onSizeReady(eq(width), eq(height));
    verify(secondCb, times(1)).onSizeReady(eq(width), eq(height));
  }

  @Test
  public void testDoesNotThrowOnPreDrawIfViewTreeObserverIsDead() {
    target.getSize(cb);

    int width = 1;
    int height = 2;
    LayoutParams layoutParams = new FrameLayout.LayoutParams(width, height);
    view.setLayoutParams(layoutParams);
    ViewTreeObserver vto = view.getViewTreeObserver();
    view.requestLayout();
    attachAndLayoutView();
    assertFalse(vto.isAlive());
    vto.dispatchOnPreDraw();

    verify(cb).onSizeReady(eq(width), eq(height));
  }

  @Test(expected = NullPointerException.class)
  public void testThrowsIfGivenNullView() {
    new TestViewTarget(null);
  }

  @Test
  public void testDecreasesDimensionsByViewPadding() {
    attachAndLayoutView();
    view.setLayoutParams(new FrameLayout.LayoutParams(100, 100));
    view.setPadding(25, 25, 25, 25);
    view.requestLayout();

    target.getSize(cb);

    verify(cb).onSizeReady(50, 50);
  }

  @Test
  public void getSize_withValidWidthAndHeight_notLaidOut_notLayoutRequested_callsSizeReady() {
    view.setRight(100);
    view.setBottom(100);
    target.getSize(cb);

    verify(cb).onSizeReady(100, 100);
  }

  @Test
  public void getSize_withLayoutParams_notLaidOut_doesCallSizeReady() {
    view.setLayoutParams(new FrameLayout.LayoutParams(10, 10));
    view.setRight(100);
    view.setBottom(100);
    target.getSize(cb);

    verify(cb, times(1)).onSizeReady(anyInt(), anyInt());
  }

  @Test
  public void getSize_withLayoutParams_emptyParams_notLaidOutOrLayoutRequested_callsSizeReady() {
    view.setLayoutParams(new FrameLayout.LayoutParams(0, 0));
    view.setRight(100);
    view.setBottom(100);
    target.getSize(cb);

    verify(cb).onSizeReady(100, 100);
  }

  @Test
  public void getSize_withValidWidthAndHeight_preV19_layoutRequested_callsSizeReady() {
    view.setLayoutParams(new FrameLayout.LayoutParams(100, 100));
    view.requestLayout();

    target.getSize(cb);

    verify(cb).onSizeReady(100, 100);
  }

  @Test
  public void getSize_withWidthAndHeightEqualToPadding_doesNotCallSizeReady() {
    view.setLayoutParams(new FrameLayout.LayoutParams(100, 100));
    view.requestLayout();
    view.setPadding(50, 50, 50, 50);

    target.getSize(cb);

    verify(cb, never()).onSizeReady(anyInt(), anyInt());
  }

  @Test
  public void clearOnDetach_onDetach_withNullRequest_doesNothing() {
    attachStateTarget.clearOnDetach();
    attachStateTarget.setRequest(null);
    attachAndLayoutView();
  }

  // This behavior isn't clearly correct, but it doesn't seem like there's any harm to clear an
  // already cleared request, so we might as well avoid the extra check/complexity in the code.
  @Test
  public void clearOnDetach_onDetach_withClearedRequest_clearsRequest() {
    attachAndLayoutView();
    attachStateTarget.clearOnDetach();
    attachStateTarget.setRequest(request);
    when(request.isCleared()).thenReturn(true);
    parent.removeView(view);

    verify(request).clear();
  }

  @Test
  public void clearOnDetach_onDetach_withRunningRequest_pausesRequestOnce() {
    attachAndLayoutView();
    attachStateTarget.clearOnDetach();
    attachStateTarget.setRequest(request);
    parent.removeView(view);

    verify(request).clear();
  }

  @Test
  public void clearOnDetach_onDetach_afterOnLoadCleared_removesListener() {
    attachAndLayoutView();
    attachStateTarget.clearOnDetach();
    attachStateTarget.onLoadCleared(/* placeholder= */ null);
    attachStateTarget.setRequest(request);
    parent.removeView(view);

    verify(request, never()).clear();
  }

  @Test
  public void clearOnDetach_moreThanOnce_registersObserverOnce() {
    attachAndLayoutView();
    attachStateTarget.setRequest(request);
    attachStateTarget.clearOnDetach().clearOnDetach();
    parent.removeView(view);

    verify(request).clear();
  }

  @Test
  public void clearOnDetach_onDetach_afterMultipleClearOnDetaches_removesListener() {
    attachAndLayoutView();
    attachStateTarget.clearOnDetach().clearOnDetach().clearOnDetach();
    attachStateTarget.onLoadCleared(/* placeholder= */ null);
    attachStateTarget.setRequest(request);
    parent.removeView(view);

    verify(request, never()).clear();
  }

  @Test
  public void clearOnDetach_onDetach_afterLoadCleared_clearsRequest() {
    attachAndLayoutView();
    attachStateTarget.clearOnDetach();
    attachStateTarget.setRequest(request);
    when(request.isCleared()).thenReturn(true);
    parent.removeView(view);

    verify(request).clear();
  }

  @Test
  public void clearOnDetach_onAttach_withNullRequest_doesNothing() {
    attachStateTarget.clearOnDetach();
    attachStateTarget.setRequest(null);
    attachAndLayoutView();
  }

  @Test
  public void clearOnDetach_onAttach_withRunningRequest_doesNotBeginRequest() {
    attachStateTarget.clearOnDetach();
    attachStateTarget.setRequest(request);
    when(request.isCleared()).thenReturn(false);
    attachAndLayoutView();

    verify(request, never()).begin();
  }

  @Test
  public void clearOnDetach_onAttach_withClearedRequest_beginsRequest() {
    attachStateTarget.clearOnDetach();
    attachStateTarget.setRequest(request);
    when(request.isCleared()).thenReturn(true);
    attachAndLayoutView();

    verify(request).begin();
  }

  @Test
  public void clearOnDetach_afterLoadClearedAndRestarted_onAttach_beginsRequest() {
    attachStateTarget.clearOnDetach();
    attachStateTarget.setRequest(request);
    when(request.isCleared()).thenReturn(true);
    attachStateTarget.onLoadCleared(/* placeholder= */ null);
    attachStateTarget.onLoadStarted(/* placeholder= */ null);
    attachAndLayoutView();

    verify(request).begin();
  }

  @Test
  public void clearOnDetach_onAttach_afterLoadCleared_doesNotBeingRequest() {
    attachStateTarget.clearOnDetach();
    attachStateTarget.setRequest(request);
    when(request.isCleared()).thenReturn(true);
    attachStateTarget.onLoadCleared(/* placeholder= */ null);
    attachAndLayoutView();

    verify(request, never()).begin();
  }

  @Test
  public void onLoadStarted_withoutClearOnDetach_doesNotAddListener() {
    attachAndLayoutView();
    target.setRequest(request);
    attachStateTarget.onLoadStarted(/* placeholder= */ null);
    parent.removeView(view);

    verify(request, never()).clear();
  }

  @Test
  public void onLoadCleared_withoutClearOnDetach_doesNotRemoveListeners() {
    final AtomicInteger count = new AtomicInteger();
    OnAttachStateChangeListener expected =
        new OnAttachStateChangeListener() {
          @Override
          public void onViewAttachedToWindow(View v) {
            count.incrementAndGet();
          }

          @Override
          public void onViewDetachedFromWindow(View v) {
            // Intentionally Empty.
          }
        };
    view.addOnAttachStateChangeListener(expected);

    attachStateTarget.onLoadCleared(/* placeholder= */ null);

    attachAndLayoutView();

    Truth.assertThat(count.get()).isEqualTo(1);
  }

  private static final class AttachStateTarget extends CustomViewTarget<View, Object> {
    AttachStateTarget(View view) {
      super(view);
    }

    @Override
    protected void onResourceCleared(@Nullable Drawable placeholder) {
      // Intentionally Empty.
    }

    @Override
    public void onLoadFailed(@Nullable Drawable errorDrawable) {
      // Intentionally Empty.
    }

    @Override
    public void onResourceReady(
        @NonNull Object resource, @Nullable Transition<? super Object> transition) {
      // Intentionally Empty.
    }
  }

  private static final class TestViewTarget extends CustomViewTarget<View, Object> {

    TestViewTarget(View view) {
      super(view);
    }

    @Override
    protected void onResourceCleared(@Nullable Drawable placeholder) {
      // Intentionally Empty.
    }

    // We're intentionally avoiding the super call.
    @SuppressWarnings("MissingSuperCall")
    @Override
    public void onResourceReady(
        @NonNull Object resource, @Nullable Transition<? super Object> transition) {
      // Avoid calling super.
    }

    // We're intentionally avoiding the super call.
    @SuppressWarnings("MissingSuperCall")
    @Override
    public void onResourceLoading(@Nullable Drawable placeholder) {
      // Avoid calling super.
    }

    // We're intentionally avoiding the super call.
    @SuppressWarnings("MissingSuperCall")
    @Override
    public void onLoadFailed(@Nullable Drawable errorDrawable) {
      // Avoid calling super.
    }
  }
}
