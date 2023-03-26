package com.bumptech.glide.integration.sqljournaldiskcache;

final class JournalTable {
  static final String TABLE_NAME = "journal";
  private static final String INDEX_TIMESTAMP_KEY = "journal_timestamp_key_idx";

  interface Columns {
    /** The cache key/cache file name. */
    String KEY = "key";
    /** The time the key was most recently created, updated, or read in UTC milliseconds. */
    String LAST_MODIFIED_TIME = "last_modified_time";
    /** 1 if the key is going to be deleted, 0 otherwise. */
    String PENDING_DELETE = "pending_delete";
    /** The length in bytes of the cache file. */
    String SIZE = "size";
  }

  static String getSqlCreateStatement() {
    return "CREATE TABLE "
        + TABLE_NAME
        + " ("
        + Columns.KEY
        + " STRING PRIMARY KEY, "
        + Columns.LAST_MODIFIED_TIME
        + " INTEGER NOT NULL, "
        + Columns.PENDING_DELETE
        + " INTEGER NOT NULL DEFAULT 0, "
        + Columns.SIZE
        + " INTEGER NOT NULL"
        + ")";
  }

  static String getIndexString() {
    return "CREATE INDEX "
        + INDEX_TIMESTAMP_KEY
        + " ON "
        + TABLE_NAME
        + " ("
        + Columns.LAST_MODIFIED_TIME
        + ", "
        + Columns.KEY
        + ")";
  }
}
