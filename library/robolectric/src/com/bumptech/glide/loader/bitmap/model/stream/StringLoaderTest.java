package com.bumptech.glide.loader.bitmap.model.stream;

import android.net.Uri;
import com.bumptech.glide.loader.bitmap.model.ModelLoader;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for the {@link StreamStringLoader} class.
 */
@RunWith(RobolectricTestRunner.class)
public class StringLoaderTest {
    // Not a magic number, just an arbitrary non zero value.
    private static final int IMAGE_SIDE = 100;

    private StreamStringLoader stringLoader;
    private ModelLoader<Uri, InputStream> uriLoader;

    @Before
    public void setUp() throws Exception {
        uriLoader = mock(ModelLoader.class);
        stringLoader = new StreamStringLoader(uriLoader);
    }

    @Test
    public void testHandlesPaths() throws IOException {
        File f = Robolectric.application.getCacheDir();
        stringLoader.getResourceFetcher(f.getAbsolutePath(), IMAGE_SIDE, IMAGE_SIDE);

        verify(uriLoader).getResourceFetcher(eq(Uri.fromFile(f)), eq(IMAGE_SIDE), eq(IMAGE_SIDE));
    }

    @Test
    public void testHandlesFileUris() throws IOException {
        File f = Robolectric.application.getCacheDir();
        stringLoader.getResourceFetcher(Uri.fromFile(f)
                .toString(), IMAGE_SIDE, IMAGE_SIDE);

        verify(uriLoader).getResourceFetcher(eq(Uri.fromFile(f)), eq(IMAGE_SIDE), eq(IMAGE_SIDE));
    }

    @Test
    public void testHandlesResourceUris() throws IOException {
        Uri resourceUri = Uri.parse("android.resource://com.bumptech.glide.tests/raw/ic_launcher");
        stringLoader.getResourceFetcher(resourceUri.toString(), IMAGE_SIDE, IMAGE_SIDE);

        verify(uriLoader).getResourceFetcher(eq(resourceUri), eq(IMAGE_SIDE), eq(IMAGE_SIDE));
    }

    @Test
    public void testHandlesHttp() {
        String url = "http://www.google.com";
        stringLoader.getResourceFetcher(url, IMAGE_SIDE, IMAGE_SIDE);

        verify(uriLoader).getResourceFetcher(eq(Uri.parse(url)), eq(IMAGE_SIDE), eq(IMAGE_SIDE));
    }

    @Test
    public void testHandlesHttps() {
        String url = "https://www.google.com";
        stringLoader.getResourceFetcher(url, IMAGE_SIDE, IMAGE_SIDE);

        verify(uriLoader).getResourceFetcher(eq(Uri.parse(url)), eq(IMAGE_SIDE), eq(IMAGE_SIDE));
    }

    @Test
    public void testHandlesContent() {
        String content = "content://com.bumptech.glide";
        stringLoader.getResourceFetcher(content, IMAGE_SIDE, IMAGE_SIDE);

        verify(uriLoader).getResourceFetcher(eq(Uri.parse(content)), eq(IMAGE_SIDE), eq(IMAGE_SIDE));
    }
}
