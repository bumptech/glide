package com.bumptech.glide.integration.sqljournaldiskcache;

final class SizeTable {
  static final String TABLE_NAME = "size";

  interface Columns {
    String ID = "id";
    /** The total size in bytes of all files in the cache (+- pending deletes and inserts). */
    String SIZE = "size";
  }

  static String getSqlCreateStatement() {
    return "CREATE TABLE "
        + TABLE_NAME
        + " ("
        + Columns.ID
        + " INTEGER PRIMARY KEY, "
        + Columns.SIZE
        + " INTEGER NOT NULL DEFAULT 0"
        + ")";
  }
}
