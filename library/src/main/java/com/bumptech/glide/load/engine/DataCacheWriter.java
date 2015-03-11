package com.bumptech.glide.load.engine;

import android.util.Log;

import com.bumptech.glide.Logs;
import com.bumptech.glide.load.Encoder;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.engine.cache.DiskCache;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Writes original source data or downsampled/transformed resource data to cache using the
 * provided {@link com.bumptech.glide.load.Encoder} or
 * {@link com.bumptech.glide.load.ResourceEncoder} and the given data or
 * {@link com.bumptech.glide.load.engine.Resource}.
 *
 * @param <DataType> The type of data that will be encoded (InputStream, ByteBuffer,
 *                  Resource<Bitmap> etc).
 */
class DataCacheWriter<DataType> implements DiskCache.Writer {
  private static final FileOpener DEFAULT_FILE_OPENER = new FileOpener();

  private final Encoder<DataType> encoder;
  private final DataType data;
  private final FileOpener fileOpener;
  private final Options options;

  DataCacheWriter(Encoder<DataType> encoder, DataType data, Options options) {
    this(encoder, data, options, DEFAULT_FILE_OPENER);
  }

  // Visible for testing.
  DataCacheWriter(Encoder<DataType> encoder, DataType data, Options options,
      FileOpener fileOpener) {
    this.encoder = encoder;
    this.data = data;
    this.options = options;
    this.fileOpener = fileOpener;
  }

  @Override
  public boolean write(File file) {
    boolean success = false;
    OutputStream os = null;
    try {
      os = fileOpener.open(file);
      success = encoder.encode(data, os, options);
    } catch (FileNotFoundException e) {
      if (Logs.isEnabled(Log.DEBUG)) {
        Logs.log(Log.DEBUG, "Failed to find file to write to disk cache", e);
      }
    } finally {
      if (os != null) {
        try {
          os.close();
        } catch (IOException e) {
          // Do nothing.
        }
      }
    }
    if (!success && Logs.isEnabled(Log.DEBUG)) {
      Logs.log(Log.DEBUG, "Failed to write to cache");
    }
    return success;
  }

  // Visible for testing.
  static class FileOpener {
    public OutputStream open(File file) throws FileNotFoundException {
      return new BufferedOutputStream(new FileOutputStream(file));
    }
  }
}
