package com.bumptech.glide.load.data;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import androidx.annotation.NonNull;
import java.io.IOException;

/** Fetches an {@link android.content.res.AssetFileDescriptor} for an asset path. */
public class FileDescriptorAssetPathFetcher extends AssetPathFetcher<AssetFileDescriptor> {
  public FileDescriptorAssetPathFetcher(AssetManager assetManager, String assetPath) {
    super(assetManager, assetPath);
  }

  @Override
  protected AssetFileDescriptor loadResource(AssetManager assetManager, String path)
      throws IOException {
    return assetManager.openFd(path);
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
