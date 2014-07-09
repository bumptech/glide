package com.bumptech.glide.request.target;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.Request;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowDisplay;
import org.robolectric.shadows.ShadowView;
import org.robolectric.shadows.ShadowViewTreeObserver;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static android.view.ViewGroup.LayoutParams;
import static android.view.ViewTreeObserver.OnPreDrawListener;
import static com.bumptech.glide.request.target.Target.SizeReadyCallback;
import static junit.framework.Assert.assertEquals;
import static junit.framework.TestCase.assertNull;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = { ViewTargetTest.SizedShadowView.class, ViewTargetTest.PreDrawShadowViewTreeObserver.class })
public class ViewTargetTest {
    private View view;
    private ViewTarget target;

    @Before
    public void setUp() {
        view = new View(Robolectric.application);
        target = new ViewTarget<View, Object>(view) {

            @Override
            public void onResourceReady(Object resource, GlideAnimation<Object> glideAnimation) {
            }

            @Override
            public void setPlaceholder(Drawable placeholder) {
            }
        };
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

        ViewTarget<View, Object> second = new ViewTarget<View, Object>(view) {
            @Override
            public void onResourceReady(Object resource, GlideAnimation<Object> glideAnimation) {
            }

            @Override
            public void setPlaceholder(Drawable placeholder) {
            }
        };

        assertEquals(request, second.getRequest());
    }

    @Test
    public void testSizeCallbackIsCalledSynchronouslyIfViewSizeSet() {
        int dimens = 333;
        SizedShadowView shadowView = Robolectric.shadowOf_(view);
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

    @Test
    public void testSizeCallbackIsCalledSynchronouslyWithScreenSizeIfLayoutParamsWrapContent() {
        LayoutParams layoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        view.setLayoutParams(layoutParams);

        int width = 1234;
        int height = 674;
        WindowManager windowManager = (WindowManager) view.getContext()
                .getSystemService(Context.WINDOW_SERVICE);
        ShadowDisplay shadowDisplay = Robolectric.shadowOf(windowManager.getDefaultDisplay());
        shadowDisplay.setWidth(width);
        shadowDisplay.setHeight(height);

        SizeReadyCallback cb = mock(SizeReadyCallback.class);
        target.getSize(cb);

        verify(cb).onSizeReady(eq(width), eq(height));
    }

    @Test
    public void testSizeCallbackIsCalledPreDrawIfNoDimensAndNoLayoutParams() {
        SizeReadyCallback cb = mock(SizeReadyCallback.class);
        target.getSize(cb);

        int width = 12;
        int height = 32;
        SizedShadowView shadowView = Robolectric.shadowOf_(view);
        shadowView.setWidth(width);
        shadowView.setHeight(height);

        PreDrawShadowViewTreeObserver shadowObserver = Robolectric.shadowOf_(view.getViewTreeObserver());
        shadowObserver.fireOnPreDrawListeners();

        verify(cb).onSizeReady(eq(width), eq(height));
    }

    @Test
    public void testSizeCallbackIsNotCalledPreDrawIfNoDimensSetOnPreDraw() {
        SizeReadyCallback cb = mock(SizeReadyCallback.class);
        target.getSize(cb);

        PreDrawShadowViewTreeObserver shadowObserver = Robolectric.shadowOf_(view.getViewTreeObserver());
        shadowObserver.fireOnPreDrawListeners();

        verify(cb, never()).onSizeReady(anyInt(), anyInt());
        TestCase.assertEquals(1, shadowObserver.getPreDrawListeners()
                .size());
    }

    @Test
    public void testSizeCallbackIsCalledPreDrawIfNoDimensAndNoLayoutParamsButLayoutParamsSetLater() {
        SizeReadyCallback cb = mock(SizeReadyCallback.class);
        target.getSize(cb);

        int width = 689;
        int height = 354;
        LayoutParams layoutParams = new LayoutParams(width, height);
        view.setLayoutParams(layoutParams);

        PreDrawShadowViewTreeObserver shadowViewTreeObserver = Robolectric.shadowOf_(view.getViewTreeObserver());
        shadowViewTreeObserver.fireOnPreDrawListeners();

        verify(cb).onSizeReady(eq(width), eq(height));
    }

    @Test
    public void testCallbackIsNotCalledTwiceIfPreDrawFiresTwice() {
        SizeReadyCallback cb = mock(SizeReadyCallback.class);
        target.getSize(cb);

        LayoutParams layoutParams = new LayoutParams(1234, 4123);
        view.setLayoutParams(layoutParams);

        PreDrawShadowViewTreeObserver shadowViewTreeObserver = Robolectric.shadowOf_(view.getViewTreeObserver());
        shadowViewTreeObserver.fireOnPreDrawListeners();
        shadowViewTreeObserver.fireOnPreDrawListeners();

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

        PreDrawShadowViewTreeObserver shadowViewTreeObserver = Robolectric.shadowOf_(view.getViewTreeObserver());
        shadowViewTreeObserver.fireOnPreDrawListeners();
        shadowViewTreeObserver.fireOnPreDrawListeners();

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

        PreDrawShadowViewTreeObserver shadowViewTreeObserver = Robolectric.shadowOf_(view.getViewTreeObserver());
        shadowViewTreeObserver.setIsAlive(false);
        shadowViewTreeObserver.fireOnPreDrawListeners();

        verify(cb).onSizeReady(eq(width), eq(height));
    }

    @Test(expected = NullPointerException.class)
    public void testThrowsIfGivenNullView() {
        ViewTarget viewTarget = new ViewTarget(null) {
            @Override
            public void onResourceReady(Object resource, GlideAnimation glideAnimation) {

            }

            @Override
            public void setPlaceholder(Drawable placeholder) {

            }
        };
    }

    @Implements(ViewTreeObserver.class)
    public static class PreDrawShadowViewTreeObserver extends ShadowViewTreeObserver {
        private CopyOnWriteArrayList<OnPreDrawListener> preDrawListeners =
                new CopyOnWriteArrayList<OnPreDrawListener>();
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
}
