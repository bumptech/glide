package com.bumptech.glide.integration.sqljournaldiskcache;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import java.io.File;
import java.io.FilenameFilter;
import java.util.List;

/** Finds and cleans up failed writes and deletes on the work thread. */
final class RecoveryManager {

  private final JournaledLruDiskCache diskCache;
  private final Journal journal;
  private final File diskCacheDir;
  private final Looper workLooper;
  private final Handler recoveryHandler;

  RecoveryManager(
      JournaledLruDiskCache diskCache, File diskCacheDir, Journal journal, Looper workLooper) {
    this.diskCache = diskCache;
    this.journal = journal;
    this.diskCacheDir = diskCacheDir;
    this.workLooper = workLooper;

    recoveryHandler = new Handler(workLooper, new RecoveryCallback());
  }

  void triggerRecovery() {
    recoveryHandler.obtainMessage(MessageIds.RECOVER).sendToTarget();
  }

  private void runRecoveryOnWorkThread() {
    if (!Looper.myLooper().equals(workLooper)) {
      throw new IllegalStateException(
          "Cannot run recovery on a thread other than the work" + " thread!");
    }
    recoverPartialWrites();
    recoverPartialDeletes();
  }

  private void recoverPartialDeletes() {
    List<String> pendingDeleteKeys = journal.getPendingDeleteKeys();
    diskCache.delete(pendingDeleteKeys);
  }

  private void recoverPartialWrites() {
    File[] partialWrites =
        diskCacheDir.listFiles(
            new FilenameFilter() {
              @Override
              public boolean accept(File dir, String filename) {
                return filename.endsWith(JournaledLruDiskCache.TEMP_FILE_INDICATOR);
              }
            });
    if (partialWrites != null) {
      for (File file : partialWrites) {
        diskCache.recoverPartialWrite(file);
      }
    }
  }

  private class RecoveryCallback implements Handler.Callback {

    @Override
    public boolean handleMessage(Message msg) {
      if (msg.what != MessageIds.RECOVER) {
        return false;
      }
      runRecoveryOnWorkThread();
      return true;
    }
  }
}
