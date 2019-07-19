package com.bumptech.glide;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.load.engine.bitmap_recycle.ArrayPool;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.engine.bitmap_recycle.LruArrayPool;
import com.bumptech.glide.load.engine.bitmap_recycle.LruBitmapPool;
import com.bumptech.glide.load.engine.cache.MemoryCache;
import java.io.FileDescriptor;
import java.io.PrintWriter;

/** Dumps some data about Glide's state to bug reports. */
public final class GlideStatsProvider extends ContentProvider {
  private static final String BITMAP_POOL_LOG_NAME = "Bitmap Pool";
  private static final String ARRAY_POOL_LOG_NAME = "Array Pool";

  @Override
  public void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
    super.dump(fd, writer, args);

    writer.write("Estimated Glide Memory Stats\n");

    Glide glide = Glide.get(getContext());

    long totalSizeBytes = 0;

    MemoryCache memoryCache = glide.getMemoryCache();
    long currentMemoryCacheSize = memoryCache.getCurrentSize();
    long maxMemoryCacheSize = memoryCache.getMaxSize();
    totalSizeBytes += currentMemoryCacheSize;
    dumpSize(writer, "Memory Cache", currentMemoryCacheSize, maxMemoryCacheSize);

    BitmapPool bitmapPool = glide.getBitmapPool();
    if (bitmapPool instanceof LruBitmapPool) {
      LruBitmapPool lruBitmapPool = (LruBitmapPool) bitmapPool;
      long currentBitmapPoolSize = lruBitmapPool.getCurrentSize();
      long maxBitmapPoolSize = lruBitmapPool.getMaxSize();
      totalSizeBytes += currentBitmapPoolSize;
      dumpSize(writer, BITMAP_POOL_LOG_NAME, currentBitmapPoolSize, maxBitmapPoolSize);
    } else {
      dumpUnknown(writer, BITMAP_POOL_LOG_NAME);
    }

    ArrayPool arrayPool = glide.getArrayPool();
    if (arrayPool instanceof LruArrayPool) {
      LruArrayPool lruArrayPool = (LruArrayPool) arrayPool;
      long currentArrayPoolSize = lruArrayPool.getCurrentSize();
      long maxArrayPoolSize = lruArrayPool.getMaxSize();
      totalSizeBytes += currentArrayPoolSize;
      dumpSize(writer, ARRAY_POOL_LOG_NAME, currentArrayPoolSize, maxArrayPoolSize);
    } else {
      dumpUnknown(writer, ARRAY_POOL_LOG_NAME);
    }

    long activeResourceByteSize = glide.calculateActiveResourceByteSize();
    totalSizeBytes += activeResourceByteSize;

    printHeader(writer, "Active Resources");
    writer.append(String.valueOf(activeResourceByteSize)).append('\n');

    printHeader(writer, "Total Size");
    writer.append(String.valueOf(totalSizeBytes)).append('\n');
  }

  private static void dumpUnknown(PrintWriter writer, String name) {
    printHeader(writer, name);
    writer.append("Unknown\n");
  }

  private static void dumpSize(
      PrintWriter writer, String name, long currentSize, long maximumSize) {
    printHeader(writer, name);
    writer
        .append(String.valueOf(currentSize))
        .append(" / ")
        .append(String.valueOf(maximumSize))
        .append('\n');
  }

  private static void printHeader(PrintWriter writer, String name) {
    writer.append(name).append(" size bytes: ");
  }

  @Override
  public boolean onCreate() {
    return true;
  }

  @SuppressWarnings("PMD.UseObjectForClearerAPI")
  @Nullable
  @Override
  public Cursor query(
      @NonNull Uri uri,
      @Nullable String[] projection,
      @Nullable String selection,
      @Nullable String[] selectionArgs,
      @Nullable String sortOrder) {
    throw new UnsupportedOperationException();
  }

  @Nullable
  @Override
  public String getType(@NonNull Uri uri) {
    throw new UnsupportedOperationException();
  }

  @Nullable
  @Override
  public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int delete(
      @NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int update(
      @NonNull Uri uri,
      @Nullable ContentValues values,
      @Nullable String selection,
      @Nullable String[] selectionArgs) {
    throw new UnsupportedOperationException();
  }
}
