package com.bumptech.glide.loader;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.test.ActivityTestCase;
import com.bumptech.glide.loader.model.ModelLoader;
import com.bumptech.glide.loader.model.ResourceLoader;
import com.bumptech.glide.loader.model.UriLoader;
import com.bumptech.glide.loader.stream.StreamLoader;
import com.bumptech.glide.tests.R;

import java.io.InputStream;
import java.net.URL;

/**
 * Created with IntelliJ IDEA.
 * User: sam
 * Date: 7/25/13
 * Time: 11:13 PM
 * To change this template use File | Settings | File Templates.
 */
public class ResourceLoaderTest extends ActivityTestCase {
    private boolean cbCalled;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        cbCalled = false;
    }

    public void testCanHandleId() {
        final Context context = getInstrumentation().getContext();
        ResourceLoader resourceLoader = new ResourceLoader(context, new UriLoader(context, new ModelLoader<URL>() {
            @Override
            public StreamLoader getStreamLoader(URL model, int width, int height) {
                return null;
            }

            @Override
            public String getId(URL model) {
                return null;
            }
        }));
        StreamLoader streamLoader = resourceLoader.getStreamLoader(R.raw.ic_launcher, 0, 0);
        streamLoader.loadStream(new StreamLoader.StreamReadyCallback() {
            @Override
            public void onStreamReady(InputStream is) {
                cbCalled = true;
                Bitmap result = BitmapFactory.decodeStream(is);
                assertNotNull(result);
            }

            @Override
            public void onException(Exception e) {
                cbCalled = true;
                assertNull(e);
            }
        });
        assertTrue(cbCalled);
    }
}
