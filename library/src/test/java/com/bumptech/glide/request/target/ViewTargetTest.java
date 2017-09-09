package com.bumptech.glide.request.target;

import static android.view.ViewGroup.LayoutParams;
import static android.view.ViewTreeObserver.OnPreDrawListener;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;
import android.view.ViewTreeObserver;
import com.bumptech.glide.request.Request;
import com.bumptech.glide.request.transition.Transition;
import com.bumptech.glide.tests.Util;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
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
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowView;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 19, shadows = { ViewTargetTest.SizedShadowView.class,
    ViewTargetTest.PreDrawShadowViewTreeObserver.class })
public class ViewTargetTest {
  private View view;
  private ViewTarget<View, Object> target;
  private SizedShadowView shadowView;
  private PreDrawShadowViewTreeObserver shadowObserver;
  @Mock private SizeReadyCallback cb;
  @Mock private Request request;
  private int sdkVersion;

  @Before
  public void setUp() {
    sdkVersion = Build.VERSION.SDK_INT;
    MockitoAnnotations.initMocks(this);
    view = new View(RuntimeEnvironment.application);
    target = new TestViewTarget(view);

    shadowView = Shadow.extract(view);
    shadowObserver = Shadow.extract(view.getViewTreeObserver());
  }

  @After
  public void tearDown() {
    Util.setSdkVersionInt(sdkVersion);
  }

  @Test
  public void testReturnsWrappedView() {
    assertEquals(view, target.getView());
  }

