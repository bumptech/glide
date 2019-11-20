package com.bumptech.glide.load.data;

import static android.system.OsConstants.SEEK_SET;

import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.system.ErrnoException;
import android.system.Os;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import java.io.IOException;

/**
 * Implementation for {@link ParcelFileDescriptor}s that rewinds file descriptors by seeking to 0.
 */
public final class ParcelFileDescriptorRewinder implements DataRewinder<ParcelFileDescriptor> {

  private final InternalRewinder rewinder;

  public static boolean isSupported() {
    // Os.lseek() is only supported on API 21+.
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
  }

  @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
  public ParcelFileDescriptorRewinder(ParcelFileDescriptor parcelFileDescriptor) {
    rewinder = new InternalRewinder(parcelFileDescriptor);
  }

  @NonNull
  @Override
  @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
  public ParcelFileDescriptor rewindAndGet() throws IOException {
    return rewinder.rewind();
  }

  @Override
  public void cleanup() {
    // Do nothing.
  }

  /**
   * Factory for producing {@link ParcelFileDescriptorRewinder}s from {@link ParcelFileDescriptor}s.
   */
  @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
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

  /**
   * Catching ErrnoException cannot be done in classes that are loaded on APIs < Lollipop. To make
   * sure that we do not do so, we catch inside this inner class instead of the outer class. The
   * only reason this class exists is to avoid VerifyError on older APIs.
   */
  @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
  private static final class InternalRewinder {
    private final ParcelFileDescriptor parcelFileDescriptor;

    InternalRewinder(ParcelFileDescriptor parcelFileDescriptor) {
      this.parcelFileDescriptor = parcelFileDescriptor;
    }

    ParcelFileDescriptor rewind() throws IOException {
      try {
        Os.lseek(parcelFileDescriptor.getFileDescriptor(), 0, SEEK_SET);
      } catch (ErrnoException e) {
        throw new IOException(e);
      }
      return parcelFileDescriptor;
    }
  }
}
