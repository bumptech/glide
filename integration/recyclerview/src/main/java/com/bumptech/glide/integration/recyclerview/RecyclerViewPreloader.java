package com.bumptech.glide.integration.recyclerview;

import android.app.Activity;
import android.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.ListPreloader;
import com.bumptech.glide.ListPreloader.PreloadModelProvider;
import com.bumptech.glide.ListPreloader.PreloadSizeProvider;
import com.bumptech.glide.RequestManager;

/**
 * Loads a few resources ahead in the direction of scrolling in any {@link RecyclerView} so that
 * images are in the memory cache just before the corresponding view in created in the list. Gives
 * the appearance of an infinitely large image cache, depending on scrolling speed, cpu speed, and
 * cache size.
 *
 * <p> Must be added as a listener to the {@link RecyclerView} using
 * {@link RecyclerView#addOnScrollListener(RecyclerView.OnScrollListener)}, or have its
 * corresponding methods called from another
 * {@link android.support.v7.widget.RecyclerView.OnScrollListener} to function. </p>
 *
 * <p> This class only works with {@link android.support.v7.widget.LinearLayoutManager} and
 * subclasses of {@link android.support.v7.widget.LinearLayoutManager}. </p>
 *
 * @param <T> The type of the model being displayed in the {@link RecyclerView}.
 */
@SuppressWarnings("unused")
public final class RecyclerViewPreloader<T> extends RecyclerView.OnScrollListener {

  private final RecyclerToListViewScrollListener recyclerScrollListener;

  /**
   * Helper constructor that accepts an {@link Activity}.
   */
  public RecyclerViewPreloader(Activity activity,
      PreloadModelProvider<T> preloadModelProvider,
      PreloadSizeProvider<T> preloadDimensionProvider, int maxPreload) {
    this(Glide.with(activity), preloadModelProvider, preloadDimensionProvider, maxPreload);
  }

  /**
   * Helper constructor that accepts an {@link FragmentActivity}.
   */
  public RecyclerViewPreloader(FragmentActivity fragmentActivity,
      PreloadModelProvider<T> preloadModelProvider, PreloadSizeProvider<T> preloadDimensionProvider,
      int maxPreload) {
    this(Glide.with(fragmentActivity), preloadModelProvider, preloadDimensionProvider, maxPreload);
  }

  /**
   * Helper constructor that accepts an {@link Fragment}.
   */
  public RecyclerViewPreloader(Fragment fragment,
      PreloadModelProvider<T> preloadModelProvider, PreloadSizeProvider<T> preloadDimensionProvider,
      int maxPreload) {
    this(Glide.with(fragment), preloadModelProvider, preloadDimensionProvider, maxPreload);
  }

  /**
   * Helper constructor that accepts an {@link android.support.v4.app.Fragment}.
   */
  public RecyclerViewPreloader(android.support.v4.app.Fragment fragment,
      PreloadModelProvider<T> preloadModelProvider, PreloadSizeProvider<T> preloadDimensionProvider,
      int maxPreload) {
    this(Glide.with(fragment), preloadModelProvider, preloadDimensionProvider, maxPreload);
  }
  /**
   * Constructor that accepts interfaces for providing the dimensions of images to preload, the list
   * of models to preload for a given position, and the request to use to load images.
   *
   * @param preloadModelProvider     Provides models to load and requests capable of loading them.
   * @param preloadDimensionProvider Provides the dimensions of images to load.
   * @param maxPreload               Maximum number of items to preload.
   */
  public RecyclerViewPreloader(RequestManager requestManager,
      PreloadModelProvider<T> preloadModelProvider,
      PreloadSizeProvider<T> preloadDimensionProvider, int maxPreload) {

    ListPreloader<T> listPreloader = new ListPreloader<>(requestManager, preloadModelProvider,
        preloadDimensionProvider, maxPreload);
    recyclerScrollListener = new RecyclerToListViewScrollListener(listPreloader);
  }

  @Override
  public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
    recyclerScrollListener.onScrolled(recyclerView, dx, dy);
  }
}
