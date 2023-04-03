package com.bumptech.glide.load.model;

import static org.junit.Assert.assertEquals;

import androidx.test.core.app.ApplicationProvider;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.engine.bitmap_recycle.LruArrayPool;
import com.bumptech.glide.util.ByteBufferUtil;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 18)
public class StreamEncoderTest {
  private StreamEncoder encoder;
  private File file;

  @Before
  public void setUp() {
    encoder = new StreamEncoder(new LruArrayPool());
    file = new File(ApplicationProvider.getApplicationContext().getCacheDir(), "test");
  }

  @After
  public void tearDown() {
    // GC before delete() to release files on Windows (https://stackoverflow.com/a/4213208/253468)
    System.gc();
    if (!file.delete()) {
      throw new IllegalStateException("Failed to delete: " + file);
    }
  }

  @Test
  public void testWritesDataFromInputStreamToOutputStream() throws IOException {
    String fakeData = "SomeRandomFakeData";
    ByteArrayInputStream is = new ByteArrayInputStream(fakeData.getBytes("UTF-8"));
    encoder.encode(is, file, new Options());

    byte[] data = ByteBufferUtil.toBytes(ByteBufferUtil.fromFile(file));

    assertEquals(fakeData, new String(data, "UTF-8"));
  }
}
