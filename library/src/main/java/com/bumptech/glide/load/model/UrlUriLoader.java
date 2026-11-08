package com.bumptech.glide.load.model;

import android.net.Uri;
import androidx.annotation.NonNull;
import com.bumptech.glide.load.Options;
import java.io.InputStream;

/**
 * Handles http/https Uris by delegating to the {@link ModelLoader} for {@link
 * com.bumptech.glide.load.model.GlideUrl GlideUrls}.
 *
 * @param <Data> The type of data this Loader will obtain for a {@link Uri}.
 */
public class UrlUriLoader<Data> implements ModelLoader<Uri, Data> {
  private final ModelLoader<GlideUrl, Data> urlLoader;

  // Public API.
  @SuppressWarnings("WeakerAccess")
  public UrlUriLoader(ModelLoader<GlideUrl, Data> urlLoader) {
    this.urlLoader = urlLoader;
  }

  @Override
  public LoadData<Data> buildLoadData(
      @NonNull Uri uri, int width, int height, @NonNull Options options) {
    GlideUrl glideUrl = new GlideUrl(uri.toString());
    return urlLoader.buildLoadData(glideUrl, width, height, options);
  }

  @Override
  public boolean handles(@NonNull Uri uri) {
    // Schemes are case-insensitive, but Uri.getScheme() preserves the original case.
    String scheme = uri.getScheme();
    return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
  }

  /**
   * Loads {@link java.io.InputStream InputStreams} from {@link android.net.Uri Uris} with http or
   * https schemes.
   */
  public static class StreamFactory implements ModelLoaderFactory<Uri, InputStream> {

    @NonNull
    @Override
    public ModelLoader<Uri, InputStream> build(MultiModelLoaderFactory multiFactory) {
      return new UrlUriLoader<>(multiFactory.build(GlideUrl.class, InputStream.class));
    }

    @Override
    public void teardown() {
      // Do nothing.
    }
  }
}
