package com.bumptech.glide.load.data;

import android.content.ContentResolver;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import androidx.annotation.NonNull;
import java.io.FileNotFoundException;
import java.io.IOException;

/** Fetches an {@link AssetFileDescriptor} for a local {@link android.net.Uri}. */
public final class AssetFileDescriptorLocalUriFetcher extends LocalUriFetcher<AssetFileDescriptor> {

  public AssetFileDescriptorLocalUriFetcher(ContentResolver contentResolver, Uri uri) {
    super(contentResolver, uri);
  }

  /**
   * useMediaStoreApisIfAvailable is part of an experiment and the constructor can be removed in a
   * future version.
   */
  public AssetFileDescriptorLocalUriFetcher(
      ContentResolver contentResolver, Uri uri, boolean useMediaStoreApisIfAvailable) {
    super(contentResolver, uri, useMediaStoreApisIfAvailable);
  }

  @Override
  protected AssetFileDescriptor loadResource(Uri uri, ContentResolver contentResolver)
      throws FileNotFoundException {
    AssetFileDescriptor result = openAssetFileDescriptor(uri);
    if (result == null) {
      throw new FileNotFoundException("FileDescriptor is null for: " + uri);
    }
    return result;
  }

  @Override
  protected void close(AssetFileDescriptor data) throws IOException {
    data.close();
  }

  @NonNull
  @Override
  public Class<AssetFileDescriptor> getDataClass() {
    return AssetFileDescriptor.class;
  }
}
