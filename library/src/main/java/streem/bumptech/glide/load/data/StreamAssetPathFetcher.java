package com.bumptech.glide.load.data;

import android.content.res.AssetManager;
import androidx.annotation.NonNull;
import java.io.IOException;
import java.io.InputStream;

/** Fetches an {@link java.io.InputStream} for an asset path. */
public class StreamAssetPathFetcher extends AssetPathFetcher<InputStream> {
  public StreamAssetPathFetcher(AssetManager assetManager, String assetPath) {
    super(assetManager, assetPath);
  }

  @Override
  protected InputStream loadResource(AssetManager assetManager, String path) throws IOException {
    return assetManager.open(path);
  }

  @Override
  protected void close(InputStream data) throws IOException {
    data.close();
  }

  @NonNull
  @Override
  public Class<InputStream> getDataClass() {
    return InputStream.class;
  }
}
