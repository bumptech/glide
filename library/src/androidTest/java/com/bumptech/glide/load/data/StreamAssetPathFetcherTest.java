package com.bumptech.glide.load.data;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.res.AssetManager;

import com.bumptech.glide.Priority;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;
import java.io.InputStream;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, emulateSdk = 18)
public class StreamAssetPathFetcherTest {
    private StreamAssetPathFetcher fetcher;
    private InputStream expected;
    private String assetPath;

    @Before
    public void setUp() throws IOException {
        AssetManager assetManager = mock(AssetManager.class);
        assetPath = "/some/asset/path";
        fetcher = new StreamAssetPathFetcher(assetManager, assetPath);
        expected = mock(InputStream.class);
        when(assetManager.open(eq(assetPath))).thenReturn(expected);
    }

    @Test
    public void testOpensInputStreamForPathWithAssetManager() throws Exception {
        assertEquals(expected, fetcher.loadData(Priority.NORMAL));
    }

    @Test
    public void testClosesOpenedInputStreamOnCleanup() throws Exception {
        fetcher.loadData(Priority.NORMAL);
        fetcher.cleanup();

        verify(expected).close();
    }

    @Test
    public void testReturnsAssetPathAsId() {
        assertEquals(assetPath, fetcher.getId());
    }

    @Test
    public void testDoesNothingOnCleanupIfNoDataLoaded() throws IOException {
        fetcher.cleanup();
        verify(expected, never()).close();
    }

    @Test
    public void testDoesNothingOnCancel() throws Exception {
        fetcher.loadData(Priority.NORMAL);
        fetcher.cancel();
        verify(expected, never()).close();
    }
}