package com.bumptech.glide.load.model.stream;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.net.Uri;

import com.bumptech.glide.load.model.ModelLoader;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.InputStream;

/**
 * Tests for the {@link StreamResourceLoader} class.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, emulateSdk = 18)
public class ResourceLoaderTest {

    @Test
    public void testCanHandleId() {
        ModelLoader<Uri, InputStream> streamUriLoader = mock(ModelLoader.class);
        when(streamUriLoader.getResourceFetcher(any(Uri.class), anyInt(), anyInt())).thenReturn(null);

        int id = android.R.drawable.star_off;

        StreamResourceLoader resourceLoader = new StreamResourceLoader(Robolectric.application, streamUriLoader);
        resourceLoader.getResourceFetcher(id, 0, 0);

        Uri contentUri = Uri.parse("android.resource://android/drawable/star_off");
        verify(streamUriLoader, atLeastOnce()).getResourceFetcher(eq(contentUri), anyInt(), anyInt());
    }
}
