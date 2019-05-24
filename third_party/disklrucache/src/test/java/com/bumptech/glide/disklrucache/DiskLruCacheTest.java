/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bumptech.glide.disklrucache;

import static com.bumptech.glide.disklrucache.DiskLruCache.JOURNAL_FILE;
import static com.bumptech.glide.disklrucache.DiskLruCache.JOURNAL_FILE_BACKUP;
import static com.bumptech.glide.disklrucache.DiskLruCache.MAGIC;
import static com.bumptech.glide.disklrucache.DiskLruCache.VERSION_1;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assume.assumeThat;

import org.apache.commons.io.FileUtils;
import org.hamcrest.core.StringStartsWith;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RunWith(JUnit4.class)
public final class DiskLruCacheTest {
  private final int appVersion = 100;
  private File cacheDir;
  private File journalFile;
  private File journalBkpFile;
  private DiskLruCache cache;

  @Rule public TemporaryFolder tempDir = new TemporaryFolder();

  @BeforeClass
  public static void setUpClass() {
    assumeThat(System.getProperty("os.name"), not(StringStartsWith.startsWith("Windows")));
  }

  @Before public void setUp() throws Exception {
    cacheDir = tempDir.newFolder("DiskLruCacheTest");
    journalFile = new File(cacheDir, JOURNAL_FILE);
    journalBkpFile = new File(cacheDir, JOURNAL_FILE_BACKUP);
    for (File file : cacheDir.listFiles()) {
      file.delete();
    }
    cache = DiskLruCache.open(cacheDir, appVersion, 2, Integer.MAX_VALUE);
  }

  @After public void tearDown() throws Exception {
    cache.close();
  }

  @Test public void emptyCache() throws Exception {
    cache.close();
    assertJournalEquals();
  }

  @Test public void writeAndReadEntry() throws Exception {
    DiskLruCache.Editor creator = cache.edit("k1");
    creator.set(0, "ABC");
    creator.set(1, "DE");
    assertThat(creator.getString(0)).isNull();
    assertThat(creator.getString(1)).isNull();
    creator.commit();

    DiskLruCache.Value value = cache.get("k1");
    assertThat(value.getString(0)).isEqualTo("ABC");
    assertThat(value.getLength(0)).isEqualTo(3);
    assertThat(value.getString(1)).isEqualTo("DE");
    assertThat(value.getLength(1)).isEqualTo(2);
  }

  @Test public void readAndWriteEntryAcrossCacheOpenAndClose() throws Exception {
    DiskLruCache.Editor creator = cache.edit("k1");
    creator.set(0, "A");
    creator.set(1, "B");
    creator.commit();
    cache.close();

    cache = DiskLruCache.open(cacheDir, appVersion, 2, Integer.MAX_VALUE);
    DiskLruCache.Value value = cache.get("k1");
    assertThat(value.getString(0)).isEqualTo("A");
    assertThat(value.getLength(0)).isEqualTo(1);
    assertThat(value.getString(1)).isEqualTo("B");
    assertThat(value.getLength(1)).isEqualTo(1);
  }

  @Test public void readAndWriteEntryWithoutProperClose() throws Exception {
    DiskLruCache.Editor creator = cache.edit("k1");
    creator.set(0, "A");
    creator.set(1, "B");
    creator.commit();

    // Simulate a dirty close of 'cache' by opening the cache directory again.
    DiskLruCache cache2 = DiskLruCache.open(cacheDir, appVersion, 2, Integer.MAX_VALUE);
    DiskLruCache.Value value = cache2.get("k1");
    assertThat(value.getString(0)).isEqualTo("A");
    assertThat(value.getLength(0)).isEqualTo(1);
    assertThat(value.getString(1)).isEqualTo("B");
    assertThat(value.getLength(1)).isEqualTo(1);
    cache2.close();
  }

  @Test public void journalWithEditAndPublish() throws Exception {
    DiskLruCache.Editor creator = cache.edit("k1");
    assertJournalEquals("DIRTY k1"); // DIRTY must always be flushed.
    creator.set(0, "AB");
    creator.set(1, "C");
    creator.commit();
    cache.close();
    assertJournalEquals("DIRTY k1", "CLEAN k1 2 1");
  }

