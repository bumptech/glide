package com.bumptech.glide.resize.load;

import com.bumptech.glide.load.resource.bitmap.ImageHeaderParser;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.IOException;
import java.io.InputStream;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNull;

@RunWith(RobolectricTestRunner.class)
public class ExifTest {

    private InputStream open(String imageName) throws IOException {
        return getClass().getResourceAsStream("exif-orientation-examples/" + imageName);
    }

    private void assertOrientation(String filePrefix, int expectedOrientation) {
        InputStream is = null;
        try {
            is = open(filePrefix + "_" + expectedOrientation + ".jpg");
            assertEquals(new ImageHeaderParser(is).getOrientation(), expectedOrientation);
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

    @Test
    public void testLandscape() throws IOException {
        for (int i = 1; i <= 8; i++) {
            assertOrientation("Landscape", i);
        }
    }

    @Test
    public void testPortrait() throws IOException {
        for (int i = 1; i <= 8; i++) {
            assertOrientation("Portrait", i);
        }
    }
}
