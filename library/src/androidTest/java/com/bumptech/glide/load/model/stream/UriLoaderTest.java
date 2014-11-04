package com.bumptech.glide.load.model.stream;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.net.Uri;

import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.UriLoader;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;

/**
 * Tests for the {@link StreamUriLoader} class.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, emulateSdk = 18)
public class UriLoaderTest {
    // Not a magic number, just arbitrary non zero.
    private static final int IMAGE_SIDE = 120;

    private UriLoader loader;
    private DataFetcher<InputStream> localUriFetcher;
    private ModelLoader<GlideUrl, InputStream> urlLoader;
    private DataFetcher<InputStream> assetUriFetcher;

    @SuppressWarnings("uncecked")
    @Before
    public void setUp() throws Exception {
        urlLoader = mock(ModelLoader.class);
        localUriFetcher = mock(DataFetcher.class);
        assetUriFetcher = mock(DataFetcher.class);

        loader = new UriLoader<InputStream>(Robolectric.application, urlLoader) {
            @Override
            protected DataFetcher<InputStream> getLocalUriFetcher(Context context, Uri uri) {
                return localUriFetcher;
            }

            @Override
            protected DataFetcher<InputStream> getAssetPathFetcher(Context context, String path) {
                return assetUriFetcher;
            }
        };
    }

    @Test
    public void testHandlesFileUris() throws IOException {
        Uri fileUri = Uri.fromFile(new File("f"));
        DataFetcher dataFetcher = loader.getResourceFetcher(fileUri, IMAGE_SIDE, IMAGE_SIDE);
        assertEquals(localUriFetcher, dataFetcher);
    }

    @Test
    public void testHandlesResourceUris() throws IOException {
        Uri resourceUri = Uri.parse("android.resource://com.bumptech.glide.tests/raw/ic_launcher");
        DataFetcher dataFetcher = loader.getResourceFetcher(resourceUri, IMAGE_SIDE, IMAGE_SIDE);
        assertEquals(localUriFetcher, dataFetcher);
    }

    @Test
    public void testHandlesContentUris() {
        Uri contentUri = Uri.parse("content://com.bumptech.glide");
        DataFetcher dataFetcher = loader.getResourceFetcher(contentUri, IMAGE_SIDE, IMAGE_SIDE);
        assertEquals(localUriFetcher, dataFetcher);
    }

    @Test
    public void testHandlesAssetUris() {
        Uri assetUri = Uri.parse("file:///android_asset/assetName");
        DataFetcher fetcher = loader.getResourceFetcher(assetUri, IMAGE_SIDE, IMAGE_SIDE);
        assertEquals(assetUriFetcher, fetcher);
    }

    @Test
    public void testHandlesHttpUris() throws MalformedURLException {
        Uri httpUri = Uri.parse("http://www.google.com");
        loader.getResourceFetcher(httpUri, IMAGE_SIDE, IMAGE_SIDE);

        verify(urlLoader).getResourceFetcher(eq(new GlideUrl(httpUri.toString())), eq(IMAGE_SIDE), eq(IMAGE_SIDE));
    }

    @Test
    public void testHandlesHttpsUris() throws MalformedURLException {
        Uri httpsUri = Uri.parse("https://www.google.com");
        loader.getResourceFetcher(httpsUri, IMAGE_SIDE, IMAGE_SIDE);

        verify(urlLoader).getResourceFetcher(eq(new GlideUrl(httpsUri.toString())), eq(IMAGE_SIDE), eq(IMAGE_SIDE));
    }

    // Test for https://github.com/bumptech/glide/issues/71.
    @Test
    public void testHandlesMostlyInvalidHttpUris() {
        Uri mostlyInvalidHttpUri =
                Uri.parse("http://myserver_url.com:80http://myserver_url.com/webapp/images/no_image.png?size=100");

        loader.getResourceFetcher(mostlyInvalidHttpUri, IMAGE_SIDE, IMAGE_SIDE);
        verify(urlLoader).getResourceFetcher(eq(new GlideUrl(mostlyInvalidHttpUri.toString())), eq(IMAGE_SIDE),
                eq(IMAGE_SIDE));
    }
}
