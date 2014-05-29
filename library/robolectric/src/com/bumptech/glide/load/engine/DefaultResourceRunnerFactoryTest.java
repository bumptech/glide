package com.bumptech.glide.load.engine;

import android.os.Handler;
import com.bumptech.glide.Metadata;
import com.bumptech.glide.request.ResourceCallback;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.cache.DiskCache;
import com.bumptech.glide.load.engine.cache.MemoryCache;
import com.bumptech.glide.load.resource.ResourceFetcher;
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
        MemoryCache memoryCache = mock(MemoryCache.class);
        EngineJobListener listener = mock(EngineJobListener.class);
        DiskCache diskCache = mock(DiskCache.class);
        Handler mainHandler = new Handler();
        Handler bgHandler = mock(Handler.class);
        ExecutorService service = mock(ExecutorService.class);
        ResourceCallback<Object> cb = mock(ResourceCallback.class);
        ResourceReferenceCounter resourceReferenceCounter = mock(ResourceReferenceCounter.class);
        Transformation<Object> transformation = mock(Transformation.class);
        int width = 100;
        int height = 100;

        DefaultResourceRunnerFactory factory = new DefaultResourceRunnerFactory(memoryCache, diskCache,
                mainHandler, service, bgHandler, resourceReferenceCounter);

        ResourceDecoder<InputStream, Object> cacheDecoder = mock(ResourceDecoder.class);
        ResourceFetcher<Object> fetcher = mock(ResourceFetcher.class);
        ResourceDecoder<Object, Object> decoder = mock(ResourceDecoder.class);
        ResourceEncoder<Object> encoder = mock(ResourceEncoder.class);
        Metadata metadata = mock(Metadata.class);

        public ResourceRunner build() {
            return factory.build(mock(Key.class), width, height, cacheDecoder, fetcher, decoder, transformation, encoder, metadata,
                    listener);
        }
    }
}
