package com.bumptech.glide.loader.bitmap.model.stream;

import android.net.Uri;
import com.bumptech.glide.loader.bitmap.model.ModelLoader;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import java.io.InputStream;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for the {@link StreamResourceLoader} class.
 */
@RunWith(RobolectricTestRunner.class)
public class ResourceLoaderTest {

    @Test
    public void testCanHandleId() {
        ModelLoader<Uri, InputStream> streamUriLoader = mock(ModelLoader.class);
        when(streamUriLoader.getResourceFetcher(any(Uri.class), anyInt(), anyInt())).thenReturn(null);

        int id = 12345;

        StreamResourceLoader resourceLoader = new StreamResourceLoader(Robolectric.application, streamUriLoader);
        resourceLoader.getResourceFetcher(id, 0, 0);

        Uri contentUri = Uri.parse("android.resource://" + Robolectric.application.getPackageName() + "/" + id);
        verify(streamUriLoader, atLeastOnce()).getResourceFetcher(eq(contentUri), anyInt(), anyInt());
    }

}
