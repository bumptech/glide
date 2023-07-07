package com.bumptech.glide.samples.flickr;

import android.content.Intent;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.ListPreloader;
import com.bumptech.glide.Priority;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader;
import com.bumptech.glide.samples.flickr.api.Api;
import com.bumptech.glide.samples.flickr.api.Photo;
import com.bumptech.glide.util.FixedPreloadSizeProvider;
import com.bumptech.glide.util.Preconditions;
import java.util.Collections;
import java.util.List;

/**
 * A fragment that shows square image thumbnails whose size is determined by the fragment's
 * arguments in a grid pattern.
 */
public class FlickrPhotoGrid extends Fragment implements PhotoViewer {
  private static final String STATE_POSITION_INDEX = "state_position_index";

  private static final String IMAGE_SIZE_KEY = "image_size";
  private static final String PRELOAD_KEY = "preload";
  private static final String THUMBNAIL_KEY = "thumbnail";

  private PhotoAdapter adapter;
  private List<Photo> currentPhotos;
  private int photoSize;
  private RecyclerView grid;
  private boolean thumbnail;
  private GlideRequest<Drawable> fullRequest;
  private GlideRequest<Drawable> thumbnailRequest;
  private GlideRequest<Drawable> preloadRequest;
  private GridLayoutManager layoutManager;

  public static FlickrPhotoGrid newInstance(int size, int preloadCount, boolean thumbnail) {
    FlickrPhotoGrid photoGrid = new FlickrPhotoGrid();
    Bundle args = new Bundle();
    args.putInt(IMAGE_SIZE_KEY, size);
    args.putInt(PRELOAD_KEY, preloadCount);
    args.putBoolean(THUMBNAIL_KEY, thumbnail);
    photoGrid.setArguments(args);
    return photoGrid;
  }

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    Bundle args = Preconditions.checkNotNull(getArguments());
    photoSize = args.getInt(IMAGE_SIZE_KEY);
    thumbnail = args.getBoolean(THUMBNAIL_KEY);

    fullRequest = GlideApp.with(this).asDrawable().centerCrop();

    thumbnailRequest =
        GlideApp.with(this).asDrawable().centerCrop().override(Api.SQUARE_THUMB_SIZE);

    preloadRequest = thumbnail ? thumbnailRequest.clone().priority(Priority.HIGH) : fullRequest;

    final View result = inflater.inflate(R.layout.flickr_photo_grid, container, false);

    final int gridMargin = getResources().getDimensionPixelOffset(R.dimen.grid_margin);
    int spanCount = getResources().getDisplayMetrics().widthPixels / (photoSize + (2 * gridMargin));
    grid = result.findViewById(R.id.flickr_photo_grid);
    layoutManager = new GridLayoutManager(getActivity(), spanCount);
    grid.setLayoutManager(layoutManager);

    grid.addItemDecoration(
        new RecyclerView.ItemDecoration() {
          @Override
          public void getItemOffsets(
              Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            outRect.set(gridMargin, gridMargin, gridMargin, gridMargin);
          }
        });
    grid.addRecyclerListener(
        new RecyclerView.RecyclerListener() {
          @Override
          public void onViewRecycled(RecyclerView.ViewHolder holder) {
            PhotoViewHolder photoViewHolder = (PhotoViewHolder) holder;
            GlideApp.with(FlickrPhotoGrid.this).clear(photoViewHolder.imageView);
          }
        });

    int heightCount = getResources().getDisplayMetrics().heightPixels / photoSize;
    grid.getRecycledViewPool().setMaxRecycledViews(0, spanCount * heightCount * 2);
    grid.setItemViewCacheSize(0);
    adapter = new PhotoAdapter();
    grid.setAdapter(adapter);

    FixedPreloadSizeProvider<Photo> preloadSizeProvider =
        new FixedPreloadSizeProvider<>(photoSize, photoSize);
    RecyclerViewPreloader<Photo> preloader =
        new RecyclerViewPreloader<>(
            Glide.with(this), adapter, preloadSizeProvider, args.getInt(PRELOAD_KEY));
    grid.addOnScrollListener(preloader);

    if (currentPhotos != null) {
      adapter.setPhotos(currentPhotos);
    }

    if (savedInstanceState != null) {
      int index = savedInstanceState.getInt(STATE_POSITION_INDEX);
      grid.scrollToPosition(index);
    }

    return result;
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState);
    if (grid != null) {
      int index = layoutManager.findFirstVisibleItemPosition();
      outState.putInt(STATE_POSITION_INDEX, index);
    }
  }

  @Override
  public void onPhotosUpdated(List<Photo> photos) {
    currentPhotos = photos;
    if (adapter != null) {
      adapter.setPhotos(currentPhotos);
    }
  }

  private class PhotoAdapter extends RecyclerView.Adapter<PhotoViewHolder>
      implements ListPreloader.PreloadModelProvider<Photo> {
    private final LayoutInflater inflater;
    private List<Photo> photos = Collections.emptyList();

    PhotoAdapter() {
      this.inflater = LayoutInflater.from(getActivity());
    }

    void setPhotos(List<Photo> photos) {
      this.photos = photos;
      notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
      return 0;
    }

    @Override
    public PhotoViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
      View view = inflater.inflate(R.layout.flickr_photo_grid_item, parent, false);
      ViewGroup.LayoutParams params = view.getLayoutParams();
      params.width = photoSize;
      params.height = photoSize;
      return new PhotoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(PhotoViewHolder holder, int position) {
      final Photo current = photos.get(position);

      fullRequest
          .load(current)
          .thumbnail(thumbnail ? thumbnailRequest.load(current) : null)
          .into(holder.imageView);

      holder.imageView.setOnClickListener(
          new View.OnClickListener() {
            @Override
            public void onClick(View view) {
              Intent intent = FullscreenActivity.getIntent(getActivity(), current);
              startActivity(intent);
            }
          });
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
      return preloadRequest.load(item);
    }
  }

  private static final class PhotoViewHolder extends RecyclerView.ViewHolder {
    private final ImageView imageView;

    PhotoViewHolder(View itemView) {
      super(itemView);
      imageView = (ImageView) itemView;
    }
  }
}
