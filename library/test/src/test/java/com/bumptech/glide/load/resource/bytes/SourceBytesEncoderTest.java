package com.bumptech.glide.load.resource.bytes;

import static com.google.common.truth.Truth.assertThat;

import com.bumptech.glide.load.EncodeStrategy;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.util.ByteBufferUtil;
import java.io.File;
import java.io.IOException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class SourceBytesEncoderTest {
  @Rule public final TemporaryFolder temporaryFolder = new TemporaryFolder();

  private final SourceBytesEncoder encoder = new SourceBytesEncoder();

  @Test
  public void getEncodeStrategy_returnsSource() {
    assertThat(encoder.getEncodeStrategy(new Options())).isEqualTo(EncodeStrategy.SOURCE);
  }

  @Test
  public void encode_writesBytesUnchanged() throws IOException {
    byte[] data = new byte[] {1, 2, 3, 4, 5};
    File file = temporaryFolder.newFile();

    boolean result =
        encoder.encode(new SourceBytesResource(new SourceBytes(data)), file, new Options());

    assertThat(result).isTrue();
    assertThat(ByteBufferUtil.toBytes(ByteBufferUtil.fromFile(file))).isEqualTo(data);
  }
}
