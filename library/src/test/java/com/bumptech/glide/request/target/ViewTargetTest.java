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

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import com.bumptech.glide.request.Request;
import com.bumptech.glide.request.transition.Transition;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.internal.ShadowExtractor;
import org.robolectric.shadows.ShadowDisplay;
import org.robolectric.shadows.ShadowView;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 18, shadows = { ViewTargetTest.SizedShadowView.class,
    ViewTargetTest.PreDrawShadowViewTreeObserver.class })
public class ViewTargetTest {
  private View view;
  private ViewTarget<View, Object> target;
  private SizedShadowView shadowView;
  private PreDrawShadowViewTreeObserver shadowObserver;

  @Before
  public void setUp() {
    view = new View(RuntimeEnvironment.application);
    target = new TestViewTarget(view);

    shadowView = (SizedShadowView) ShadowExtractor.extract(view);
    shadowObserver =
        (PreDrawShadowViewTreeObserver) ShadowExtractor.extract(view.getViewTreeObserver());
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
    Request request = mock(Request.class);

    target.setRequest(request);

    assertEquals(request, target.getRequest());
  }

  @Test
  public void testRetrievesRequestFromPreviousTargetForView() {
    Request request = mock(Request.class);

    target.setRequest(request);

    ViewTarget<View, Object> second = new TestViewTarget(view);

    assertEquals(request, second.getRequest());
  }

  @Test
  public void testSizeCallbackIsCalledSynchronouslyIfViewSizeSet() {
    int dimens = 333;
    shadowView.setWidth(dimens);
    shadowView.setHeight(dimens);

    SizeReadyCallback cb = mock(SizeReadyCallback.class);
    target.getSize(cb);

    verify(cb).onSizeReady(eq(dimens), eq(dimens));
  }

  @Test
  public void testSizeCallbackIsCalledSynchronouslyIfLayoutParamsConcreteSizeSet() {
    int dimens = 444;
    LayoutParams layoutParams = new LayoutParams(dimens, dimens);
    view.setLayoutParams(layoutParams);

    SizeReadyCallback cb = mock(SizeReadyCallback.class);
    target.getSize(cb);

    verify(cb).onSizeReady(eq(dimens), eq(dimens));
  }

  private void setDisplayDimens(Integer width, Integer height) {
    WindowManager windowManager =
        (WindowManager) RuntimeEnvironment.application.getSystemService(Context.WINDOW_SERVICE);
    ShadowDisplay shadowDisplay = Shadows.shadowOf(windowManager.getDefaultDisplay());
    if (width != null) {
      shadowDisplay.setWidth(width);
    }

    if (height != null) {
      shadowDisplay.setHeight(height);
    }
  }

  private void setDisplayWidth(int width) {
    setDisplayDimens(width, null);
  }

  private void setDisplayHeight(int height) {
    setDisplayDimens(null, height);
  }

  @Test
  public void testBothParamsWrapContent() {
    LayoutParams layoutParams =
        new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    view.setLayoutParams(layoutParams);

    int width = 123;
    int height = 456;
    setDisplayDimens(width, height);
    SizeReadyCallback cb = mock(SizeReadyCallback.class);
    target.getSize(cb);

    verify(cb).onSizeReady(eq(width), eq(height));
  }

  @Test
  public void testWrapContentWidthWithValidHeight() {
    int displayWidth = 500;
    setDisplayWidth(displayWidth);

    int height = 100;
    LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, height);
    view.setLayoutParams(params);

    SizeReadyCallback cb = mock(SizeReadyCallback.class);
    target.getSize(cb);

    verify(cb).onSizeReady(eq(displayWidth), eq(height));
  }

  @Test
  public void testWrapContentHeightWithValidWidth() {
    int displayHeight = 700;
    setDisplayHeight(displayHeight);
    int width = 100;
    LayoutParams params = new LayoutParams(width, LayoutParams.WRAP_CONTENT);
    view.setLayoutParams(params);

    SizeReadyCallback cb = mock(SizeReadyCallback.class);
    target.getSize(cb);

    verify(cb).onSizeReady(eq(width), eq(displayHeight));
  }

  @Test
  public void testWrapContentWidthWithMatchParentHeight() {
    int displayWidth = 1234;
    setDisplayWidth(displayWidth);

    LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
    view.setLayoutParams(params);

    SizeReadyCallback cb = mock(SizeReadyCallback.class);
    target.getSize(cb);

    verify(cb, never()).onSizeReady(anyInt(), anyInt());

    int height = 32;
    shadowView.setHeight(height);

    shadowObserver.fireOnPreDrawListeners();

    verify(cb).onSizeReady(eq(displayWidth), eq(height));
  }

  @Test
  public void testWrapContentHeightWithMatchParentWidth() {
    int displayHeight = 5812;
    setDisplayHeight(displayHeight);

    LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
    view.setLayoutParams(params);

    SizeReadyCallback cb = mock(SizeReadyCallback.class);
    target.getSize(cb);

    verify(cb, never()).onSizeReady(anyInt(), anyInt());

    int width = 32;
    shadowView.setWidth(width);
    shadowObserver.fireOnPreDrawListeners();

    verify(cb).onSizeReady(eq(width), eq(displayHeight));
  }

  @Test
  public void testMatchParentWidthAndHeight() {
    LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    view.setLayoutParams(params);

    SizeReadyCallback cb = mock(SizeReadyCallback.class);
    target.getSize(cb);

    verify(cb, never()).onSizeReady(anyInt(), anyInt());

    int width = 32;
    int height = 45;
    shadowView.setWidth(width);
    shadowView.setHeight(height);
    shadowObserver.fireOnPreDrawListeners();

    verify(cb).onSizeReady(eq(width), eq(height));
  }

  @Test
  public void testSizeCallbackIsCalledPreDrawIfNoDimensAndNoLayoutParams() {
    SizeReadyCallback cb = mock(SizeReadyCallback.class);
    target.getSize(cb);

    int width = 12;
    int height = 32;
    shadowView.setWidth(width);
    shadowView.setHeight(height);
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
    shadowView.setWidth(width);
    shadowView.setHeight(height);
    shadowObserver.fireOnPreDrawListeners();

    InOrder order = inOrder((Object[]) cbs);
    for (SizeReadyCallback cb : cbs) {
      order.verify(cb).onSizeReady(eq(width), eq(height));
    }
  }

  @Test
  public void testDoesNotNotifyCallbackTwiceIfAddedTwice() {
    SizeReadyCallback cb = mock(SizeReadyCallback.class);
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
    SizeReadyCallback cb = mock(SizeReadyCallback.class);
    target.getSize(cb);
    shadowObserver.fireOnPreDrawListeners();

    verify(cb, never()).onSizeReady(anyInt(), anyInt());
    assertThat(shadowObserver.getPreDrawListeners()).hasSize(1);
  }

  @Test
  public void testSizeCallbackIsCalledPreDrawIfNoDimensAndNoLayoutParamsButLayoutParamsSetLater() {
    SizeReadyCallback cb = mock(SizeReadyCallback.class);
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
    SizeReadyCallback cb = mock(SizeReadyCallback.class);
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
    SizeReadyCallback cb = mock(SizeReadyCallback.class);
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
    SizeReadyCallback cb = mock(SizeReadyCallback.class);
    view.setLayoutParams(new LayoutParams(100, 100));
    view.setPadding(25, 25, 25, 25);

    target.getSize(cb);

    verify(cb).onSizeReady(50, 50);
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

    public void setWidth(int width) {
      this.width = width;
    }

    public void setHeight(int height) {
      this.height = height;
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