  @Test public void revertedNewFileIsRemoveInJournal() throws Exception {
    DiskLruCache.Editor creator = cache.edit("k1");
    assertJournalEquals("DIRTY k1"); // DIRTY must always be flushed.
    creator.set(0, "AB");
    creator.set(1, "C");
    creator.abort();
    cache.close();
    assertJournalEquals("DIRTY k1", "REMOVE k1");
  }

  @Test public void unterminatedEditIsRevertedOnClose() throws Exception {
    cache.edit("k1");
    cache.close();
    assertJournalEquals("DIRTY k1", "REMOVE k1");
  }

  @Test public void journalDoesNotIncludeReadOfYetUnpublishedValue() throws Exception {
    DiskLruCache.Editor creator = cache.edit("k1");
    assertThat(cache.get("k1")).isNull();
    creator.set(0, "A");
    creator.set(1, "BC");
    creator.commit();
    cache.close();
    assertJournalEquals("DIRTY k1", "CLEAN k1 1 2");
  }

  @Test public void journalWithEditAndPublishAndRead() throws Exception {
    DiskLruCache.Editor k1Creator = cache.edit("k1");
    k1Creator.set(0, "AB");
    k1Creator.set(1, "C");
    k1Creator.commit();
    DiskLruCache.Editor k2Creator = cache.edit("k2");
    k2Creator.set(0, "DEF");
    k2Creator.set(1, "G");
    k2Creator.commit();
    DiskLruCache.Value k1Value = cache.get("k1");
    cache.close();
    assertJournalEquals("DIRTY k1", "CLEAN k1 2 1", "DIRTY k2", "CLEAN k2 3 1", "READ k1");
  }

  @Test public void cannotOperateOnEditAfterPublish() throws Exception {
    DiskLruCache.Editor editor = cache.edit("k1");
    editor.set(0, "A");
    editor.set(1, "B");
    editor.commit();
    assertInoperable(editor);
  }

  @Test public void cannotOperateOnEditAfterRevert() throws Exception {
    DiskLruCache.Editor editor = cache.edit("k1");
    editor.set(0, "A");
    editor.set(1, "B");
    editor.abort();
    assertInoperable(editor);
  }

  @Test public void explicitRemoveAppliedToDiskImmediately() throws Exception {
    DiskLruCache.Editor editor = cache.edit("k1");
    editor.set(0, "ABC");
    editor.set(1, "B");
    editor.commit();
    File k1 = getCleanFile("k1", 0);
    assertThat(readFile(k1)).isEqualTo("ABC");
    cache.remove("k1");
    assertThat(k1.exists()).isFalse();
  }

  @Test public void openWithDirtyKeyDeletesAllFilesForThatKey() throws Exception {
    cache.close();
    File cleanFile0 = getCleanFile("k1", 0);
    File cleanFile1 = getCleanFile("k1", 1);
    File dirtyFile0 = getDirtyFile("k1", 0);
    File dirtyFile1 = getDirtyFile("k1", 1);
    writeFile(cleanFile0, "A");
    writeFile(cleanFile1, "B");
    writeFile(dirtyFile0, "C");
    writeFile(dirtyFile1, "D");
    createJournal("CLEAN k1 1 1", "DIRTY   k1");
    cache = DiskLruCache.open(cacheDir, appVersion, 2, Integer.MAX_VALUE);
    assertThat(cleanFile0.exists()).isFalse();
    assertThat(cleanFile1.exists()).isFalse();
    assertThat(dirtyFile0.exists()).isFalse();
    assertThat(dirtyFile1.exists()).isFalse();
    assertThat(cache.get("k1")).isNull();
  }

  @Test public void openWithInvalidVersionClearsDirectory() throws Exception {
    cache.close();
    generateSomeGarbageFiles();
    createJournalWithHeader(MAGIC, "0", "100", "2", "");
    cache = DiskLruCache.open(cacheDir, appVersion, 2, Integer.MAX_VALUE);
    assertGarbageFilesAllDeleted();
  }

  @Test public void openWithInvalidAppVersionClearsDirectory() throws Exception {
    cache.close();
    generateSomeGarbageFiles();
    createJournalWithHeader(MAGIC, "1", "101", "2", "");
    cache = DiskLruCache.open(cacheDir, appVersion, 2, Integer.MAX_VALUE);
    assertGarbageFilesAllDeleted();
  }

