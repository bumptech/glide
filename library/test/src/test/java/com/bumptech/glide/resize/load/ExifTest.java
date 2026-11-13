package com.bumptech.glide.resize.load;

import static com.bumptech.glide.RobolectricConstants.ROBOLECTRIC_SDK;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;

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
@Config(sdk = ROBOLECTRIC_SDK)
public class ExifTest {

  private ArrayPool byteArrayPool;

  private InputStream open(String imageName) {
    return TestResourceUtil.openResource(getClass(), imageName);
  }

  private void assertOrientation(String filePrefix, int expectedOrientation) {
    InputStream is = null;
    try {
      is = open(filePrefix + "_" + expectedOrientation + ".jpg");
      assertEquals(
          new DefaultImageHeaderParser().getOrientation(is, byteArrayPool), expectedOrientation);
    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      if (is != null) {
        try {
          is.close();
        } catch (IOException e) {
          // Do nothing.
        }
      }
    }
  }

  @Before
  public void setUp() {
    byteArrayPool = new LruArrayPool();
  }

  @Test
  public void issue387() throws IOException {
    InputStream is = TestResourceUtil.openResource(getClass(), "issue387_rotated_jpeg.jpg");
    assertThat(new DefaultImageHeaderParser().getOrientation(is, byteArrayPool)).isEqualTo(6);
  }

  @Test
  public void landscape() throws IOException {
    for (int i = 1; i <= 8; i++) {
      assertOrientation("Landscape", i);
    }
  }

  @Test
  public void portrait() throws IOException {
    for (int i = 1; i <= 8; i++) {
      assertOrientation("Portrait", i);
    }
  }

  @Test
  public void handlesInexactSizesInByteArrayPools() {
    for (int i = 1; i <= 8; i++) {
      byteArrayPool.put(new byte[ArrayPool.STANDARD_BUFFER_SIZE_BYTES]);
      assertOrientation("Portrait", i);
    }
    for (int i = 1; i <= 8; i++) {
      byteArrayPool.put(new byte[ArrayPool.STANDARD_BUFFER_SIZE_BYTES]);
      assertOrientation("Landscape", i);
    }
  }
}
