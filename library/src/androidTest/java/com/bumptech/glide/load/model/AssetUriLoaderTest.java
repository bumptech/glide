package com.bumptech.glide.load.model;

import android.content.res.AssetManager;
import android.net.Uri;
import com.bumptech.glide.load.data.DataFetcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, emulateSdk = 18)
public class AssetUriLoaderTest {
    private static final int IMAGE_SIDE = 10;

    @Mock AssetUriLoader.AssetFetcherFactory<Object> factory;
    private AssetUriLoader<Object> loader;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        loader = new AssetUriLoader<Object>(Robolectric.application.getAssets(), factory);
    }

    @Test
    public void testHandlesAssetUris() {
        Uri assetUri = Uri.parse("file:///android_asset/assetName");
        DataFetcher<Object> expected = mock(DataFetcher.class);
        when(factory.buildFetcher(any(AssetManager.class), eq("assetName"))).thenReturn(expected);
        assertTrue(loader.handles(assetUri));
        assertEquals(expected, loader.getDataFetcher(assetUri, IMAGE_SIDE, IMAGE_SIDE));
    }
}