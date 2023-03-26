package com.bumptech.glide.integration.sqljournaldiskcache;

import androidx.test.core.app.ApplicationProvider;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.junit.rules.ExternalResource;

final class DiskCacheUtils {

  private DiskCacheUtils() {}

  static void writeToFile(File file, String data) {
    byte[] bytes = data.getBytes();
    writeToFile(file, bytes);
  }

  static void writeToFile(File file, byte[] bytes) {
    try (OutputStream os = new FileOutputStream(file)) {
      os.write(bytes);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  static byte[] readFromFile(File file) {
    byte[] result = new byte[(int) file.length()];

    try (FileInputStream is = new FileInputStream(file)) {
      int readSoFar = 0;
      int read;
      while ((read = is.read(result, readSoFar, result.length - readSoFar)) != -1
          && readSoFar < result.length) {
        readSoFar += read;
      }
      if (readSoFar != result.length) {
        throw new IllegalStateException("Failed to read all data from: " + file);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return result;
  }

  private static void deleteRecursively(File file) {
    if (file.isDirectory()) {
      File[] children = file.listFiles();
      if (children != null) {
        for (File f : children) {
          deleteRecursively(f);
        }
      }
    } else {
      if (!file.delete() && file.exists()) {
        throw new IllegalStateException("Failed to delete; " + file);
      }
    }
  }

  static final class DiskCacheDirRule extends ExternalResource {

    private File cacheDir;

    @Override
    protected void before() throws Throwable {
      cacheDir =
          new File(ApplicationProvider.getApplicationContext().getCacheDir(), "test_sql_cache");
      super.before();
    }

    @Override
    protected void after() {
      super.after();
      deleteRecursively(cacheDir);
    }

    void cleanup() {
      deleteRecursively(cacheDir);
    }

    File diskCacheDir() {
      return cacheDir;
    }
  }
}
