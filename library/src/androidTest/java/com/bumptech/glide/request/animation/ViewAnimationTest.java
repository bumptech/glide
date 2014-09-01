package com.bumptech.glide.request.animation;

import android.view.animation.AlphaAnimation;
import android.widget.ImageView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.bumptech.glide.request.animation.GlideAnimation.ViewAdapter;
import static junit.framework.Assert.assertFalse;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class ViewAnimationTest {
    private AlphaAnimation animation;
    private ViewAnimation viewAnimation;
    private ViewAdapter adapter;
    private ImageView view;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() {
        animation = new AlphaAnimation(0f, 1f);
        view = mock(ImageView.class);
        adapter = mock(ViewAdapter.class);
        when(adapter.getView()).thenReturn(view);
        viewAnimation = new ViewAnimation(animation);
    }

    @Test
    public void testClearsAnimationOnAnimate() {
        viewAnimation.animate(null, adapter);

        verify(view).clearAnimation();
    }

    @Test
    public void testAlwaysReturnsFalse() {
        assertFalse(viewAnimation.animate(null, adapter));
    }

    @Test
    public void testStartsAnimationOnAnimate() {
        viewAnimation.animate(null, adapter);
        verify(view).startAnimation(eq(animation));
    }
}
