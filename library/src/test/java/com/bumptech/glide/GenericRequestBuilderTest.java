package com.bumptech.glide;

import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.manager.RequestManager;
import com.bumptech.glide.provider.LoadProvider;
import com.bumptech.glide.request.Request;
import com.bumptech.glide.request.target.Target;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
@RunWith(RobolectricTestRunner.class)
public class GenericRequestBuilderTest {
    private RequestManager requestManager;

    @Before
    public void setUp() {
        requestManager = mock(RequestManager.class);
    }

    @Test(expected = NullPointerException.class)
    public void testThrowsIfContextIsNull() {
        new GenericRequestBuilder(null,
                new Object(), mock(LoadProvider.class), Object.class, mock(Glide.class), requestManager);
    }

    @Test(expected = NullPointerException.class)
    public void testThrowsIfNonNullModelAndNullLoadProvider() {
        new GenericRequestBuilder(Robolectric.application, new Object(), null, Object.class, mock(Glide.class),
                requestManager);
    }

    @Test
    public void testDoesNotThrowWhenModelAndLoaderNull() {
        new GenericRequestBuilder(Robolectric.application, null, null, Object.class, mock(Glide.class),
                requestManager);
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

    @Test
    public void testAddsNewRequestToRequestManager() {
        getNullModelRequest().into(mock(Target.class));
        verify(requestManager).addRequest(any(Request.class));
    }

    @Test
    public void testRemovesPreviousRequestFromRequestManager() {
        Request previous = mock(Request.class);
        Target target = mock(Target.class);
        when(target.getRequest()).thenReturn(previous);

        getNullModelRequest().into(target);

        verify(requestManager).removeRequest(eq(previous));
    }

    private GenericRequestBuilder getNullModelRequest() {
        return new GenericRequestBuilder(Robolectric.application, null, null, Object.class, mock(Glide.class),
                requestManager);
    }
}
