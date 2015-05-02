package com.bumptech.glide.samples.gallery;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.ListPreloader;
import com.bumptech.glide.RequestManager;

import java.util.List;

/**
 * Displays media store data in a recycler view.
 */
public class RecyclerViewFragment extends Fragment
    implements LoaderManager.LoaderCallbacks<List<MediaStoreData>> {

  private RecyclerView recyclerView;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    getLoaderManager().initLoader(R.id.loader_id_media_store_data, null, this);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    View result = inflater.inflate(R.layout.recycler_view, container, false);
    recyclerView = (RecyclerView) result.findViewById(R.id.recycler_view);
    GridLayoutManager layoutManager = new GridLayoutManager(getActivity(), 1);
    layoutManager.setOrientation(RecyclerView.HORIZONTAL);
    recyclerView.setLayoutManager(layoutManager);
    recyclerView.setHasFixedSize(true);

    return result;
  }

  @Override
  public Loader<List<MediaStoreData>> onCreateLoader(int i, Bundle bundle) {
    return new MediaStoreDataLoader(getActivity());
  }

  @Override
  public void onLoadFinished(Loader<List<MediaStoreData>> loader,
      List<MediaStoreData> mediaStoreData) {
    RequestManager requestManager = Glide.with(this);
    RecyclerAdapter adapter =
        new RecyclerAdapter(getActivity(), mediaStoreData, requestManager);
    ListPreloader<MediaStoreData> preloader =
        new ListPreloader<>(requestManager, adapter, adapter, 3);
    RecyclerViewPreloaderListener recyclerViewPreloaderListener =
        new RecyclerViewPreloaderListener(preloader);
    recyclerView.setOnScrollListener(recyclerViewPreloaderListener);
    recyclerView.setAdapter(adapter);
  }

  @Override
  public void onLoaderReset(Loader<List<MediaStoreData>> loader) {
    // Do nothing.
  }

  private static class RecyclerViewPreloaderListener extends RecyclerView.OnScrollListener {
    private final AbsListView.OnScrollListener scrollListener;
    private int lastFirstVisible = -1;
    private int lastVisibleCount = -1;
    private int lastItemCount = -1;

    public RecyclerViewPreloaderListener(AbsListView.OnScrollListener scrollListener) {
      this.scrollListener = scrollListener;
    }

    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
      // Adapter the recycler view scroll listener interface to match ListView's.
      LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
      int firstVisible = layoutManager.findFirstVisibleItemPosition();
      int visibleCount = Math.abs(firstVisible - layoutManager.findLastVisibleItemPosition());
      int itemCount = recyclerView.getAdapter().getItemCount();
      if (firstVisible != lastFirstVisible || visibleCount != lastVisibleCount
          || itemCount != lastItemCount) {
        scrollListener.onScroll(null, firstVisible, visibleCount, itemCount);
        lastFirstVisible = firstVisible;
        lastFirstVisible = visibleCount;
        lastItemCount = itemCount;
      }
    }
  }
}
