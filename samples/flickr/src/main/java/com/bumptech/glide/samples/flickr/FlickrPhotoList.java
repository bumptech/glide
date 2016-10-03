package com.bumptech.glide.samples.flickr;

import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;
import static com.bumptech.glide.request.RequestOptions.centerCropTransform;
import static com.bumptech.glide.request.RequestOptions.diskCacheStrategyOf;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.ListPreloader;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.samples.flickr.api.Api;
import com.bumptech.glide.samples.flickr.api.Photo;
import com.bumptech.glide.util.ViewPreloadSizeProvider;
import java.util.Collections;
import java.util.List;

/**
 * A fragment that shows cropped image thumbnails half the width of the screen in a scrolling list.
 */
public class FlickrPhotoList extends Fragment implements PhotoViewer {
  private static final int PRELOAD_AHEAD_ITEMS = 5;
  private static final String STATE_POSITION_INDEX = "state_position_index";
  private static final String STATE_POSITION_OFFSET = "state_position_offset";
  private FlickrPhotoListAdapter adapter;
  private List<Photo> currentPhotos;
  private RecyclerView list;
  private RequestBuilder<Drawable> fullRequest;
  private RequestBuilder<Drawable> thumbRequest;
  private ViewPreloadSizeProvider<Photo> preloadSizeProvider;
  private LinearLayoutManager layoutManager;

  public static FlickrPhotoList newInstance() {
    return new FlickrPhotoList();
  }

  @Override
  public void onPhotosUpdated(List<Photo> photos) {
    currentPhotos = photos;
    if (adapter != null) {
      adapter.setPhotos(currentPhotos);
    }
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    final View result = inflater.inflate(R.layout.flickr_photo_list, container, false);

    list = (RecyclerView) result.findViewById(R.id.flickr_photo_list);
    layoutManager = new LinearLayoutManager(getActivity());
    list.setLayoutManager(layoutManager);
    adapter = new FlickrPhotoListAdapter();
    list.setAdapter(adapter);

    preloadSizeProvider = new ViewPreloadSizeProvider<>();
    RecyclerViewPreloader<Photo> preloader = new RecyclerViewPreloader<>(Glide.with(this), adapter,
        preloadSizeProvider, PRELOAD_AHEAD_ITEMS);
    list.addOnScrollListener(preloader);
    list.setItemViewCacheSize(0);

    if (currentPhotos != null) {
      adapter.setPhotos(currentPhotos);
    }

    final RequestManager requestManager = Glide.with(this);
    fullRequest = requestManager
        .asDrawable()
        .apply(centerCropTransform(getActivity())
            .placeholder(new ColorDrawable(Color.GRAY)));

    thumbRequest = requestManager
        .asDrawable()
        .apply(diskCacheStrategyOf(DiskCacheStrategy.DATA)
            .override(Api.SQUARE_THUMB_SIZE))
        .transition(withCrossFade());

    list.setRecyclerListener(new RecyclerView.RecyclerListener() {
      @Override
      public void onViewRecycled(RecyclerView.ViewHolder holder) {
        PhotoTitleViewHolder vh = (PhotoTitleViewHolder) holder;
        requestManager.clear(vh.imageView);
      }
    });

    if (savedInstanceState != null) {
      int index = savedInstanceState.getInt(STATE_POSITION_INDEX);
      int offset = savedInstanceState.getInt(STATE_POSITION_OFFSET);
      layoutManager.scrollToPositionWithOffset(index, offset);
    }

    return result;
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    if (list != null) {
      int index = layoutManager.findFirstVisibleItemPosition();
      View topView = list.getChildAt(0);
      int offset = topView != null ? topView.getTop() : 0;
      outState.putInt(STATE_POSITION_INDEX, index);
      outState.putInt(STATE_POSITION_OFFSET, offset);
    }
  }

  private class FlickrPhotoListAdapter extends RecyclerView.Adapter<PhotoTitleViewHolder>
      implements ListPreloader.PreloadModelProvider<Photo> {
    private final LayoutInflater inflater;
    private List<Photo> photos = Collections.emptyList();

    public FlickrPhotoListAdapter() {
      this.inflater = LayoutInflater.from(getActivity());
    }

    public void setPhotos(List<Photo> photos) {
      this.photos = photos;
      notifyDataSetChanged();
    }

    @Override
    public PhotoTitleViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
      View view = inflater.inflate(R.layout.flickr_photo_list_item, parent, false);
      PhotoTitleViewHolder vh = new PhotoTitleViewHolder(view);
      preloadSizeProvider.setView(vh.imageView);
      return vh;
    }

    @Override
    public void onBindViewHolder(PhotoTitleViewHolder holder, int position) {
      final Photo current = photos.get(position);
      fullRequest.load(current)
          .thumbnail(thumbRequest.load(current))
          .into(holder.imageView);

      holder.imageView.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
          Intent intent = FullscreenActivity.getIntent(getActivity(), current);
          startActivity(intent);
        }
      });

      holder.titleView.setText(current.getTitle());
    }

    @Override
    public long getItemId(int i) {
      return RecyclerView.NO_ID;
    }

    @Override
    public int getItemCount() {
      return photos.size();
    }

    @Override
    public List<Photo> getPreloadItems(int position) {
      return photos.subList(position, position + 1);
    }

    @Override
    public RequestBuilder<Drawable> getPreloadRequestBuilder(Photo item) {
      return fullRequest.thumbnail(thumbRequest.load(item)).load(item);
    }
  }

  private static class PhotoTitleViewHolder extends RecyclerView.ViewHolder {
    private final TextView titleView;
    private final ImageView imageView;

    public PhotoTitleViewHolder(View itemView) {
      super(itemView);
      imageView = (ImageView) itemView.findViewById(R.id.photo_view);
      titleView = (TextView) itemView.findViewById(R.id.title_view);
    }
  }
}
