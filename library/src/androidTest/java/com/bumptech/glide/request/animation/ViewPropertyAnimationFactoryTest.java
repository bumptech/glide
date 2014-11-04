package com.bumptech.glide.request.animation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ViewPropertyAnimationFactoryTest {

    private ViewPropertyAnimationFactory<Object> factory;

    @Before
    public void setUp() {
        ViewPropertyAnimation.Animator animator = mock(ViewPropertyAnimation.Animator.class);
        factory = new ViewPropertyAnimationFactory<Object>(animator);
    }

    @Test
    public void testReturnsNoAnimationIfFromMemoryCache() {
        assertEquals(NoAnimation.get(), factory.build(true /*isFromMemoryCache*/, true /*isFirstResource*/));
    }

    @Test
    public void testReturnsNoAnimationIfNotFirstResource() {
        assertEquals(NoAnimation.get(), factory.build(false /*isFromMemoryCache*/, false /*isFirstResource*/));
    }

    @Test
    public void testReturnsAnimationIfNotFromMemoryCacheAndFirstResource() {
        assertNotEquals(NoAnimation.get(), factory.build(false /*isFromMemoryCache*/, true /*isFirstResource*/));
    }
}