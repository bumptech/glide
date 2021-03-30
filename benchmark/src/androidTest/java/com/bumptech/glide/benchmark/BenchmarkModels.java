package com.bumptech.glide.benchmark;

import android.app.Application;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import androidx.annotation.RawRes;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.bumptech.glide.benchmark.GlideBenchmarkRule.AfterStep;
import com.bumptech.glide.benchmark.GlideBenchmarkRule.BeforeStep;
import com.bumptech.glide.benchmark.data.DataOpener.FileOpener;
import com.google.common.base.Preconditions;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that simulate the complete Glide flow by providing supported Model types directly to Glide.
 *
 * <p>While these benchmarks most directly mimic Glide's current behavior, they have some additional
 * noise due to the large number of steps involved. Other benchmarks in this project may be more
 * targeted and provide more accurate results for smaller tweaks.
 */
@RunWith(AndroidJUnit4.class)
public class BenchmarkModels {
  private final Application app = ApplicationProvider.getApplicationContext();
  private final int smallResourceId = R.raw.small;
  private final int hugeHeaderResourceId = R.raw.huge_header;
  @Rule public final GlideBenchmarkRule glideBenchmarkRule = new GlideBenchmarkRule();

  @Test
  public void smallAsCacheFile() throws Exception {
    benchmarkAsCacheFile(smallResourceId);
  }

  @Test
  public void hugeHeaderAsCacheFile() throws Exception {
    benchmarkAsCacheFile(hugeHeaderResourceId);
  }

  @Test
  public void smallAsResourceId() throws Exception {
    benchmarkModel(smallResourceId);
  }

  @Test
  public void hugeHeaderAsResourceId() throws Exception {
    benchmarkModel(hugeHeaderResourceId);
  }

  @Test
  public void smallAsResourceUri() throws Exception {
    Uri uri = resourceUriFromId(smallResourceId);
    benchmarkModel(uri);
  }

  @Test
  public void hugeHeaderAsResourceUri() throws Exception {
    Uri uri = resourceUriFromId(hugeHeaderResourceId);
    benchmarkModel(uri);
  }

  @Test
  public void smallAsMediaStoreUri() throws Exception {
    benchmarkAsMediaStoreUri(smallResourceId);
  }

  @Test
  public void hugeHeaderAsMediaStoreUri() throws Exception {
    benchmarkAsMediaStoreUri(hugeHeaderResourceId);
  }

  @Test
  public void pixel3aAsMediaStoreUri() throws Exception {
    benchmarkAsMediaStoreUri(R.raw.pixel3a_portrait);
  }

  @Test
  public void pixel3aExifRotatedAsMediaStoreUri() throws Exception {
    benchmarkAsMediaStoreUri(R.raw.pixel3a_exif_rotated);
  }

  @Test
  public void pixel3aMvimgExifRotatedAsMediaStoreUri() throws Exception {
    benchmarkAsMediaStoreUri(R.raw.pixel3a_mvimg_exif_rotated);
  }

  @Test
  public void smallAsMediaStoreFilepath() throws Exception {
    benchmarkAsMediaStoreFilepath(smallResourceId);
  }

  @Test
  public void pixel3aAsMediaStoreFilepath() throws Exception {
    benchmarkAsMediaStoreFilepath(R.raw.pixel3a_portrait);
  }

  @Test
  public void pixel3aExifRotatedAsMediaStoreFilepath() throws Exception {
    benchmarkAsMediaStoreFilepath(R.raw.pixel3a_exif_rotated);
  }

  @Test
  public void pixel3aMvimgExifRotatedAsMediaStoreFilepath() throws Exception {
    benchmarkAsMediaStoreFilepath(R.raw.pixel3a_mvimg_exif_rotated);
  }

  @Test
  public void hugeHeaderAsMediaStoreFilepath() throws Exception {
    benchmarkAsMediaStoreFilepath(hugeHeaderResourceId);
  }

