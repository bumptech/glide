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

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
public class DrawableCrossFadeViewAnimationTest {
    private CrossFadeHarness harness;

    @Before
    public void setup() {
        harness = new CrossFadeHarness();
    }

    @Test
    public void testStartsDefaultAnimationIfNoPreviousDrawableIsNotSet() {
        harness.animation.animate(harness.previous, harness.current, harness.view);
        verify(harness.view).startAnimation(any(Animation.class));
    }

    @Test
    public void testReturnsFalseIfStartsDefaultAnimation() {
        assertFalse(harness.animation.animate(harness.previous, harness.current, harness.view));
    }

    @Test
    public void testSetsTransitionDrawableIfPreviousIsNotNull() {
        harness.previous = new ColorDrawable(Color.WHITE);
        harness.animation.animate(harness.previous, harness.current, harness.view);

        verify(harness.view).setImageDrawable(any(TransitionDrawable.class));
    }

    @Test
    public void testReturnsTrueIfSetsTransitionDrawable() {
        harness.previous = new ColorDrawable(Color.RED);
        assertTrue(harness.animation.animate(harness.previous, harness.current, harness.view));
    }

    @SuppressWarnings("unchecked")
    private static class CrossFadeHarness {
        Drawable previous = null;
        Drawable current = new ColorDrawable(Color.GRAY);
        ImageView view = mock(ImageView.class);
        Animation defaultAnimation = new AlphaAnimation(0f, 1f);
        int duration = 200;
        DrawableCrossFadeViewAnimation<Drawable> animation = new DrawableCrossFadeViewAnimation<Drawable>(
                defaultAnimation, duration);
    }
}

