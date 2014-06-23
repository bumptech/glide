package com.bumptech.glide.load.model.stream;

import android.net.Uri;
import com.bumptech.glide.load.model.ModelLoader;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import java.io.InputStream;

import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class MediaStoreStreamLoaderTest {

    @Test
    public void testIncludesRelevantFieldsInId() {
        String mimeType = "image/";
        long dateModified = 12222;
        int orientation = 4;
        Uri uri = Uri.parse("content://fake");
        ModelLoader<Uri, InputStream> wrapped = mock(ModelLoader.class);

        String wrappedId = "test1234";
        when(wrapped.getId(eq(uri))).thenReturn(wrappedId);
        MediaStoreStreamLoader loader = new MediaStoreStreamLoader(Robolectric.application, wrapped,
                mimeType, dateModified, orientation);

        String id = loader.getId(uri);

        assertTrue(id.contains(wrappedId));
        assertTrue(id.contains(String.valueOf(dateModified)));
        assertTrue(id.contains(String.valueOf(orientation)));
        assertTrue(id.contains(mimeType));
    }

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