  @Test public void openWithInvalidValueCountClearsDirectory() throws Exception {
    cache.close();
    generateSomeGarbageFiles();
    createJournalWithHeader(MAGIC, "1", "100", "1", "");
    cache = DiskLruCache.open(cacheDir, appVersion, 2, Integer.MAX_VALUE);
    assertGarbageFilesAllDeleted();
  }

  @Test public void openWithInvalidBlankLineClearsDirectory() throws Exception {
    cache.close();
    generateSomeGarbageFiles();
    createJournalWithHeader(MAGIC, "1", "100", "2", "x");
    cache = DiskLruCache.open(cacheDir, appVersion, 2, Integer.MAX_VALUE);
    assertGarbageFilesAllDeleted();
  }

  @Test public void openWithInvalidJournalLineClearsDirectory() throws Exception {
    cache.close();
    generateSomeGarbageFiles();
    createJournal("CLEAN k1 1 1", "BOGUS");
    cache = DiskLruCache.open(cacheDir, appVersion, 2, Integer.MAX_VALUE);
    assertGarbageFilesAllDeleted();
    assertThat(cache.get("k1")).isNull();
  }

  @Test public void openWithInvalidFileSizeClearsDirectory() throws Exception {
    cache.close();
    generateSomeGarbageFiles();
    createJournal("CLEAN k1 0000x001 1");
    cache = DiskLruCache.open(cacheDir, appVersion, 2, Integer.MAX_VALUE);
    assertGarbageFilesAllDeleted();
    assertThat(cache.get("k1")).isNull();
  }

  @Test public void openWithTruncatedLineDiscardsThatLine() throws Exception {
    cache.close();
    writeFile(getCleanFile("k1", 0), "A");
    writeFile(getCleanFile("k1", 1), "B");
    Writer writer = new FileWriter(journalFile);
    writer.write(MAGIC + "\n" + VERSION_1 + "\n100\n2\n\nCLEAN k1 1 1"); // no trailing newline
    writer.close();
    cache = DiskLruCache.open(cacheDir, appVersion, 2, Integer.MAX_VALUE);
    assertThat(cache.get("k1")).isNull();

    // The journal is not corrupt when editing after a truncated line.
    set("k1", "C", "D");
    cache.close();
    cache = DiskLruCache.open(cacheDir, appVersion, 2, Integer.MAX_VALUE);
    assertValue("k1", "C", "D");
  }

  @Test public void openWithTooManyFileSizesClearsDirectory() throws Exception {
    cache.close();
    generateSomeGarbageFiles();
    createJournal("CLEAN k1 1 1 1");
    cache = DiskLruCache.open(cacheDir, appVersion, 2, Integer.MAX_VALUE);
    assertGarbageFilesAllDeleted();
    assertThat(cache.get("k1")).isNull();
  }

  @Test public void nullKeyThrows() throws Exception {
    try {
      cache.edit(null);
      Assert.fail();
    } catch (NullPointerException expected) {
    }
  }

  @Test public void createNewEntryWithTooFewValuesFails() throws Exception {
    DiskLruCache.Editor creator = cache.edit("k1");
    creator.set(1, "A");
    try {
      creator.commit();
      Assert.fail();
    } catch (IllegalStateException expected) {
    }

    assertThat(getCleanFile("k1", 0).exists()).isFalse();
    assertThat(getCleanFile("k1", 1).exists()).isFalse();
    assertThat(getDirtyFile("k1", 0).exists()).isFalse();
    assertThat(getDirtyFile("k1", 1).exists()).isFalse();
    assertThat(cache.get("k1")).isNull();

    DiskLruCache.Editor creator2 = cache.edit("k1");
    creator2.set(0, "B");
    creator2.set(1, "C");
    creator2.commit();
  }

  @Test public void revertWithTooFewValues() throws Exception {
    DiskLruCache.Editor creator = cache.edit("k1");
    creator.set(1, "A");
    creator.abort();
    assertThat(getCleanFile("k1", 0).exists()).isFalse();
    assertThat(getCleanFile("k1", 1).exists()).isFalse();
    assertThat(getDirtyFile("k1", 0).exists()).isFalse();
    assertThat(getDirtyFile("k1", 1).exists()).isFalse();
    assertThat(cache.get("k1")).isNull();
  }

