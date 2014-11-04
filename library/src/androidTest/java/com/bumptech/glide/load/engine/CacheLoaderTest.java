package com.bumptech.glide.load.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.cache.DiskCache;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.IOException;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, emulateSdk = 18)
public class CacheLoaderTest {
    private DiskCache diskCache;
    private CacheLoader cacheLoader;
    private Key key;
    private ResourceDecoder<File, Object> decoder;
    private Resource<Object> expected;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() {
        diskCache = mock(DiskCache.class);
        cacheLoader = new CacheLoader(diskCache);
        key = mock(Key.class);
        decoder = mock(ResourceDecoder.class);
        expected =  mock(Resource.class);
    }

    @Test
    public void testCacheDecoderIsCalledIfInCache() throws IOException {
        File result = new File("test");
        when(diskCache.get(eq(key))).thenReturn(result);

        int width = 100;
        int height = 101;
        cacheLoader.load(key, decoder, width, height);

        verify(decoder).decode(eq(result), eq(width), eq(height));
    }

    @Test
    public void testReturnsDecodedResourceIfInCache() throws IOException {
        int width = 50;
        int height = 75;
        File file = new File("test");
        when(diskCache.get(eq(key))).thenReturn(file);
        when(decoder.decode(eq(file), eq(width), eq(height))).thenReturn(expected);

        assertEquals(expected, cacheLoader.load(key, decoder, width, height));
    }

    @Test
    public void testReturnsNullIfNotInCache() {
        assertNull(cacheLoader.load(key, decoder, 100, 100));
    }

    @Test
    public void testDiskCacheEntryIsDeletedIfCacheDecoderThrows() throws IOException {
        when(diskCache.get(eq(key))).thenReturn(new File("test"));
        when(decoder.decode(any(File.class), anyInt(), anyInt())).thenThrow(new IOException("Test"));

        cacheLoader.load(key, decoder, 100, 100);

        verify(diskCache).delete(eq(key));
    }

    @Test
    public void testDiskCacheEntryIsDeletedIfDiskCacheContainsIdAndCacheDecoderReturnsNull() throws IOException {
        when(diskCache.get(eq(key))).thenReturn(new File("test"));
        when(decoder.decode(any(File.class), anyInt(), anyInt())).thenReturn(null);

        cacheLoader.load(key, decoder, 100, 101);

        verify(diskCache).delete(eq(key));
    }
}