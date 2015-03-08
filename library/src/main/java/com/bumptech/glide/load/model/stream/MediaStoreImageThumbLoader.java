package com.bumptech.glide.load.model.stream;

import android.content.Context;
import android.net.Uri;

import com.bumptech.glide.load.data.mediastore.MediaStoreUtil;
import com.bumptech.glide.load.data.mediastore.ThumbFetcher;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;
import com.bumptech.glide.signature.ObjectKey;

import java.io.InputStream;
import java.util.Map;

/**
 * Loads {@link InputStream}s from media store image {@link Uri}s that point to pre-generated
 * thumbnails for those {@link Uri}s in the media store.
 */
public class MediaStoreImageThumbLoader implements ModelLoader<Uri, InputStream> {
  public final Context context;

  public MediaStoreImageThumbLoader(Context context) {
    this.context = context.getApplicationContext();
  }

  @Override
  public LoadData<InputStream> buildLoadData(Uri model, int width, int height,
      Map<String, Object> options) {
    if (MediaStoreUtil.isThumbnailSize(width, height)) {
      return new LoadData<>(new ObjectKey(model), ThumbFetcher.buildImageFetcher(context, model));
    } else {
      return null;
    }
  }

  @Override
  public boolean handles(Uri model) {
    return MediaStoreUtil.isMediaStoreImageUri(model);
  }

  /**
   * Factory that loads {@link InputStream}s from media store image {@link Uri}s.
   */
  public static class Factory implements ModelLoaderFactory<Uri, InputStream> {

    @Override
    public ModelLoader<Uri, InputStream> build(Context context,
        MultiModelLoaderFactory multiFactory) {
      return new MediaStoreImageThumbLoader(context);
    }

    @Override
    public void teardown() {
      // Do nothing.
    }
  }
}
