package com.bumptech.glide.load.engine.cache;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import com.bumptech.glide.signature.ObjectKey;
import com.bumptech.glide.tests.Util;
import java.io.File;
import java.io.IOException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 18)
public class DiskLruCacheWrapperTest {
  private DiskCache cache;
  private byte[] data;
  private ObjectKey key;
  private File dir;

  @Before
  public void setUp() {
    dir = RuntimeEnvironment.application.getCacheDir();
    cache = DiskLruCacheWrapper.create(dir, 10 * 1024 * 1024);
    key = new ObjectKey("test" + Math.random());
    data = new byte[] { 1, 2, 3, 4, 5, 6 };
  }

  @After
  public void tearDown() {
    try {
      cache.clear();
    } finally {
      deleteRecursive(dir);
    }
  }

  private void deleteRecursive(File file) {
    if (!file.isDirectory()) {
      if (!file.delete()) {
        throw new IllegalStateException("Failed to delete: " + file);
      }
      return;
    }

    File[] files = file.listFiles();
    if (files == null) {
      return;
    }

    for (File child : files) {
      deleteRecursive(child);
    }
  }

  @Test
  public void testCanInsertAndGet() throws IOException {
    cache.put(key, new DiskCache.Writer() {
      @Override
      public boolean write(File file) {
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
    cache.put(key, new DiskCache.Writer() {
      @Override
      public boolean write(File file) {
        return false;
      }
    });

    assertNull(cache.get(key));
  }

  @Test
  public void testDoesNotCommitIfWriterWritesButReturnsFalse() {
    cache.put(key, new DiskCache.Writer() {
      @Override
      public boolean write(File file) {
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
        public boolean write(File file) {
          throw new RuntimeException("test");
        }
      });
    } catch (RuntimeException e) {
      // Expected.
    }

    cache.put(key, new DiskCache.Writer() {
      @Override
      public boolean write(File file) {
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
}
