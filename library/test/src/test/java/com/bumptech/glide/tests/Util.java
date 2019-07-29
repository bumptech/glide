package com.bumptech.glide.tests;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.RETURNS_DEFAULTS;
import static org.mockito.Mockito.mock;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.engine.Resource;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.util.ReflectionHelpers;

// FIXME move to testutil module
public class Util {

  /**
   * Gives the proper generic type to the {@link ArgumentCaptor}. Only useful when the captor's
   * {@code T} is also a generic type. Without this it's really ugly to have a properly typed captor
   * object.
   */
  @SuppressWarnings("unchecked")
  public static <T> ArgumentCaptor<T> cast(ArgumentCaptor<?> captor) {
    return (ArgumentCaptor<T>) captor;
  }

  public static DataSource isADataSource() {
    return isA(DataSource.class);
  }

  public static Context anyContext() {
    return any();
  }

  /**
   * Creates a Mockito argument matcher to be used in verify. It returns a generic typed {@link
   * Resource} to prevent unchecked warnings.
   */
  @SuppressWarnings("unchecked")
  public static <T> Resource<T> anyResource() {
    return any(Resource.class);
  }

  /**
   * Creates a Mockito mock object. It returns a generic typed {@link Resource} to prevent unchecked
   * warnings.
   */
  @SuppressWarnings("unchecked")
  public static <T> Resource<T> mockResource() {
    return mock(Resource.class);
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
      assertEquals(expectedLength, is.read(result));
      assertEquals(-1, is.read());
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

  public static final class WriteDigest implements Answer<Void> {
    private final String toWrite;

    public WriteDigest(String toWrite) {
      this.toWrite = toWrite;
    }

    @Override
    public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
      MessageDigest md = (MessageDigest) invocationOnMock.getArguments()[0];
      md.update(toWrite.getBytes("UTF-8"));
      return null;
    }
  }

  public static final class ReturnsSelfAnswer implements Answer<Object> {

    @Override
    public Object answer(InvocationOnMock invocation) throws Throwable {
      Object mock = invocation.getMock();
      if (invocation.getMethod().getReturnType().isInstance(mock)) {
        return mock;
      } else {
        return RETURNS_DEFAULTS.answer(invocation);
      }
    }
  }

  public static final class CallDataReady<T> implements Answer<Void> {

    private final T data;

    public CallDataReady(T data) {
      this.data = data;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
      DataFetcher.DataCallback<T> callback =
          (DataFetcher.DataCallback<T>) invocationOnMock.getArguments()[1];
      callback.onDataReady(data);
      return null;
    }
  }

  public static final class CreateBitmap implements Answer<Bitmap> {

    @Override
    public Bitmap answer(InvocationOnMock invocation) throws Throwable {
      int width = (Integer) invocation.getArguments()[0];
      int height = (Integer) invocation.getArguments()[1];
      Bitmap.Config config = (Bitmap.Config) invocation.getArguments()[2];
      return Bitmap.createBitmap(width, height, config);
    }
  }
}
