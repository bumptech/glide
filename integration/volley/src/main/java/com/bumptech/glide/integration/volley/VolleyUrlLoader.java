package com.bumptech.glide.integration.volley;

import android.content.Context;
import androidx.annotation.NonNull;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;
import java.io.InputStream;

/** A simple model loader for fetching media over http/https using Volley. */
public class VolleyUrlLoader implements ModelLoader<GlideUrl, InputStream> {

  private final RequestQueue requestQueue;
  private final VolleyRequestFactory requestFactory;

  // Public API.
  @SuppressWarnings("unused")
  public VolleyUrlLoader(RequestQueue requestQueue) {
    this(requestQueue, VolleyStreamFetcher.DEFAULT_REQUEST_FACTORY);
  }

  // Public API.
  @SuppressWarnings("WeakerAccess")
  public VolleyUrlLoader(RequestQueue requestQueue, VolleyRequestFactory requestFactory) {
    this.requestQueue = requestQueue;
    this.requestFactory = requestFactory;
  }

  @Override
  public boolean handles(@NonNull GlideUrl url) {
    return true;
  }

  @Override
  public LoadData<InputStream> buildLoadData(
      @NonNull GlideUrl url, int width, int height, @NonNull Options options) {
    return new LoadData<>(url, new VolleyStreamFetcher(requestQueue, url, requestFactory));
  }

  /** The default factory for {@link VolleyUrlLoader}s. */
  // Public API.
  @SuppressWarnings("WeakerAccess")
  public static class Factory implements ModelLoaderFactory<GlideUrl, InputStream> {
    private static volatile RequestQueue internalQueue;

    private final VolleyRequestFactory requestFactory;
    private final RequestQueue requestQueue;

    /** Constructor for a new Factory that runs requests using a static singleton request queue. */
    public Factory(Context context) {
      this(getInternalQueue(context));
    }

    /** Constructor for a new Factory that runs requests using the given {@link RequestQueue}. */
    public Factory(RequestQueue requestQueue) {
      this(requestQueue, VolleyStreamFetcher.DEFAULT_REQUEST_FACTORY);
    }

    /**
     * Constructor for a new Factory with a custom Volley request factory that runs requests using
     * the given {@link RequestQueue}.
     */
    public Factory(RequestQueue requestQueue, VolleyRequestFactory requestFactory) {
      this.requestFactory = requestFactory;
      this.requestQueue = requestQueue;
    }

    @NonNull
    @Override
    public ModelLoader<GlideUrl, InputStream> build(MultiModelLoaderFactory factory) {
      return new VolleyUrlLoader(requestQueue, requestFactory);
    }

    @Override
    public void teardown() {
      // Do nothing.
    }

    private static RequestQueue getInternalQueue(Context context) {
      if (internalQueue == null) {
        synchronized (Factory.class) {
          if (internalQueue == null) {
            internalQueue = Volley.newRequestQueue(context);
          }
        }
      }
      return internalQueue;
    }
  }
}
