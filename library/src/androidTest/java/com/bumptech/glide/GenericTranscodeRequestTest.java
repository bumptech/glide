package com.bumptech.glide;

import static com.bumptech.glide.tests.Util.arg;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bumptech.glide.load.model.ModelLoader;
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

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, emulateSdk = 18, shadows = GlideShadowLooper.class)
public class GenericTranscodeRequestTest {
    private RequestManager.OptionsApplier optionsApplier;
    private GenericTranscodeRequest<String, Object, Object> request;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() {
        optionsApplier = mock(RequestManager.OptionsApplier.class);
        when(optionsApplier.apply(any(GenericRequestBuilder.class))).thenAnswer(arg(0));
        request = new GenericTranscodeRequest<String, Object, Object>(Robolectric.application,
                Glide.get(Robolectric.application), String.class, mock(ModelLoader.class), Object.class,
                Object.class, mock(RequestTracker.class), mock(Lifecycle.class), optionsApplier);
    }

    @After
    public void tearDown() {
        Glide.tearDown();
    }

    @Test
    public void testTranscodeAppliesDefaultOptions() {
        ResourceTranscoder<Object, Object> transcoder = mock(ResourceTranscoder.class);
        GenericRequestBuilder<String, Object, Object, Object> builder = request.transcode(transcoder, Object.class);
        verify(optionsApplier).apply(eq(builder));
    }
}