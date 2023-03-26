package com.bumptech.glide.integration.sqljournaldiskcache;

import android.content.ContentValues;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.database.sqlite.SQLiteTransactionListener;
import androidx.annotation.NonNull;
import androidx.core.util.Pools.Pool;
import com.bumptech.glide.util.pool.FactoryPools;
import com.bumptech.glide.util.pool.FactoryPools.Poolable;
import com.bumptech.glide.util.pool.FactoryPools.Resetter;
import com.bumptech.glide.util.pool.StateVerifier;
import java.util.concurrent.atomic.AtomicLong;

final class SizeJournal {
  private static final String UPDATE_CACHE_SIZE_SQL =
      "UPDATE "
          + SizeTable.TABLE_NAME
          + " SET "
          + SizeTable.Columns.SIZE
          + " = "
          + SizeTable.Columns.SIZE
          + " + ?";
  private static final int UPDATE_CACHE_SIZE_SIZE_INCREMENT_IDX = 1;

  private static final String CONTAINS_SIZE_QUERY = "SELECT COUNT(*) FROM " + SizeTable.TABLE_NAME;
  private static final String CACHE_SIZE_QUERY =
      "SELECT " + SizeTable.Columns.SIZE + " FROM " + SizeTable.TABLE_NAME;
  private final AtomicLong size = new AtomicLong();
  private final SqliteStatementPool updateCacheSizePool;
  private final Pool<SizeSQLiteTransactionListener> sizeListenerPool =
      FactoryPools.threadSafe(
          /* size= */ 20,
          new FactoryPools.Factory<SizeSQLiteTransactionListener>() {
            @Override
            public SizeSQLiteTransactionListener create() {
              return new SizeSQLiteTransactionListener();
            }
          },
          new Resetter<SizeSQLiteTransactionListener>() {
            @Override
            public void reset(@NonNull SizeSQLiteTransactionListener object) {
              object.clear();
            }
          });

  private final DiskCacheDbHelper dbHelper;

  SizeJournal(DiskCacheDbHelper dbHelper) {
    this.dbHelper = dbHelper;
    updateCacheSizePool = new SqliteStatementPool(dbHelper);
  }

  void open() {
    SQLiteDatabase db = dbHelper.getReadableDatabase();
    boolean containsSize =
        0 != DatabaseUtils.longForQuery(db, CONTAINS_SIZE_QUERY, null /*selectionArgs*/);
    final long currentSize;
    if (!containsSize) {
      ContentValues values = new ContentValues();
      values.put(SizeTable.Columns.SIZE, 0);
      db.insert(SizeTable.TABLE_NAME, null /*nullColumnHack*/, values);
      currentSize = 0;
    } else {
      currentSize = DatabaseUtils.longForQuery(db, CACHE_SIZE_QUERY, null /*selectionArgs*/);
    }
    size.set(currentSize);
  }

  void clearInTransaction() {
    SQLiteDatabase db = dbHelper.getReadableDatabase();
    db.delete(SizeTable.TABLE_NAME, null /*whereClause*/, null /*whereArgs*/);
    size.set(0);
  }

  long getCacheSizeBytes() {
    return size.get();
  }

  SizeSQLiteTransactionListener prepareSizeTransaction() {
    SizeSQLiteTransactionListener result = sizeListenerPool.acquire();
    if (result == null) {
      result = new SizeSQLiteTransactionListener();
    }
    return result;
  }

  void endSizeTransaction(SizeSQLiteTransactionListener listener) {
    listener.clear();
    sizeListenerPool.release(listener);
  }

  void decrementSizeInTransaction(SizeSQLiteTransactionListener sizeListener, long decrementBy) {
    incrementSizeInTransaction(sizeListener, -decrementBy);
  }

  void incrementSizeInTransaction(SizeSQLiteTransactionListener sizeListener, long incrementBy) {
    sizeListener.updatedSize = incrementBy;

    SQLiteStatement updateCacheSizeStatement = updateCacheSizePool.obtain(UPDATE_CACHE_SIZE_SQL);
    try {
      updateCacheSizeStatement.bindLong(UPDATE_CACHE_SIZE_SIZE_INCREMENT_IDX, incrementBy);
      updateCacheSizeStatement.executeUpdateDelete();
      size.addAndGet(incrementBy);
    } finally {
      updateCacheSizePool.offer(UPDATE_CACHE_SIZE_SQL, updateCacheSizeStatement);
    }
  }

  /** A listener that reverts size changes upon transaction failure. */
  final class SizeSQLiteTransactionListener implements SQLiteTransactionListener, Poolable {
    private long updatedSize;

    void clear() {
      updatedSize = 0;
    }

    @Override
    public void onBegin() {}

    @Override
    public void onCommit() {}

    @Override
    public void onRollback() {
      // Revert the increment of size on transaction failure.
      size.addAndGet(-updatedSize);
    }

    @NonNull
    @Override
    public StateVerifier getVerifier() {
      return StateVerifier.newInstance();
    }
  }
}
