package com.bumptech.glide;

import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.provider.LoadProvider;
import com.bumptech.glide.request.target.Target;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import static org.mockito.Mockito.mock;

@SuppressWarnings("unchecked")
@RunWith(RobolectricTestRunner.class)
public class GenericRequestBuilderTest {

    @Test(expected = NullPointerException.class)
    public void testThrowsIfContextIsNull() {
        new GenericRequestBuilder(null,
                new Object(), mock(LoadProvider.class), Object.class, mock(Glide.class));
    }

    @Test(expected = NullPointerException.class)
    public void testThrowsIfNonNullModelAndNullLoadProvider() {
        new GenericRequestBuilder(Robolectric.application, new Object(), null, Object.class, mock(Glide.class));
    }

    @Test
    public void testDoesNotThrowWhenModelAndLoaderNull() {
        new GenericRequestBuilder(Robolectric.application, null, null, Object.class, mock(Glide.class));
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
    public void testDoesNotThrowWithNullModelWhenSkipDiskCacheCalled() {
        getNullModelRequest().skipDiskCache(true).skipDiskCache(false);
    }

    @Test
    public void testDoesNotThrowWithNullModelWhenRequestIsBuilt() {
        getNullModelRequest().into(mock(Target.class));
    }

    private GenericRequestBuilder getNullModelRequest() {
        return new GenericRequestBuilder(Robolectric.application, null, null, Object.class, mock(Glide.class));
    }
}
