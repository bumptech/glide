package com.bumptech.glide.test;

import android.content.Context;
import android.content.res.Resources;
import androidx.annotation.RawRes;
import androidx.test.core.app.ApplicationProvider;
import com.google.common.io.ByteStreams;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.rules.ExternalResource;

/** Converts raw resources into specific model types (Uris, Files, byte arrays etc). */
public final class ModelGeneratorRule extends ExternalResource {
  private static final String TEMP_FOLDER_NAME = "model_generator_rule_cache";

  private final Context context = ApplicationProvider.getApplicationContext();
  private final AtomicInteger fileNameCounter = new AtomicInteger();

  private File getTempDir() {
    File tempDir = new File(context.getCacheDir(), TEMP_FOLDER_NAME);
    if (!tempDir.mkdirs() && (!tempDir.exists() || !tempDir.isDirectory())) {
      throw new IllegalStateException("Failed to mkdirs for: " + tempDir);
    }
    return tempDir;
  }

  private File nextTempFile() {
    String name = "model_generator" + fileNameCounter.getAndIncrement();
    return new File(getTempDir(), name);
  }

  public File asFile(@RawRes int resourceId) throws IOException {
    return writeToFile(resourceId);
  }

  public byte[] asByteArray(@RawRes int resourceId) throws IOException {
    Resources resources = context.getResources();
    InputStream is = resources.openRawResource(resourceId);
    return ByteStreams.toByteArray(is);
  }

  private File writeToFile(@RawRes int resourceId) throws IOException {
    byte[] data = asByteArray(resourceId);
    File result = nextTempFile();
    try (OutputStream os = new FileOutputStream(result)) {
      os.write(data);
    }
    return result;
  }

  @Override
  protected void after() {
    super.after();
    cleanupTempDir();
  }

  private void cleanupTempDir() {
    File tempDir = getTempDir();
    File[] children = tempDir.listFiles();
    if (children != null) {
      for (File child : children) {
        if (child.isDirectory()) {
          throw new IllegalStateException("Expected a file, but was a directory: " + child);
        }
        if (!child.delete()) {
          throw new IllegalStateException("Failed to delete: " + child);
        }
      }
    }
    if (!tempDir.delete()) {
      throw new IllegalStateException("Failed to delete temp dir: " + tempDir);
    }
  }
}
