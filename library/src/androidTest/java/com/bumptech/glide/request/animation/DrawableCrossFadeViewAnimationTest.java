package com.bumptech.glide.request.animation;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.bumptech.glide.request.animation.GlideAnimation.ViewAdapter;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class DrawableCrossFadeViewAnimationTest {
    private CrossFadeHarness harness;

    @Before
    public void setup() {
        harness = new CrossFadeHarness();
    }

    @Test
    public void testStartsDefaultAnimationIfNoPreviousDrawableIsNotSet() {
        when(harness.adapter.getView()).thenReturn(harness.view);
        harness.animation.animate(harness.current, harness.adapter);
        verify(harness.view).startAnimation(any(Animation.class));
    }

    @Test
    public void testIgnoresNullViews() {
        when(harness.adapter.getView()).thenReturn(null);
        harness.animation.animate(harness.current, harness.adapter);
    }

    @Test
    public void testReturnsFalseIfStartsDefaultAnimation() {
        assertFalse(harness.animation.animate(harness.current, harness.adapter));
    }

    @Test
    public void testSetsTransitionDrawableIfPreviousIsNotNull() {
        Drawable previous = new ColorDrawable(Color.WHITE);
        when(harness.adapter.getCurrentDrawable()).thenReturn(previous);
        harness.animation.animate(harness.current, harness.adapter);

        verify(harness.adapter).setDrawable(any(TransitionDrawable.class));
    }

    @Test
    public void testReturnsTrueIfSetsTransitionDrawable() {
        Drawable previous = new ColorDrawable(Color.RED);
        when(harness.adapter.getCurrentDrawable()).thenReturn(previous);
        assertTrue(harness.animation.animate(harness.current, harness.adapter));
    }

    @SuppressWarnings("unchecked")
    private static class CrossFadeHarness {
        Drawable current = new ColorDrawable(Color.GRAY);
        ViewAdapter adapter = mock(ViewAdapter.class);
        ImageView view = mock(ImageView.class);
        Animation defaultAnimation = new AlphaAnimation(0f, 1f);
        int duration = 200;
        DrawableCrossFadeViewAnimation<Drawable> animation = new DrawableCrossFadeViewAnimation<Drawable>(
                defaultAnimation, duration);
    }
}

