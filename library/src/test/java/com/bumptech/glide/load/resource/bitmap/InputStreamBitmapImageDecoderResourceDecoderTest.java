package com.bumptech.glide.load.resource.bitmap;

import static com.google.common.truth.Truth.assertThat;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.bumptech.glide.load.Options;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public final class InputStreamBitmapImageDecoderResourceDecoderTest {

  private Options options;

  @Before
  public void setUp() {
    options = new Options();
  }

  @Test
  public void decode_withHeapBuffer_readsFullStream() throws IOException {
    InputStreamBitmapImageDecoderResourceDecoder decoder =
        new InputStreamBitmapImageDecoderResourceDecoder(/* useHeapBuffer= */ true);
    byte[] data = new byte[] {1, 2, 3, 4};
    InputStream stream = new ByteArrayInputStream(data);

    try {
      decoder.decode(stream, 100, 100, options);
    } catch (Exception e) {
      // Expecting potential failure due to missing shadows in unit test environment,
      // but the stream should still be read.
    }

    // Verify that the stream was fully read
    assertThat(stream.read()).isEqualTo(-1);
  }

  @Test
  public void decode_withDirectBuffer_readsFullStream() throws IOException {
    InputStreamBitmapImageDecoderResourceDecoder decoder =
        new InputStreamBitmapImageDecoderResourceDecoder(/* useHeapBuffer= */ false);
    byte[] data = new byte[] {1, 2, 3, 4};
    InputStream stream = new ByteArrayInputStream(data);

    try {
      decoder.decode(stream, 100, 100, options);
    } catch (Exception e) {
      // Expecting potential failure due to missing shadows in unit test environment.
    }

    // Verify that the stream was fully read
    assertThat(stream.read()).isEqualTo(-1);
  }
}
