package com.bumptech.glide.request.target;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.Display;
import android.view.View;
import android.view.View.OnAttachStateChangeListener;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnPreDrawListener;
import android.view.WindowManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;
import com.bumptech.glide.request.Request;
import com.bumptech.glide.request.transition.Transition;
import com.bumptech.glide.tests.Util;
import com.bumptech.glide.util.Preconditions;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowView;

@RunWith(RobolectricTestRunner.class)
@Config(
    sdk = 19,
    shadows = {
      ViewTargetTest.SizedShadowView.class,
      ViewTargetTest.PreDrawShadowViewTreeObserver.class
    })
public class ViewTargetTest {
  private View view;
  private ViewTarget<View, Object> target;
  private SizedShadowView shadowView;
  private PreDrawShadowViewTreeObserver shadowObserver;
  @Mock private SizeReadyCallback cb;
  @Mock private Request request;
  private int sdkVersion;
  private AttachStateTarget attachStateTarget;

  @Before
  public void setUp() {
    sdkVersion = Build.VERSION.SDK_INT;
    MockitoAnnotations.initMocks(this);
    view = new View(ApplicationProvider.getApplicationContext());
    target = new TestViewTarget(view);
    attachStateTarget = new AttachStateTarget(view);

    shadowView = Shadow.extract(view);
    shadowObserver = Shadow.extract(view.getViewTreeObserver());
  }

  @After
  public void tearDown() {
    Util.setSdkVersionInt(sdkVersion);
    ViewTarget.SizeDeterminer.maxDisplayLength = null;
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

    ViewTarget<View, Object> second = new TestViewTarget(view);

    assertEquals(request, second.getRequest());
  }

  @Test
  public void testSizeCallbackIsCalledSynchronouslyIfViewSizeSet() {
    int dimens = 333;
    shadowView.setWidth(dimens).setHeight(dimens).setIsLaidOut(true);

    target.getSize(cb);

    verify(cb).onSizeReady(eq(dimens), eq(dimens));
  }

  @Test
  public void testSizeCallbackIsCalledSynchronouslyIfLayoutParamsConcreteSizeSet() {
    int dimens = 444;
    LayoutParams layoutParams = new LayoutParams(dimens, dimens);
    view.setLayoutParams(layoutParams);
    shadowView.setIsLaidOut(true);

    target.getSize(cb);

    verify(cb).onSizeReady(eq(dimens), eq(dimens));
  }

  @Test
  public void getSize_withBothWrapContent_usesDisplayDimens() {
    LayoutParams layoutParams =
        new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    view.setLayoutParams(layoutParams);
    shadowView.setIsLaidOut(true);

    setDisplayDimens(200, 300);

    target.getSize(cb);

    verify(cb).onSizeReady(300, 300);
  }

  @Test
  public void getSize_withWrapContentWidthAndValidHeight_usesDisplayDimenAndValidHeight() {
    int height = 100;
    LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, height);
    view.setLayoutParams(params);
    shadowView.setIsLaidOut(true);

    setDisplayDimens(100, 200);

    target.getSize(cb);

