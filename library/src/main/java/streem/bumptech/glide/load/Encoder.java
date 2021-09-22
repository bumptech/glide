package com.bumptech.glide.load;

import androidx.annotation.NonNull;
import java.io.File;

/**
 * An interface for writing data to some persistent data store (i.e. a local File cache).
 *
 * @param <T> The type of the data that will be written.
 */
public interface Encoder<T> {
  /**
   * Writes the given data to the given output stream and returns True if the write completed
   * successfully and should be committed.
   *
   * @param data The data to write.
   * @param file The file to write the data to.
   * @param options The set of options to apply when encoding.
   */
  boolean encode(@NonNull T data, @NonNull File file, @NonNull Options options);
}
