package com.bumptech.glide.load.model.stream;

import static org.junit.Assume.assumeTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.net.Uri;

import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.tests.Util;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Tests for the {@link StreamStringLoader} class.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, emulateSdk = 18)
public class StringLoaderTest {
    // Not a magic number, just an arbitrary non zero value.
    private static final int IMAGE_SIDE = 100;

    private StreamStringLoader stringLoader;
    private ModelLoader<Uri, InputStream> uriLoader;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        uriLoader = mock(ModelLoader.class);
        stringLoader = new StreamStringLoader(uriLoader);
    }

    @Test
    public void testHandlesPaths() throws IOException {
        // TODO on windows it will fail with schema being the drive letter (C:\... -> C)
        assumeTrue(!Util.isWindows());
        File f = Robolectric.application.getCacheDir();
        stringLoader.getResourceFetcher(f.getAbsolutePath(), IMAGE_SIDE, IMAGE_SIDE);

        verify(uriLoader).getResourceFetcher(eq(Uri.fromFile(f)), eq(IMAGE_SIDE), eq(IMAGE_SIDE));
    }

    @Test
    public void testCanHandleComplexFilePaths() {
        assumeTrue(!Util.isWindows());
        String testPath = "/storage/emulated/0/DCIM/Camera/IMG_20140520_100001:nopm:.jpg,mimeType=image/jpeg,"
                + "2448x3264,orientation=0,date=Tue";
        stringLoader.getResourceFetcher(testPath, IMAGE_SIDE, IMAGE_SIDE);

        Uri expected = Uri.fromFile(new File(testPath));
        verify(uriLoader).getResourceFetcher(eq(expected), eq(IMAGE_SIDE), eq(IMAGE_SIDE));
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
