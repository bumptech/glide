package com.bumptech.glide.integration.sqljournaldiskcache;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDoneException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import com.bumptech.glide.integration.sqljournaldiskcache.SizeJournal.SizeSQLiteTransactionListener;
import com.bumptech.glide.util.Preconditions;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class Journal {
  private static final String TAG = "Journal";

  // You must restart the app after enabling these logs for the change to take affect.
  // We cache isLoggable to avoid the performance hit of checking repeatedly.
  private static final boolean LOG_VERBOSE = Log.isLoggable(TAG, Log.VERBOSE);
  private static final boolean LOG_DEBUG = Log.isLoggable(TAG, Log.DEBUG);
  private static final boolean LOG_WARN = Log.isLoggable(TAG, Log.WARN);

  private static final String ROW_ID = "rowid";
  private static final String WHERE_KEY = JournalTable.Columns.KEY + " = ?";
  private static final String WHERE_PENDING_DELETE = JournalTable.Columns.PENDING_DELETE + " != 0";

  // If a commit fails (renameTo returns false) and then the app dies before the commit is aborted,
  // we will end up with a temp file and an entry in the journal for a key. New puts for that key
  // should be able to complete successfully, so we use insert or replace to allow the entry to be
  // updated. We don't normally expect to be replacing entries.
  private static final String INSERT_NEW_KEY_SQL =
      "INSERT OR REPLACE INTO "
          + JournalTable.TABLE_NAME
          + "("
          + JournalTable.Columns.KEY
          + ", "
          + JournalTable.Columns.LAST_MODIFIED_TIME
          + ", "
          + JournalTable.Columns.SIZE
          + ") VALUES (?, ?, ?)";
  private static final int INSERT_NEW_KEY_KEY_IDX = 1;
  private static final int INSERT_NEW_KEY_MODIFIED_TIME_IDX = 2;
  private static final int INSERT_NEW_KEY_SIZE_IDX = 3;

  private static final String CONTAINS_KEY_SQL =
      "SELECT COUNT(*) FROM " + JournalTable.TABLE_NAME + " WHERE " + WHERE_KEY;
  private static final int CONTAINS_KEY_KEY_IDX = 1;

  private static final String SELECT_ENTRY_SIZE_NOT_PENDING_SQL =
      "SELECT "
          + JournalTable.Columns.SIZE
          + " FROM "
          + JournalTable.TABLE_NAME
          + " WHERE "
          + WHERE_KEY
          + " AND "
          + JournalTable.Columns.PENDING_DELETE
          + " = 0";
  private static final int SELECT_ENTRY_SIZE_NOT_PENDING_KEY_IDX = 1;

  private static final String DELETE_ENTRY_SQL =
      "DELETE FROM " + JournalTable.TABLE_NAME + " WHERE " + WHERE_KEY;
  private static final int DELETE_ENTRY_KEY_IDX = 1;

  private static final String[] LRU_PROJECTION =
      new String[] {JournalTable.Columns.KEY, JournalTable.Columns.SIZE};
  private static final String[] STALE_PROJECTION =
      new String[] {JournalTable.Columns.KEY, JournalTable.Columns.LAST_MODIFIED_TIME, ROW_ID};
  private static final String LRU_WHERE = JournalTable.Columns.PENDING_DELETE + " = 0";
  private static final String STALE_WHERE =
      ROW_ID + " > ? AND " + JournalTable.Columns.LAST_MODIFIED_TIME + " < ?";
  // rowid comes from https://www.sqlite.org/rowidtable.html. See b/206890186.
  private static final String LRU_ORDER_BY =
      JournalTable.Columns.LAST_MODIFIED_TIME + " ASC, rowid ASC";
  private static final String STALE_ORDER_BY = ROW_ID + " ASC";
  private static final int LRU_BATCH_SIZE = 25;
  private static final int STALE_BATCH_SIZE = 25;

  private static final String SUM_SIZE_WHERE_NOT_PENDING_DELETE =
      "SELECT SUM("
          + JournalTable.Columns.SIZE
          + ") FROM "
          + JournalTable.TABLE_NAME
          + " WHERE "
          + JournalTable.Columns.PENDING_DELETE
          + " = 0";

  private static final String[] PENDING_DELETE_PROJECTION = new String[] {JournalTable.Columns.KEY};

  private static final int DELETE_BATCH_SIZE = 200;

  private final DiskCacheDbHelper dbHelper;
  private final SqliteStatementPool statementPool;
  private final Clock clock;
  private final Handler updateTimesHandler;
  private final SizeJournal sizeJournal;

  Journal(
      DiskCacheDbHelper dbHelper,
      Looper workThreadLooper,
      int updateModifiedTimeBatchSize,
      Clock clock) {
    this.dbHelper = dbHelper;
    this.sizeJournal = new SizeJournal(dbHelper);
    statementPool = new SqliteStatementPool(dbHelper);
    this.clock = clock;

    updateTimesHandler =
        new Handler(
            workThreadLooper,
            new UpdateTimesCallback(dbHelper, updateModifiedTimeBatchSize, clock));
  }

  long getCurrentSizeBytes() {
    return sizeJournal.getCacheSizeBytes();
  }

  void open() {
    sizeJournal.open();
  }

  void clear() {
    SQLiteDatabase db = dbHelper.getWritableDatabase();
    db.beginTransactionNonExclusive();
    try {
      db.delete(JournalTable.TABLE_NAME, null /*whereClause*/, null /*whereArgs*/);
      sizeJournal.clearInTransaction();
      db.setTransactionSuccessful();
    } finally {
      db.endTransaction();
    }
  }

  void get(String key) {
    updateTimesHandler.obtainMessage(MessageIds.ADD_LAST_MODIFIED_KEY, key).sendToTarget();
  }

  void put(String key, long sizeBytes) {
    Preconditions.checkArgument(!TextUtils.isEmpty(key));
    SQLiteDatabase db = dbHelper.getWritableDatabase();
    SQLiteStatement insertStatement = statementPool.obtain(INSERT_NEW_KEY_SQL);
    insertStatement.bindString(INSERT_NEW_KEY_KEY_IDX, key);
    insertStatement.bindLong(INSERT_NEW_KEY_MODIFIED_TIME_IDX, clock.currentTimeMillis());
    insertStatement.bindLong(INSERT_NEW_KEY_SIZE_IDX, sizeBytes);

    SizeSQLiteTransactionListener sizeListener = sizeJournal.prepareSizeTransaction();
    db.beginTransactionWithListenerNonExclusive(sizeListener);
    try {
      insertStatement.executeInsert();
      sizeJournal.incrementSizeInTransaction(sizeListener, sizeBytes);
      db.setTransactionSuccessful();
    } finally {
      db.endTransaction();
      statementPool.offer(INSERT_NEW_KEY_SQL, insertStatement);
      sizeJournal.endSizeTransaction(sizeListener);
    }
  }

  List<String> getPendingDeleteKeys() {
    List<String> result = new ArrayList<>();
    SQLiteDatabase db = dbHelper.getReadableDatabase();
    Cursor cursor =
        db.query(
            JournalTable.TABLE_NAME,
            PENDING_DELETE_PROJECTION,
            WHERE_PENDING_DELETE,
            null /*selectionArgs*/,
            null /*groupBy*/,
            null /*having*/,
            null /*orderBy*/);
    try {
      while (cursor.moveToNext()) {
        String key = cursor.getString(cursor.getColumnIndexOrThrow(JournalTable.Columns.KEY));
        if (TextUtils.isEmpty(key)) {
          if (LOG_WARN) {
            Log.w(TAG, "Found empty or null key: %s, skipping delete: " + key);
          }
        } else {
          result.add(key);
        }
      }
    } finally {
      cursor.close();
    }
    return result;
  }

  List<String> getLeastRecentlyUsed(long targetByteCount) {
    SQLiteDatabase db = dbHelper.getReadableDatabase();
    List<String> keys = new ArrayList<>();
    long currentByteCount = 0;
    int currentOffset = 0;
    boolean isOutOfEntries = false;
    while (!isOutOfEntries && currentByteCount < targetByteCount) {
      Cursor cursor =
          db.query(
              JournalTable.TABLE_NAME,
              LRU_PROJECTION,
              LRU_WHERE,
              null /*selectionArgs*/,
              null /*groupBy*/,
              null /*having*/,
              LRU_ORDER_BY,
              currentOffset + ", " + LRU_BATCH_SIZE);
      try {
        int keyIdx = cursor.getColumnIndexOrThrow(JournalTable.Columns.KEY);
        int sizeIdx = cursor.getColumnIndexOrThrow(JournalTable.Columns.SIZE);
        while (cursor.moveToNext() && currentByteCount < targetByteCount) {
          String key = cursor.getString(keyIdx);
          keys.add(key);

          long sizeBytes = cursor.getLong(sizeIdx);
          currentByteCount += sizeBytes;
        }
        isOutOfEntries = cursor.getCount() < LRU_BATCH_SIZE;
      } finally {
        cursor.close();
      }
      currentOffset += LRU_BATCH_SIZE;
    }

    // TODO(judds): for a sufficiently large file or small cache size and a failed attempt to commit
    // a put, this can happen because our journal size will temporarily not match our File size.
    // If this becomes an issue, we can safely just clear the cache here instead of throwing because
    // we were about to delete all the files anyway.
    if (isOutOfEntries && currentByteCount < targetByteCount) {
      throw new IllegalStateException(
          "Size mismatch"
              + ", expected to be able to evict at least "
              + targetByteCount
              + " bytes"
              + ", but only found "
              + currentByteCount
              + " bytes worth of entries!");
    }

    return keys;
  }

  List<String> getStaleEntries(long staleTimeThresholdMs) {
    SQLiteDatabase db = dbHelper.getReadableDatabase();
    List<String> keys = new ArrayList<>();
    long currentRowId = 0L;
    boolean isOutOfEntries = false;
    while (!isOutOfEntries) {
      try (Cursor cursor =
          db.query(
              JournalTable.TABLE_NAME,
              STALE_PROJECTION,
              LRU_WHERE + " AND " + STALE_WHERE,
              new String[] {String.valueOf(currentRowId), String.valueOf(staleTimeThresholdMs)},
              null /*groupBy*/,
              null /*having*/,
              STALE_ORDER_BY,
              String.valueOf(STALE_BATCH_SIZE))) {
        int keyIdx = cursor.getColumnIndexOrThrow(JournalTable.Columns.KEY);
        while (cursor.moveToNext()) {
          keys.add(cursor.getString(keyIdx));
          currentRowId = cursor.getLong(cursor.getColumnIndexOrThrow(ROW_ID));
        }
        isOutOfEntries = cursor.getCount() < STALE_BATCH_SIZE;
      }
    }
    return keys;
  }

  /**
   * Removes the pending entry from the journal for the given key and returns the size in bytes of
   * the entry, or returns 0 if no such entry exists.
   */
  void abortPut(String key) {
    SQLiteDatabase db = dbHelper.getWritableDatabase();
    SQLiteStatement containsKeyStatement = statementPool.obtain(CONTAINS_KEY_SQL);
    containsKeyStatement.bindString(CONTAINS_KEY_KEY_IDX, key);
    SQLiteStatement selectNotPendingSizeStatement =
        statementPool.obtain(SELECT_ENTRY_SIZE_NOT_PENDING_SQL);
    selectNotPendingSizeStatement.bindString(SELECT_ENTRY_SIZE_NOT_PENDING_KEY_IDX, key);
    SQLiteStatement deleteEntryStatement = statementPool.obtain(DELETE_ENTRY_SQL);
    deleteEntryStatement.bindString(DELETE_ENTRY_KEY_IDX, key);

    SizeSQLiteTransactionListener sizeListener = sizeJournal.prepareSizeTransaction();
    db.beginTransactionWithListenerNonExclusive(sizeListener);
    try {
      // We may be asked to abort a put that failed before the entry was updated, so we will
      // occasionally fail to find the entry here.
      boolean isKeyPresent = 0 != containsKeyStatement.simpleQueryForLong();
      if (!isKeyPresent) {
        return;
      }
      long entrySize;
      try {
        entrySize = selectNotPendingSizeStatement.simpleQueryForLong();
      } catch (SQLiteDoneException e) {
        // No row found for this key that is not pending delete.
        entrySize = 0;
      }
      int deleted = deleteEntryStatement.executeUpdateDelete();
      if (deleted != 1) {
        throw new IllegalStateException(
            "Failed to delete entry"
                + ", key: "
                + key
                + ", size: "
                + entrySize
                + ", actually deleted: "
                + deleted);
      } else {
        // If the item is pending delete its size is 0 here - skip decrementing.
        if (entrySize != 0) {
          sizeJournal.decrementSizeInTransaction(sizeListener, entrySize);
        }
      }
      db.setTransactionSuccessful();
    } finally {
      db.endTransaction();
      statementPool.offer(CONTAINS_KEY_SQL, containsKeyStatement);
      statementPool.offer(SELECT_ENTRY_SIZE_NOT_PENDING_SQL, selectNotPendingSizeStatement);
      statementPool.offer(DELETE_ENTRY_SQL, deleteEntryStatement);
      sizeJournal.endSizeTransaction(sizeListener);
    }
  }

  void delete(List<String> keys) {
    SQLiteDatabase db = dbHelper.getWritableDatabase();
    for (int startPosition = 0; startPosition < keys.size(); startPosition += DELETE_BATCH_SIZE) {
      int endPosition = Math.min(keys.size(), startPosition + DELETE_BATCH_SIZE);
      List<String> batch = keys.subList(startPosition, endPosition);
      int batchSize = batch.size();
      if (batchSize == 0) {
        if (LOG_WARN) {
          Log.w(
              TAG,
              "Unexpectedly 0 sized batch between: "
                  + startPosition
                  + " and endPosition: "
                  + endPosition);
        }
        continue;
      }
      String[] keysToDelete = batch.toArray(new String[batchSize]);
      db.beginTransactionNonExclusive();
      try {
        int deleted =
            db.delete(JournalTable.TABLE_NAME, buildKeySelectionSet(batchSize), keysToDelete);
        if (deleted != keysToDelete.length && LOG_WARN) {
          Log.w(
              TAG,
              "Failed to delete all expected entries"
                  + ", expected: "
                  + keysToDelete.length
                  + ", deleted: "
                  + deleted);
        }
        db.setTransactionSuccessful();
      } finally {
        db.endTransaction();
      }
    }
  }

  private static String buildKeySelectionSet(int count) {
    Preconditions.checkArgument(count > 0);
    String prefix = " IN(";
    String postfix = "?)";
    // (?,?,?), so 2 characters per item, except for the last one which is one character.
    int commaSeparatedCount = count - 1;
    StringBuilder sb =
        new StringBuilder(
                JournalTable.Columns.KEY.length()
                    + prefix.length()
                    + (2 * commaSeparatedCount)
                    + postfix.length())
            .append(JournalTable.Columns.KEY)
            .append(prefix);
    for (int i = 0; i < commaSeparatedCount; i++) {
      sb.append("?,");
    }
    return sb.append(postfix).toString();
  }

  void markPendingDelete(List<String> keys) {
    ContentValues values = new ContentValues();
    values.put(JournalTable.Columns.PENDING_DELETE, 1);
    SQLiteDatabase db = dbHelper.getWritableDatabase();

    for (int startPosition = 0; startPosition < keys.size(); startPosition += DELETE_BATCH_SIZE) {
      int endPosition = Math.min(keys.size(), startPosition + DELETE_BATCH_SIZE);
      List<String> batch = keys.subList(startPosition, endPosition);
      int batchSize = batch.size();

      String[] keysToDelete = batch.toArray(new String[batchSize]);
      String keySelectionSet = buildKeySelectionSet(batchSize);
      SizeSQLiteTransactionListener sizeListener = sizeJournal.prepareSizeTransaction();
      db.beginTransactionWithListenerNonExclusive(sizeListener);
      try {
        long sumOfSizesOfNewlyPendingEntries =
            DatabaseUtils.longForQuery(
                db, SUM_SIZE_WHERE_NOT_PENDING_DELETE + " AND " + keySelectionSet, keysToDelete);
        sizeJournal.decrementSizeInTransaction(sizeListener, sumOfSizesOfNewlyPendingEntries);
        db.update(JournalTable.TABLE_NAME, values, keySelectionSet, keysToDelete);
        db.setTransactionSuccessful();
      } finally {
        db.endTransaction();
        sizeJournal.endSizeTransaction(sizeListener);
      }
    }
  }

  private static class UpdateTimesCallback implements Handler.Callback {
    private final SQLiteOpenHelper dbHelper;
    private final int updateModifiedTimeBatchSize;
    private final List<String> keysToUpdate;
    private final String batchUpdatedModifiedTimeSql;
    private final Clock clock;

    private SQLiteStatement sqlStatement;

    UpdateTimesCallback(SQLiteOpenHelper dbHelper, int updateModifiedTimeBatchSize, Clock clock) {
      this.clock = clock;
      Preconditions.checkArgument(updateModifiedTimeBatchSize > 0);
      this.dbHelper = dbHelper;
      this.updateModifiedTimeBatchSize = updateModifiedTimeBatchSize;
      keysToUpdate = new ArrayList<>(updateModifiedTimeBatchSize);

      batchUpdatedModifiedTimeSql =
          "UPDATE "
              + JournalTable.TABLE_NAME
              + " SET "
              + JournalTable.Columns.LAST_MODIFIED_TIME
              + " = ?"
              + " WHERE "
              + buildKeySelectionSet(updateModifiedTimeBatchSize);
    }

    private SQLiteStatement getSqlStatement() {
      if (sqlStatement == null) {
        sqlStatement = dbHelper.getWritableDatabase().compileStatement(batchUpdatedModifiedTimeSql);
      }
      return sqlStatement;
    }

    private void updateTimes() {
      long startTime = clock.currentTimeMillis();
      SQLiteDatabase db = dbHelper.getWritableDatabase();
      SQLiteStatement statement = getSqlStatement();
      long modifiedTime = clock.currentTimeMillis();
      statement.bindLong(1, modifiedTime);
      int size = keysToUpdate.size();
      for (int i = 0; i < size; i++) {
        String key = keysToUpdate.get(i);
        // 1 indexed, with the modified time as the first argument.
        statement.bindString(i + 2, key);
      }
      db.beginTransactionNonExclusive();
      try {
        int updated = statement.executeUpdateDelete();
        if (updated != updateModifiedTimeBatchSize && LOG_DEBUG) {
          Set<String> uniqueKeys = new HashSet<>(keysToUpdate);
          // This can happen in one of two cases:
          // 1. Files are deleted out from under us (by the system), triggering a cache rebuild.
          // 2. The corresponding entries are evicted while they're in the get queue.
          Log.d(
              TAG,
              "Failed to update modified time for all rows"
                  + ", time: "
                  + modifiedTime
                  + ", expected: "
                  + updateModifiedTimeBatchSize
                  + ", actually updated: "
                  + updated
                  + ", unique keys: "
                  + uniqueKeys.size());
        }
        db.setTransactionSuccessful();
      } finally {
        db.endTransaction();
      }

      if (LOG_VERBOSE) {
        Log.v(
            TAG,
            "Completed update times with "
                + keysToUpdate.size()
                + " updates in "
                + (clock.currentTimeMillis() - startTime));
      }
    }

    @Override
    public boolean handleMessage(Message msg) {
      if (msg.what != MessageIds.ADD_LAST_MODIFIED_KEY) {
        return false;
      }
      String updatedKey = (String) msg.obj;
      if (!keysToUpdate.contains(updatedKey)) {
        keysToUpdate.add(updatedKey);
      }
      if (keysToUpdate.size() == updateModifiedTimeBatchSize) {
        updateTimes();
        keysToUpdate.clear();
      }

      return true;
    }
  }
}
