package com.bumptech.glide.load.data;

import static android.system.OsConstants.SEEK_SET;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.system.ErrnoException;
import android.system.Os;
import androidx.annotation.NonNull;
import java.io.IOException;

/**
 * Implementation for {@link ParcelFileDescriptor}s that rewinds file descriptors by seeking to 0.
 */
public final class ParcelFileDescriptorRewinder implements DataRewinder<ParcelFileDescriptor> {

  private final ParcelFileDescriptor parcelFileDescriptor;

  public static boolean isSupported() {
    // Os.lseek() is only supported on API 21+.
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
  }

  public ParcelFileDescriptorRewinder(ParcelFileDescriptor parcelFileDescriptor) {
    if (!isSupported()) {
      throw new UnsupportedOperationException("This class should only be instantiated on L+");
    }
    this.parcelFileDescriptor = parcelFileDescriptor;
  }

  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  @NonNull
  @Override
  public ParcelFileDescriptor rewindAndGet() throws IOException {
    try {
      // This will probably fail for pipes, but in that case Glide loading will fall back to
      // InputStreams, which should handle most cases.
      Os.lseek(parcelFileDescriptor.getFileDescriptor(), 0, SEEK_SET);
    } catch (ErrnoException e) {
      throw new IOException("Unable to rewind", e);
    }

    return parcelFileDescriptor;
  }

  @Override
  public void cleanup() {
    // Do nothing.
  }

  /**
   * Factory for producing {@link ParcelFileDescriptorRewinder}s from {@link ParcelFileDescriptor}s.
   */
  public static final class Factory implements DataRewinder.Factory<ParcelFileDescriptor> {

    @NonNull
    @Override
    public DataRewinder<ParcelFileDescriptor> build(
        @NonNull ParcelFileDescriptor parcelFileDescriptor) {
      return new ParcelFileDescriptorRewinder(parcelFileDescriptor);
    }

    @NonNull
    @Override
    public Class<ParcelFileDescriptor> getDataClass() {
      return ParcelFileDescriptor.class;
    }
  }
}
