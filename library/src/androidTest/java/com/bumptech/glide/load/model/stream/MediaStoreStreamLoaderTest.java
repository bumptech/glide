package com.bumptech.glide.load.model.stream;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.net.Uri;

import com.bumptech.glide.load.model.ModelLoader;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.InputStream;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, emulateSdk = 18)
public class MediaStoreStreamLoaderTest {

    @Test
    public void testRetrievesFetcherFromWrappedUriLoader() {
        Uri uri = Uri.parse("content://fake");
        int width = 123;
        int height = 456;
        String mimeType = "video/";
        ModelLoader<Uri, InputStream> wrapped = mock(ModelLoader.class);
        MediaStoreStreamLoader loader = new MediaStoreStreamLoader(Robolectric.application, wrapped);

        loader.getResourceFetcher(uri, width, height);
        verify(wrapped).getResourceFetcher(eq(uri), eq(width), eq(height));
    }
}