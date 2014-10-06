package com.bumptech.glide;

import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;
import com.bumptech.glide.provider.DataLoadProvider;
import com.bumptech.glide.provider.LoadProvider;
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

        Glide glide = mock(Glide.class);
        when(glide.buildTranscoder(any(Class.class), any(Class.class))).thenReturn(mock(ResourceTranscoder.class));
        when(glide.buildDataProvider(any(Class.class), any(Class.class))).thenReturn(mock(DataLoadProvider.class));

        model = "testModel";
        GenericRequestBuilder original = new GenericRequestBuilder(Robolectric.application, model,
                mock(LoadProvider.class), null, glide, null, null);
        request = new GifTypeRequest<String>(original, mock(ModelLoader.class), optionsApplier);
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