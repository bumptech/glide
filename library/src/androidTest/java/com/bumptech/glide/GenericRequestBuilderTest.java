package com.bumptech.glide;

import static com.bumptech.glide.tests.BackgroundUtil.testInBackground;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.widget.ImageView;

import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.manager.Lifecycle;
import com.bumptech.glide.manager.RequestTracker;
import com.bumptech.glide.provider.LoadProvider;
import com.bumptech.glide.request.Request;
import com.bumptech.glide.request.animation.GlideAnimationFactory;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.tests.BackgroundUtil;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@SuppressWarnings("unchecked")
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, emulateSdk = 18)
public class GenericRequestBuilderTest {
    private RequestTracker requestTracker;

    @Before
    public void setUp() {
        requestTracker = mock(RequestTracker.class);
    }

    @Test(expected = NullPointerException.class)
    public void testThrowsIfContextIsNull() {
        new GenericRequestBuilder(null, Object.class, mock(LoadProvider.class), Object.class, mock(Glide.class),
                requestTracker, mock(Lifecycle.class));
    }

    @Test(expected = NullPointerException.class)
    public void testThrowsIfNonNullModelAndNullLoadProvider() {
        new GenericRequestBuilder(Robolectric.application, Object.class, null, Object.class, mock(Glide.class),
                requestTracker, mock(Lifecycle.class));
    }

    @Test(expected = NullPointerException.class)
    public void testThrowsWhenGlideAnimationFactoryIsNull() {
        getNullModelRequest().animate((GlideAnimationFactory) null);
    }

    @Test(expected =  IllegalArgumentException.class)
    public void testThrowsWhenOverrideWidthLessThanZero() {
        getNullModelRequest().override(-1, 100);
    }

    @Test(expected =  IllegalArgumentException.class)
    public void testThrowsWhenOverrideWidthEqualToZero() {
        getNullModelRequest().override(0, 100);
    }

    @Test(expected =  IllegalArgumentException.class)
    public void testThrowsWhenOverrideHeightLessThanZero() {
        getNullModelRequest().override(100, -5);
    }

    @Test(expected =  IllegalArgumentException.class)
    public void testThrowsWhenOverrideHeightEqualToZero() {
        getNullModelRequest().override(100, 0);
    }

    @Test
    public void testDoesNotThrowWhenWidthIsSizeOriginal() {
        getNullModelRequest().override(Target.SIZE_ORIGINAL, 100);
    }

    @Test
    public void testDoesNotThrowWhenHeightIsSizeOriginal() {
        getNullModelRequest().override(100, Target.SIZE_ORIGINAL);
    }

    @Test
    public void testDoesNotThrowWhenModelAndLoaderNull() {
        new GenericRequestBuilder(Robolectric.application, null, null, Object.class, mock(Glide.class), requestTracker,
                mock(Lifecycle.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testProvidingSelfAsThumbnailThrows() {
        GenericRequestBuilder request = getNullModelRequest();
        request.thumbnail(request);
    }

    @Test(expected = IllegalStateException.class)
    public void testProvidingSelfAsChildOfThumbnailThrows() {
        GenericRequestBuilder first = getNullModelRequest();
        GenericRequestBuilder second = first.clone();
        second.thumbnail(first);
        first.thumbnail(second);
        first.into(mock(Target.class));
    }

    @Test
    public void testCanPassedClonedSelfToThumbnail() {
        GenericRequestBuilder first = getNullModelRequest();
        GenericRequestBuilder second = first.clone();
        GenericRequestBuilder third = second.clone();
        first.thumbnail(second.thumbnail(third)).into(mock(Target.class));
    }

    @Test
    public void testDoesNotThrowWithNullModelWhenDecoderSet() {
        getNullModelRequest().decoder(mock(ResourceDecoder.class));
    }

    @Test
    public void testDoesNotThrowWithNullModelWhenCacheDecoderSet() {
        getNullModelRequest().cacheDecoder(mock(ResourceDecoder.class));
    }

    @Test
    public void testDoesNotThrowWithNullModelWhenEncoderSet() {
        getNullModelRequest().encoder(mock(ResourceEncoder.class));
    }

    @Test
    public void testDoesNotThrowWithNullModelWhenDiskCacheStrategySet() {
        getNullModelRequest().diskCacheStrategy(DiskCacheStrategy.ALL);
    }

    @Test
    public void testDoesNotThrowWithNullModelWhenRequestIsBuilt() {
        getNullModelRequest().into(mock(Target.class));
    }

    @Test
    public void testAddsNewRequestToRequestTracker() {
        getNullModelRequest().into(mock(Target.class));
        verify(requestTracker).runRequest(any(Request.class));
    }

    @Test
    public void testRemovesPreviousRequestFromRequestTracker() {
        Request previous = mock(Request.class);
        Target target = mock(Target.class);
        when(target.getRequest()).thenReturn(previous);

        getNullModelRequest().into(target);

        verify(requestTracker).removeRequest(eq(previous));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowsIfGivenNullTarget() {
        getNullModelRequest().into((Target) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowsIfGivenNullView() {
        getNullModelRequest().into((ImageView) null);
    }

    @Test(expected = RuntimeException.class)
    public void testThrowsIfIntoViewCalledOnBackgroundThread() throws InterruptedException {
        final ImageView imageView = new ImageView(Robolectric.application);
        testInBackground(new BackgroundUtil.BackgroundTester() {
            @Override
            public void runTest() throws Exception {
                getNullModelRequest().into(imageView);

            }
        });
    }

    @Test(expected = RuntimeException.class)
    public void testThrowsIfIntoTargetCalledOnBackgroundThread() throws InterruptedException {
        final Target target = mock(Target.class);
        testInBackground(new BackgroundUtil.BackgroundTester() {
            @Override
            public void runTest() throws Exception {
                getNullModelRequest().into(target);
            }
        });
    }

    private GenericRequestBuilder getNullModelRequest() {
        Glide glide = mock(Glide.class);
        when(glide.buildImageViewTarget(any(ImageView.class), any(Class.class))).thenReturn(
                mock(Target.class));
        return new GenericRequestBuilder(Robolectric.application, null, null, Object.class, glide, requestTracker,
                mock(Lifecycle.class)).load(null);
    }
}
