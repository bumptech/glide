package com.bumptech.glide.loader;

import android.net.Uri;
import android.test.AndroidTestCase;
import com.bumptech.glide.loader.bitmap.model.ModelLoader;
import com.bumptech.glide.loader.bitmap.model.stream.StreamUriLoader;
import com.bumptech.glide.loader.bitmap.resource.LocalUriFetcher;
import com.bumptech.glide.loader.bitmap.resource.ResourceFetcher;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Tests for the {@link StreamUriLoader} class.
 */
public class UriLoaderTest extends AndroidTestCase {

    private StreamUriLoader loader;
    private ResourceFetcher<InputStream> urlLoader;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        urlLoader = new ResourceFetcher<InputStream>() {

            @Override
            public InputStream loadResource() throws Exception {
                return null;
            }

            @Override
            public String getId() {
                return null;
            }

            @Override
            public void cancel() {
            }
        };
        loader = new StreamUriLoader(getContext(), new ModelLoader<URL, InputStream>() {
            @Override
            public ResourceFetcher<InputStream> getResourceFetcher(URL model, int width, int height) {
                return urlLoader;
            }

            @Override
            public String getId(URL model) {
                return null;
            }
        });
    }

    public void testHandlesFileUris() throws IOException {
        Uri fileUri = Uri.fromFile(new File("f"));
        ResourceFetcher resourceFetcher = loader.getResourceFetcher(fileUri, 0, 0);
        assertTrue(resourceFetcher instanceof LocalUriFetcher);
    }

    public void testHandlesResourceUris() throws IOException {
        Uri resourceUri = Uri.parse("android.resource://com.bumptech.glide.tests/raw/ic_launcher");
        ResourceFetcher resourceFetcher = loader.getResourceFetcher(resourceUri, 0, 0);
        assertTrue(resourceFetcher instanceof LocalUriFetcher);
    }

    public void testHandlesContentUris() {
        Uri contentUri = Uri.parse("content://com.bumptech.glide");
        ResourceFetcher resourceFetcher = loader.getResourceFetcher(contentUri, 0, 0);
        assertTrue(resourceFetcher instanceof LocalUriFetcher);
    }

    public void testHandlesHttpUris() {
        Uri httpUri = Uri.parse("http://www.google.com");
        ResourceFetcher resourceFetcher = loader.getResourceFetcher(httpUri, 0, 0);
        assertEquals(urlLoader, resourceFetcher);
    }

    public void testHandlesHttpsUris() {
        Uri httpsUri = Uri.parse("https://www.google.com");
        ResourceFetcher resourceFetcher = loader.getResourceFetcher(httpsUri, 0, 0);
        assertEquals(urlLoader, resourceFetcher);
    }
}