  @Test
  public void testReturnsNullFromGetRequestIfNoRequestSet() {
    assertNull(target.getRequest());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testThrowsIfViewTagIsNotRequestObject() {
    view.setTag(new Object());
    target.getRequest();
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
  public void getSize_withValidDimens_noLayoutRequested_callsSizeReady() {
    int dimens = 333;
    shadowView
        .setWidth(dimens)
        .setHeight(dimens);

    target.getSize(cb);

    verify(cb).onSizeReady(eq(dimens), eq(dimens));
  }

  @Test
  public void getSize_withValidDimens_layoutRequested_doesNotCallSizeReady() {
    int dimens = 333;
    shadowView
        .setWidth(dimens)
        .setHeight(dimens);
    view.requestLayout();

    target.getSize(cb);

    verify(cb, never()).onSizeReady(anyInt(), anyInt());
  }

  @Test
  public void getSize_withFixedLayoutParams_callsSizeReady() {
    int dimens = 444;
    LayoutParams layoutParams = new LayoutParams(dimens, dimens);
    view.setLayoutParams(layoutParams);
    view.requestLayout();

    target.getSize(cb);

    verify(cb).onSizeReady(eq(dimens), eq(dimens));
  }

  @Test
  public void getSize_withFixedWidthSetHeight_noLayoutRequested_callsSizeReady() {
    LayoutParams layoutParams = new LayoutParams(400 /*width*/, 0 /*height*/);
    shadowView.setHeight(200);
    view.setLayoutParams(layoutParams);

    target.getSize(cb);

    verify(cb).onSizeReady(eq(400), eq(200));
  }

  @Test
  public void getSize_withFixedWidthSetHeight_layoutRequested_callsSizeReady() {
    LayoutParams layoutParams = new LayoutParams(400 /*width*/, 0 /*height*/);
    shadowView.setHeight(200);
    view.setLayoutParams(layoutParams);
    view.requestLayout();

    target.getSize(cb);

    verify(cb, never()).onSizeReady(anyInt(), anyInt());
  }


  @Test
  public void getSize_withFixedHeightSetWidth_noLayoutRequested_callsSizeReady() {
    LayoutParams layoutParams = new LayoutParams(0 /*width*/, 400 /*height*/);
    shadowView.setWidth(200);
    view.setLayoutParams(layoutParams);

    target.getSize(cb);

    verify(cb).onSizeReady(eq(200), eq(400));
  }

  @Test
  public void getSize_withFixedHeightSetWidth_layoutRequested_callsSizeReady() {
    LayoutParams layoutParams = new LayoutParams(0 /*width*/, 400 /*height*/);
    shadowView.setWidth(200);
    view.setLayoutParams(layoutParams);
    view.requestLayout();

    target.getSize(cb);

    verify(cb, never()).onSizeReady(anyInt(), anyInt());
  }

  @Test
  public void getSize_withBothWrapContent_isValid_andReturnsSizeOriginal() {
    LayoutParams layoutParams =
        new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    view.setLayoutParams(layoutParams);
    view.requestLayout();

    target.getSize(cb);

    verify(cb).onSizeReady(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL);
  }

  @Test
  public void getSize_withWrapContentWidthAndValidHeight_isValid_andUsesSizeOriginalWidth() {
    int height = 100;
    LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, height);
    view.setLayoutParams(params);
    view.requestLayout();

    target.getSize(cb);

    verify(cb).onSizeReady(Target.SIZE_ORIGINAL, height);
  }

  @Test
  public void getSize_withWrapContentHeightAndValidWidth_isValid_andUsesSizeOriginalHeight() {
    int width = 100;
    LayoutParams params = new LayoutParams(width, LayoutParams.WRAP_CONTENT);
    view.setLayoutParams(params);
    view.requestLayout();

    target.getSize(cb);

    verify(cb).onSizeReady(width, Target.SIZE_ORIGINAL);
  }

  @Test
  public void getSize_withWrapContentHeightSetWidth_noLayoutRequested_callsSizeReady() {
    int width = 100;
    LayoutParams params = new LayoutParams(0, LayoutParams.WRAP_CONTENT);
    view.setLayoutParams(params);
    shadowView.setWidth(width);

    target.getSize(cb);

    verify(cb).onSizeReady(width, Target.SIZE_ORIGINAL);
  }

  @Test
  public void getSize_withWrapContentHeightSetWidth_previousHeight_usesSizeOriginal() {
    int width = 100;
    int oldHeight = 500;
    LayoutParams params = new LayoutParams(0, LayoutParams.WRAP_CONTENT);
    view.setLayoutParams(params);
    shadowView
        .setWidth(width)
        .setHeight(oldHeight);

    target.getSize(cb);

    verify(cb).onSizeReady(width, Target.SIZE_ORIGINAL);

  }

  @Test
  public void getSize_withWrapContentHeightViewWidth_layoutRequested_doesNotCallSizeReady() {
    int width = 100;
    LayoutParams params = new LayoutParams(0, LayoutParams.WRAP_CONTENT);
    view.setLayoutParams(params);
    shadowView.setWidth(width);
    view.requestLayout();

    target.getSize(cb);

    verify(cb, never()).onSizeReady(anyInt(), anyInt());
  }

  @Test
  public void getSize_withWrapContentWidthAndMatchParentHeight_callsSizeReadyOnPreDraw() {
    LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
    view.setLayoutParams(params);

    target.getSize(cb);

    verify(cb, never()).onSizeReady(anyInt(), anyInt());

    int height = 32;
    shadowView
        .setHeight(height);
    view.requestLayout();

    shadowObserver.fireOnPreDrawListeners();

    verify(cb).onSizeReady(Target.SIZE_ORIGINAL, height);
  }

  @Test
  public void getSize_withWrapContentWidthMatchParentHeightAndSetHeight_noLayoutRequested_calls() {
    LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
    view.setLayoutParams(params);
    shadowView.setHeight(200);

    target.getSize(cb);

    verify(cb).onSizeReady(Target.SIZE_ORIGINAL, 200);
  }

  @Test
  public void getSize_withWrapContentWidthMatchParentHeightAndSetHeight_layoutRequested_calls() {
    LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
    view.setLayoutParams(params);
    shadowView.setHeight(200);
    view.requestLayout();

    target.getSize(cb);

    verify(cb).onSizeReady(Target.SIZE_ORIGINAL, 200);
  }

  @Test
  public void getSize_withMatchParentWidthAndWrapContentHeight_callsSizeReadyOnPreDraw() {
    LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
    view.setLayoutParams(params);

    target.getSize(cb);

    verify(cb, never()).onSizeReady(anyInt(), anyInt());

    int width = 32;
    shadowView
        .setWidth(width);
    view.requestLayout();
    shadowObserver.fireOnPreDrawListeners();

    verify(cb).onSizeReady(width, Target.SIZE_ORIGINAL);
  }


  @Test
  public void getSize_withMatchParentWidthWrapContentHeightAndSetHeight_noLayoutRequested_calls() {
    LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
    view.setLayoutParams(params);
    shadowView.setWidth(200);

    target.getSize(cb);

    verify(cb).onSizeReady(200, Target.SIZE_ORIGINAL);
  }

  @Test
  public void getSize_withMatchParentWidthWrapContentHeightAndSetHeight_layoutRequested_calls() {
    LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
    view.setLayoutParams(params);
    shadowView.setWidth(200);
    view.requestLayout();

    target.getSize(cb);

    verify(cb).onSizeReady(200, Target.SIZE_ORIGINAL);
  }

  @Test
  public void getSize_withMatchParentWidthAndHeight_validDimens_layoutRequested_callsSizeReady() {
    LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    view.setLayoutParams(params);

    target.getSize(cb);

    verify(cb, never()).onSizeReady(anyInt(), anyInt());

    int width = 32;
    int height = 45;
    shadowView
        .setWidth(width)
        .setHeight(height)
        .requestLayout();
    shadowObserver.fireOnPreDrawListeners();

    verify(cb).onSizeReady(eq(width), eq(height));
  }

  @Test
  public void getSize_onPreDraw_withValidWidthAndHeight_noLayoutRequested_callsSizeReady() {
    target.getSize(cb);

    int width = 12;
    int height = 32;
    shadowView
        .setWidth(width)
        .setHeight(height);
    shadowObserver.fireOnPreDrawListeners();

    verify(cb).onSizeReady(eq(width), eq(height));
  }

  @Test
  public void getSize_onPreDraw_withValidWidthAndHeight_layoutRequested_doesNotCallSizeReady() {
    target.getSize(cb);

    int width = 12;
    int height = 32;
    shadowView
        .setWidth(width)
        .setHeight(height);
    view.requestLayout();
    shadowObserver.fireOnPreDrawListeners();

    verify(cb, never()).onSizeReady(anyInt(), anyInt());
  }

  @Test
  public void getSize_withWrapContentSize_callsSizeReadyWithSizeOriginal() {
    view.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
    target.getSize(cb);

    verify(cb).onSizeReady(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL);
  }

  @Test
  public void getSize_withValidViewDimensions_andWrapContent_callsSizeReadyWithSizeOriginal() {
    view.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
    shadowView
        .setWidth(100)
        .setHeight(100);
    target.getSize(cb);

    verify(cb).onSizeReady(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL);
  }

  @Test
  public void getSize_withValidViewDimensions_andFixedParams_callsSizeReadyWithParams() {
    view.setLayoutParams(new LayoutParams(100, 100));
    shadowView
        .setWidth(50)
        .setHeight(50);
    target.getSize(cb);

    verify(cb).onSizeReady(100, 100);
  }

  @Test
  public void getSize_withValidViewDimensions_invalidParams_callsSizeReadyWithViewDimensions() {
    view.setLayoutParams(new LayoutParams(0, 0));
    shadowView
        .setWidth(100)
        .setHeight(100);
    target.getSize(cb);

    verify(cb).onSizeReady(100, 100);
  }

  @Test
  public void testSizeCallbacksAreCalledInOrderPreDraw() {
    SizeReadyCallback[] cbs = new SizeReadyCallback[25];
    for (int i = 0; i < cbs.length; i++) {
      cbs[i] = mock(SizeReadyCallback.class);
      target.getSize(cbs[i]);
    }

    int width = 100, height = 111;
    shadowView
        .setWidth(width)
        .setHeight(height);
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
    shadowObserver.fireOnPreDrawListeners();

    verify(cb).onSizeReady(eq(width), eq(height));
  }

  @Test
  public void testCallbackIsNotCalledTwiceIfPreDrawFiresTwice() {
    target.getSize(cb);

    LayoutParams layoutParams = new LayoutParams(1234, 4123);
    view.setLayoutParams(layoutParams);
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
    view.layout(0, 0, 100, 100);

    target.getSize(cb);

    verify(cb).onSizeReady(50, 50);
  }

  @Test
  public void getSize_withValidWidthAndHeight_notLaidOut_notLayoutRequested_callsSizeReady() {
    view.setLayoutParams(new LayoutParams(0, 0));
    shadowView
        .setWidth(100)
        .setHeight(100);
    target.getSize(cb);

    verify(cb).onSizeReady(100, 100);
  }

  @Test
  public void getSize_withLayoutParams_notLaidOut_notLayoutRequested_callsSizeReady() {
    view.setLayoutParams(new LayoutParams(10, 10));
    target.getSize(cb);

    verify(cb, times(1)).onSizeReady(anyInt(), anyInt());
  }

  @Test
  public void getSize_withLayoutParams_emptyParams_notLaidOutOrLayoutRequested_callsSizeReady() {
   view
        .setLayoutParams(new LayoutParams(0, 0));
    shadowView
        .setWidth(100)
        .setHeight(100);
    target.getSize(cb);

    verify(cb).onSizeReady(100, 100);
  }

  @Test
  public void getSize_withValidWidthAndHeight_preV19_layoutRequested_doesNotCallSizeReady() {
    Util.setSdkVersionInt(18);
    shadowView
        .setWidth(100)
        .setHeight(100)
        .requestLayout();

    target.getSize(cb);

    verify(cb, never()).onSizeReady(anyInt(), anyInt());
  }

  @Test
  public void getSize_withWidthAndHeightEqualToPadding_doesNotCallSizeReady() {
    shadowView
        .setWidth(100)
        .setHeight(100);

    view.setPadding(50, 50, 50, 50);

    target.getSize(cb);

    verify(cb, never()).onSizeReady(anyInt(), anyInt());
  }

  @Implements(ViewTreeObserver.class)
  public static class PreDrawShadowViewTreeObserver {
    private CopyOnWriteArrayList<OnPreDrawListener> preDrawListeners = new CopyOnWriteArrayList<>();
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

    @SuppressWarnings("WeakerAccess")
    @Implementation
    public boolean isAlive() {
      return isAlive;
    }

    private void checkIsAlive() {
      if (!isAlive()) {
        throw new IllegalStateException("ViewTreeObserver is not alive!");
      }
    }

    void setIsAlive(boolean isAlive) {
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

  @Implements(View.class)
  public static class SizedShadowView extends ShadowView {

    private int width;
    private int height;
    private LayoutParams params;

    public SizedShadowView setWidth(int width) {
      this.width = width;
      return this;
    }

    public SizedShadowView setHeight(int height) {
      this.height = height;
      return this;
    }

    // Implemented because get/setLayoutParams is not implemented by ShadowView.
    @Implementation
    @SuppressWarnings("unused")
    public void setLayoutParams(LayoutParams params) {
      this.params = params;
    }

    // Implemented because get/setLayoutParams is not implemented by ShadowView.
    @Implementation
    @SuppressWarnings("unused")
    public LayoutParams getLayoutParams() {
      return params;
    }

    @Implementation
    public int getWidth() {
      return width;
    }

    @Implementation
    public int getHeight() {
      return height;
    }
  }


  private static class TestViewTarget extends ViewTarget<View, Object> {

    TestViewTarget(View view) {
      super(view);
    }

    @Override
    public void onLoadStarted(Drawable placeholder) {

    }

    @Override
    public void onLoadFailed(Drawable errorDrawable) {

    }

    @Override
    public void onResourceReady(Object resource, Transition<? super Object> transition) {

    }

    @Override
    public void onLoadCleared(Drawable placeholder) {

    }
  }
}
