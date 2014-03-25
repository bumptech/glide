package com.bumptech.glide.loader.bitmap.model;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.test.ActivityTestCase;
import com.bumptech.glide.loader.bitmap.model.ModelLoader;
import com.bumptech.glide.loader.bitmap.model.stream.StreamResourceLoader;
import com.bumptech.glide.loader.bitmap.model.stream.StreamUriLoader;
import com.bumptech.glide.loader.bitmap.resource.ResourceFetcher;
import com.bumptech.glide.tests.R;
import org.mockito.Mockito;

import java.io.InputStream;
import java.net.URL;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for the {@link StreamResourceLoader} class.
 */
@SuppressWarnings("unchecked")
public class ResourceLoaderTest extends ActivityTestCase {
    public void testCanHandleId() {
        final Context context = getInstrumentation().getContext();
        ModelLoader<Uri, InputStream> streamUriLoader = mock(StreamUriLoader.class);

        StreamResourceLoader resourceLoader = new StreamResourceLoader(context, streamUriLoader);
        resourceLoader.getResourceFetcher(R.raw.ic_launcher, 0, 0);


        Uri contentUri = Uri.parse("android.resource://com.bumptech.glide.tests/" + R.raw.ic_launcher);
        verify(streamUriLoader).getResourceFetcher(eq(contentUri), anyInt(), anyInt());
    }
}
