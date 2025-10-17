package com.bumptech.glide.integration.sqljournaldiskcache;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class DiskCacheDbHelperUpgradeTest {
  private final Context context = ApplicationProvider.getApplicationContext();

  @Test
  public void onUpgrade_fromVersionOneToTwo_producesFunctionalTablesAndColumns()
      throws IOException {
    try (DiskCacheDbHelper versionOneHelper =
        new DiskCacheDbHelper(context, /* isInMemory= */ false, /* databaseVersion= */ 1)) {
      versionOneHelper.getWritableDatabase();
    }

    try (DiskCacheDbHelper versionTwoHelper =
        new DiskCacheDbHelper(context, /* isInMemory= */ false, /* databaseVersion= */ 2)) {
      versionTwoHelper.getWritableDatabase();
    }

    ensureWeCanReadFromDiskCache();
  }

  // A poor mans way of ensuring that we can read from the various sqlite tables in the way we
  // expect.
  private void ensureWeCanReadFromDiskCache() throws IOException {
    try (DiskCacheDbHelper diskCacheDbHelper = DiskCacheDbHelper.forProd(context)) {
      JournaledLruDiskCache diskCache =
          new JournaledLruDiskCache(
              context.getCacheDir(),
              diskCacheDbHelper,
              /* maximumSizeBytes= */ Long.MAX_VALUE,
              /* staleEvictionThresholdMs= */ Long.MAX_VALUE,
              new DefaultClock());

      String key = "key";
      File file = diskCache.beginPut(key);
      try {
        try (FileOutputStream os = new FileOutputStream(file)) {
          os.write(1);
        }
        diskCache.commitPut(key, file);
      } finally {
        diskCache.abortPutIfNotCommitted(key, file);
      }

      assertThat(diskCache.get(key)).isNotNull();
    }
  }
}
