package com.bumptech.glide.load.resource.bytes;

import android.util.Log;
import androidx.annotation.NonNull;
import com.bumptech.glide.load.EncodeStrategy;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.util.ByteBufferUtil;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Writes {@link SourceBytes} to a {@link File} unchanged.
 *
 * <p>The bytes are the original data, so they are written with {@link EncodeStrategy#SOURCE}. This
 * lets disk cache strategies that cache resources, such as {@link
 * com.bumptech.glide.load.engine.DiskCacheStrategy#ALL}, store them like any other source data.
 */
public final class SourceBytesEncoder implements ResourceEncoder<SourceBytes> {
  private static final String TAG = "SourceBytesEncoder";

  @NonNull
  @Override
  public EncodeStrategy getEncodeStrategy(@NonNull Options options) {
    return EncodeStrategy.SOURCE;
  }

  @Override
  public boolean encode(
      @NonNull Resource<SourceBytes> data, @NonNull File file, @NonNull Options options) {
    try {
      ByteBufferUtil.toFile(ByteBuffer.wrap(data.get().getBytes()), file);
      return true;
    } catch (IOException e) {
      if (Log.isLoggable(TAG, Log.WARN)) {
        Log.w(TAG, "Failed to write source bytes", e);
      }
      return false;
    }
  }
}
