package com.bumptech.glide;

import android.content.res.AssetManager;
import android.test.InstrumentationTestCase;
import com.bumptech.glide.resize.load.ExifOrientationParser;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created with IntelliJ IDEA.
 * User: sam
 * Date: 8/14/13
 * Time: 9:18 AM
 * To change this template use File | Settings | File Templates.
 */
public class ExifTest extends InstrumentationTestCase {
    private AssetManager assets;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        assets = getInstrumentation().getContext().getResources().getAssets();
    }

    private InputStream open(String imageName) throws IOException {
        return assets.open("exif-orientation-examples/" + imageName);
    }

    private void assertOrientation(String filePrefix, int expectedOrientation) {
        InputStream is = null;
        try {
            is = open(filePrefix + "_" + expectedOrientation + ".jpg");
            assertEquals(new ExifOrientationParser(is).getOrientation(), expectedOrientation);
        } catch (IOException e) {
            e.printStackTrace();
            assertNull(e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) { }
            }
        }
    }

    public void testLandscape() throws IOException {
        for (int i = 1; i <= 8; i++) {
            assertOrientation("Landscape", i);
        }
    }

    public void testPortrait() throws IOException {
        for (int i = 1; i <= 8; i++) {
            assertOrientation("Portrait", i);
        }
    }
}