  @Test public void updateExistingEntryWithTooFewValuesReusesPreviousValues() throws Exception {
    DiskLruCache.Editor creator = cache.edit("k1");
    creator.set(0, "A");
    creator.set(1, "B");
    creator.commit();

    DiskLruCache.Editor updater = cache.edit("k1");
    updater.set(0, "C");
    updater.commit();

    DiskLruCache.Value value = cache.get("k1");
    assertThat(value.getString(0)).isEqualTo("C");
    assertThat(value.getLength(0)).isEqualTo(1);
    assertThat(value.getString(1)).isEqualTo("B");
    assertThat(value.getLength(1)).isEqualTo(1);
  }

  @Test public void growMaxSize() throws Exception {
    cache.close();
    cache = DiskLruCache.open(cacheDir, appVersion, 2, 10);
    set("a", "a", "aaa"); // size 4
    set("b", "bb", "bbbb"); // size 6
    cache.setMaxSize(20);
    set("c", "c", "c"); // size 12
    assertThat(cache.size()).isEqualTo(12);
  }

  @Test public void shrinkMaxSizeEvicts() throws Exception {
    cache.close();
    cache = DiskLruCache.open(cacheDir, appVersion, 2, 20);
    set("a", "a", "aaa"); // size 4
    set("b", "bb", "bbbb"); // size 6
    set("c", "c", "c"); // size 12
    cache.setMaxSize(10);
    cache.executorService.shutdown();
    cache.executorService.awaitTermination(500, TimeUnit.MILLISECONDS);
    assertThat(cache.size()).isEqualTo(8 /* 12 - 4 */);
  }

  @Test public void evictOnInsert() throws Exception {
    cache.close();
    cache = DiskLruCache.open(cacheDir, appVersion, 2, 10);

    set("a", "a", "aaa"); // size 4
    set("b", "bb", "bbbb"); // size 6
    assertThat(cache.size()).isEqualTo(10);

    // Cause the size to grow to 12 should evict 'A'.
    set("c", "c", "c");
    cache.flush();
    assertThat(cache.size()).isEqualTo(8);
    assertAbsent("a");
    assertValue("b", "bb", "bbbb");
    assertValue("c", "c", "c");

    // Causing the size to grow to 10 should evict nothing.
    set("d", "d", "d");
    cache.flush();
    assertThat(cache.size()).isEqualTo(10);
    assertAbsent("a");
    assertValue("b", "bb", "bbbb");
    assertValue("c", "c", "c");
    assertValue("d", "d", "d");

    // Causing the size to grow to 18 should evict 'B' and 'C'.
    set("e", "eeee", "eeee");
    cache.flush();
    assertThat(cache.size()).isEqualTo(10);
    assertAbsent("a");
    assertAbsent("b");
    assertAbsent("c");
    assertValue("d", "d", "d");
    assertValue("e", "eeee", "eeee");
  }

  @Test public void evictOnUpdate() throws Exception {
    cache.close();
    cache = DiskLruCache.open(cacheDir, appVersion, 2, 10);

    set("a", "a", "aa"); // size 3
    set("b", "b", "bb"); // size 3
    set("c", "c", "cc"); // size 3
    assertThat(cache.size()).isEqualTo(9);

    // Causing the size to grow to 11 should evict 'A'.
    set("b", "b", "bbbb");
    cache.flush();
    assertThat(cache.size()).isEqualTo(8);
    assertAbsent("a");
    assertValue("b", "b", "bbbb");
    assertValue("c", "c", "cc");
  }

  @Test public void evictionHonorsLruFromCurrentSession() throws Exception {
    cache.close();
    cache = DiskLruCache.open(cacheDir, appVersion, 2, 10);
    set("a", "a", "a");
    set("b", "b", "b");
    set("c", "c", "c");
    set("d", "d", "d");
    set("e", "e", "e");
    cache.get("b"); // 'B' is now least recently used.

    // Causing the size to grow to 12 should evict 'A'.
    set("f", "f", "f");
    // Causing the size to grow to 12 should evict 'C'.
    set("g", "g", "g");
    cache.flush();
    assertThat(cache.size()).isEqualTo(10);
    assertAbsent("a");
    assertValue("b", "b", "b");
    assertAbsent("c");
    assertValue("d", "d", "d");
    assertValue("e", "e", "e");
    assertValue("f", "f", "f");
  }