    verify(cb).onSizeReady(200, height);
  }

  @Test
  public void getSize_withWrapContentHeightAndValidWidth_returnsWidthAndDisplayDimen() {
    int width = 100;
    LayoutParams params = new LayoutParams(width, LayoutParams.WRAP_CONTENT);
    view.setLayoutParams(params);
    shadowView.setIsLaidOut(true);

    setDisplayDimens(200, 100);

    target.getSize(cb);

    verify(cb).onSizeReady(width, 200);
  }

  @Test
  public void getSize_withWrapContentWidthAndMatchParentHeight_usesDisplayDimenWidthAndHeight() {
    LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
    view.setLayoutParams(params);

    setDisplayDimens(500, 600);

    target.getSize(cb);

    verify(cb, never()).onSizeReady(anyInt(), anyInt());

    int height = 32;
    shadowView.setHeight(height).setIsLaidOut(true);

    shadowObserver.fireOnPreDrawListeners();

    verify(cb).onSizeReady(600, height);
  }

  @Test
  public void getSize_withMatchParentWidthAndWrapContentHeight_usesWidthAndDisplayDimenHeight() {
    LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
    view.setLayoutParams(params);

    setDisplayDimens(300, 400);

    target.getSize(cb);

    verify(cb, never()).onSizeReady(anyInt(), anyInt());

    int width = 32;
    shadowView.setWidth(width).setIsLaidOut(true);
    shadowObserver.fireOnPreDrawListeners();

    verify(cb).onSizeReady(width, 400);
  }

  @Test
  public void testMatchParentWidthAndHeight() {
    LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    view.setLayoutParams(params);

    target.getSize(cb);

    verify(cb, never()).onSizeReady(anyInt(), anyInt());

    int width = 32;
    int height = 45;
    shadowView.setWidth(width).setHeight(height).setIsLaidOut(true);
    shadowObserver.fireOnPreDrawListeners();

    verify(cb).onSizeReady(eq(width), eq(height));
  }

  @Test
  public void testSizeCallbackIsCalledPreDrawIfNoDimensAndNoLayoutParams() {
    target.getSize(cb);

    int width = 12;
    int height = 32;
    shadowView.setWidth(width).setHeight(height).setIsLaidOut(true);
    shadowObserver.fireOnPreDrawListeners();

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
    shadowView.setWidth(width).setHeight(height).setIsLaidOut(true);
    shadowObserver.fireOnPreDrawListeners();

    InOrder order = inOrder((Object[]) cbs);
    for (SizeReadyCallback cb : cbs) {
      order.verify(cb).onSizeReady(eq(width), eq(height));
    }
  }

  @Test
  public void testDoesNotNotifyCallbackTwiceIfAddedTwice() {
    target.getSize(cb);
    target.getSize(cb);

    view.setLayoutParams(new LayoutParams(100, 100));
    shadowView.setIsLaidOut(true);
    shadowObserver.fireOnPreDrawListeners();

    verify(cb, times(1)).onSizeReady(anyInt(), anyInt());
  }

  @Test
  public void testDoesNotAddMultipleListenersIfMultipleCallbacksAreAdded() {
    SizeReadyCallback cb1 = mock(SizeReadyCallback.class);
    SizeReadyCallback cb2 = mock(SizeReadyCallback.class);
    target.getSize(cb1);
    target.getSize(cb2);
    assertThat(shadowObserver.getPreDrawListeners()).hasSize(1);
  }

  @Test
  public void testDoesAddSecondListenerIfFirstListenerIsRemovedBeforeSecondRequest() {
    SizeReadyCallback cb1 = mock(SizeReadyCallback.class);
    target.getSize(cb1);

    view.setLayoutParams(new LayoutParams(100, 100));
    shadowView.setIsLaidOut(true);
    shadowObserver.fireOnPreDrawListeners();

    assertThat(shadowObserver.getPreDrawListeners()).hasSize(0);

    SizeReadyCallback cb2 = mock(SizeReadyCallback.class);
    view.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    target.getSize(cb2);

    view.setLayoutParams(new LayoutParams(100, 100));
    shadowObserver.fireOnPreDrawListeners();

    verify(cb2).onSizeReady(anyInt(), anyInt());
  }

  @Test
  public void testSizeCallbackIsNotCalledPreDrawIfNoDimensSetOnPreDraw() {
    target.getSize(cb);
    shadowObserver.fireOnPreDrawListeners();

    verify(cb, never()).onSizeReady(anyInt(), anyInt());
    assertThat(shadowObserver.getPreDrawListeners()).hasSize(1);
  }

  @Test
  public void testSizeCallbackIsCalledPreDrawIfNoDimensAndNoLayoutParamsButLayoutParamsSetLater() {
    target.getSize(cb);

    int width = 689;
    int height = 354;
    LayoutParams layoutParams = new LayoutParams(width, height);
    view.setLayoutParams(layoutParams);
    shadowView.setIsLaidOut(true);
    shadowObserver.fireOnPreDrawListeners();

    verify(cb).onSizeReady(eq(width), eq(height));
  }

  @Test
  public void testCallbackIsNotCalledTwiceIfPreDrawFiresTwice() {
    target.getSize(cb);

    LayoutParams layoutParams = new LayoutParams(1234, 4123);
    view.setLayoutParams(layoutParams);
    shadowView.setIsLaidOut(true);
    shadowObserver.fireOnPreDrawListeners();
    shadowObserver.fireOnPreDrawListeners();

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
    LayoutParams layoutParams = new LayoutParams(width, height);
    view.setLayoutParams(layoutParams);
    shadowView.setIsLaidOut(true);
    shadowObserver.fireOnPreDrawListeners();
    shadowObserver.fireOnPreDrawListeners();

    verify(firstCb, times(1)).onSizeReady(eq(width), eq(height));
    verify(secondCb, times(1)).onSizeReady(eq(width), eq(height));
  }

  @Test
  public void testDoesNotThrowOnPreDrawIfViewTreeObserverIsDead() {
    target.getSize(cb);

    int width = 1;
    int height = 2;
    LayoutParams layoutParams = new LayoutParams(width, height);
    view.setLayoutParams(layoutParams);
    shadowView.setIsLaidOut(true);
    shadowObserver.setIsAlive(false);
    shadowObserver.fireOnPreDrawListeners();

    verify(cb).onSizeReady(eq(width), eq(height));
  }

  @Test(expected = NullPointerException.class)
  public void testThrowsIfGivenNullView() {
    new TestViewTarget(null);
  }

  @Test
  public void testDecreasesDimensionsByViewPadding() {
    view.setLayoutParams(new LayoutParams(100, 100));
    view.setPadding(25, 25, 25, 25);
    shadowView.setIsLaidOut(true);

    target.getSize(cb);

    verify(cb).onSizeReady(50, 50);
  }

  @Test
  public void getSize_withValidWidthAndHeight_notLaidOut_notLayoutRequested_callsSizeReady() {
    shadowView.setWidth(100).setHeight(100).setIsLaidOut(false);
    target.getSize(cb);

    verify(cb).onSizeReady(100, 100);
  }

  @Test
  public void getSize_withLayoutParams_notLaidOut_doesCallSizeReady() {
    shadowView
        .setLayoutParams(new LayoutParams(10, 10))
        .setWidth(100)
        .setHeight(100)
        .setIsLaidOut(false);
    target.getSize(cb);

    verify(cb, times(1)).onSizeReady(anyInt(), anyInt());
  }

  @Test
  public void getSize_withLayoutParams_emptyParams_notLaidOutOrLayoutRequested_callsSizeReady() {
    shadowView
        .setLayoutParams(new LayoutParams(0, 0))
        .setWidth(100)
        .setHeight(100)
        .setIsLaidOut(false);
    target.getSize(cb);

    verify(cb).onSizeReady(100, 100);
  }

  @Test
  public void getSize_withValidWidthAndHeight_preV19_layoutRequested_callsSizeReady() {
    Util.setSdkVersionInt(18);
    shadowView.setWidth(100).setHeight(100).requestLayout();

    target.getSize(cb);

    verify(cb).onSizeReady(100, 100);
  }

  @Test
  public void getSize_withWidthAndHeightEqualToPadding_doesNotCallSizeReady() {
    shadowView.setWidth(100).setHeight(100).setIsLaidOut(true);

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
    shadowView.callOnAttachedToWindow();
  }

  // This behavior isn't clearly correct, but it doesn't seem like there's any harm to clear an
  // already cleared request, so we might as well avoid the extra check/complexity in the code.
  @Test
  public void clearOnDetach_onDetach_withClearedRequest_clearsRequest() {
    attachStateTarget.clearOnDetach();
    attachStateTarget.setRequest(request);
    when(request.isCleared()).thenReturn(true);
    shadowView.callOnDetachedFromWindow();

    verify(request).clear();
  }

  @Test
  public void clearOnDetach_onDetach_withRunningRequest_pausesRequestOnce() {
    attachStateTarget.clearOnDetach();
    attachStateTarget.setRequest(request);
    shadowView.callOnDetachedFromWindow();

    verify(request).clear();
  }

  @Test
  public void clearOnDetach_onDetach_afterOnLoadCleared_removesListener() {
    attachStateTarget.clearOnDetach();
    attachStateTarget.onLoadCleared(/* placeholder= */ null);
    attachStateTarget.setRequest(request);
    shadowView.callOnDetachedFromWindow();

    verify(request, never()).clear();
  }

  @Test
  public void clearOnDetach_moreThanOnce_registersObserverOnce() {
    attachStateTarget.clearOnDetach().clearOnDetach();

    assertThat(shadowView.attachStateListeners).hasSize(1);
  }

  @Test
  public void clearOnDetach_onDetach_afterMultipleClearOnDetaches_removesListener() {
    attachStateTarget.clearOnDetach().clearOnDetach().clearOnDetach();
    attachStateTarget.onLoadCleared(/* placeholder= */ null);
    attachStateTarget.setRequest(request);
    shadowView.callOnDetachedFromWindow();

    verify(request, never()).clear();
  }

  // This behavior isn't clearly correct, but it doesn't seem like there's any harm to clear an
  // already cleared request, so we might as well avoid the extra check/complexity in the code.
  @Test
  public void clearOnDetach_onDetach_afterLoadCleared_clearsRequest() {
    attachStateTarget.clearOnDetach();
    attachStateTarget.setRequest(request);
    when(request.isCleared()).thenReturn(true);
    shadowView.callOnDetachedFromWindow();

    verify(request).clear();
  }

  @Test
  public void clearOnDetach_onAttach_withNullRequest_doesNothing() {
    attachStateTarget.clearOnDetach();
    attachStateTarget.setRequest(null);
    shadowView.callOnAttachedToWindow();
  }

  @Test
  public void clearOnDetach_onAttach_withRunningRequest_doesNotBeginRequest() {
    attachStateTarget.clearOnDetach();
    attachStateTarget.setRequest(request);
    when(request.isCleared()).thenReturn(false);
    shadowView.callOnAttachedToWindow();

    verify(request, never()).begin();
  }

  @Test
  public void clearOnDetach_onAttach_withClearedRequest_beginsRequest() {
    attachStateTarget.clearOnDetach();
    attachStateTarget.setRequest(request);
    when(request.isCleared()).thenReturn(true);
    shadowView.callOnAttachedToWindow();

    verify(request).begin();
  }

  @Test
  public void clearOnDetach_afterLoadClearedAndRestarted_onAttach_beingsRequest() {
    attachStateTarget.clearOnDetach();
    attachStateTarget.setRequest(request);
    when(request.isCleared()).thenReturn(true);
    attachStateTarget.onLoadCleared(/* placeholder= */ null);
    attachStateTarget.onLoadStarted(/* placeholder= */ null);
    shadowView.callOnAttachedToWindow();

    verify(request).begin();
  }

  @Test
  public void clearOnDetach_onAttach_afterLoadCleared_doesNotBeingRequest() {
    attachStateTarget.clearOnDetach();
    attachStateTarget.setRequest(request);
    when(request.isCleared()).thenReturn(true);
    attachStateTarget.onLoadCleared(/* placeholder= */ null);
    shadowView.callOnAttachedToWindow();

    verify(request, never()).begin();
  }

  @Test
  public void onLoadStarted_withoutClearOnDetach_doesNotAddListener() {
    attachStateTarget.onLoadStarted(/* placeholder= */ null);

    assertThat(shadowView.attachStateListeners).isEmpty();
  }

  // containsExactly does not need its result checked.
  @SuppressWarnings("ResultOfMethodCallIgnored")
  @Test
  public void onLoadCleared_withoutClearOnDetach_doesNotRemoveListeners() {
    OnAttachStateChangeListener expected =
        new OnAttachStateChangeListener() {
          @Override
          public void onViewAttachedToWindow(View v) {}

          @Override
          public void onViewDetachedFromWindow(View v) {}
        };
    shadowView.addOnAttachStateChangeListener(expected);

    attachStateTarget.onLoadCleared(/* placeholder= */ null);

    assertThat(shadowView.attachStateListeners).containsExactly(expected);
  }

  @Implements(ViewTreeObserver.class)
  public static final class PreDrawShadowViewTreeObserver {
    private final CopyOnWriteArrayList<OnPreDrawListener> preDrawListeners =
        new CopyOnWriteArrayList<>();
    private boolean isAlive = true;

    @SuppressWarnings("unused")
    @Implementation
    public void addOnPreDrawListener(OnPreDrawListener listener) {
      checkIsAlive();
      preDrawListeners.add(listener);
    }

    @SuppressWarnings("unused")
    @Implementation
    public void removeOnPreDrawListener(OnPreDrawListener listener) {
      checkIsAlive();
      preDrawListeners.remove(listener);
    }

    @Implementation
    @SuppressWarnings("WeakerAccess")
    public boolean isAlive() {
      return isAlive;
    }

    private void checkIsAlive() {
      if (!isAlive()) {
        throw new IllegalStateException("ViewTreeObserver is not alive!");
      }
    }

    void setIsAlive(@SuppressWarnings("SameParameterValue") boolean isAlive) {
      this.isAlive = isAlive;
    }

    void fireOnPreDrawListeners() {
      for (OnPreDrawListener listener : preDrawListeners) {
        listener.onPreDraw();
      }
    }

    List<OnPreDrawListener> getPreDrawListeners() {
      return preDrawListeners;
    }
  }

  // Shadows require stronger access and unused values.
  @SuppressWarnings({"UnusedReturnValue", "WeakerAccess", "unused"})
  @Implements(View.class)
  public static final class SizedShadowView extends ShadowView {
    @RealObject private View view;
    private int width;
    private int height;
    private LayoutParams layoutParams;
    private boolean isLaidOut;
    private boolean isLayoutRequested;
    final Set<OnAttachStateChangeListener> attachStateListeners = new HashSet<>();

    public SizedShadowView setWidth(int width) {
      this.width = width;
      return this;
    }

    public SizedShadowView setHeight(int height) {
      this.height = height;
      return this;
    }

    @Implementation
    public void addOnAttachStateChangeListener(OnAttachStateChangeListener listener) {
      attachStateListeners.add(listener);
    }

    @Implementation
    public void removeOnAttachStateChangeListener(OnAttachStateChangeListener listener) {
      attachStateListeners.remove(listener);
    }

    @Implementation
    public void onAttachedToWindow() {
      for (OnAttachStateChangeListener listener : attachStateListeners) {
        listener.onViewAttachedToWindow(view);
      }
    }

    @Implementation
    public void onDetachedFromWindow() {
      for (OnAttachStateChangeListener listener : attachStateListeners) {
        listener.onViewDetachedFromWindow(view);
      }
    }

    @Override
    public void callOnAttachedToWindow() {
      super.callOnAttachedToWindow();
    }

    @Override
    public void callOnDetachedFromWindow() {
      super.callOnDetachedFromWindow();
    }

    @Implementation
    public SizedShadowView setLayoutParams(LayoutParams layoutParams) {
      this.layoutParams = layoutParams;
      return this;
    }

    @Implementation
    public SizedShadowView setIsLaidOut(boolean isLaidOut) {
      this.isLaidOut = isLaidOut;
      return this;
    }

    @Implementation
    @Override
    public void requestLayout() {
      isLayoutRequested = true;
    }

    @Implementation
    public int getWidth() {
      return width;
    }

    @Implementation
    public int getHeight() {
      return height;
    }

    @Implementation
    public boolean isLaidOut() {
      return isLaidOut;
    }

    @Implementation
    public boolean isLayoutRequested() {
      return isLayoutRequested;
    }

    @Implementation
    public LayoutParams getLayoutParams() {
      return layoutParams;
    }
  }

  private static final class AttachStateTarget extends ViewTarget<View, Object> {
    AttachStateTarget(View view) {
      super(view);
    }

    @Override
    public void onResourceReady(
        @NonNull Object resource, @Nullable Transition<? super Object> transition) {}
  }

  private static final class TestViewTarget extends ViewTarget<View, Object> {

    TestViewTarget(View view) {
      super(view);
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
    public void onLoadCleared(@Nullable Drawable placeholder) {
      // Avoid calling super.
    }

    // We're intentionally avoiding the super call.
    @SuppressWarnings("MissingSuperCall")
    @Override
    public void onLoadStarted(@Nullable Drawable placeholder) {
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
