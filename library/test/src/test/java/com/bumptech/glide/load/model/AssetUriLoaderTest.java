package com.bumptech.glide.load.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import android.content.res.AssetManager;
import android.net.Uri;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.util.Preconditions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 18)
public class AssetUriLoaderTest {
  private static final int IMAGE_SIDE = 10;

  @Mock private AssetUriLoader.AssetFetcherFactory<Object> factory;
  @Mock private DataFetcher<Object> fetcher;
  private AssetUriLoader<Object> loader;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    loader = new AssetUriLoader<>(RuntimeEnvironment.application.getAssets(), factory);
  }

  @Test
  public void testHandlesAssetUris() {
    Uri assetUri = Uri.parse("file:///android_asset/assetName");
    when(factory.buildFetcher(any(AssetManager.class), eq("assetName"))).thenReturn(fetcher);
    assertTrue(loader.handles(assetUri));
    assertEquals(
        fetcher,
        Preconditions.checkNotNull(
            loader.buildLoadData(assetUri, IMAGE_SIDE, IMAGE_SIDE, new Options())).fetcher);
  }
}
