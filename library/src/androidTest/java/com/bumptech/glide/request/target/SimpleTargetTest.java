package com.bumptech.glide.request.target;

import com.bumptech.glide.request.animation.GlideAnimation;
import org.junit.Test;

import static org.mockito.Mockito.mock;

public class SimpleTargetTest {

    @Test(expected = IllegalArgumentException.class)
    public void testThrowsOnGetSizeIfGivenWidthIsLessThanZero() {
        getTarget(-1, 1).getSize(mock(SizeReadyCallback.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowsOnGetSizeIfGivenWidthIsEqualToZero() {
        getTarget(0, 1).getSize(mock(SizeReadyCallback.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowsOnGetSizeIfGivenHeightIsLessThanZero() {
        getTarget(1, -1).getSize(mock(SizeReadyCallback.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowsOnGetSizeIfGivenHeightIsEqualToZero() {
        getTarget(1, 0).getSize(mock(SizeReadyCallback.class));
    }

    @Test
    public void testCanBeConstructedWithoutDimensions() {
        getTarget();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowsOnGetSizeIfConstructedWithoutDimensions() {
        getTarget().getSize(mock(SizeReadyCallback.class));
    }

    private SimpleTarget<Object> getTarget() {
        return new SimpleTarget<Object>() {
            @Override
            public void onResourceReady(Object resource, GlideAnimation<? super Object> glideAnimation) {
                // Do nothing.
            }
        };
    }

    private SimpleTarget<Object> getTarget(int width, int height) {
        return new SimpleTarget<Object>(width, height) {
            @Override
            public void onResourceReady(Object resource, GlideAnimation<? super Object> glideAnimation) {
                // Do nothing.
            }
        };
    }
}