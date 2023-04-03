package com.bumptech.glide.load.data;

import static com.google.common.truth.Truth.assertThat;

import com.bumptech.glide.load.engine.bitmap_recycle.ArrayPool;
import com.bumptech.glide.load.engine.bitmap_recycle.LruArrayPool;
import com.bumptech.glide.load.resource.bitmap.DefaultImageHeaderParser;
import com.bumptech.glide.testutil.TestResourceUtil;
import java.io.IOException;
import java.io.InputStream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 18)
public class ExifOrientationStreamTest {
  private ArrayPool byteArrayPool;

  private InputStream openOrientationExample(boolean isLandscape, int item) {
    String filePrefix = isLandscape ? "Landscape" : "Portrait";
    return TestResourceUtil.openResource(getClass(), filePrefix + "_" + item + ".jpg");
  }

  @Before
  public void setUp() {
    byteArrayPool = new LruArrayPool();
  }

  @Test
  public void testIncludesGivenExifOrientation() throws IOException {
    for (int i = 0; i < 8; i++) {
      for (int j = 0; j < 8; j++) {
        InputStream toWrap = openOrientationExample(true /*isLandscape*/, j + 1);
        InputStream wrapped = new ExifOrientationStream(toWrap, i);
        DefaultImageHeaderParser parser = new DefaultImageHeaderParser();
        assertThat(parser.getOrientation(wrapped, byteArrayPool)).isEqualTo(i);

        toWrap = openOrientationExample(false /*isLandscape*/, j + 1);
        wrapped = new ExifOrientationStream(toWrap, i);
        assertThat(parser.getOrientation(wrapped, byteArrayPool)).isEqualTo(i);
      }
    }
  }
}
