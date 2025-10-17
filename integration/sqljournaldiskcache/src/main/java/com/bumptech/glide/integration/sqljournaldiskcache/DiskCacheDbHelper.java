package com.bumptech.glide.integration.sqljournaldiskcache;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import androidx.annotation.VisibleForTesting;

/** The database helper for managing tables for {@link JournaledLruDiskCache}. */
final class DiskCacheDbHelper extends SQLiteOpenHelper {
  private static final int DATABASE_VERSION = 2; // judds.
  private static final String DATABASE_NAME = "disk_cache";

  static DiskCacheDbHelper forProd(Context context) {
    return new DiskCacheDbHelper(context, /* isInMemory= */ false);
  }

  static DiskCacheDbHelper forTesting(Context context) {
    return new DiskCacheDbHelper(context, /* isInMemory= */ true);
  }

  private DiskCacheDbHelper(Context context, boolean isInMemory) {
    this(context, isInMemory, DATABASE_VERSION);
  }

  @VisibleForTesting
  DiskCacheDbHelper(Context context, boolean isInMemory, int databaseVersion) {
    super(context, isInMemory ? null : DATABASE_NAME, /* factory= */ null, databaseVersion);
    setWriteAheadLoggingEnabled(true);
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    db.execSQL(JournalTable.getSqlCreateStatement());
    db.execSQL(JournalTable.getIndexString());
    db.execSQL(SizeTable.getSqlCreateStatement());
  }

  @Override
  public void onOpen(SQLiteDatabase db) {
    db.execSQL("PRAGMA legacy_alter_table=ON");
    db.setForeignKeyConstraintsEnabled(false);
    try {
      super.onOpen(db);
    } finally {
      db.setForeignKeyConstraintsEnabled(true);
    }
  }

  // We're matching the existing production behavior, which uses STRING even though it should use
  // TEXT
  @SuppressLint("SQLiteString")
  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    if (oldVersion < 2) {
      // Dropping the journal table will also drop the index: https://sqlite.org/lang_droptable.html
      db.execSQL("DROP TABLE IF EXISTS journal");
      db.execSQL("DROP TABLE IF EXISTS size");

      db.execSQL(
          "CREATE TABLE journal("
              + "key STRING PRIMARY KEY, "
              + "last_modified_time INTEGER NOT NULL, "
              + "pending_delete INTEGER NOT NULL DEFAULT 0, "
              + "size INTEGER NOT NULL"
              + ")");
      db.execSQL(
          "CREATE INDEX journal_timestamp_key_idx"
              + " ON journal ("
              + "last_modified_time, "
              + "key"
              + ")");
      db.execSQL(
          "CREATE TABLE size("
              + "id INTEGER PRIMARY KEY, "
              + "size INTEGER NOT NULL DEFAULT 0"
              + ")");
    }
  }
}
