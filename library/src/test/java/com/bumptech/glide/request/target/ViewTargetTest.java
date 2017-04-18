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
  public void testSizeCallbackIsCalledSynchronouslyIfViewSizeSet() {
    int dimens = 333;
    shadowView
        .setWidth(dimens)
        .setHeight(dimens)
        .setIsLaidOut(true);

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
  public void getSize_withBothWrapContent_returnsSizeOriginal() {
    LayoutParams layoutParams =
        new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    view.setLayoutParams(layoutParams);
    shadowView.setIsLaidOut(true);

    target.getSize(cb);

    verify(cb).onSizeReady(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL);
  }

  @Test
  public void getSize_withWrapContentWidthAndValidHeight_usesSizeOriginalWidthValidHeight() {
    int height = 100;
    LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, height);
    view.setLayoutParams(params);
    shadowView.setIsLaidOut(true);

    target.getSize(cb);

    verify(cb).onSizeReady(Target.SIZE_ORIGINAL, height);
  }

  @Test
  public void getSize_withWrapContentHeightAndValidWidth_returnsWidthAndSizeOriginalHeight() {
    int width = 100;
    LayoutParams params = new LayoutParams(width, LayoutParams.WRAP_CONTENT);
    view.setLayoutParams(params);
    shadowView.setIsLaidOut(true);

    target.getSize(cb);

    verify(cb).onSizeReady(width, Target.SIZE_ORIGINAL);
  }

  @Test
  public void getSize_withWrapContentWidthAndMatchParentHeight_usesSizeOriginalWidthAndHeight() {
    LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
    view.setLayoutParams(params);

    target.getSize(cb);

    verify(cb, never()).onSizeReady(anyInt(), anyInt());

    int height = 32;
    shadowView
        .setHeight(height)
        .setIsLaidOut(true);

    shadowObserver.fireOnPreDrawListeners();

    verify(cb).onSizeReady(Target.SIZE_ORIGINAL, height);
  }

  @Test
  public void getSize_withMatchParentWidthAndWrapContentHeight_usesWidthAndSizeOriginalHeight() {
    LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
    view.setLayoutParams(params);

    target.getSize(cb);

    verify(cb, never()).onSizeReady(anyInt(), anyInt());

    int width = 32;
    shadowView
        .setWidth(width)
        .setIsLaidOut(true);
    shadowObserver.fireOnPreDrawListeners();

    verify(cb).onSizeReady(width, Target.SIZE_ORIGINAL);
  }

  @Test
  public void testMatchParentWidthAndHeight() {
    LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    view.setLayoutParams(params);

    target.getSize(cb);

    verify(cb, never()).onSizeReady(anyInt(), anyInt());

    int width = 32;
    int height = 45;
    shadowView
        .setWidth(width)
        .setHeight(height)
        .setIsLaidOut(true);
    shadowObserver.fireOnPreDrawListeners();

    verify(cb).onSizeReady(eq(width), eq(height));
  }

  @Test
  public void testSizeCallbackIsCalledPreDrawIfNoDimensAndNoLayoutParams() {
    target.getSize(cb);

    int width = 12;
    int height = 32;
    shadowView
        .setWidth(width)
        .setHeight(height)
        .setIsLaidOut(true);
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

    int width = 100, height = 111;
    shadowView
        .setWidth(width)
        .setHeight(height)
        .setIsLaidOut(true);
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
  public void getSize_withValidWidthAndHeight_notLaidOut_doesNotCallSizeReady() {
    shadowView
        .setWidth(100)
        .setHeight(100)
        .setIsLaidOut(false);
    target.getSize(cb);

    verify(cb, never()).onSizeReady(anyInt(), anyInt());
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
  public void getSize_withLayoutParams_zeroWidthHeight_notLaidOut_doesNotCallSizeReady() {
    shadowView
        .setLayoutParams(new LayoutParams(0, 0))
        .setWidth(100)
        .setHeight(100)
        .setIsLaidOut(false);
    target.getSize(cb);

    verify(cb, never()).onSizeReady(anyInt(), anyInt());
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
        .setHeight(100)
        .setIsLaidOut(true);

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

    @Implementation
    public boolean isAlive() {
      return isAlive;
    }

    private void checkIsAlive() {
      if (!isAlive()) {
        throw new IllegalStateException("ViewTreeObserver is not alive!");
      }
    }

    public void setIsAlive(boolean isAlive) {
      this.isAlive = isAlive;
    }

    public void fireOnPreDrawListeners() {
      for (OnPreDrawListener listener : preDrawListeners) {
        listener.onPreDraw();
      }
    }

    public List<OnPreDrawListener> getPreDrawListeners() {
      return preDrawListeners;
    }
  }

  @Implements(View.class)
  public static class SizedShadowView extends ShadowView {
    private int width;
    private int height;
    private LayoutParams layoutParams;
    private boolean isLaidOut;
    private boolean isLayoutRequested;

    public SizedShadowView setWidth(int width) {
      this.width = width;
      return this;
    }

    public SizedShadowView setHeight(int height) {
      this.height = height;
      return this;
    }

    public SizedShadowView setLayoutParams(LayoutParams layoutParams) {
      this.layoutParams = layoutParams;
      return this;
    }

    public SizedShadowView setIsLaidOut(boolean isLaidOut) {
      this.isLaidOut = isLaidOut;
      return this;
    }

    @Implementation
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

  private static class TestViewTarget extends ViewTarget<View, Object> {

    public TestViewTarget(View view) {
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
