package com.bumptech.glide.load.engine;

import android.os.Handler;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.Encoder;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.engine.cache.DiskCache;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.File;
import java.util.concurrent.ExecutorService;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

@RunWith(RobolectricTestRunner.class)
public class DefaultResourceRunnerFactoryTest {
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
        EngineJobListener listener = mock(EngineJobListener.class);
        DiskCache diskCache = mock(DiskCache.class);
        Handler mainHandler = new Handler();
        ExecutorService diskCacheService = mock(ExecutorService.class);
        ExecutorService resizeService = mock(ExecutorService.class);
        Transformation<Object> transformation = mock(Transformation.class);
        int width = 100;
        int height = 100;

        DefaultResourceRunnerFactory factory = new DefaultResourceRunnerFactory(diskCache,
                mainHandler, diskCacheService, resizeService);

        ResourceDecoder<File, Object> cacheDecoder = mock(ResourceDecoder.class);
        DataFetcher<Object> fetcher = mock(DataFetcher.class);
        ResourceDecoder<Object, Object> decoder = mock(ResourceDecoder.class);
        ResourceEncoder<Object> encoder = mock(ResourceEncoder.class);
        Priority priority = Priority.LOW;
        boolean isMemoryCacheable;
        DiskCacheStrategy diskCacheStrategy;

        public ResourceRunner build() {
            return factory.build(mock(EngineKey.class), width, height, cacheDecoder, fetcher, mock(Encoder.class),
                    decoder, transformation, encoder, mock(ResourceTranscoder.class), priority, isMemoryCacheable,
                    diskCacheStrategy, listener);
        }
    }
}
