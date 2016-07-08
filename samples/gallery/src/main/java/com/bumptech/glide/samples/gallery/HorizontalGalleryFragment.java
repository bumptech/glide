package com.bumptech.glide.samples.gallery;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader;
import java.util.List;

/**
 * Displays media store data in a recycler view.
 */
public class HorizontalGalleryFragment extends Fragment
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
    RecyclerViewPreloader<MediaStoreData> preloader =
        new RecyclerViewPreloader<>(requestManager, adapter, adapter, 3);
    recyclerView.addOnScrollListener(preloader);
    recyclerView.setAdapter(adapter);
  }

  @Override
  public void onLoaderReset(Loader<List<MediaStoreData>> loader) {
    // Do nothing.
  }
}
