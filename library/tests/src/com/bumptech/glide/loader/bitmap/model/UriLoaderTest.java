package com.bumptech.glide.loader.bitmap.model;

import android.content.Context;
import android.net.Uri;
import android.test.AndroidTestCase;
import com.bumptech.glide.loader.bitmap.model.stream.StreamUriLoader;
import com.bumptech.glide.loader.bitmap.resource.ResourceFetcher;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for the {@link StreamUriLoader} class.
 */
public class UriLoaderTest extends AndroidTestCase {
    // Not a magic number, just arbitrary non zero.
    private static final int IMAGE_SIDE = 120;

    private UriLoader loader;
    private ResourceFetcher<InputStream> localUriFetcher;
    private ModelLoader<URL, InputStream> urlLoader;

    @SuppressWarnings("unchecked")
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        urlLoader = mock(ModelLoader.class);
        localUriFetcher = mock(ResourceFetcher.class);

        loader = new UriLoader<InputStream>(getContext(), urlLoader) {
            @Override
            protected ResourceFetcher<InputStream> getLocalUriFetcher(Context context, Uri uri) {
                return localUriFetcher;
            }
        };
    }

    public void testHandlesFileUris() throws IOException {
        Uri fileUri = Uri.fromFile(new File("f"));
        ResourceFetcher resourceFetcher = loader.getResourceFetcher(fileUri, IMAGE_SIDE, IMAGE_SIDE);
        assertEquals(localUriFetcher, resourceFetcher);
    }

    public void testHandlesResourceUris() throws IOException {
        Uri resourceUri = Uri.parse("android.resource://com.bumptech.glide.tests/raw/ic_launcher");
        ResourceFetcher resourceFetcher = loader.getResourceFetcher(resourceUri, IMAGE_SIDE, IMAGE_SIDE);
        assertEquals(localUriFetcher, resourceFetcher);
    }

    public void testHandlesContentUris() {
        Uri contentUri = Uri.parse("content://com.bumptech.glide");
        ResourceFetcher resourceFetcher = loader.getResourceFetcher(contentUri, IMAGE_SIDE, IMAGE_SIDE);
        assertEquals(localUriFetcher, resourceFetcher);
    }

    public void testHandlesHttpUris() throws MalformedURLException {
        Uri httpUri = Uri.parse("http://www.google.com");
        loader.getResourceFetcher(httpUri, IMAGE_SIDE, IMAGE_SIDE);

        verify(urlLoader).getResourceFetcher(eq(new URL(httpUri.toString())), eq(IMAGE_SIDE), eq(IMAGE_SIDE));
    }

    public void testHandlesHttpsUris() throws MalformedURLException {
        Uri httpsUri = Uri.parse("https://www.google.com");
        loader.getResourceFetcher(httpsUri, IMAGE_SIDE, IMAGE_SIDE);

        verify(urlLoader).getResourceFetcher(eq(new URL(httpsUri.toString())), eq(IMAGE_SIDE), eq(IMAGE_SIDE));
    }
}
