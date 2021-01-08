package com.bumptech.glide.load.engine.cache;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Mockito.mock;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.signature.ObjectKey;
import com.bumptech.glide.tests.Util;
import java.io.File;
import java.io.IOException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 18)
public class DiskLruCacheWrapperTest {
  private DiskCache cache;
  private byte[] data;
  private ObjectKey key;
  private File dir;

  @Before
  public void setUp() {
    dir = ApplicationProvider.getApplicationContext().getCacheDir();
    cache = DiskLruCacheWrapper.create(dir, 10 * 1024 * 1024);
    key = new ObjectKey("test" + Math.random());
    data = new byte[] {1, 2, 3, 4, 5, 6};
  }

  @After
  public void tearDown() {
    try {
      cache.clear();
    } finally {
      deleteRecursive(dir);
    }
  }

  private static void deleteRecursive(File file) {
    if (file.isDirectory()) {
      File[] files = file.listFiles();
      if (files != null) {
        for (File f : files) {
          deleteRecursive(f);
        }
      }
    }
    // GC before delete() to release files on Windows (https://stackoverflow.com/a/4213208/253468)
    System.gc();
    if (!file.delete() && file.exists()) {
      throw new RuntimeException("Failed to delete: " + file);
    }
  }

  @Test
  public void testCanInsertAndGet() throws IOException {
    cache.put(
        key,
        new DiskCache.Writer() {
          @Override
          public boolean write(@NonNull File file) {
            try {
              Util.writeFile(file, data);
            } catch (IOException e) {
              fail(e.toString());
            }
            return true;
          }
        });

    byte[] received = Util.readFile(cache.get(key), data.length);

    assertArrayEquals(data, received);
  }

  @Test
  public void testDoesNotCommitIfWriterReturnsFalse() {
    cache.put(
        key,
        new DiskCache.Writer() {
          @Override
          public boolean write(@NonNull File file) {
            return false;
          }
        });

    assertNull(cache.get(key));
  }

  @Test
  public void testDoesNotCommitIfWriterWritesButReturnsFalse() {
    cache.put(
        key,
        new DiskCache.Writer() {
          @Override
          public boolean write(@NonNull File file) {
            try {
              Util.writeFile(file, data);
            } catch (IOException e) {
              fail(e.toString());
            }
            return false;
          }
        });

    assertNull(cache.get(key));
  }

  @Test
  public void testEditIsAbortedIfWriterThrows() throws IOException {
    try {
      cache.put(
          key,
          new DiskCache.Writer() {
            @Override
            public boolean write(@NonNull File file) {
              throw new RuntimeException("test");
            }
          });
    } catch (RuntimeException e) {
      // Expected.
    }

    cache.put(
        key,
        new DiskCache.Writer() {
          @Override
          public boolean write(@NonNull File file) {
            try {
              Util.writeFile(file, data);
            } catch (IOException e) {
              fail(e.toString());
            }
            return true;
          }
        });

    byte[] received = Util.readFile(cache.get(key), data.length);

    assertArrayEquals(data, received);
  }

  // Tests #2465.
  @Test
  public void clearDiskCache_afterOpeningDiskCache_andDeleteDirectoryOutsideGlide_doesNotThrow() {
    assumeTrue("A file handle is likely open, so cannot delete dir", !Util.isWindows());
    DiskCache cache = DiskLruCacheWrapper.create(dir, 1024 * 1024);
    cache.get(mock(Key.class));
    deleteRecursive(dir);
    cache.clear();
  }

  // Tests #2465.
  @Test
  public void get_afterDeleteDirectoryOutsideGlideAndClose_doesNotThrow() {
    assumeTrue("A file handle is likely open, so cannot delete dir", !Util.isWindows());
    DiskCache cache = DiskLruCacheWrapper.create(dir, 1024 * 1024);
    cache.get(mock(Key.class));
    deleteRecursive(dir);
    cache.clear();

    cache.get(mock(Key.class));
  }
}
