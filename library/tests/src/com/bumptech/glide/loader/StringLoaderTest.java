package com.bumptech.glide.loader;

import android.net.Uri;
import android.test.AndroidTestCase;
import com.bumptech.glide.loader.model.ModelLoader;
import com.bumptech.glide.loader.model.StringLoader;
import com.bumptech.glide.loader.stream.StreamLoader;

import java.io.File;
import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: sam
 * Date: 7/25/13
 * Time: 5:32 PM
 * To change this template use File | Settings | File Templates.
 */
public class StringLoaderTest extends AndroidTestCase {

    private StringLoader stringLoader;
    private ModelLoader<Uri> uriLoader;
    private Uri uri;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        uri = null;
        uriLoader = new ModelLoader<Uri>() {

            @Override
            public StreamLoader getStreamOpener(Uri model, int width, int height) {
                uri = model;
                return null;
            }

            @Override
            public String getId(Uri model) {
                return null;
            }

            @Override
            public void clear() {
            }
        };

        stringLoader = new StringLoader(uriLoader);
    }

    public void testHandlesPaths() throws IOException {
        File f = getContext().getCacheDir();
        stringLoader.getStreamOpener(f.getAbsolutePath(), 100, 100);
        assertEquals("file", uri.getScheme());
    }

    public void testHandlesFileUris() throws IOException {
        File f = getContext().getCacheDir();
        stringLoader.getStreamOpener(Uri.fromFile(f).toString(), 100, 100);
        assertEquals("file", uri.getScheme());
    }

    public void testHandlesResourceUris() throws IOException {
        Uri resourceUri = Uri.parse("android.resource://com.bumptech.glide.tests/raw/ic_launcher");
        stringLoader.getStreamOpener(resourceUri.toString(), 100, 100);
        assertEquals("android.resource", uri.getScheme());
    }

    public void testHandlesHttp() {
        String url = "http://www.google.com";
        stringLoader.getStreamOpener(url, 100, 100);
        assertEquals("http", uri.getScheme());
    }

    public void testHandlesHttps() {
        String url = "https://www.google.com";
        stringLoader.getStreamOpener(url, 100, 100);
        assertEquals("https", uri.getScheme());
    }

    public void testHandlesContent() {
        String content = "content://com.bumptech.glide";
        stringLoader.getStreamOpener(content, 100, 100);
        assertEquals("content", uri.getScheme());
    }
}
