package com.bumptech.glide.samples.flickr;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import com.bumptech.glide.DrawableRequestBuilder;
import com.bumptech.glide.GenericRequestBuilder;
import com.bumptech.glide.Glide;
import com.bumptech.glide.ListPreloader;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.samples.flickr.api.Api;
import com.bumptech.glide.samples.flickr.api.Photo;
import com.bumptech.glide.util.FixedPreloadSizeProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * A fragment that shows square image thumbnails whose size is determined by the framgent's arguments in a grid
 * pattern.
 */
public class FlickrPhotoGrid extends Fragment implements PhotoViewer {
    private static final String STATE_POSITION_INDEX = "state_position_index";

    private static final String IMAGE_SIZE_KEY = "image_size";
    private static final String PRELOAD_KEY = "preload";
    private static final String THUMBNAIL_KEY = "thumbnail";

    private PhotoAdapter adapter;
    private List<Photo> currentPhotos;
    private int photoSize;
    private GridView grid;
    private boolean thumbnail;
    private DrawableRequestBuilder<Photo> fullRequest;
    private DrawableRequestBuilder<Photo> thumbnailRequest;
    private DrawableRequestBuilder<Photo> preloadRequest;

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Bundle args = getArguments();
        photoSize = args.getInt(IMAGE_SIZE_KEY);
        thumbnail = args.getBoolean(THUMBNAIL_KEY);

        fullRequest = Glide.with(this)
                .from(Photo.class)
                .centerCrop()
                .crossFade(R.anim.fade_in, 150);

        thumbnailRequest = Glide.with(this)
                .from(Photo.class)
                .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                .crossFade(R.anim.fade_in, 150)
                .override(Api.SQUARE_THUMB_SIZE, Api.SQUARE_THUMB_SIZE);

        preloadRequest = thumbnail ? thumbnailRequest.clone().priority(Priority.HIGH) : fullRequest;

        final View result = inflater.inflate(R.layout.flickr_photo_grid, container, false);

        grid = (GridView) result.findViewById(R.id.images);
        grid.setColumnWidth(photoSize);
        adapter = new PhotoAdapter();
        grid.setAdapter(adapter);

        final FixedPreloadSizeProvider<Photo> preloadSizeProvider =
                new FixedPreloadSizeProvider<Photo>(photoSize, photoSize);
        final ListPreloader<Photo> preloader =
                new ListPreloader<Photo>(adapter, preloadSizeProvider, args.getInt(PRELOAD_KEY));
        grid.setOnScrollListener(preloader);

        if (currentPhotos != null) {
            adapter.setPhotos(currentPhotos);
        }

        if (savedInstanceState != null) {
            int index = savedInstanceState.getInt(STATE_POSITION_INDEX);
            grid.setSelection(index);
        }

        return result;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (grid != null) {
            int index = grid.getFirstVisiblePosition();
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

    private class PhotoAdapter extends BaseAdapter implements ListPreloader.PreloadModelProvider<Photo> {
        private List<Photo> photos = new ArrayList<Photo>(0);
        private final LayoutInflater inflater;

        public PhotoAdapter() {
            this.inflater = LayoutInflater.from(getActivity());
        }

        public void setPhotos(List<Photo> photos) {
            this.photos = photos;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return photos.size();
        }

        @Override
        public Object getItem(int i) {
            return photos.get(i);
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(int position, View view, ViewGroup container) {
            final Photo current = photos.get(position);
            final ImageView imageView;
            if (view == null) {
                imageView = (ImageView) inflater.inflate(R.layout.flickr_photo_grid_item, container, false);
                ViewGroup.LayoutParams params = imageView.getLayoutParams();
                params.width = photoSize;
                params.height = photoSize;
            } else {
                imageView = (ImageView) view;
            }

            fullRequest
                    .load(current)
                    .thumbnail(thumbnail ? thumbnailRequest.load(current) : null)
                    .into(imageView);

            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = FullscreenActivity.getIntent(getActivity(), current);
                    startActivity(intent);
                }
            });

            return imageView;
        }

        @Override
        public List<Photo> getPreloadItems(int position) {
            return photos.subList(position, position + 1);
        }

        @Override
        public GenericRequestBuilder getPreloadRequestBuilder(Photo item) {
            return preloadRequest.load(item);
        }
    }
}