  @Test public void evictionHonorsLruFromPreviousSession() throws Exception {
    set("a", "a", "a");
    set("b", "b", "b");
    set("c", "c", "c");
    set("d", "d", "d");
    set("e", "e", "e");
    set("f", "f", "f");
    cache.get("b"); // 'B' is now least recently used.
    assertThat(cache.size()).isEqualTo(12);
    cache.close();
    cache = DiskLruCache.open(cacheDir, appVersion, 2, 10);

    set("g", "g", "g");
    cache.flush();
    assertThat(cache.size()).isEqualTo(10);
    assertAbsent("a");
    assertValue("b", "b", "b");
    assertAbsent("c");
    assertValue("d", "d", "d");
    assertValue("e", "e", "e");
    assertValue("f", "f", "f");
    assertValue("g", "g", "g");
  }

  @Test public void cacheSingleEntryOfSizeGreaterThanMaxSize() throws Exception {
    cache.close();
    cache = DiskLruCache.open(cacheDir, appVersion, 2, 10);
    set("a", "aaaaa", "aaaaaa"); // size=11
    cache.flush();
    assertAbsent("a");
  }

  @Test public void cacheSingleValueOfSizeGreaterThanMaxSize() throws Exception {
    cache.close();
    cache = DiskLruCache.open(cacheDir, appVersion, 2, 10);
    set("a", "aaaaaaaaaaa", "a"); // size=12
    cache.flush();
    assertAbsent("a");
  }

