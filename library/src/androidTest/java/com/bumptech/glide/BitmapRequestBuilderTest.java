package com.bumptech.glide;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.withSettings;

import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;

import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.provider.LoadProvider;
import com.bumptech.glide.request.animation.GlideAnimationFactory;
import com.bumptech.glide.tests.GlideShadowLooper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Collection;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, emulateSdk = 18, shadows = GlideShadowLooper.class)
@SuppressWarnings({ "unchecked", "deprecation" })
public class BitmapRequestBuilderTest {
    private GenericRequestBuilder original;
    private LoadProvider loadProvider;

    @Before
    public void setUp() {
        Glide glide = mock(Glide.class);
        original = new GenericRequestBuilder(Robolectric.application,
                Object.class, mock(LoadProvider.class), null, glide, null, null);
        loadProvider = mock(LoadProvider.class);
    }

    @After
    public void tearDown() {
        Glide.tearDown();
    }

    @Test
    public void testCrossFadeAppliesToBitmap() {
        testCrossFadeAppliesTo(new BitmapRequestBuilder(loadProvider, Bitmap.class, original));
    }

    @Test
    public void testCrossFadeAppliesToDrawable() {
        testCrossFadeAppliesTo(new BitmapRequestBuilder(loadProvider, Drawable.class, original));
    }

    @Test
    public void testCrossFadeAppliesToGlideDrawable() {
        testCrossFadeAppliesTo(new BitmapRequestBuilder(loadProvider, GlideDrawable.class, original));
    }

    @Test
    public void testCrossFadeAppliesToCustomDrawable() {
        class CustomDrawable extends ColorDrawable {
        }
        testCrossFadeAppliesTo(new BitmapRequestBuilder(loadProvider, CustomDrawable.class, original));
    }


    @Test
    public void testCrossFadeNotAppliedToString() {
        testCrossFadeNotAppliedTo(new BitmapRequestBuilder(loadProvider, String.class, original));
    }

    @Test
    public void testCrossFadeNotAppliedToCustom() {
        class Custom {
        }
        testCrossFadeNotAppliedTo(new BitmapRequestBuilder(loadProvider, Custom.class, original));
    }

    private void testCrossFadeAppliesTo(BitmapRequestBuilder builder) {
        Collection<GlideAnimationFactory> previousValues = new ArrayList<GlideAnimationFactory>();
        previousValues.add(null);

        BitmapRequestBuilder paramless = spy(builder, "paramless");
        paramless.crossFade();
        verifyAnimateCalledWithNewValue(paramless, previousValues);

        BitmapRequestBuilder duration = spy(builder, "duration");
        duration.crossFade(0);
        verifyAnimateCalledWithNewValue(duration, previousValues);

        BitmapRequestBuilder withId = spy(builder, "withId");
        withId.crossFade(0, 0);
        verifyAnimateCalledWithNewValue(withId, previousValues);

        BitmapRequestBuilder withAnim = spy(builder, "withAnim");
        withAnim.crossFade(null, 0);
        verifyAnimateCalledWithNewValue(withAnim, previousValues);
    }

    private BitmapRequestBuilder spy(BitmapRequestBuilder builder, String name) {
        return mock(BitmapRequestBuilder.class, withSettings()
                .name(name)
                .spiedInstance(builder)
                .defaultAnswer(CALLS_REAL_METHODS)
        );
    }

    private void verifyAnimateCalledWithNewValue(
            BitmapRequestBuilder builder, Collection<GlideAnimationFactory> previousValues) {
        ArgumentCaptor<GlideAnimationFactory> captor = ArgumentCaptor.forClass(GlideAnimationFactory.class);
        verify(builder).animate(captor.capture());
        assertThat(previousValues).doesNotContain(captor.getValue());
    }

    private void testCrossFadeNotAppliedTo(BitmapRequestBuilder builder) {
        try {
            builder.crossFade();
            fail("Expected an exception");
        } catch (UnsupportedOperationException ignore) {
            // pass
        }
        try {
            builder.crossFade(0);
            fail("Expected an exception");
        } catch (UnsupportedOperationException ignore) {
            // pass
        }
        try {
            builder.crossFade(0, 0);
            fail("Expected an exception");
        } catch (UnsupportedOperationException ignore) {
            // pass
        }
        try {
            builder.crossFade(null, 0);
            fail("Expected an exception");
        } catch (UnsupportedOperationException ignore) {
            // pass
        }
    }
}
