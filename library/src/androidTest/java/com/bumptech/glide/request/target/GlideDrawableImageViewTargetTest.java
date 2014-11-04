package com.bumptech.glide.request.target;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import com.bumptech.glide.load.resource.drawable.GlideDrawable;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, emulateSdk = 18)
public class GlideDrawableImageViewTargetTest {

    @Test
    public void testSetsDrawableOnViewInSetResource() {
        ImageView view = new ImageView(Robolectric.application);
        GlideDrawableImageViewTarget target = new GlideDrawableImageViewTarget(view);
        GlideDrawable expected = new MockAnimatedDrawable();

        target.setResource(expected);

        assertEquals(expected, view.getDrawable());
    }

    @Test
    public void testWrapsDrawableInSquaringDrawableIfDrawableAndViewAreSquare() {
        ImageView mockView = mock(ImageView.class);
        when(mockView.getWidth()).thenReturn(100);
        when(mockView.getHeight()).thenReturn(100);
        GlideDrawableImageViewTarget target = new GlideDrawableImageViewTarget(mockView);
        GlideDrawable drawable = new MockAnimatedDrawable() {
            @Override
            public int getIntrinsicHeight() {
                return 100;
            }

            @Override
            public int getIntrinsicWidth() {
                return 100;
            }
        };


        target.onResourceReady(drawable, null);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Drawable drawable = (Drawable) invocation.getArguments()[0];
                assertThat(drawable).isInstanceOf(SquaringDrawable.class);
                return null;
            }
        }).when(mockView).setImageDrawable(any(Drawable.class));
        verify(mockView).setImageDrawable(any(BitmapDrawable.class));
    }

    @Test
    public void testDoesNotWrapInSquaringDrawableIfDrawableIsAnimated() {
        ImageView mockView = mock(ImageView.class);
        when(mockView.getWidth()).thenReturn(100);
        when(mockView.getHeight()).thenReturn(100);
        GlideDrawableImageViewTarget target = new GlideDrawableImageViewTarget(mockView);
        GlideDrawable drawable = new MockAnimatedDrawable() {
            @Override
            public int getIntrinsicHeight() {
                return 100;
            }

            @Override
            public int getIntrinsicWidth() {
                return 100;
            }
        };
         target.onResourceReady(drawable, null);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Drawable drawable = (Drawable) invocation.getArguments()[0];
                assertThat(drawable).isNotInstanceOf(SquaringDrawable.class);
                return null;
            }
        }).when(mockView).setImageDrawable(any(Drawable.class));
        verify(mockView).setImageDrawable(any(BitmapDrawable.class));
    }

    @Test
    public void testDoesNotWrapInSquaringDrawableIfDrawableIsNotSquare() {
        ImageView mockView = mock(ImageView.class);
        when(mockView.getWidth()).thenReturn(100);
        when(mockView.getHeight()).thenReturn(100);
        GlideDrawableImageViewTarget target = new GlideDrawableImageViewTarget(mockView);
        GlideDrawable drawable = new MockAnimatedDrawable() {
            @Override
            public int getIntrinsicHeight() {
                return 100;
            }

            @Override
            public int getIntrinsicWidth() {
                return 150;
            }
        };
         target.onResourceReady(drawable, null);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Drawable drawable = (Drawable) invocation.getArguments()[0];
                assertThat(drawable).isNotInstanceOf(SquaringDrawable.class);
                return null;
            }
        }).when(mockView).setImageDrawable(any(Drawable.class));
        verify(mockView).setImageDrawable(any(BitmapDrawable.class));
    }

    @Test
    public void testDoesNotWrapInSquaringDrawableIfViewNotSquare() {
        ImageView mockView = mock(ImageView.class);
        when(mockView.getWidth()).thenReturn(100);
        when(mockView.getHeight()).thenReturn(150);
        GlideDrawableImageViewTarget target = new GlideDrawableImageViewTarget(mockView);
        GlideDrawable drawable = new MockAnimatedDrawable() {
            @Override
            public int getIntrinsicHeight() {
                return 100;
            }

            @Override
            public int getIntrinsicWidth() {
                return 100;
            }
        };
         target.onResourceReady(drawable, null);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Drawable drawable = (Drawable) invocation.getArguments()[0];
                assertThat(drawable).isNotInstanceOf(SquaringDrawable.class);
                return null;
            }
        }).when(mockView).setImageDrawable(any(Drawable.class));
        verify(mockView).setImageDrawable(any(BitmapDrawable.class));
    }

    @Test
    public void testStartsAnimatableDrawablesInOnReasourceReady() {
        MockAnimatedDrawable drawable = new MockAnimatedDrawable();
        GlideDrawableImageViewTarget target = new GlideDrawableImageViewTarget(new ImageView(Robolectric.application));
        target.onResourceReady(drawable, null);

        assertTrue(drawable.isStarted);
    }

    @Test
    public void testStartsAnimatableDrawablesOnStart() {
        MockAnimatedDrawable drawable = new MockAnimatedDrawable();
        GlideDrawableImageViewTarget target = new GlideDrawableImageViewTarget(new ImageView(Robolectric.application));
        target.onResourceReady(drawable, null);
        target.onStop();
        target.onStart();

        assertTrue(drawable.isStarted);
    }

    @Test
    public void testDoesNotStartNullDrawablesOnStart() {
        GlideDrawableImageViewTarget target = new GlideDrawableImageViewTarget(new ImageView(Robolectric.application));
        target.onStart();
    }

    @Test
    public void testStopsAnimatedDrawablesOnStop() {
        MockAnimatedDrawable drawable = new MockAnimatedDrawable();
        GlideDrawableImageViewTarget target = new GlideDrawableImageViewTarget(new ImageView(Robolectric.application));
        target.onResourceReady(drawable, null);
        target.onStop();

        assertFalse(drawable.isStarted);
    }

    @Test
    public void testDoesNotStopNullDrawablesOnStop() {
        GlideDrawableImageViewTarget target = new GlideDrawableImageViewTarget(new ImageView(Robolectric.application));
        target.onStop();
    }

    @Test
    public void testSetsLoopCountOnDrawable() {
        int maxLoopCount = 6;
        MockAnimatedDrawable drawable = new MockAnimatedDrawable();
        GlideDrawableImageViewTarget target = new GlideDrawableImageViewTarget(new ImageView(Robolectric.application),
                maxLoopCount);
        target.onResourceReady(drawable, null);
        assertEquals(maxLoopCount, drawable.loopCount);
    }

    private static class MockAnimatedDrawable extends GlideDrawable {
        private boolean isStarted;
        private int loopCount;

        @Override
        public void start() {
            isStarted = true;
        }

        @Override
        public void stop() {
            isStarted = false;
        }

        @Override
        public boolean isRunning() {
            return false;
        }

        @Override
        public void draw(Canvas canvas) {

        }

        @Override
        public void setAlpha(int i) {

        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {

        }

        @Override
        public int getOpacity() {
            return 0;
        }

        @Override
        public boolean isAnimated() {
            return false;
        }

        @Override
        public void setLoopCount(int loopCount) {
            this.loopCount = loopCount;
        }
    }
}