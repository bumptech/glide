package com.bumptech.glide.load.model;

import static org.junit.Assert.assertEquals;

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
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 18)
public class StreamEncoderTest {
  private StreamEncoder encoder;
  private File file;

  @Before
  public void setUp() {
    encoder = new StreamEncoder(new LruArrayPool());
    file = new File(RuntimeEnvironment.application.getCacheDir(), "test");
  }

  @After
  public void tearDown() {
    file.delete();
  }

  @Test
  public void testWritesDataFromInputStreamToOutputStream() throws IOException {
    String fakeData = "SomeRandomFakeData";
    ByteArrayInputStream is = new ByteArrayInputStream(fakeData.getBytes());
    encoder.encode(is, file, new Options());

    byte[] data = ByteBufferUtil.toBytes(ByteBufferUtil.fromFile(file));

    assertEquals(fakeData, new String(data));
  }
}
