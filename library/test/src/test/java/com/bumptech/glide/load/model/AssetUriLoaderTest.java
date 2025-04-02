package com.bumptech.glide.load.model;

import static com.bumptech.glide.RobolectricConstants.ROBOLECTRIC_SDK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import android.content.res.AssetManager;
import android.net.Uri;
import androidx.test.core.app.ApplicationProvider;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.util.Preconditions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = ROBOLECTRIC_SDK)
public class AssetUriLoaderTest {
  private static final int IMAGE_SIDE = 10;

  @Mock private AssetUriLoader.AssetFetcherFactory<Object> factory;
  @Mock private DataFetcher<Object> fetcher;
  private AssetUriLoader<Object> loader;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    loader = new AssetUriLoader<>(ApplicationProvider.getApplicationContext().getAssets(), factory);
  }

  @Test
  public void testHandlesAssetUris() {
    Uri assetUri = Uri.parse("file:///android_asset/assetName");
    when(factory.buildFetcher(any(AssetManager.class), eq("assetName"))).thenReturn(fetcher);
    assertTrue(loader.handles(assetUri));
    assertEquals(
        fetcher,
        Preconditions.checkNotNull(
                loader.buildLoadData(assetUri, IMAGE_SIDE, IMAGE_SIDE, new Options()))
            .fetcher);
  }
}
