package com.bumptech.glide.request.target;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.bumptech.glide.request.Request;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, emulateSdk = 18)
public class PreloadTargetTest {

    @Test
    public void testCallsSizeReadyWithGivenDimensions() {
        int width = 1234;
        int height = 456;
        PreloadTarget<Object> target = PreloadTarget.obtain(width, height);
        SizeReadyCallback cb = mock(SizeReadyCallback.class);
        target.getSize(cb);

        verify(cb).onSizeReady(eq(width), eq(height));
    }

    @Test
    public void testClearsTargetInOnResourceReady() {
        Request request = mock(Request.class);
        PreloadTarget<Object> target = PreloadTarget.obtain(100, 100);
        target.setRequest(request);
        target.onResourceReady(new Object(), null);

        verify(request).clear();
    }
}