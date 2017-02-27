package com.bumptech.glide.testlib;

import static com.google.common.truth.Truth.assertThat;

import android.graphics.Bitmap;
import android.os.Build;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.util.ReflectionHelpers;

/**
 * Generic test utilities.
 */
public final class Util {

  private Util() {
    // Utility class.
  }

  public static String getExpectedClassId(Class<?> clazz) {
    return clazz.getSimpleName() + "." + clazz.getPackage().getName();
  }

  /**
   * Gives the proper generic type to the {@link ArgumentCaptor}.
   * Only useful when the captor's {@code T} is also a generic type.
   * Without this it's really ugly to have a properly typed captor object.
   */
  @SuppressWarnings("unchecked")
  public static <T> ArgumentCaptor<T> cast(ArgumentCaptor<?> captor) {
    return (ArgumentCaptor<T>) captor;
  }
  public static boolean isWindows() {
    return System.getProperty("os.name").startsWith("Windows");
  }

  public static void writeFile(File file, byte[] data) throws IOException {
    OutputStream out = new FileOutputStream(file);
    try {
      out.write(data);
      out.flush();
      out.close();
    } finally {
      try {
        out.close();
      } catch (IOException ex) {
        // Do nothing.
      }
    }
  }

  public static byte[] readFile(File file, int expectedLength) throws IOException {
    InputStream is = new FileInputStream(file);
    byte[] result = new byte[expectedLength];
    try {
      assertThat(is.read(result)).isEqualTo(expectedLength);
      assertThat(is.read()).isEqualTo(-1);
    } finally {
      try {
        is.close();
      } catch (IOException e) {
        // Do nothing.
      }
    }
    return result;
  }

  public static void setSdkVersionInt(int version) {
    ReflectionHelpers.setStaticField(Build.VERSION.class, "SDK_INT", version);
  }

  /**
   * Writes the given {@link String} to the {@link MessageDigest} it's called with.
   */
  public static class WriteDigest implements Answer<Void> {
    private String toWrite;

    public WriteDigest(String toWrite) {
      this.toWrite = toWrite;
    }

    @Override
    public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
      MessageDigest md = (MessageDigest) invocationOnMock.getArguments()[0];
      md.update(toWrite.getBytes());
      return null;
    }
  }

  /**
   * Returns the called mock.
   */
  public static class ReturnsSelfAnswer implements Answer<Object> {

    public Object answer(InvocationOnMock invocation) throws Throwable {
      Object mock = invocation.getMock();
      if (invocation.getMethod().getReturnType().isInstance(mock)) {
        return mock;
      } else {
        return Mockito.RETURNS_DEFAULTS.answer(invocation);
      }
    }
  }

  /**
   * Creates a {@link Bitmap} with the width, height and config it's called with.
   */
  public static class CreateBitmap implements Answer<Bitmap> {

    @Override
    public Bitmap answer(InvocationOnMock invocation) throws Throwable {
      int width = (Integer) invocation.getArguments()[0];
      int height = (Integer) invocation.getArguments()[1];
      Bitmap.Config config = (Bitmap.Config) invocation.getArguments()[2];
      return Bitmap.createBitmap(width, height, config);
    }
  }
}
