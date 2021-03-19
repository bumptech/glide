package com.bumptech.glide;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;

import android.content.Context;
import android.graphics.drawable.Drawable;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.engine.cache.DiskCache;
import com.bumptech.glide.load.engine.cache.DiskCache.Factory;
import com.bumptech.glide.load.engine.cache.DiskLruCacheWrapper;
import com.bumptech.glide.test.ResourceIds;
import com.bumptech.glide.test.ResourceIds.raw;
import com.bumptech.glide.testutil.ConcurrencyHelper;
import com.bumptech.glide.testutil.TearDownGlide;
import java.io.File;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

// Tests #2465.
@RunWith(AndroidJUnit4.class)
public class ExternallyClearedDiskCacheTest {
  @Rule public final TearDownGlide tearDownGlide = new TearDownGlide();
  private final ConcurrencyHelper concurrency = new ConcurrencyHelper();
  private Context context;
  private File cacheDir;

  @Before
  public void setUp() {
    context = ApplicationProvider.getApplicationContext();
    cacheDir = context.getCacheDir();
  }

  @After
  public void tearDown() {
    // Force us to wait until Glide's threads shut down.
    Glide.tearDown();
    deleteRecursively(cacheDir);
  }

  @Test
  public void clearDiskCache_afterOpeningDiskCache_andDeleteDirectoryOutsideGlide_doesNotThrow() {
    DiskCache cache = DiskLruCacheWrapper.create(cacheDir, 1024 * 1024);
    cache.get(mock(Key.class));
    deleteRecursively(cacheDir);
    cache.clear();
  }

  @Test
  public void get_afterDeleteDirectoryOutsideGlideAndClose_doesNotThrow() {
    DiskCache cache = DiskLruCacheWrapper.create(cacheDir, 1024 * 1024);
    cache.get(mock(Key.class));
    deleteRecursively(cacheDir);
    cache.clear();

    cache.get(mock(Key.class));
  }

  @Test
  public void loadFromCache_afterDiskCacheDeletedAndCleared_doesNotFail() {
    final DiskCache cache = DiskLruCacheWrapper.create(cacheDir, 1024 * 1024);
    cache.get(mock(Key.class));
    deleteRecursively(cacheDir);
    cache.clear();

    Glide.init(
        context,
        new GlideBuilder()
            .setDiskCache(
                new Factory() {
                  @Override
                  public DiskCache build() {
                    return cache;
                  }
                }));

    Drawable drawable =
        concurrency.get(Glide.with(context).load(ResourceIds.raw.canonical).submit());
    assertThat(drawable).isNotNull();
  }

  @Test
  public void loadFromCache_afterDiskCacheDeleted_doesNotFail() {
    final DiskCache cache = DiskLruCacheWrapper.create(cacheDir, 1024 * 1024);
    cache.get(mock(Key.class));
    deleteRecursively(cacheDir);

    Glide.init(
        context,
        new GlideBuilder()
            .setDiskCache(
                new Factory() {
                  @Override
                  public DiskCache build() {
                    return cache;
                  }
                }));

    Drawable drawable = concurrency.get(Glide.with(context).load(raw.canonical).submit());
    assertThat(drawable).isNotNull();
  }

  private static void deleteRecursively(File file) {
    if (file.isDirectory()) {
      File[] files = file.listFiles();
      if (files != null) {
        for (File f : files) {
          deleteRecursively(f);
        }
      }
    }
    if (!file.delete() && file.exists()) {
      throw new RuntimeException("Failed to delete: " + file);
    }
  }
}
