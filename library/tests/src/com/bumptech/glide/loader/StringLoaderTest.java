package com.bumptech.glide.loader;

import android.net.Uri;
import android.test.AndroidTestCase;
import com.bumptech.glide.loader.bitmap.model.ModelLoader;
import com.bumptech.glide.loader.bitmap.model.StringLoader;
import com.bumptech.glide.loader.bitmap.resource.ResourceFetcher;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Tests for the {@link StringLoader} class.
 */
public class StringLoaderTest extends AndroidTestCase {

    private StringLoader stringLoader;
    private Uri uri;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        uri = null;
        ModelLoader<Uri, InputStream> uriLoader = new ModelLoader<Uri, InputStream>() {
            @Override
            public ResourceFetcher<InputStream> getResourceFetcher(Uri model, int width, int height) {
                uri = model;
                return null;
            }

            @Override
            public String getId(Uri model) {
                return null;
            }

        };

        stringLoader = new StringLoader(uriLoader);
    }

    public void testHandlesPaths() throws IOException {
        File f = getContext().getCacheDir();
        stringLoader.getResourceFetcher(f.getAbsolutePath(), 100, 100);
        assertEquals("file", uri.getScheme());
    }

    public void testHandlesFileUris() throws IOException {
        File f = getContext().getCacheDir();
        stringLoader.getResourceFetcher(Uri.fromFile(f)
                .toString(), 100, 100);
        assertEquals("file", uri.getScheme());
    }

    public void testHandlesResourceUris() throws IOException {
        Uri resourceUri = Uri.parse("android.resource://com.bumptech.glide.tests/raw/ic_launcher");
        stringLoader.getResourceFetcher(resourceUri.toString(), 100, 100);
        assertEquals("android.resource", uri.getScheme());
    }

    public void testHandlesHttp() {
        String url = "http://www.google.com";
        stringLoader.getResourceFetcher(url, 100, 100);
        assertEquals("http", uri.getScheme());
    }

    public void testHandlesHttps() {
        String url = "https://www.google.com";
        stringLoader.getResourceFetcher(url, 100, 100);
        assertEquals("https", uri.getScheme());
    }

    public void testHandlesContent() {
        String content = "content://com.bumptech.glide";
        stringLoader.getResourceFetcher(content, 100, 100);
        assertEquals("content", uri.getScheme());
    }
}
