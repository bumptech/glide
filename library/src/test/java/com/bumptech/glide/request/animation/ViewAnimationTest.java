package com.bumptech.glide.request.animation;

import android.view.animation.AlphaAnimation;
import android.widget.ImageView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static junit.framework.Assert.assertFalse;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
public class ViewAnimationTest {
    private AlphaAnimation animation;
    private ImageView view;
    private ViewAnimation viewAnimation;

    @Before
    public void setUp() {
        animation = new AlphaAnimation(0f, 1f);
        view = mock(ImageView.class);
        viewAnimation = new ViewAnimation(animation);
    }

    @Test
    public void testClearsAnimationOnAnimate() {
        viewAnimation.animate(null, null, view);

        verify(view).clearAnimation();
    }

    @Test
    public void testAlwaysReturnsFalse() {
        assertFalse(viewAnimation.animate(null, null, view));
    }

    @Test
    public void testStartsAnimationOnAnimate() {
        viewAnimation.animate(null, null, view);
        verify(view).startAnimation(eq(animation));
    }
}
