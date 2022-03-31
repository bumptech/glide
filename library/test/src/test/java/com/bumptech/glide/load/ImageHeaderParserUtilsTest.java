package com.bumptech.glide.load;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assume.assumeTrue;

import android.content.Context;
import android.os.ParcelFileDescriptor;
import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.bumptech.glide.load.data.ParcelFileDescriptorRewinder;
import com.bumptech.glide.load.engine.bitmap_recycle.ArrayPool;
import com.bumptech.glide.load.engine.bitmap_recycle.LruArrayPool;
import com.bumptech.glide.util.ByteBufferUtil;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ImageHeaderParserUtilsTest {
  private final List<FakeImageHeaderParser> fakeParsers =
      Arrays.asList(new FakeImageHeaderParser(), new FakeImageHeaderParser());
  private List<ImageHeaderParser> parsers;
  private final Context context = ApplicationProvider.getApplicationContext();
  private final byte[] expectedData = new byte[] {0, 1, 2, 3, 4, 5, 6, 7, 8};
  private final LruArrayPool lruArrayPool = new LruArrayPool();

  @Before
  public void setUp() {
    parsers = new ArrayList<ImageHeaderParser>();
    for (FakeImageHeaderParser parser : fakeParsers) {
      parsers.add(parser);
    }
  }

  @Test
  public void getType_withTwoParsers_andStream_rewindsBeforeEachParser() throws IOException {
    ImageHeaderParserUtils.getType(parsers, new ByteArrayInputStream(expectedData), lruArrayPool);

    assertAllParsersReceivedTheSameData();
  }

  @Test
  public void getType_withTwoParsers_andByteBuffer_rewindsBeforeEachParser() throws IOException {
    ImageHeaderParserUtils.getType(parsers, ByteBuffer.wrap(expectedData));

    assertAllParsersReceivedTheSameData();
  }

  @Test
  public void getType_withTwoParsers_andFileDescriptor_rewindsBeforeEachParser()
      throws IOException {
    // This test can't work if file descriptor rewinding isn't supported. Sadly that means this
    // test doesn't work in Robolectric.
    assumeTrue(ParcelFileDescriptorRewinder.isSupported());

    ParcelFileDescriptor fileDescriptor = null;
    try {
      fileDescriptor = asFileDescriptor(expectedData);
      ParcelFileDescriptorRewinder rewinder = new ParcelFileDescriptorRewinder(fileDescriptor);
      ImageHeaderParserUtils.getType(parsers, rewinder, lruArrayPool);
    } finally {
      if (fileDescriptor != null) {
        fileDescriptor.close();
      }
    }

    assertAllParsersReceivedTheSameData();
  }

  @Test
  public void getOrientation_withTwoParsers_andStream_rewindsBeforeEachParser() throws IOException {
    ImageHeaderParserUtils.getOrientation(
        parsers, new ByteArrayInputStream(expectedData), lruArrayPool);

    assertAllParsersReceivedTheSameData();
  }

  @Test
  public void getOrientation_withTwoParsers_andByteBuffer_rewindsBeforeEachParser()
      throws IOException {
    ImageHeaderParserUtils.getOrientation(parsers, ByteBuffer.wrap(expectedData), lruArrayPool);

    assertAllParsersReceivedTheSameData();
  }

  @Test
  public void getOrientation_withTwoParsers_andFileDescriptor_rewindsBeforeEachParser()
      throws IOException {
    // This test can't work if file descriptor rewinding isn't supported. Sadly that means this
    // test doesn't work in Robolectric.
    assumeTrue(ParcelFileDescriptorRewinder.isSupported());
    ParcelFileDescriptor fileDescriptor = null;
    try {
      fileDescriptor = asFileDescriptor(expectedData);
      ParcelFileDescriptorRewinder rewinder = new ParcelFileDescriptorRewinder(fileDescriptor);
      ImageHeaderParserUtils.getOrientation(parsers, rewinder, lruArrayPool);
    } finally {
      if (fileDescriptor != null) {
        fileDescriptor.close();
      }
    }

    assertAllParsersReceivedTheSameData();
  }

  private void assertAllParsersReceivedTheSameData() {
    for (FakeImageHeaderParser parser : fakeParsers) {
      assertThat(parser.data).isNotNull();
      assertThat(parser.data).asList().containsExactlyElementsIn(asList(expectedData)).inOrder();
    }
  }

  private static List<Byte> asList(byte[] data) {
    List<Byte> result = new ArrayList<>();
    for (byte item : data) {
      result.add(item);
    }
    return result;
  }

  private ParcelFileDescriptor asFileDescriptor(byte[] data) throws IOException {
    File file = new File(context.getCacheDir(), "temp");
    OutputStream os = null;
    try {
      os = new FileOutputStream(file);
      os.write(data);
      os.close();
    } finally {
      if (os != null) {
        os.close();
      }
    }
    return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
  }

  private static final class FakeImageHeaderParser implements ImageHeaderParser {

    private byte[] data;

    private void readData(InputStream is) throws IOException {
      readData(ByteBufferUtil.fromStream(is));
    }

    // This is rather roundabout, but it's a simple way of reading the remaining data in the buffer.
    private void readData(ByteBuffer byteBuffer) {

      byte[] data = new byte[byteBuffer.remaining()];
      // A 0 length means we read no data. If we try to pass this to ByteBuffer it will throw. We'd
      // rather not get that obscure exception and instead have an assertion above trigger because
      // we didn't read enough data. So we work around the exception here if we have no data to
      // read.
      if (data.length != 0) {
        byteBuffer.get(data, byteBuffer.position(), byteBuffer.remaining());
      }
      this.data = data;
    }

    @NonNull
    @Override
    public ImageType getType(@NonNull InputStream is) throws IOException {
      readData(is);
      return ImageType.UNKNOWN;
    }

    @NonNull
    @Override
    public ImageType getType(@NonNull ByteBuffer byteBuffer) throws IOException {
      readData(byteBuffer);
      return ImageType.UNKNOWN;
    }

    @Override
    public int getOrientation(@NonNull InputStream is, @NonNull ArrayPool byteArrayPool)
        throws IOException {
      readData(is);
      return ImageHeaderParser.UNKNOWN_ORIENTATION;
    }

    @Override
    public int getOrientation(@NonNull ByteBuffer byteBuffer, @NonNull ArrayPool byteArrayPool)
        throws IOException {
      readData(byteBuffer);
      return ImageHeaderParser.UNKNOWN_ORIENTATION;
    }
  }
}