  @Test public void constructorDoesNotAllowZeroCacheSize() throws Exception {
    try {
      DiskLruCache.open(cacheDir, appVersion, 2, 0);
      Assert.fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  @Test public void constructorDoesNotAllowZeroValuesPerEntry() throws Exception {
    try {
      DiskLruCache.open(cacheDir, appVersion, 0, 10);
      Assert.fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  @Test public void removeAbsentElement() throws Exception {
    cache.remove("a");
  }

  @Test public void readingTheSameFileMultipleTimes() throws Exception {
    set("a", "a", "b");
    DiskLruCache.Value value = cache.get("a");
    assertThat(value.getFile(0)).isSameAs(value.getFile(0));
  }

  @Test public void rebuildJournalOnRepeatedReads() throws Exception {
    set("a", "a", "a");
    set("b", "b", "b");
    long lastJournalLength = 0;
    while (true) {
      long journalLength = journalFile.length();
      assertValue("a", "a", "a");
      assertValue("b", "b", "b");
      if (journalLength < lastJournalLength) {
        System.out
            .printf("Journal compacted from %s bytes to %s bytes\n", lastJournalLength,
                journalLength);
        break; // Test passed!
      }
      lastJournalLength = journalLength;
    }
  }

  @Test public void rebuildJournalOnRepeatedEdits() throws Exception {
    long lastJournalLength = 0;
    while (true) {
      long journalLength = journalFile.length();
      set("a", "a", "a");
      set("b", "b", "b");
      if (journalLength < lastJournalLength) {
        System.out
            .printf("Journal compacted from %s bytes to %s bytes\n", lastJournalLength,
                journalLength);
        break;
      }
      lastJournalLength = journalLength;
    }

    // Sanity check that a rebuilt journal behaves normally.
    assertValue("a", "a", "a");
    assertValue("b", "b", "b");
  }

  /** @see <a href="https://github.com/JakeWharton/DiskLruCache/issues/28">Issue #28</a> */
  @Test public void rebuildJournalOnRepeatedReadsWithOpenAndClose() throws Exception {
    set("a", "a", "a");
    set("b", "b", "b");
    long lastJournalLength = 0;
    while (true) {
      long journalLength = journalFile.length();
      assertValue("a", "a", "a");
      assertValue("b", "b", "b");
      cache.close();
      cache = DiskLruCache.open(cacheDir, appVersion, 2, Integer.MAX_VALUE);
      if (journalLength < lastJournalLength) {
        System.out
            .printf("Journal compacted from %s bytes to %s bytes\n", lastJournalLength,
                journalLength);
        break; // Test passed!
      }
      lastJournalLength = journalLength;
    }
  }

  /** @see <a href="https://github.com/JakeWharton/DiskLruCache/issues/28">Issue #28</a> */
  @Test public void rebuildJournalOnRepeatedEditsWithOpenAndClose() throws Exception {
    long lastJournalLength = 0;
    while (true) {
      long journalLength = journalFile.length();
      set("a", "a", "a");
      set("b", "b", "b");
      cache.close();
      cache = DiskLruCache.open(cacheDir, appVersion, 2, Integer.MAX_VALUE);
      if (journalLength < lastJournalLength) {
        System.out
            .printf("Journal compacted from %s bytes to %s bytes\n", lastJournalLength,
                journalLength);
        break;
      }
      lastJournalLength = journalLength;
    }
  }

  @Test public void restoreBackupFile() throws Exception {
    DiskLruCache.Editor creator = cache.edit("k1");
    creator.set(0, "ABC");
    creator.set(1, "DE");
    creator.commit();
    cache.close();

    assertThat(journalFile.renameTo(journalBkpFile)).isTrue();
    assertThat(journalFile.exists()).isFalse();

    cache = DiskLruCache.open(cacheDir, appVersion, 2, Integer.MAX_VALUE);

    DiskLruCache.Value value = cache.get("k1");
    assertThat(value.getString(0)).isEqualTo("ABC");
    assertThat(value.getLength(0)).isEqualTo(3);
    assertThat(value.getString(1)).isEqualTo("DE");
    assertThat(value.getLength(1)).isEqualTo(2);

    assertThat(journalBkpFile.exists()).isFalse();
    assertThat(journalFile.exists()).isTrue();
  }

  @Test public void journalFileIsPreferredOverBackupFile() throws Exception {
    DiskLruCache.Editor creator = cache.edit("k1");
    creator.set(0, "ABC");
    creator.set(1, "DE");
    creator.commit();
    cache.flush();

    FileUtils.copyFile(journalFile, journalBkpFile);

    creator = cache.edit("k2");
    creator.set(0, "F");
    creator.set(1, "GH");
    creator.commit();
    cache.close();

    assertThat(journalFile.exists()).isTrue();
    assertThat(journalBkpFile.exists()).isTrue();

    cache = DiskLruCache.open(cacheDir, appVersion, 2, Integer.MAX_VALUE);

    DiskLruCache.Value valueA = cache.get("k1");
    assertThat(valueA.getString(0)).isEqualTo("ABC");
    assertThat(valueA.getLength(0)).isEqualTo(3);
    assertThat(valueA.getString(1)).isEqualTo("DE");
    assertThat(valueA.getLength(1)).isEqualTo(2);

    DiskLruCache.Value valueB = cache.get("k2");
    assertThat(valueB.getString(0)).isEqualTo("F");
    assertThat(valueB.getLength(0)).isEqualTo(1);
    assertThat(valueB.getString(1)).isEqualTo("GH");
    assertThat(valueB.getLength(1)).isEqualTo(2);

    assertThat(journalBkpFile.exists()).isFalse();
    assertThat(journalFile.exists()).isTrue();
  }

  @Test public void openCreatesDirectoryIfNecessary() throws Exception {
    cache.close();
    File dir = tempDir.newFolder("testOpenCreatesDirectoryIfNecessary");
    cache = DiskLruCache.open(dir, appVersion, 2, Integer.MAX_VALUE);
    set("a", "a", "a");
    assertThat(new File(dir, "a.0").exists()).isTrue();
    assertThat(new File(dir, "a.1").exists()).isTrue();
    assertThat(new File(dir, "journal").exists()).isTrue();
  }

  @Test public void fileDeletedExternally() throws Exception {
    set("a", "a", "a");
    getCleanFile("a", 1).delete();
    assertThat(cache.get("a")).isNull();
  }

  @Test public void editSameVersion() throws Exception {
    set("a", "a", "a");
    DiskLruCache.Value value = cache.get("a");
    DiskLruCache.Editor editor = value.edit();
    editor.set(1, "a2");
    editor.commit();
    assertValue("a", "a", "a2");
  }

  @Test public void editSnapshotAfterChangeAborted() throws Exception {
    set("a", "a", "a");
    DiskLruCache.Value value = cache.get("a");
    DiskLruCache.Editor toAbort = value.edit();
    toAbort.set(0, "b");
    toAbort.abort();
    DiskLruCache.Editor editor = value.edit();
    editor.set(1, "a2");
    editor.commit();
    assertValue("a", "a", "a2");
  }

  @Test public void editSnapshotAfterChangeCommitted() throws Exception {
    set("a", "a", "a");
    DiskLruCache.Value value = cache.get("a");
    DiskLruCache.Editor toAbort = value.edit();
    toAbort.set(0, "b");
    toAbort.commit();
    assertThat(value.edit()).isNull();
  }

  @Test public void editSinceEvicted() throws Exception {
    cache.close();
    cache = DiskLruCache.open(cacheDir, appVersion, 2, 10);
    set("a", "aa", "aaa"); // size 5
    DiskLruCache.Value value = cache.get("a");
    set("b", "bb", "bbb"); // size 5
    set("c", "cc", "ccc"); // size 5; will evict 'A'
    cache.flush();
    assertThat(value.edit()).isNull();
  }

  @Test public void editSinceEvictedAndRecreated() throws Exception {
    cache.close();
    cache = DiskLruCache.open(cacheDir, appVersion, 2, 10);
    set("a", "aa", "aaa"); // size 5
    DiskLruCache.Value value = cache.get("a");
    set("b", "bb", "bbb"); // size 5
    set("c", "cc", "ccc"); // size 5; will evict 'A'
    set("a", "a", "aaaa"); // size 5; will evict 'B'
    cache.flush();
    assertThat(value.edit()).isNull();
  }

  /** @see <a href="https://github.com/JakeWharton/DiskLruCache/issues/2">Issue #2</a> */
  @Test public void aggressiveClearingHandlesWrite() throws Exception {
    FileUtils.deleteDirectory(cacheDir);
    set("a", "a", "a");
    assertValue("a", "a", "a");
  }

  /** @see <a href="https://github.com/JakeWharton/DiskLruCache/issues/2">Issue #2</a> */
  @Test public void aggressiveClearingHandlesEdit() throws Exception {
    set("a", "a", "a");
    DiskLruCache.Editor a = cache.get("a").edit();
    FileUtils.deleteDirectory(cacheDir);
    a.set(1, "a2");
    a.commit();
  }

  @Test public void removeHandlesMissingFile() throws Exception {
    set("a", "a", "a");
    getCleanFile("a", 0).delete();
    cache.remove("a");
  }

  /** @see <a href="https://github.com/JakeWharton/DiskLruCache/issues/2">Issue #2</a> */
  @Test public void aggressiveClearingHandlesPartialEdit() throws Exception {
    set("a", "a", "a");
    set("b", "b", "b");
    DiskLruCache.Editor a = cache.get("a").edit();
    a.set(0, "a1");
    FileUtils.deleteDirectory(cacheDir);
    a.set(1, "a2");
    a.commit();
    assertThat(cache.get("a")).isNull();
  }

  /** @see <a href="https://github.com/JakeWharton/DiskLruCache/issues/2">Issue #2</a> */
  @Test public void aggressiveClearingHandlesRead() throws Exception {
    FileUtils.deleteDirectory(cacheDir);
    assertThat(cache.get("a")).isNull();
  }

  private void assertJournalEquals(String... expectedBodyLines) throws Exception {
    List<String> expectedLines = new ArrayList<String>();
    expectedLines.add(MAGIC);
    expectedLines.add(VERSION_1);
    expectedLines.add("100");
    expectedLines.add("2");
    expectedLines.add("");
    expectedLines.addAll(Arrays.asList(expectedBodyLines));
    assertThat(readJournalLines()).isEqualTo(expectedLines);
  }

  private void createJournal(String... bodyLines) throws Exception {
    createJournalWithHeader(MAGIC, VERSION_1, "100", "2", "", bodyLines);
  }

  private void createJournalWithHeader(String magic, String version, String appVersion,
      String valueCount, String blank, String... bodyLines) throws Exception {
    Writer writer = new FileWriter(journalFile);
    writer.write(magic + "\n");
    writer.write(version + "\n");
    writer.write(appVersion + "\n");
    writer.write(valueCount + "\n");
    writer.write(blank + "\n");
    for (String line : bodyLines) {
      writer.write(line);
      writer.write('\n');
    }
    writer.close();
  }

  private List<String> readJournalLines() throws Exception {
    List<String> result = new ArrayList<String>();
    BufferedReader reader = new BufferedReader(new FileReader(journalFile));
    String line;
    while ((line = reader.readLine()) != null) {
      result.add(line);
    }
    reader.close();
    return result;
  }

  private File getCleanFile(String key, int index) {
    return new File(cacheDir, key + "." + index);
  }

  private File getDirtyFile(String key, int index) {
    return new File(cacheDir, key + "." + index + ".tmp");
  }

  private static String readFile(File file) throws Exception {
    Reader reader = new FileReader(file);
    StringWriter writer = new StringWriter();
    char[] buffer = new char[1024];
    int count;
    while ((count = reader.read(buffer)) != -1) {
      writer.write(buffer, 0, count);
    }
    reader.close();
    return writer.toString();
  }

  public static void writeFile(File file, String content) throws Exception {
    FileWriter writer = new FileWriter(file);
    writer.write(content);
    writer.close();
  }

  private static void assertInoperable(DiskLruCache.Editor editor) throws Exception {
    try {
      editor.getString(0);
      Assert.fail();
    } catch (IllegalStateException expected) {
    }
    try {
      editor.set(0, "A");
      Assert.fail();
    } catch (IllegalStateException expected) {
    }
    try {
      editor.getFile(0);
      Assert.fail();
    } catch (IllegalStateException expected) {
    }
    try {
      editor.commit();
      Assert.fail();
    } catch (IllegalStateException expected) {
    }
    try {
      editor.abort();
      Assert.fail();
    } catch (IllegalStateException expected) {
    }
  }

  private void generateSomeGarbageFiles() throws Exception {
    File dir1 = new File(cacheDir, "dir1");
    File dir2 = new File(dir1, "dir2");
    writeFile(getCleanFile("g1", 0), "A");
    writeFile(getCleanFile("g1", 1), "B");
    writeFile(getCleanFile("g2", 0), "C");
    writeFile(getCleanFile("g2", 1), "D");
    writeFile(getCleanFile("g2", 1), "D");
    writeFile(new File(cacheDir, "otherFile0"), "E");
    dir1.mkdir();
    dir2.mkdir();
    writeFile(new File(dir2, "otherFile1"), "F");
  }

  private void assertGarbageFilesAllDeleted() throws Exception {
    assertThat(getCleanFile("g1", 0)).doesNotExist();
    assertThat(getCleanFile("g1", 1)).doesNotExist();
    assertThat(getCleanFile("g2", 0)).doesNotExist();
    assertThat(getCleanFile("g2", 1)).doesNotExist();
    assertThat(new File(cacheDir, "otherFile0")).doesNotExist();
    assertThat(new File(cacheDir, "dir1")).doesNotExist();
  }

  private void set(String key, String value0, String value1) throws Exception {
    DiskLruCache.Editor editor = cache.edit(key);
    editor.set(0, value0);
    editor.set(1, value1);
    editor.commit();
  }

  private void assertAbsent(String key) throws Exception {
    DiskLruCache.Value value = cache.get(key);
    if (value != null) {
      Assert.fail();
    }
    assertThat(getCleanFile(key, 0)).doesNotExist();
    assertThat(getCleanFile(key, 1)).doesNotExist();
    assertThat(getDirtyFile(key, 0)).doesNotExist();
    assertThat(getDirtyFile(key, 1)).doesNotExist();
  }

  private void assertValue(String key, String value0, String value1) throws Exception {
    DiskLruCache.Value value = cache.get(key);
    assertThat(value.getString(0)).isEqualTo(value0);
    assertThat(value.getLength(0)).isEqualTo(value0.length());
    assertThat(value.getString(1)).isEqualTo(value1);
    assertThat(value.getLength(1)).isEqualTo(value1.length());
    assertThat(getCleanFile(key, 0)).exists();
    assertThat(getCleanFile(key, 1)).exists();
  }
}
