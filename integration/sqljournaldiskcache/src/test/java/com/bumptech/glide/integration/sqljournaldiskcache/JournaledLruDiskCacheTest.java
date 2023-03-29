package com.bumptech.glide.integration.sqljournaldiskcache;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.os.Looper;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.bumptech.glide.integration.sqljournaldiskcache.DiskCacheUtils.DiskCacheDirRule;
import com.bumptech.glide.util.Preconditions;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class JournaledLruDiskCacheTest {
  private final Context context = ApplicationProvider.getApplicationContext();

  @Rule public final DiskCacheDirRule diskCacheDirRule = new DiskCacheDirRule();

  private final TestClock testClock = new TestClock();
  private JournaledLruDiskCache cache;
  private int size;
  private FileSystem fileSystem;
  private DiskCacheDbHelper dbHelper;
  private File cacheDir;

  @Before
  public void setUp() {
    dbHelper = DiskCacheDbHelper.forTesting(context);

    cacheDir = diskCacheDirRule.diskCacheDir();
    fileSystem = spy(new FileSystem() {});
    size = 1024;
    cache = newCache();
  }

  private JournaledLruDiskCache newCache() {
    return newCache(/* evictionSlopMultiplier= */ 0f);
  }

  private JournaledLruDiskCache newCache(float evictionSlopMultiplier) {
    return new JournaledLruDiskCache(
        cacheDir,
        dbHelper,
        fileSystem,
        size,
        Looper.getMainLooper(),
        evictionSlopMultiplier,
        /* updateModifiedTimeBatchSize= */ 1,
        /* staleEvictionThresholdMs= */ Long.MAX_VALUE,
        testClock::currentTimeMillis);
  }

  @After
  public void tearDown() {
    dbHelper.close();
  }

  @Test
  public void beginPut_createsCanaryFile() {
    cache.beginPut("key");
    assertThat(cacheDir.listFiles()).hasLength(1);
  }

  @Test
  public void beginPut_withExistingFileForKey_returnsNull() {
    String key = "key";
    File file = cache.beginPut(key);
    try {
      DiskCacheUtils.writeToFile(file, "data");
      cache.commitPut(key, file);
    } finally {
      cache.abortPutIfNotCommitted(key, file);
    }
    File secondPutFile = cache.beginPut(key);
    assertThat(secondPutFile).isNull();
  }

  @Test
  public void commitPut_withFailedPreviousWrite_leavesSizeConsistent() {
    String key = "key";

    File temp = cache.beginPut(key);
    try {
      when(fileSystem.rename(temp, new File(cacheDir, key))).thenReturn(false).thenCallRealMethod();
      // Write a file so large it should get evicted immediately
      byte[] bytes = new byte[size * 2];
      DiskCacheUtils.writeToFile(temp, bytes);
      cache.commitPut(key, temp);
    } finally {
      cache.abortPutIfNotCommitted(key, temp);
    }

    // Verify file was evicted and size is 0 since the big file was evicted.
    assertThat(getSize(cacheDir)).isEqualTo(0);
    assertThat(cache.getCurrentSizeBytes()).isEqualTo(0);
  }

  @Test
  public void commitPut_withFailedPreviousWrite_replacesContent() {
    String key = "key";

    File temp = cache.beginPut(key);
    // This is a spy, rename below actually performs the rename (which just renames nothing to
    // nothing in this case), it must come before writeToFile or commitPut.
    when(fileSystem.rename(temp, new File(cacheDir, key))).thenReturn(false).thenCallRealMethod();
    DiskCacheUtils.writeToFile(temp, "first data");
    cache.commitPut(key, temp);

    // If the app crashes prior to abortIfNotCommitted:
    cache = newCache();

    String expectedData = "second data";
    temp = cache.beginPut(key);
    try {
      DiskCacheUtils.writeToFile(temp, expectedData);
      cache.commitPut(key, temp);
    } finally {
      cache.abortPutIfNotCommitted(key, temp);
    }

    assertThat(cache.get(key)).isNotNull();
    assertThat(readFromFile(cache.get(key))).isEqualTo(expectedData);
  }

  @Test
  public void testAbortPutIfNotCommitted_handlesNullFiles() {
    String key = "key";
    cache.beginPut(key);
    cache.abortPutIfNotCommitted(key, null);
  }

  @Test
  public void abortPutIfNotCommitted_decrementsSizeIfRenameToFails() {
    // Write a large File and then fail to rename it so the journal size temporarily doesn't match
    // the file system size. The slop multiplier will then cause the cache to calculate an amount
    // to delete that is more than the number of Files available, unless we've properly accounted
    // for the rename failure.
    cache = newCache(/* evictionSlopMultiplier= */ 0.5f);

    String largeKey = "large";
    File file = cache.beginPut(largeKey);
    try {
      // This is a spy, rename below actually performs the rename (which just renames nothing to
      // nothing in this case), it must come before writeToFile or commitPut.
      when(fileSystem.rename(file, new File(cacheDir, largeKey))).thenReturn(false);
      byte[] bytes = new byte[size - 1];
      DiskCacheUtils.writeToFile(file, bytes);
      cache.commitPut(largeKey, file);
    } finally {
      cache.abortPutIfNotCommitted(largeKey, file);
    }

    String smallKey = "key";
    int totalSmallFiles = 2;
    for (int i = 0; i < totalSmallFiles; i++) {
      String key = smallKey + i;
      File smallFile = cache.beginPut(key);
      try {
        byte[] bytes = new byte[(size / totalSmallFiles) - 1];
        DiskCacheUtils.writeToFile(smallFile, bytes);
        cache.commitPut(key, smallFile);
      } finally {
        cache.abortPutIfNotCommitted(key, smallFile);
      }
    }

    for (int i = 0; i < totalSmallFiles; i++) {
      assertThat(cache.get(smallKey + i)).isNotNull();
    }
  }

  @Test
  public void abortPutIfNotCommitted_decrementsSizeInJournalIfRenameToFails() {
    cache = newCache(/* evictionSlopMultiplier= */ 0.5f);

    String largeKey = "large";
    File file = cache.beginPut(largeKey);
    try {
      // This is a spy, rename below actually performs the rename (which just renames nothing to
      // nothing in this case), it must come before writeToFile or commitPut.
      when(fileSystem.rename(file, new File(cacheDir, largeKey))).thenReturn(false);
      byte[] bytes = new byte[size - 1];
      DiskCacheUtils.writeToFile(file, bytes);
      cache.commitPut(largeKey, file);
    } finally {
      cache.abortPutIfNotCommitted(largeKey, file);
    }

    // Re-open the cache.
    cache = newCache(/* evictionSlopMultiplier= */ 0.5f);

    String smallKey = "key";
    int totalSmallFiles = 2;
    for (int i = 0; i < totalSmallFiles; i++) {
      String key = smallKey + i;
      File smallFile = cache.beginPut(key);
      try {
        byte[] bytes = new byte[(size / totalSmallFiles) - 1];
        DiskCacheUtils.writeToFile(smallFile, bytes);
        cache.commitPut(key, smallFile);
      } finally {
        cache.abortPutIfNotCommitted(key, smallFile);
      }
    }

    for (int i = 0; i < totalSmallFiles; i++) {
      assertThat(cache.get(smallKey + i)).isNotNull();
    }
  }

  @Test(expected = IllegalMonitorStateException.class)
  public void testAbortPutIfNotCommitted_withoutBeginPut_throws() {
    cache.abortPutIfNotCommitted("fakeKey", new File(cacheDir, "fakeFile"));
  }

  @Test
  public void get_afterCommittedPut_returnsFileWithData() {
    String key = "myKey";
    String data = "data";

    File toPut = cache.beginPut(key);
    try {
      DiskCacheUtils.writeToFile(toPut, data);
      cache.commitPut(key, toPut);
    } finally {
      cache.abortPutIfNotCommitted(key, toPut);
    }

    File fromGet = cache.get(key);

    assertThat(readFromFile(fromGet)).isEqualTo(data);
  }

  @Test
  public void get_beforePut_returnsNull() {
    assertThat(cache.get("key")).isNull();
  }

  @Test
  public void get_afterAbortedPut_returnsNull() {
    String key = "key";
    File toPut = cache.beginPut(key);
    try {
      String data = "data";
      DiskCacheUtils.writeToFile(toPut, data);
    } finally {
      cache.abortPutIfNotCommitted(key, toPut);
    }

    File fromGet = cache.get(key);
    assertThat(fromGet).isNull();
  }

  @Test
  public void abortPutIfNotCommitted_whenNotCommitted_discardsData() {
    String key = "key";
    File toPut = cache.beginPut(key);
    try {
      String data = "data";
      DiskCacheUtils.writeToFile(toPut, data);
    } finally {
      cache.abortPutIfNotCommitted(key, toPut);
    }

    assertThat(cacheDir.listFiles()).hasLength(1);
    assertThat(getSize(cacheDir)).isEqualTo(0L);
  }

  @Test
  public void commitPut_runsEvictionIfNecessary() {
    int totalFiles = 5;
    byte[] data = new byte[size / 3];
    for (int i = 0; i < totalFiles; i++) {
      String key = "key" + i;
      File file = cache.beginPut(key);
      try {
        DiskCacheUtils.writeToFile(file, data);
        cache.commitPut(key, file);
      } finally {
        cache.abortPutIfNotCommitted(key, file);
      }
    }

    onIdleWorkerThread();
    assertThat(getSize(cacheDir)).isLessThan((long) size);
  }

  @Test
  public void eviction_removesFirstPutFile() {
    int totalFiles = 3;
    byte[] data = new byte[(size / totalFiles) + 1];
    String keyBase = "key";
    for (int i = 0; i < totalFiles; i++) {
      String key = keyBase + i;
      File file = cache.beginPut(key);
      try {
        DiskCacheUtils.writeToFile(file, data);
        cache.commitPut(key, file);
      } finally {
        cache.abortPutIfNotCommitted(key, file);
      }
      testClock.advance(Duration.ofMillis(1));
    }

    onIdleWorkerThread();

    assertThat(cache.get(keyBase + 0)).isNull();
    assertThat(cache.get(keyBase + 1)).isNotNull();
    assertThat(cache.get(keyBase + 2)).isNotNull();
  }

  // Eviction is triggered by posts.
  private static void onIdleWorkerThread() {
    shadowOf(Looper.getMainLooper()).idle();
  }

  @Test
  public void eviction_withGets_removesLeastRecentlyUsedFile() {
    int totalFiles = 3;
    byte[] data = new byte[(size / totalFiles) + 1];
    String keyBase = "key";
    for (int i = 0; i < totalFiles; i++) {
      String key = keyBase + i;
      File file = cache.beginPut(key);
      try {
        DiskCacheUtils.writeToFile(file, data);
        cache.commitPut(key, file);
      } finally {
        cache.abortPutIfNotCommitted(key, file);
      }

      if (i == 1) {
        testClock.advance(Duration.ofMillis(1));
        cache.get(keyBase + 0);
      }
      testClock.advance(Duration.ofMillis(1));
    }

    onIdleWorkerThread();

    assertThat(cache.get(keyBase + 0)).isNotNull();
    assertThat(cache.get(keyBase + 1)).isNull();
    assertThat(cache.get(keyBase + 2)).isNotNull();
  }

  @Test
  public void eviction_withManyEntries_updatesSizeCorrectly() {
    int numSmallFiles = 3;
    byte[] largeData = new byte[size - 1];
    byte[] smallData = new byte[(size / numSmallFiles) - 1];
    String largeKey = "largeKey";

    for (int i = 0; i < 2; i++) {
      String key = largeKey + i;
      File largeFile = cache.beginPut(key);
      try {
        DiskCacheUtils.writeToFile(largeFile, largeData);
        cache.commitPut(key, largeFile);
      } finally {
        cache.abortPutIfNotCommitted(key, largeFile);
      }
      testClock.advance(Duration.ofMillis(1));
    }

    String smallkey = "smallKey";
    for (int i = 0; i < numSmallFiles; i++) {
      String key = smallkey + i;
      File file = cache.beginPut(key);
      try {
        DiskCacheUtils.writeToFile(file, smallData);
        cache.commitPut(key, file);
      } finally {
        cache.abortPutIfNotCommitted(key, file);
      }
      testClock.advance(Duration.ofMillis(1));
    }

    onIdleWorkerThread();

    for (int i = 0; i < numSmallFiles; i++) {
      assertThat(cache.get(smallkey + i)).isNotNull();
    }
  }

  // The goal here is to ensure our sql batching works as expected. We aim for more than 999 files
  // because sql only allows 999 arguments for a single query.
  @Test
  public void eviction_writeManyFiles_evictsManyEntries() throws IOException {
    String smallKey = "small";
    for (int i = 0; i < 1000; i++) {
      String key = smallKey + i;
      File file = cache.beginPut(key);
      try {
        if (!file.createNewFile()) {
          throw new IllegalStateException("Failed to create: " + file);
        }
        cache.commitPut(key, file);
      } finally {
        cache.abortPutIfNotCommitted(key, file);
      }
      testClock.advance(Duration.ofMillis(1));
    }

    String largeKey = "large";
    File largeFile = cache.beginPut(largeKey);
    try {
      byte[] bytes = new byte[size + 1];
      DiskCacheUtils.writeToFile(largeFile, bytes);
      cache.commitPut(largeKey, largeFile);
    } finally {
      cache.abortPutIfNotCommitted(largeKey, largeFile);
    }

    onIdleWorkerThread();

    assertThat(cacheDir.listFiles()).hasLength(1);
  }

  @Test
  public void delete_missingFile_ignored() {
    cache.delete("fakeKey");
  }

  @Test
  public void delete_removesEntryForKey() {
    String key = "key";
    File temp = cache.beginPut(key);
    try {
      DiskCacheUtils.writeToFile(temp, "data");
      cache.commitPut(key, temp);
    } finally {
      cache.abortPutIfNotCommitted(key, temp);
    }

    assertThat(cache.get(key)).isNotNull();
    assertThat(cacheDir.listFiles()).hasLength(2);

    cache.delete(key);

    assertThat(cache.get(key)).isNull();
    assertThat(cacheDir.listFiles()).hasLength(1);
  }

  @Test
  public void delete_withInProgressWriteForKey_doesNotDeleteKey() {
    String key = "key";
    File file = new File(cacheDir, key);
    when(fileSystem.delete(file)).thenReturn(false);
    when(fileSystem.exists(file)).thenReturn(false).thenReturn(true);

    assertThat(cache.delete(Collections.singletonList(key))).isEmpty();
  }

  @Test
  public void delete_onPreviouslyFailedKey_doesNotDecrementCacheSizeTwice() {
    String key = "key";
    File file = new File(cacheDir, key);
    // first delete attempt, second delete attempt.
    when(fileSystem.delete(file)).thenReturn(false).thenCallRealMethod();

    File temp = cache.beginPut(key);
    try {
      byte[] bytes = new byte[size - 1];
      DiskCacheUtils.writeToFile(temp, bytes);
      cache.commitPut(key, temp);
    } finally {
      cache.abortPutIfNotCommitted(key, temp);
    }

    cache.delete(key);
    cache.delete(key);

    // We should have successfully deleted the file.
    assertThat(cacheDir.listFiles()).hasLength(1);

    String otherKey = "other";
    for (int i = 0; i < 2; i++) {
      String currentKey = otherKey + i;
      temp = cache.beginPut(currentKey);
      try {
        byte[] bytes = new byte[size - 1];
        DiskCacheUtils.writeToFile(temp, bytes);
        cache.commitPut(currentKey, temp);
      } finally {
        cache.abortPutIfNotCommitted(currentKey, file);
      }
    }

    onIdleWorkerThread();
    // Only one File should remain. Two will if we double counted the delete of the single key.
    assertThat(cacheDir.listFiles()).hasLength(2);
  }

  @Test
  public void clear_removesAllEntriesAndFiles() {
    String firstKey = "key1";
    File temp = cache.beginPut(firstKey);
    try {
      DiskCacheUtils.writeToFile(temp, "data1");
      cache.commitPut(firstKey, temp);
    } finally {
      cache.abortPutIfNotCommitted(firstKey, temp);
    }
    testClock.advance(Duration.ofMillis(1));

    String secondKey = "key2";
    temp = cache.beginPut(secondKey);
    try {
      DiskCacheUtils.writeToFile(temp, secondKey);
      cache.commitPut(secondKey, temp);
    } finally {
      cache.abortPutIfNotCommitted(secondKey, temp);
    }

    assertThat(cache.get(firstKey)).isNotNull();
    assertThat(cache.get(secondKey)).isNotNull();
    assertThat(cacheDir.listFiles()).hasLength(3);

    cache.clear();

    assertThat(cache.get(firstKey)).isNull();
    assertThat(cache.get(secondKey)).isNull();
    // Now it should just contain the canary.
    assertThat(cacheDir.listFiles()).hasLength(1);
  }

  @Test
  public void recovery_withPartialWriteAndJournalEntry_deletesTempFileAndDecrementsSize() {
    String successKey = "success";
    File successTemp = cache.beginPut(successKey);
    try {
      byte[] bytes = new byte[size / 2];
      DiskCacheUtils.writeToFile(successTemp, bytes);
      cache.commitPut(successKey, successTemp);
    } finally {
      cache.abortPutIfNotCommitted(successKey, successTemp);
    }
    onIdleWorkerThread();
    Preconditions.checkNotNull(cache.get(successKey));

    String failKey = "fail";
    File failPermanent = new File(cacheDir, failKey);
    File failTemp = cache.beginPut(failKey);
    when(fileSystem.rename(failTemp, failPermanent)).thenReturn(false).thenCallRealMethod();

    // Simulate a crash by failing to calll abortPutIfNotCommitted.
    byte[] bytes1 = new byte[(size / 2) - 1];
    DiskCacheUtils.writeToFile(failTemp, bytes1);
    cache.commitPut(failKey, failTemp);

    // We should have the success permanent file, the failed temp file, and the canary file.
    assertThat(cacheDir.listFiles()).hasLength(3);

    // Re-open the cache.
    cache = newCache();

    String secondSuccessKey = "secondSuccess";
    File secondSuccessTemp = cache.beginPut(secondSuccessKey);
    try {
      byte[] bytes = new byte[(size / 2) - 1];
      DiskCacheUtils.writeToFile(secondSuccessTemp, bytes);
      cache.commitPut(secondSuccessKey, secondSuccessTemp);
    } finally {
      cache.abortPutIfNotCommitted(secondSuccessKey, secondSuccessTemp);
    }

    onIdleWorkerThread();
    assertThat(cache.get(successKey)).isNotNull();
    assertThat(cache.get(failKey)).isNull();
    assertThat(cache.get(secondSuccessKey)).isNotNull();
  }

  @Test
  public void recovery_withPartialWriteAndNoJournalEntry_deletesTempFile() {
    String partialWriteKey = "partialWriteKey";
    File partialWriteTemp = cache.beginPut(partialWriteKey);
    byte[] bytes1 = new byte[size];
    DiskCacheUtils.writeToFile(partialWriteTemp, bytes1);

    cache = newCache();

    // Verify we haven't done unexpected things to the cache size.
    String baseKey = "key";
    for (int i = 0; i < 4; i++) {
      String key = baseKey + i;
      File temp = cache.beginPut(key);
      try {
        byte[] bytes = new byte[(size / 4) + 1];
        DiskCacheUtils.writeToFile(temp, bytes);
        cache.commitPut(key, temp);
      } finally {
        cache.abortPutIfNotCommitted(key, temp);
      }
      testClock.advance(Duration.ofMillis(1));
    }

    onIdleWorkerThread();

    // Canary + 3 smaller files.
    assertThat(cacheDir.listFiles()).hasLength(4);

    for (int i = 0; i < 4; i++) {
      String key = baseKey + i;
      if (i == 0) {
        assertThat(cache.get(key)).isNull();
      } else {
        assertThat(cache.get(key)).isNotNull();
      }
    }
  }

  @Test
  public void recovery_withPendingDeleteForFile_deletesFileAndEntry() {
    String key = "key";
    File permanentFile = new File(cacheDir, key);
    when(fileSystem.delete(permanentFile)).thenReturn(false).thenCallRealMethod();

    File temp = cache.beginPut(key);
    try {
      DiskCacheUtils.writeToFile(temp, "data");
      cache.commitPut(key, temp);
    } finally {
      cache.abortPutIfNotCommitted(key, temp);
    }

    // Failed delete.
    cache.delete(key);

    // Failed delete + canary.
    assertThat(cacheDir.listFiles()).hasLength(2);

    cache = newCache();

    String otherKey = "other";
    temp = cache.beginPut(otherKey);
    try {
      DiskCacheUtils.writeToFile(temp, "otherData");
      cache.commitPut(otherKey, temp);
    } finally {
      cache.abortPutIfNotCommitted(otherKey, temp);
    }

    onIdleWorkerThread();
    assertThat(cache.get(key)).isNull();
    // Canary + second key.
    assertThat(cacheDir.listFiles()).hasLength(2);
  }

  @Test
  public void recovery_withInProgressWrite_doesNotDeleteFile() {
    String key = "key";
    String data = "data";
    File temp = cache.beginPut(key);
    try {
      DiskCacheUtils.writeToFile(temp, data);
      cache.commitPut(key, temp);
    } finally {
      cache.abortPutIfNotCommitted(key, temp);
    }
    // Simulate a concurrent recovery attempt now obtaining the write lock.
    cache.recoverPartialWrite(temp);
    // Make sure that it doesn't delete the fully written file
    File cacheFile = cache.get(key);
    assertThat(cacheFile).isNotNull();
    assertThat(readFromFile(cacheFile)).isEqualTo(data);
  }

  @Test
  public void setMaximumSizeBytes_increaseCacheSize_doesNotEvictEntries() {
    String key = "key";
    File toPut = cache.beginPut(key);

    cache.setMaximumSizeBytes(size * 3);
    try {
      // write a file that exceeds the old maximum
      byte[] bytes = new byte[size * 3];
      DiskCacheUtils.writeToFile(toPut, bytes);
      cache.commitPut(key, toPut);
    } finally {
      cache.abortPutIfNotCommitted(key, toPut);
    }

    assertThat(getSize(cacheDir)).isEqualTo(size * 3);
    assertThat(cache.getCurrentSizeBytes()).isEqualTo(size * 3);
  }

  @Test
  public void setMaximumSizeBytes_increaseCacheSize_evictEntries() {
    String key = "key";
    File toPut = cache.beginPut(key);

    cache.setMaximumSizeBytes(size * 2);
    try {
      // write a file that exceeds the new max
      byte[] bytes = new byte[size * 3];
      DiskCacheUtils.writeToFile(toPut, bytes);
      cache.commitPut(key, toPut);
    } finally {
      cache.abortPutIfNotCommitted(key, toPut);
    }

    onIdleWorkerThread();
    assertThat(getSize(cacheDir)).isEqualTo(0);
    assertThat(cache.getCurrentSizeBytes()).isEqualTo(0);
  }

  @Test
  public void setMaximumSizeBytes_decreaseCacheSize_doesNotEvictEntries() {
    String key = "key";
    File toPut = cache.beginPut(key);
    int tinySize = 20;
    try {
      // write a file that satisfies original and new cache space
      byte[] bytes = new byte[tinySize];
      DiskCacheUtils.writeToFile(toPut, bytes);
      cache.commitPut(key, toPut);
    } finally {
      cache.abortPutIfNotCommitted(key, toPut);
    }

    onIdleWorkerThread();
    assertThat(getSize(cacheDir)).isEqualTo(tinySize);
    assertThat(cache.getCurrentSizeBytes()).isEqualTo(tinySize);

    // shrinking size should not evict
    int newMax = size - 100;
    assertThat(newMax).isLessThan(size);
    cache.setMaximumSizeBytes(newMax);

    onIdleWorkerThread();
    assertThat(getSize(cacheDir)).isAtMost(tinySize);
    assertThat(cache.getCurrentSizeBytes()).isAtMost(tinySize);
  }

  @Test
  public void setMaximumSizeBytes_decreaseCacheSize_evictEntries() {
    String key = "key";
    File toPut = cache.beginPut(key);

    try {
      // write a file that satisfies original cache space
      byte[] bytes = new byte[size];
      DiskCacheUtils.writeToFile(toPut, bytes);
      cache.commitPut(key, toPut);
    } finally {
      cache.abortPutIfNotCommitted(key, toPut);
    }

    onIdleWorkerThread();
    assertThat(getSize(cacheDir)).isEqualTo(size);
    assertThat(cache.getCurrentSizeBytes()).isEqualTo(size);

    // shrinking size should evict cache as needed
    int newMax = size - 100;
    assertThat(newMax).isLessThan(size);
    cache.setMaximumSizeBytes(newMax);

    onIdleWorkerThread();
    assertThat(getSize(cacheDir)).isAtMost(newMax);
    assertThat(cache.getCurrentSizeBytes()).isAtMost(newMax);
  }

  @Test
  public void setMaximumSizeBytes_decreaseCacheSize_evictStaleEntries() {
    String keyStale = "keyStale";
    String keyLru = "keyLru";
    File toPutStale = cache.beginPut(keyStale);
    File toPutLru = cache.beginPut(keyLru);
    int smallSizeBytes = 1;

    try {
      byte[] bytes = new byte[size - smallSizeBytes];
      DiskCacheUtils.writeToFile(toPutStale, bytes);
      cache.commitPut(keyStale, toPutStale);
    } finally {
      cache.abortPutIfNotCommitted(keyStale, toPutStale);
    }

    // make the next entry far ahead in the future
    testClock.set(90);

    try {
      byte[] bytes = new byte[smallSizeBytes];
      DiskCacheUtils.writeToFile(toPutLru, bytes);
      cache.commitPut(keyLru, toPutLru);
    } finally {
      cache.abortPutIfNotCommitted(keyLru, toPutLru);
    }

    onIdleWorkerThread();
    assertThat(getSize(cacheDir)).isEqualTo(size);
    assertThat(cache.getCurrentSizeBytes()).isEqualTo(size);

    // shrinking size should evict cache as needed
    int newMax = size - 100;
    assertThat(newMax).isLessThan(size);
    assertThat(smallSizeBytes).isLessThan(newMax);
    cache.setMaximumSizeBytes(newMax);

    onIdleWorkerThread();
    assertThat(getSize(cacheDir)).isAtMost(smallSizeBytes);
    assertThat(cache.getCurrentSizeBytes()).isAtMost(smallSizeBytes);
  }

  @Test
  public void setMaximumSizeBytes_decreaseCacheSize_evictLruEntries() {
    String keyStale = "keyStale";
    String keyLru = "keyLru";
    File toPutStale = cache.beginPut(keyStale);
    File toPutLru = cache.beginPut(keyLru);
    int smallSizeBytes = 1;

    try {
      byte[] bytes = new byte[smallSizeBytes];
      DiskCacheUtils.writeToFile(toPutStale, bytes);
      cache.commitPut(keyStale, toPutStale);
    } finally {
      cache.abortPutIfNotCommitted(keyStale, toPutStale);
    }

    // make the next entry far ahead in the future
    testClock.set(90);

    try {
      byte[] bytes = new byte[size - smallSizeBytes];
      DiskCacheUtils.writeToFile(toPutLru, bytes);
      cache.commitPut(keyLru, toPutLru);
    } finally {
      cache.abortPutIfNotCommitted(keyLru, toPutLru);
    }

    onIdleWorkerThread();
    assertThat(getSize(cacheDir)).isEqualTo(size);
    assertThat(cache.getCurrentSizeBytes()).isEqualTo(size);

    // shrinking size should evict cache as needed
    int newMax = size - 100;
    assertThat(newMax).isLessThan(size);
    assertThat(smallSizeBytes).isLessThan(newMax);
    cache.setMaximumSizeBytes(newMax);

    onIdleWorkerThread();
    assertThat(getSize(cacheDir)).isAtMost(smallSizeBytes);
    assertThat(cache.getCurrentSizeBytes()).isAtMost(smallSizeBytes);
  }

  private static long getSize(File file) {
    long result = 0;
    if (file.isDirectory()) {
      for (File f : file.listFiles()) {
        result += getSize(f);
      }
    } else {
      result = file.length();
    }
    return result;
  }

  private static String readFromFile(File file) {
    byte[] data = DiskCacheUtils.readFromFile(file);
    return new String(data);
  }
}
