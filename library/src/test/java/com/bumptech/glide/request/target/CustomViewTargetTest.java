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
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.Display;
import android.view.View;
import android.view.View.OnAttachStateChangeListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;
import com.bumptech.glide.request.Request;
import com.bumptech.glide.request.transition.Transition;
import com.bumptech.glide.util.Preconditions;
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
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

/**
 * Test for {@link CustomViewTarget}.
 *
 * <p>TODO: This should really be in the tests subproject, but that causes errors because the R
 * class referenced in {@link CustomViewTarget} can't be found. This should be fixable with some
 * gradle changes, but I've so far failed to figure out the right set of commands.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 19, manifest = "build/intermediates/manifests/full/debug/AndroidManifest.xml")
public class CustomViewTargetTest {
  private ActivityController<Activity> activity;
  private View view;
  private ViewGroup parent;
  private CustomViewTarget<View, Object> target;
  @Mock private SizeReadyCallback cb;
  @Mock private Request request;
  private int sdkVersion;
  private AttachStateTarget attachStateTarget;

  @Before
  public void setUp() {
    sdkVersion = Build.VERSION.SDK_INT;
    MockitoAnnotations.initMocks(this);
    activity = Robolectric.buildActivity(Activity.class).create().start().postCreate(null).resume();
    view = new View(activity.get());
    target = new TestViewTarget(view);
    attachStateTarget = new AttachStateTarget(view);

    LinearLayout linearLayout = new LinearLayout(activity.get());
    View expandView = new View(activity.get());
    LinearLayout.LayoutParams linearLayoutParams =
        new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, /* height= */ 0);
    linearLayoutParams.weight = 1f;
    expandView.setLayoutParams(linearLayoutParams);
    linearLayout.addView(expandView);

    parent = new FrameLayout(activity.get());
    parent.addView(view);
    linearLayout.addView(parent);

    activity.get().setContentView(linearLayout);
  }

  @After
  public void tearDown() {
    setSdkVersionInt(sdkVersion);
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
    // activity.get().setContentView(view);
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

  @Test
  public void getSize_withBothWrapContent_usesDisplayDimens() {
    LayoutParams layoutParams =
        new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    view.setLayoutParams(layoutParams);

    setDisplayDimens(200, 300);

    activity.visible();
    view.layout(0, 0, 0, 0);

    target.getSize(cb);

    verify(cb).onSizeReady(300, 300);
  }

  @Test
  public void getSize_withWrapContentWidthAndValidHeight_usesDisplayDimenAndValidHeight() {
    int height = 100;
    LayoutParams params = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, height);
    view.setLayoutParams(params);

    setDisplayDimens(100, 200);

    activity.visible();
    view.setRight(0);

    target.getSize(cb);

    verify(cb).onSizeReady(200, height);
  }

  @Test
  public void getSize_withWrapContentHeightAndValidWidth_returnsWidthAndDisplayDimen() {
    int width = 100;
    LayoutParams params = new FrameLayout.LayoutParams(width, LayoutParams.WRAP_CONTENT);
    view.setLayoutParams(params);
    setDisplayDimens(200, 100);
    parent.getLayoutParams().height = 200;

    activity.visible();

    target.getSize(cb);

    verify(cb).onSizeReady(width, 200);
  }

  @Test
  public void getSize_withWrapContentWidthAndMatchParentHeight_usesDisplayDimenWidthAndHeight() {
    LayoutParams params =
        new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
    view.setLayoutParams(params);

    setDisplayDimens(500, 600);

    target.getSize(cb);

    verify(cb, never()).onSizeReady(anyInt(), anyInt());

    int height = 32;
    parent.getLayoutParams().height = height;
    activity.visible();

    view.getViewTreeObserver().dispatchOnPreDraw();

    verify(cb).onSizeReady(500, height);
  }

  @Test
  public void getSize_withMatchParentWidthAndWrapContentHeight_usesWidthAndDisplayDimenHeight() {
    LayoutParams params =
        new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
    view.setLayoutParams(params);

    setDisplayDimens(300, 400);

    target.getSize(cb);

    verify(cb, never()).onSizeReady(anyInt(), anyInt());

    int width = 32;
    parent.getLayoutParams().width = 32;
    activity.visible();
    view.getViewTreeObserver().dispatchOnPreDraw();

    verify(cb).onSizeReady(width, 352);
  }

  @Test
  public void testMatchParentWidthAndHeight() {
    LayoutParams params =
        new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    view.setLayoutParams(params);

    target.getSize(cb);

    verify(cb, never()).onSizeReady(anyInt(), anyInt());

    activity.visible();
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
    activity.visible();
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
    activity.visible();
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
    activity.visible();
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
    activity.visible();
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

    activity.visible();
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
    activity.visible();
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
    activity.visible();
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
    activity.visible();
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
    activity.visible();
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
    setSdkVersionInt(18);
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

  private void setDisplayDimens(Integer width, Integer height) {
    WindowManager windowManager =
        (WindowManager)
            ApplicationProvider.getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
    Display display = Preconditions.checkNotNull(windowManager).getDefaultDisplay();
    if (width != null) {
      Shadows.shadowOf(display).setWidth(width);
    }

    if (height != null) {
      Shadows.shadowOf(display).setHeight(height);
    }
  }

  @Test
  public void clearOnDetach_onDetach_withNullRequest_doesNothing() {
    attachStateTarget.clearOnDetach();
    attachStateTarget.setRequest(null);
    activity.visible();
  }

  // This behavior isn't clearly correct, but it doesn't seem like there's any harm to clear an
  // already cleared request, so we might as well avoid the extra check/complexity in the code.
  @Test
  public void clearOnDetach_onDetach_withClearedRequest_clearsRequest() {
    activity.visible();
    attachStateTarget.clearOnDetach();
    attachStateTarget.setRequest(request);
    when(request.isCleared()).thenReturn(true);
    parent.removeView(view);

    verify(request).clear();
  }

  @Test
  public void clearOnDetach_onDetach_withRunningRequest_pausesRequestOnce() {
    activity.visible();
    attachStateTarget.clearOnDetach();
    attachStateTarget.setRequest(request);
    parent.removeView(view);

    verify(request).clear();
  }

  @Test
  public void clearOnDetach_onDetach_afterOnLoadCleared_removesListener() {
    activity.visible();
    attachStateTarget.clearOnDetach();
    attachStateTarget.onLoadCleared(/* placeholder= */ null);
    attachStateTarget.setRequest(request);
    parent.removeView(view);

    verify(request, never()).clear();
  }

  @Test
  public void clearOnDetach_moreThanOnce_registersObserverOnce() {
    activity.visible();
    attachStateTarget.setRequest(request);
    attachStateTarget.clearOnDetach().clearOnDetach();
    parent.removeView(view);

    verify(request).clear();
  }

  @Test
  public void clearOnDetach_onDetach_afterMultipleClearOnDetaches_removesListener() {
    activity.visible();
    attachStateTarget.clearOnDetach().clearOnDetach().clearOnDetach();
    attachStateTarget.onLoadCleared(/* placeholder= */ null);
    attachStateTarget.setRequest(request);
    parent.removeView(view);

    verify(request, never()).clear();
  }

  @Test
  public void clearOnDetach_onDetach_afterLoadCleared_clearsRequest() {
    activity.visible();
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
    activity.visible();
  }

  @Test
  public void clearOnDetach_onAttach_withRunningRequest_doesNotBeginRequest() {
    attachStateTarget.clearOnDetach();
    attachStateTarget.setRequest(request);
    when(request.isCleared()).thenReturn(false);
    activity.visible();

    verify(request, never()).begin();
  }

  @Test
  public void clearOnDetach_onAttach_withClearedRequest_beginsRequest() {
    attachStateTarget.clearOnDetach();
    attachStateTarget.setRequest(request);
    when(request.isCleared()).thenReturn(true);
    activity.visible();

    verify(request).begin();
  }

  @Test
  public void clearOnDetach_afterLoadClearedAndRestarted_onAttach_beginsRequest() {
    attachStateTarget.clearOnDetach();
    attachStateTarget.setRequest(request);
    when(request.isCleared()).thenReturn(true);
    attachStateTarget.onLoadCleared(/* placeholder= */ null);
    attachStateTarget.onLoadStarted(/* placeholder= */ null);
    activity.visible();

    verify(request).begin();
  }

  @Test
  public void clearOnDetach_onAttach_afterLoadCleared_doesNotBeingRequest() {
    attachStateTarget.clearOnDetach();
    attachStateTarget.setRequest(request);
    when(request.isCleared()).thenReturn(true);
    attachStateTarget.onLoadCleared(/* placeholder= */ null);
    activity.visible();

    verify(request, never()).begin();
  }

  @Test
  public void onLoadStarted_withoutClearOnDetach_doesNotAddListener() {
    activity.visible();
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

    activity.visible();

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

  private static void setSdkVersionInt(int version) {
    ReflectionHelpers.setStaticField(Build.VERSION.class, "SDK_INT", version);
  }
}
