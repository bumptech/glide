package com.bumptech.glide.integration.okhttp;

import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;
import com.squareup.okhttp.OkHttpClient;
import java.io.InputStream;

/**
 * A simple model loader for fetching media over http/https using OkHttp.
 *
 * @deprecated replaced with com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader.
 */
@Deprecated
public class OkHttpUrlLoader implements ModelLoader<GlideUrl, InputStream> {

  private final OkHttpClient client;

  public OkHttpUrlLoader(OkHttpClient client) {
    this.client = client;
  }

  @Override
  public boolean handles(GlideUrl url) {
    return true;
  }

  @Override
  public LoadData<InputStream> buildLoadData(GlideUrl model, int width, int height,
      Options options) {
    return new LoadData<>(model, new OkHttpStreamFetcher(client, model));
  }

  /**
   * The default factory for {@link OkHttpUrlLoader}s.
   */
  public static class Factory implements ModelLoaderFactory<GlideUrl, InputStream> {
    private static volatile OkHttpClient internalClient;
    private OkHttpClient client;

    private static OkHttpClient getInternalClient() {
      if (internalClient == null) {
        synchronized (Factory.class) {
          if (internalClient == null) {
            internalClient = new OkHttpClient();
          }
        }
      }
      return internalClient;
    }

    /**
     * Constructor for a new Factory that runs requests using a static singleton client.
     */
    public Factory() {
      this(getInternalClient());
    }

    /**
     * Constructor for a new Factory that runs requests using given client.
     */
    public Factory(OkHttpClient client) {
      this.client = client;
    }

    @Override
    public ModelLoader<GlideUrl, InputStream> build(MultiModelLoaderFactory multiFactory) {
      return new OkHttpUrlLoader(client);
    }

    @Override
    public void teardown() {
      // Do nothing, this instance doesn't own the client.
    }
  }
}
