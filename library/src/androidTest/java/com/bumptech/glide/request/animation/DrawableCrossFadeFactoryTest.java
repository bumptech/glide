package com.bumptech.glide.request.animation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.mock;

import android.graphics.drawable.Drawable;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, emulateSdk = 18)
public class DrawableCrossFadeFactoryTest {

    private DrawableCrossFadeFactory<Drawable> factory;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() {
        ViewAnimationFactory<Drawable> viewAnimationFactory = mock(ViewAnimationFactory.class);
        factory = new DrawableCrossFadeFactory<Drawable>(viewAnimationFactory, 100 /*duration*/);
    }

    @Test
    public void testReturnsNoAnimationIfFromMemoryCache() {
        assertEquals(NoAnimation.<Drawable>get(), factory.build(true /*isFromMemoryCache*/, true /*isFirstResource*/));
    }

    @Test
    public void testReturnsReturnsAnimationIfNotFromMemoryCacheAndIsFirstResource() {
        assertNotEquals(NoAnimation.<Drawable>get(),
                factory.build(false /*isFromMemoryCache*/, true /*isFirstResource*/));
    }

    @Test
    public void testReturnsAnimationIfNotFromMemocyCacheAndNotIsFirstResource() {
        assertNotEquals(NoAnimation.<Drawable>get(),
                factory.build(false /*isFromMemoryCache*/, false /*isFirstResource*/));
    }
}