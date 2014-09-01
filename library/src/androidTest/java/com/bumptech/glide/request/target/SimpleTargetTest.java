package com.bumptech.glide.request.target;

import com.bumptech.glide.request.animation.GlideAnimation;

import org.junit.Test;

public class SimpleTargetTest {

    @Test(expected = IllegalArgumentException.class)
    public void testThrowsIfGivenWidthIsLessThanZero() {
        new SimpleTarget<Object>(-1, 1) {

            @Override
            public void onResourceReady(Object resource, GlideAnimation<Object> glideAnimation) {

            }
        };
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowsIfGivenWidthIsEqualToZero() {
        new SimpleTarget<Object>(0, 1) {

            @Override
            public void onResourceReady(Object resource, GlideAnimation<Object> glideAnimation) {

            }
        };
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowsIfGivenHeightIsLessThanZero() {
        new SimpleTarget<Object>(1, -1) {

            @Override
            public void onResourceReady(Object resource, GlideAnimation<Object> glideAnimation) {

            }
        };
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowsIfGivenHeightIsEqualToZero() {
        new SimpleTarget<Object>(1, 0) {

            @Override
            public void onResourceReady(Object resource, GlideAnimation<Object> glideAnimation) {

            }
        };
    }
}