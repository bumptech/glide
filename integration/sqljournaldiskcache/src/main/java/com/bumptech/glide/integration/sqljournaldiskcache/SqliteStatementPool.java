package com.bumptech.glide.integration.sqljournaldiskcache;

import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

final class SqliteStatementPool {
  private static final int MAX_SIZE = 10;

  private final Map<String, Queue<SQLiteStatement>> pool = new HashMap<>();
  private final SQLiteOpenHelper dbHelper;

  SqliteStatementPool(SQLiteOpenHelper dbHelper) {
    this.dbHelper = dbHelper;
  }

  SQLiteStatement obtain(String sql) {
    SQLiteStatement statement = null;
    synchronized (pool) {
      Queue<SQLiteStatement> queueForSql = pool.get(sql);
      if (queueForSql != null) {
        statement = queueForSql.poll();
      }
    }
    if (statement == null) {
      statement = dbHelper.getWritableDatabase().compileStatement(sql);
    }
    return statement;
  }

  void offer(String sql, SQLiteStatement statement) {
    statement.clearBindings();
    synchronized (pool) {
      Queue<SQLiteStatement> queueForSql = pool.get(sql);
      if (queueForSql == null) {
        queueForSql = new ArrayDeque<>(MAX_SIZE);
        pool.put(sql, queueForSql);
      }
      if (queueForSql.size() < MAX_SIZE) {
        queueForSql.offer(statement);
      }
    }
  }
}
