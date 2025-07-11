package com.bumptech.glide.load.data;

import android.content.ContentResolver;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import androidx.annotation.NonNull;
import java.io.FileNotFoundException;
import java.io.IOException;

/** Fetches an {@link android.os.ParcelFileDescriptor} for a local {@link android.net.Uri}. */
public class FileDescriptorLocalUriFetcher extends LocalUriFetcher<ParcelFileDescriptor> {
  public FileDescriptorLocalUriFetcher(
      ContentResolver contentResolver, Uri uri, boolean useMediaStoreApisIfAvailable) {
    super(contentResolver, uri, useMediaStoreApisIfAvailable);
  }

  @Override
  protected ParcelFileDescriptor loadResource(Uri uri, ContentResolver contentResolver)
      throws FileNotFoundException {
    AssetFileDescriptor assetFileDescriptor = openAssetFileDescriptor(uri);
    if (assetFileDescriptor == null) {
      throw new FileNotFoundException("FileDescriptor is null for: " + uri);
    }
    return assetFileDescriptor.getParcelFileDescriptor();
  }

  @Override
  protected void close(ParcelFileDescriptor data) throws IOException {
    data.close();
  }

  @NonNull
  @Override
  public Class<ParcelFileDescriptor> getDataClass() {
    return ParcelFileDescriptor.class;
  }
}
