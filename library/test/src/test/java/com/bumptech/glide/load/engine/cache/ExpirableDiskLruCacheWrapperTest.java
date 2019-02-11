package com.bumptech.glide.load.engine.cache;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Mockito.mock;

import android.os.SystemClock;
import android.support.annotation.NonNull;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.signature.ObjectKey;
import com.bumptech.glide.tests.Util;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 18)
public class ExpirableDiskLruCacheWrapperTest {

  private DiskCache cache;
  private byte[] data;
  private ObjectKey key;
  private File dir;
  private long expirationMillis;

  @Before
  public void setUp() {
    expirationMillis = TimeUnit.SECONDS.toMillis(10);
    dir = RuntimeEnvironment.application.getCacheDir();
    cache = ExpirableDiskLruCacheWrapper.create(dir, 10 * 1024 * 1024, expirationMillis);
    key = new ObjectKey("test" + Math.random());
    data = new byte[]{1, 2, 3, 4, 5, 6};
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
    cache.put(key, new DiskCache.Writer() {
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
  public void testExpiredReturnNull() {
    long expirationMillis = 100;
    cache = ExpirableDiskLruCacheWrapper.create(dir, 1024 * 1024, expirationMillis);
    cache.put(key, new DiskCache.Writer() {
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

    // wait until value has expired
    SystemClock.sleep(expirationMillis * 2);

    File file = cache.get(key);

    assertNull(file);
  }

  @Test
  public void testDoesNotCommitIfWriterReturnsFalse() {
    cache.put(key, new DiskCache.Writer() {
      @Override
      public boolean write(@NonNull File file) {
        return false;
      }
    });

    assertNull(cache.get(key));
  }

  @Test
  public void testDoesNotCommitIfWriterWritesButReturnsFalse() {
    cache.put(key, new DiskCache.Writer() {
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
      cache.put(key, new DiskCache.Writer() {
        @Override
        public boolean write(@NonNull File file) {
          throw new RuntimeException("test");
        }
      });
    } catch (RuntimeException e) {
      // Expected.
    }

    cache.put(key, new DiskCache.Writer() {
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
    DiskCache cache = ExpirableDiskLruCacheWrapper.create(dir, 1024 * 1024, expirationMillis);
    cache.get(mock(Key.class));
    deleteRecursive(dir);
    cache.clear();
  }

  // Tests #2465.
  @Test
  public void get_afterDeleteDirectoryOutsideGlideAndClose_doesNotThrow() {
    assumeTrue("A file handle is likely open, so cannot delete dir", !Util.isWindows());
    DiskCache cache = ExpirableDiskLruCacheWrapper.create(dir, 1024 * 1024, expirationMillis);
    cache.get(mock(Key.class));
    deleteRecursive(dir);
    cache.clear();

    cache.get(mock(Key.class));
  }
}
