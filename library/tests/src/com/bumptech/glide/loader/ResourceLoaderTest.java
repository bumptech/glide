package com.bumptech.glide.loader;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.test.ActivityTestCase;
import com.bumptech.glide.loader.bitmap.model.ModelLoader;
import com.bumptech.glide.loader.bitmap.model.ResourceLoader;
import com.bumptech.glide.loader.bitmap.model.UriLoader;
import com.bumptech.glide.loader.bitmap.resource.ResourceFetcher;
import com.bumptech.glide.tests.R;

import java.io.InputStream;
import java.net.URL;

/**
 * Tests for the {@link ResourceLoader} class.
 */
public class ResourceLoaderTest extends ActivityTestCase {

    public void testCanHandleId() {
        final Context context = getInstrumentation().getContext();
        ResourceLoader resourceLoader = new ResourceLoader(context, new UriLoader(context,
                new ModelLoader<URL, InputStream>() {
            @Override
            public ResourceFetcher<InputStream> getResourceFetcher(URL model, int width, int height) {
                return null;
            }

            @Override
            public String getId(URL model) {
                return null;
            }
        }));
        ResourceFetcher<InputStream> streamLoader = resourceLoader.getResourceFetcher(R.raw.ic_launcher, 0, 0);
        InputStream received = null;
        try {
            received = streamLoader.loadResource();
        } catch (Exception e) {
            assertNull(e);
        }
        assertNotNull(received);
        Bitmap result = BitmapFactory.decodeStream(received);
        assertNotNull(result);
    }
}
