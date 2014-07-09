package com.bumptech.glide.request.animation;

import android.widget.ImageView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import static junit.framework.Assert.assertFalse;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
public class ViewPropertyAnimationTest {
    private ViewPropertyAnimation.Animator animator;
    private ViewPropertyAnimation animation;

    @Before
    public void setUp() {
        animator = mock(ViewPropertyAnimation.Animator.class);
        animation = new ViewPropertyAnimation(animator);
    }

    @Test
    public void testAlwaysReturnsFalse() {
        assertFalse(animation.animate(null, new Object(), new ImageView(Robolectric.application)));
    }

    @Test
    public void testCallsAnimatorWithGivenView() {
        ImageView view = new ImageView(Robolectric.application);
        animation.animate(null, new Object(), view);

        verify(animator).animate(eq(view));
    }
}
