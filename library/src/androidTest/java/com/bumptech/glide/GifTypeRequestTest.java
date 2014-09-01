package com.bumptech.glide;

import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;
import com.bumptech.glide.manager.Lifecycle;
import com.bumptech.glide.manager.RequestTracker;
import com.bumptech.glide.tests.GlideShadowLooper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.InputStream;

import static com.bumptech.glide.tests.Util.arg;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = GlideShadowLooper.class)
public class GifTypeRequestTest {
    private RequestManager.OptionsApplier optionsApplier;
    private String model;
    private GifTypeRequest<String> request;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() {
        optionsApplier = mock(RequestManager.OptionsApplier.class);
        when(optionsApplier.apply(anyObject(), any(GenericRequestBuilder.class))).thenAnswer(arg(1));
        model = "testModel";
        request = new GifTypeRequest<String>(Robolectric.application, model, mock(ModelLoader.class),
                Glide.get(Robolectric.application), mock(RequestTracker.class), mock(Lifecycle.class), optionsApplier);
    }

    @After
    public void tearDown() {
        Glide.tearDown();
    }

    @Test
    public void testTranscodeAppliesDefaultOptions() {
        ResourceTranscoder<GifDrawable, GifDrawable> transcoder = mock(ResourceTranscoder.class);
        GenericRequestBuilder<String, InputStream, GifDrawable, GifDrawable> builder = request.transcode(transcoder,
                GifDrawable.class);
        verify(optionsApplier).apply(eq(model), eq(builder));
    }

    @Test
    public void testToBytesApplesDefaultOptions() {
        GenericRequestBuilder<String, InputStream, GifDrawable, byte[]> builder = request.toBytes();
        verify(optionsApplier).apply(eq(model), eq(builder));
    }
}