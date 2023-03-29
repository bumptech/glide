package com.bumptech.glide.integration.sqljournaldiskcache;

import java.io.File;
import java.io.IOException;

/**
 * Wraps a few common {@link File} methods to provide a few higher level functions and allow for
 * mocking uncommon error cases.
 */
interface FileSystem {

  default boolean delete(File file) {
    return file.delete();
  }

  default boolean exists(File file) {
    return file.exists();
  }

  default boolean createNewFile(File file) throws IOException {
    return file.createNewFile();
  }

  default boolean rename(File from, File to) {
    return from.renameTo(to);
  }

  default long length(File file) {
    return file.length();
  }

  default long getDirectorySize(File file) {
    long size = 0;
    if (file.isDirectory()) {
      for (File f : file.listFiles()) {
        size += getDirectorySize(f);
      }
    } else {
      size = file.length();
    }
    return size;
  }

  default boolean deleteAll(File file) {
    boolean result = true;
    if (file.isDirectory()) {
      for (File f : file.listFiles()) {
        result = deleteAll(f) && result;
      }
    } else {
      result = file.delete();
    }
    return result;
  }

  default boolean setLastModified(File file, long newLastModifiedTime) {
    return file.setLastModified(newLastModifiedTime);
  }
}
