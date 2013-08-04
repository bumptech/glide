package com.bumptech.glide.loader;

import android.net.Uri;
import android.test.AndroidTestCase;
import com.bumptech.glide.loader.model.ModelLoader;
import com.bumptech.glide.loader.model.UriLoader;
import com.bumptech.glide.loader.stream.LocalUriLoader;
import com.bumptech.glide.loader.stream.StreamLoader;

import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * Created with IntelliJ IDEA.
 * User: sam
 * Date: 7/25/13
 * Time: 6:36 PM
 * To change this template use File | Settings | File Templates.
 */
public class UriLoaderTest extends AndroidTestCase {

    private UriLoader loader;
    private StreamLoader urlLoader;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        urlLoader = new StreamLoader() {

            @Override
            public void loadStream(Object t, StreamReadyCallback cb) {
            }

            @Override
            public void cancel() {
            }
        };
        loader = new UriLoader(getContext(), new ModelLoader<URL>() {
            @Override
            public StreamLoader getStreamLoader(URL model, int width, int height) {
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
        StreamLoader streamLoader = loader.getStreamLoader(fileUri, 0, 0);
        assertTrue(streamLoader instanceof LocalUriLoader);
    }

    public void testHandlesResourceUris() throws IOException {
        Uri resourceUri = Uri.parse("android.resource://com.bumptech.glide.tests/raw/ic_launcher");// + R.raw.ic_launcher);
        StreamLoader streamLoader = loader.getStreamLoader(resourceUri, 0, 0);
        assertTrue(streamLoader instanceof LocalUriLoader);
    }

    public void testHandlesContentUris() {
        Uri contentUri = Uri.parse("content://com.bumptech.glide");
        StreamLoader streamLoader = loader.getStreamLoader(contentUri, 0, 0);
        assertTrue(streamLoader instanceof LocalUriLoader);
    }

    public void testHandlesHttpUris() {
        Uri httpUri = Uri.parse("http://www.google.com");
        StreamLoader streamLoader = loader.getStreamLoader(httpUri, 0, 0);
        assertEquals(urlLoader, streamLoader);
    }

    public void testHandlesHttpsUris() {
        Uri httpsUri = Uri.parse("https://www.google.com");
        StreamLoader streamLoader = loader.getStreamLoader(httpsUri, 0, 0);
        assertEquals(urlLoader, streamLoader);
    }
}
