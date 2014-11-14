package com.bumptech.glide.samples.flickr;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.bumptech.glide.DrawableRequestBuilder;
import com.bumptech.glide.GenericRequestBuilder;
import com.bumptech.glide.Glide;
import com.bumptech.glide.ListPreloader;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.samples.flickr.api.Api;
import com.bumptech.glide.samples.flickr.api.Photo;

import java.util.ArrayList;
import java.util.List;

/**
 * A fragment that shows cropped image thumbnails half the width of the screen in a scrolling list.
 */
public class FlickrPhotoList extends Fragment implements PhotoViewer {
    private static final String STATE_POSITION_INDEX = "state_position_index";
    private static final String STATE_POSITION_OFFSET = "state_position_offset";
    private FlickrPhotoListAdapter adapter;
    private List<Photo> currentPhotos;
    private ListPreloader<Photo> preloader;
    private ListView list;
    private DrawableRequestBuilder<Photo> fullRequest;
    private DrawableRequestBuilder<Photo> thumbRequest;

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View result = inflater.inflate(R.layout.flickr_photo_list, container, false);
        list = (ListView) result.findViewById(R.id.flickr_photo_list);
        adapter = new FlickrPhotoListAdapter();
        list.setAdapter(adapter);
        preloader = new ListPreloader<Photo>(adapter, 5);
        list.setOnScrollListener(preloader);
        if (currentPhotos != null) {
            adapter.setPhotos(currentPhotos);
        }

        fullRequest = Glide.with(FlickrPhotoList.this)
                .from(Photo.class)
                .placeholder(new ColorDrawable(Color.GRAY))
                .centerCrop();

        thumbRequest = Glide.with(FlickrPhotoList.this)
                .from(Photo.class)
                .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                .override(Api.SQUARE_THUMB_SIZE, Api.SQUARE_THUMB_SIZE);

        if (savedInstanceState != null) {
            int index = savedInstanceState.getInt(STATE_POSITION_INDEX);
            int offset = savedInstanceState.getInt(STATE_POSITION_OFFSET);
            list.setSelectionFromTop(index, offset);
        }

        return result;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (list != null) {
            int index = list.getFirstVisiblePosition();
            View topView = list.getChildAt(0);
            int offset = topView != null ? topView.getTop() : 0;
            outState.putInt(STATE_POSITION_INDEX, index);
            outState.putInt(STATE_POSITION_OFFSET, offset);
        }
    }

    private static class ViewHolder {
        private final TextView titleText;
        private final ImageView imageView;

        public ViewHolder(ImageView imageView, TextView titleText) {
            this.imageView = imageView;
            this.titleText = titleText;
        }
    }

    private class FlickrPhotoListAdapter extends BaseAdapter implements ListPreloader.PreloadModelProvider<Photo> {
        private int[] photoDimens = null;
        private final LayoutInflater inflater;
        private List<Photo> photos = new ArrayList<Photo>(0);

        public FlickrPhotoListAdapter() {
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
            return null;
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(int position, View view, ViewGroup container) {
            final Photo current = photos.get(position);
            final ViewHolder viewHolder;
            if (view == null) {
                view = inflater.inflate(R.layout.flickr_photo_list_item, container, false);
                final ImageView imageView = (ImageView) view.findViewById(R.id.photo_view);
                TextView titleView = (TextView) view.findViewById(R.id.title_view);
                viewHolder = new ViewHolder(imageView, titleView);
                view.setTag(viewHolder);
                if (photoDimens == null) {
                    photoDimens = new int[]{imageView.getWidth(), imageView.getHeight()};
                }
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            fullRequest
                    .thumbnail(thumbRequest.load(current))
                    .load(current)
                    .into(viewHolder.imageView);

            viewHolder.imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = FullscreenActivity.getIntent(getActivity(), current);
                    startActivity(intent);
                }
            });

            viewHolder.titleText.setText(current.getTitle());
            return view;
        }

        @Override
        public List<Photo> getPreloadItems(int start, int end) {
            return photos.subList(start, end);
        }

        @Override
        public GenericRequestBuilder getPreloadRequestBuilder(Photo item, int position) {
            return fullRequest
                    .thumbnail(thumbRequest.load(item))
                    .load(item);
        }

        @Override
        public int[] getPreloadDimensions(Photo item, int position) {
            return photoDimens;
        }
    }
}
