package com.bumptech.glide.benchmark;

import android.Manifest.permission;
import android.app.Application;
import android.content.pm.PackageManager;
import android.net.Uri;
import androidx.benchmark.BenchmarkState;
import androidx.benchmark.junit4.BenchmarkRule;
import androidx.core.content.ContextCompat;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.google.common.base.Preconditions;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Callable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Benchmarks that do not involve Glide just to figure out how media store is behaving on a given
 * device.
 */
@RunWith(AndroidJUnit4.class)
public class BenchmarkMediaStoreData {
  // Pick a media store Uri on the device that you know has lat/lng.
  private static final Uri MEDIA_STORE_URI = Uri.parse("content://media/external/images/media/194");
  private final Application app = ApplicationProvider.getApplicationContext();
  @Rule private final BenchmarkRule benchmarkRule = new BenchmarkRule();

  @Before
  public void setUp() {
    benchmarkRule.getState().pauseTiming();
    Preconditions.checkState(
        ContextCompat.checkSelfPermission(app, permission.ACCESS_MEDIA_LOCATION)
            == PackageManager.PERMISSION_GRANTED);
    benchmarkRule.getState().resumeTiming();
  }

  @Test
  public void readCacheFileFully() throws Exception {
    benchmarkRule.getState().pauseTiming();
    InputStream is = null;
    OutputStream os = null;
    final File file;
    try {
      is = app.getContentResolver().openInputStream(MEDIA_STORE_URI);
      file = File.createTempFile("tempBenchmarkModel", "jpg", app.getCacheDir());
      os = new FileOutputStream(file);

      byte[] buffer = new byte[1024 * 1024];
      int read;
      while ((read = is.read(buffer, 0, buffer.length)) != -1) {
        os.write(buffer, 0, read);
      }
      os.close();
    } finally {
      if (is != null) {
        is.close();
      }
      if (os != null) {
        os.close();
      }
    }
    benchmarkRule.getState().resumeTiming();

    BenchmarkState state = benchmarkRule.getState();
    while (state.keepRunning()) {
      readFully(
          new Callable<InputStream>() {
            @Override
            public InputStream call() throws Exception {
              return new FileInputStream(file);
            }
          });
    }
  }

  @Test
  public void readMediaStoreFileFully() throws Exception {
    BenchmarkState state = benchmarkRule.getState();
    while (state.keepRunning()) {
      readFully(
          new Callable<InputStream>() {
            @Override
            public InputStream call() throws Exception {
              return app.getContentResolver().openInputStream(MEDIA_STORE_URI);
            }
          });
    }
  }

  private void readFully(Callable<InputStream> openInputStream) throws Exception {
    InputStream is = null;
    try {
      is = openInputStream.call();
      byte[] buffer = new byte[1024 * 1024];
      while (is.read(buffer, 0, buffer.length) != -1) {
        // Continue
      }
    } finally {
      if (is != null) {
        is.close();
      }
    }
  }
}