  private Uri resourceUriFromId(@RawRes int resourceId) {
    glideBenchmarkRule.pauseTiming();
    try {
      return new Uri.Builder()
          .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
          .authority(app.getPackageName())
          .appendPath(app.getResources().getResourceTypeName(resourceId))
          .appendPath(app.getResources().getResourceEntryName(resourceId))
          .build();
    } finally {
      glideBenchmarkRule.resumeTiming();
    }
  }

  private Uri mediaStoreUriFromId(@RawRes int resourceId) throws IOException {
    glideBenchmarkRule.pauseTiming();
    try {
      Uri mediaStoreUri =
          app.getContentResolver()
              .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, new ContentValues());
      InputStream is = null;
      OutputStream os = null;
      try {
        is = app.getResources().openRawResource(resourceId);
        os = app.getContentResolver().openOutputStream(mediaStoreUri);
        byte[] buffer = new byte[1024 * 1024];
        int read;
        while ((read = is.read(buffer, /* off= */ 0, buffer.length)) != -1) {
          os.write(buffer, /* off= */ 0, read);
        }
        // Make sure we actually write all of the data or fail by throwing immediately.
        os.close();
        return mediaStoreUri;
      } finally {
        if (is != null) {
          try {
            is.close();
          } catch (IOException e) {
            // Ignored.
          }
        }
        if (os != null) {
          try {
            os.close();
          } catch (IOException e) {
            // Ignored.
          }
        }
      }
    } finally {
      glideBenchmarkRule.resumeTiming();
    }
  }

  private void benchmarkAsMediaStoreUri(@RawRes int resourceId) throws Exception {
    Uri mediaStoreUri = mediaStoreUriFromId(resourceId);
    try {
      benchmarkModel(mediaStoreUri);
    } finally {
      cleanupMediaStoreUri(mediaStoreUri);
    }
  }

  private void cleanupMediaStoreUri(Uri mediaStoreUri) {
    glideBenchmarkRule.pauseTiming();
    int result = app.getContentResolver().delete(mediaStoreUri, /* extras= */ null);
    Preconditions.checkState(result == 1);
    glideBenchmarkRule.resumeTiming();
  }

  private void benchmarkAsMediaStoreFilepath(@RawRes int resourceId) throws Exception {
    Uri mediaStoreUri = mediaStoreUriFromId(resourceId);
    try {
      benchmarkModel(getMediaStoreFilepath(mediaStoreUri));
    } finally {
      cleanupMediaStoreUri(mediaStoreUri);
    }
  }

  private String getMediaStoreFilepath(Uri mediaStoreUri) {
    glideBenchmarkRule.pauseTiming();
    String[] projection = new String[] {MediaStore.Images.Media.DATA};
    Cursor cursor =
        app.getContentResolver()
            .query(
                mediaStoreUri,
                projection,
                /* selection= */ null,
                /* selectionArgs= */ null,
                /* sortOrder= */ null);
    try {
      Preconditions.checkState(cursor.moveToFirst());
      return cursor.getString(0);
    } finally {
      cursor.close();
      glideBenchmarkRule.resumeTiming();
    }
  }

  private void benchmarkAsCacheFile(@RawRes final int resourceId) throws Exception {
    final FileOpener fileOpener = new FileOpener();
    glideBenchmarkRule.runBenchmark(
        new BeforeStep<File>() {
          @Override
          public File act() throws IOException {
            return fileOpener.acquire(resourceId);
          }
        },
        new AfterStep<File>() {
          @Override
          public void act(File beforeData) {
            fileOpener.close(beforeData);
          }
        });
  }

  private void benchmarkModel(final Object model) throws Exception {
    glideBenchmarkRule.runBenchmark(
        new BeforeStep<Object>() {
          @Override
          public Object act() {
            return model;
          }
        },
        new AfterStep<Object>() {
          @Override
          public void act(Object beforeData) {}
        });
  }
}
