package com.bumptech.glide.load.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.engine.bitmap_recycle.LruArrayPool;
import com.bumptech.glide.util.ByteBufferUtil;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Executable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 18)
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

  @Test
  public void testFileNullException() throws IOException {
    String fakeData = "SomeRandomFakeData";
    final ByteArrayInputStream is = new ByteArrayInputStream(fakeData.getBytes("UTF-8"));
    final boolean[] success = {false};

      class Test implements ThrowingRunnable{
        @Override
        public void run() throws Throwable {
          success[0] = encoder.encode(is, null, new Options());
        }
      }

    // Write some data to #file making sure file.delete() executes properly
    encoder.encode(is, file, new Options());


    // Assert a NullPointerException is thrown here.
    assertThrows(NullPointerException.class, new Test());

    // Assert encoding process failed.
    assertFalse(success[0]);
  }

  @Test
  public void encode_withNullOptions_completesSuccessfully() throws IOException {
    // testing the importance of Options argument in #encode() method
    String fakeData = "SomeRandomFakeData";
    ByteArrayInputStream is = new ByteArrayInputStream(fakeData.getBytes("UTF-8"));
    boolean success = false;

    // Pass null as Option argument
    success = encoder.encode(is, file, null);

    // Encoding process successful
    assertTrue(success);

    byte[] data = ByteBufferUtil.toBytes(ByteBufferUtil.fromFile(file));

    assertEquals(fakeData, new String(data, "UTF-8"));
  }
}
