package com.bumptech.glide.load.model.stream;

import android.net.Uri;
import com.bumptech.glide.load.model.ModelLoader;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import java.io.InputStream;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
public class MediaStoreStreamLoaderTest {

    @Test
    public void testRetrievesFetcherFromWrappedUriLoader() {
        Uri uri = Uri.parse("content://fake");
        int width = 123;
        int height = 456;
        String mimeType = "video/";
        ModelLoader<Uri, InputStream> wrapped = mock(ModelLoader.class);
        MediaStoreStreamLoader loader = new MediaStoreStreamLoader(Robolectric.application, wrapped, mimeType, 1234, 4);

        loader.getResourceFetcher(uri, width, height);
        verify(wrapped).getResourceFetcher(eq(uri), eq(width), eq(height));
    }
}