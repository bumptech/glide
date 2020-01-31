package com.bumptech.glide.load.data;

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.bumptech.glide.load.engine.bitmap_recycle.ArrayPool;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * Runs some tests based on a random seed that asserts the output of writing to our buffered stream
 * matches the output of writing to {@link java.io.ByteArrayOutputStream}.
 */
@RunWith(JUnit4.class)
public class BufferedOutputStreamFuzzTest {
  private static final int TESTS = 500;
  private static final int BUFFER_SIZE = 10;
  private static final int WRITES_PER_TEST = 50;
  private static final int MAX_BYTES_PER_WRITE = BUFFER_SIZE * 6;
  private static final Random RANDOM = new Random(-3207167907493985134L);

  @Mock private ArrayPool arrayPool;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    when(arrayPool.get(anyInt(), eq(byte[].class)))
        .thenAnswer(
            new Answer<byte[]>() {
              @Override
              public byte[] answer(InvocationOnMock invocation) throws Throwable {
                int size = (Integer) invocation.getArguments()[0];
                return new byte[size];
              }
            });
  }

  @Test
  public void runFuzzTest() throws IOException {
    for (int i = 0; i < TESTS; i++) {
      runTest(RANDOM);
    }
  }

  private void runTest(Random random) throws IOException {
    List<Write> writes = new ArrayList<>(WRITES_PER_TEST);
    for (int i = 0; i < WRITES_PER_TEST; i++) {
      WriteType writeType = getType(random);
      writes.add(getWrite(random, writeType));
    }

    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

    ByteArrayOutputStream wrapped = new ByteArrayOutputStream();
    BufferedOutputStream bufferedOutputStream =
        new BufferedOutputStream(wrapped, arrayPool, BUFFER_SIZE);

    for (Write write : writes) {
      switch (write.writeType) {
        case BYTE:
          byteArrayOutputStream.write(write.data[0]);
          bufferedOutputStream.write(write.data[0]);
          break;
        case BUFFER:
          byteArrayOutputStream.write(write.data);
          bufferedOutputStream.write(write.data);
          break;
        case OFFSET_BUFFER:
          byteArrayOutputStream.write(write.data, write.offset, write.length);
          bufferedOutputStream.write(write.data, write.offset, write.length);
          break;
        default:
          throw new IllegalArgumentException();
      }
    }

    byte[] fromByteArrayStream = byteArrayOutputStream.toByteArray();
    bufferedOutputStream.close();
    byte[] fromWrappedStream = wrapped.toByteArray();
    if (!Arrays.equals(fromWrappedStream, fromByteArrayStream)) {
      StringBuilder writesBuilder = new StringBuilder();
      for (Write write : writes) {
        writesBuilder.append(write).append("\n");
      }
      fail(
          "Expected: "
              + Arrays.toString(fromByteArrayStream)
              + "\n"
              + "but got: "
              + Arrays.toString(fromWrappedStream)
              + "\n"
              + writesBuilder.toString());
    }
  }

  private Write getWrite(Random random, WriteType type) {
    switch (type) {
      case BYTE:
        return getByteWrite(random);
      case BUFFER:
        return getBufferWrite(random);
      case OFFSET_BUFFER:
        return getOffsetBufferWrite(random);
      default:
        throw new IllegalArgumentException("Unrecognized type: " + type);
    }
  }

  private Write getOffsetBufferWrite(Random random) {
    int dataSize = random.nextInt(MAX_BYTES_PER_WRITE * 2);
    byte[] data = new byte[dataSize];
    int length = dataSize == 0 ? 0 : random.nextInt(dataSize);
    int offset = dataSize - length <= 0 ? 0 : random.nextInt(dataSize - length);
    random.nextBytes(data);
    return new Write(data, length, offset, WriteType.OFFSET_BUFFER);
  }

  private Write getBufferWrite(Random random) {
    byte[] data = new byte[random.nextInt(MAX_BYTES_PER_WRITE)];
    random.nextBytes(data);
    return new Write(data, /*length=*/ data.length, /*offset=*/ 0, WriteType.BUFFER);
  }

  private Write getByteWrite(Random random) {
    byte[] data = new byte[1];
    random.nextBytes(data);
    return new Write(data, /*length=*/ 1, /*offset=*/ 0, WriteType.BYTE);
  }

  private WriteType getType(Random random) {
    return WriteType.values()[random.nextInt(WriteType.values().length)];
  }

  private static final class Write {
    private final byte[] data;
    private final int length;
    private final int offset;
    private final WriteType writeType;

    @Override
    public String toString() {
      return "Write{"
          + "data="
          + Arrays.toString(data)
          + ", length="
          + length
          + ", offset="
          + offset
          + ", writeType="
          + writeType
          + '}';
    }

    Write(byte[] data, int length, int offset, WriteType writeType) {
      this.data = data;
      this.length = length;
      this.offset = offset;
      this.writeType = writeType;
    }
  }

  private enum WriteType {
    BYTE,
    BUFFER,
    OFFSET_BUFFER
  }
}
