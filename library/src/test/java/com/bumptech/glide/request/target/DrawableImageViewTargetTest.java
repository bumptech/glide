package com.bumptech.glide.request.target;

import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import com.bumptech.glide.request.animation.GlideAnimation;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class DrawableImageViewTargetTest {
    private ImageView view;
    private DrawableImageViewTarget target;

    @Before
    public void setUp() {
        view = new ImageView(Robolectric.application);
        target = new DrawableImageViewTarget(view);
    }

    @Test
    public void testSetsDrawableOnViewInOnResourceReadyWhenAnimationIsNull() {
        Drawable resource = new ColorDrawable(Color.BLUE);
        target.onResourceReady(resource, null);

        assertEquals(resource, view.getDrawable());
    }

    @Test
    public void testSetsDrawableOnViewInOnResourceReadyWhenAnimationReturnsFalse() {
        GlideAnimation animation = mock(GlideAnimation.class);
        when(animation.animate(any(Drawable.class), any(Drawable.class), any(ImageView.class)))
                .thenReturn(false);
        Drawable resource = new ColorDrawable(Color.GRAY);
        target.onResourceReady(resource, animation);

        assertEquals(resource, view.getDrawable());
    }

    @Test
    public void testDoesNotSetDrawableOnViewInOnResourceReadyWhenAnimationReturnsTrue() {
        Drawable resource = new ColorDrawable(Color.RED);
        GlideAnimation animation = mock(GlideAnimation.class);
        when(animation.animate((Drawable) isNull(), eq(resource), eq(view))).thenReturn(true);
        target.onResourceReady(resource, animation);

        assertNull(view.getDrawable());
    }

    @Test
    public void testSetsPlaceholderOnView() {
        Drawable placeholder = new ColorDrawable(Color.RED);
        target.setPlaceholder(placeholder);

        assertEquals(placeholder, view.getDrawable());
    }

    @Test
    public void testProvidesCurrentPlaceholderToAnimationIfPresent() {
        Drawable placeholder = new ColorDrawable(Color.BLACK);
        view.setImageDrawable(placeholder);

        GlideAnimation animation = mock(GlideAnimation.class);

        target.onResourceReady(new ColorDrawable(Color.GREEN), animation);

        verify(animation).animate(eq(placeholder), any(Drawable.class), any(ImageView.class));
    }

    @Test
    public void testWrapsDrawableInSquaringDrawableIfDrawableAndViewAreSquare() {
        ImageView mockView = mock(ImageView.class);
        when(mockView.getWidth()).thenReturn(100);
        when(mockView.getHeight()).thenReturn(100);
        DrawableImageViewTarget target = new DrawableImageViewTarget(mockView);
        Drawable drawable = new ColorDrawable(Color.RED) {
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
                assertTrue(drawable instanceof SquaringDrawable);
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
        DrawableImageViewTarget target = new DrawableImageViewTarget(mockView);
        Drawable drawable = new AnimationDrawable() {
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
                assertFalse(drawable instanceof SquaringDrawable);
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
        DrawableImageViewTarget target = new DrawableImageViewTarget(mockView);
        Drawable drawable = new ColorDrawable(Color.RED) {
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
                assertFalse(drawable instanceof SquaringDrawable);
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
        DrawableImageViewTarget target = new DrawableImageViewTarget(mockView);
        Drawable drawable = new ColorDrawable(Color.RED) {
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
                assertFalse(drawable instanceof SquaringDrawable);
                return null;
            }
        }).when(mockView).setImageDrawable(any(Drawable.class));
        verify(mockView).setImageDrawable(any(BitmapDrawable.class));
    }
}