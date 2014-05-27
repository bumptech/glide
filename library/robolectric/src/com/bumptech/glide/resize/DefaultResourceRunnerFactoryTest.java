package com.bumptech.glide.resize;

import android.os.Handler;
import com.bumptech.glide.loader.bitmap.resource.ResourceFetcher;
import com.bumptech.glide.resize.cache.DiskCache;
import com.bumptech.glide.resize.cache.ResourceCache;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.InputStream;
import java.util.concurrent.ExecutorService;

import static junit.framework.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

@RunWith(RobolectricTestRunner.class)
public class DefaultResourceRunnerFactoryTest {
    private static final String ID = "asdfasdf";
    private DefaultFactoryHarness harness;

    @Before
    public void setUp() {
        harness = new DefaultFactoryHarness();
    }

    @Test
    public void testProducesNonNullRunners() {
        assertNotNull(harness.build());
    }

    @SuppressWarnings("unchecked")
    private class DefaultFactoryHarness {
        ResourceCache resourceCache = mock(ResourceCache.class);
        EngineJobListener listener = mock(EngineJobListener.class);
        DiskCache diskCache = mock(DiskCache.class);
        Handler mainHandler = new Handler();
        Handler bgHandler = mock(Handler.class);
        ExecutorService service = mock(ExecutorService.class);
        ResourceCallback<Object> cb = mock(ResourceCallback.class);
        int width = 100;
        int height = 100;

        DefaultResourceRunnerFactory factory = new DefaultResourceRunnerFactory(resourceCache, diskCache,
                mainHandler, service, bgHandler);

        ResourceDecoder<InputStream, Object> cacheDecoder = mock(ResourceDecoder.class);
        ResourceFetcher<Object> fetcher = mock(ResourceFetcher.class);
        ResourceDecoder<Object, Object> decoder = mock(ResourceDecoder.class);
        ResourceEncoder<Object> encoder = mock(ResourceEncoder.class);
        Metadata metadata = mock(Metadata.class);

        public ResourceRunner build() {
            return factory.build(ID, width, height, cacheDecoder, fetcher, decoder, encoder, metadata, listener, cb);
        }
    }
}
