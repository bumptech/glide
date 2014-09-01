package com.bumptech.glide;

import android.graphics.Bitmap;

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

import static com.bumptech.glide.tests.Util.arg;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = GlideShadowLooper.class)
public class BitmapTypeRequestTest {
    private RequestManager.OptionsApplier optionsApplier;
    private String model;
    private BitmapTypeRequest request;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() {
        optionsApplier = mock(RequestManager.OptionsApplier.class);
        when(optionsApplier.apply(anyObject(), any(GenericRequestBuilder.class))).thenAnswer(arg(1));
        model = "testModel";
        request = new BitmapTypeRequest(Robolectric.application, model, mock(ModelLoader.class),
                mock(ModelLoader.class), Glide.get(Robolectric.application), mock(RequestTracker.class),
                mock(Lifecycle.class), optionsApplier);
    }

    @After
    public void tearDown() {
        Glide.tearDown();
    }

    @Test
    public void testAppliesDefaultOptionsOnToBytes() {
        BitmapRequestBuilder builder = request.toBytes();
        verify(optionsApplier).apply(eq(model), eq(builder));
    }

    @Test
    public void testAppliesDefaultOptionsOnToBytesWithArgs() {
        BitmapRequestBuilder builder = request.toBytes(Bitmap.CompressFormat.PNG, 2);
        verify(optionsApplier).apply(eq(model), eq(builder));
    }

    @Test
    public void testAppliesDefaultOptionsOnTranscode() {
        ResourceTranscoder<Bitmap, Object> transcoder = mock(ResourceTranscoder.class);
        BitmapRequestBuilder builder = request.transcode(transcoder, Object.class);
        verify(optionsApplier).apply(eq(model), eq(builder));
    }
}