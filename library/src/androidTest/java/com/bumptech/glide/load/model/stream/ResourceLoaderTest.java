package com.bumptech.glide.load.model.stream;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import android.net.Uri;

import com.bumptech.glide.load.model.ModelLoader;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
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

    @Mock ModelLoader<Uri, InputStream> streamUriLoader;
    private StreamResourceLoader resourceLoader;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        resourceLoader = new StreamResourceLoader(Robolectric.application, streamUriLoader);
    }

    @Test
    public void testCanHandleId() {
        int id = android.R.drawable.star_off;
        resourceLoader.getResourceFetcher(id, 0, 0);

        Uri contentUri = Uri.parse("android.resource://android/drawable/star_off");
        verify(streamUriLoader).getResourceFetcher(eq(contentUri), anyInt(), anyInt());
    }

    @Test
    public void testDoesNotThrowOnInvalidOrMissingId() {
        assertThat(resourceLoader.getResourceFetcher(1234, 0, 0)).isNull();
        verify(streamUriLoader, never()).getResourceFetcher(any(Uri.class), anyInt(), anyInt());
    }
}
