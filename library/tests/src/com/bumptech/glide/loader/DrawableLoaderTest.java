package com.bumptech.glide.loader;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.test.ActivityTestCase;
import com.bumptech.glide.loader.model.DrawableLoader;
import com.bumptech.glide.loader.stream.StreamLoader;
import com.bumptech.glide.tests.R;

import java.io.InputStream;

/**
 * Created with IntelliJ IDEA.
 * User: sam
 * Date: 7/25/13
 * Time: 11:13 PM
 * To change this template use File | Settings | File Templates.
 */
public class DrawableLoaderTest extends ActivityTestCase {
    private boolean cbCalled;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        cbCalled = false;
    }

    public void testCanHandleId() {
        DrawableLoader drawableLoader = new DrawableLoader(getInstrumentation().getContext());
        StreamLoader streamLoader = drawableLoader.getStreamLoader(R.raw.ic_launcher, 0, 0);
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
