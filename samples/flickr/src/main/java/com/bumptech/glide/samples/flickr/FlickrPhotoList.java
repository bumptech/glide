package com.bumptech.glide.samples.flickr;

import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.ListPreloader;
import com.bumptech.glide.RequestBuilder;
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
  private GlideRequest<Drawable> fullRequest;
  private GlideRequest<Drawable> thumbRequest;
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
  public View onCreateView(
      @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    final View result = inflater.inflate(R.layout.flickr_photo_list, container, false);

    list = result.findViewById(R.id.flickr_photo_list);
    layoutManager = new LinearLayoutManager(getActivity());
    list.setLayoutManager(layoutManager);
    adapter = new FlickrPhotoListAdapter();
    list.setAdapter(adapter);

    preloadSizeProvider = new ViewPreloadSizeProvider<>();
    RecyclerViewPreloader<Photo> preloader =
        new RecyclerViewPreloader<>(
            GlideApp.with(this), adapter, preloadSizeProvider, PRELOAD_AHEAD_ITEMS);
    list.addOnScrollListener(preloader);
    list.setItemViewCacheSize(0);

    if (currentPhotos != null) {
      adapter.setPhotos(currentPhotos);
    }

    final GlideRequests glideRequests = GlideApp.with(this);
    fullRequest =
        glideRequests.asDrawable().centerCrop().placeholder(new ColorDrawable(Color.GRAY));

    thumbRequest =
        glideRequests
            .asDrawable()
            .diskCacheStrategy(DiskCacheStrategy.DATA)
            .override(Api.SQUARE_THUMB_SIZE)
            .transition(withCrossFade());

    list.setRecyclerListener(
        new RecyclerView.RecyclerListener() {
          @Override
          public void onViewRecycled(RecyclerView.ViewHolder holder) {
            PhotoTitleViewHolder vh = (PhotoTitleViewHolder) holder;
            glideRequests.clear(vh.imageView);
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
  public void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState);
    if (list != null) {
      int index = layoutManager.findFirstVisibleItemPosition();
      View topView = list.getChildAt(0);
      int offset = topView != null ? topView.getTop() : 0;
      outState.putInt(STATE_POSITION_INDEX, index);
      outState.putInt(STATE_POSITION_OFFSET, offset);
    }
  }

  private final class FlickrPhotoListAdapter extends RecyclerView.Adapter<PhotoTitleViewHolder>
      implements ListPreloader.PreloadModelProvider<Photo> {
    private final LayoutInflater inflater;
    private List<Photo> photos = Collections.emptyList();

    FlickrPhotoListAdapter() {
      this.inflater = LayoutInflater.from(getActivity());
    }

    void setPhotos(List<Photo> photos) {
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
      fullRequest.load(current).thumbnail(thumbRequest.load(current)).into(holder.imageView);

      holder.imageView.setOnClickListener(
          new View.OnClickListener() {
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

    @NonNull
    @Override
    public List<Photo> getPreloadItems(int position) {
      return photos.subList(position, position + 1);
    }

    @Nullable
    @Override
    public RequestBuilder<Drawable> getPreloadRequestBuilder(@NonNull Photo item) {
      return fullRequest.thumbnail(thumbRequest.load(item)).load(item);
    }
  }

  private static final class PhotoTitleViewHolder extends RecyclerView.ViewHolder {
    private final TextView titleView;
    private final ImageView imageView;

    PhotoTitleViewHolder(View itemView) {
      super(itemView);
      imageView = itemView.findViewById(R.id.photo_view);
      titleView = itemView.findViewById(R.id.title_view);
    }
  }
}
